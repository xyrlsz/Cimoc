package main

import (
	"embed"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"

	"xcimoc-data-server/config"
	"xcimoc-data-server/database"
	"xcimoc-data-server/handlers"
	"xcimoc-data-server/middleware"
	"xcimoc-data-server/query"
	"xcimoc-data-server/utils"

	"github.com/gin-gonic/gin"
)

//go:embed admin/index.html
var adminHTML embed.FS

func main() {
	// 命令行修改管理员密码（支持 --config）
	if hasSetAdminCommand() {
		// 手动提取 --config 参数（如果有）
		configPath := extractFlag("config")

		// 提取密码：密码必须是 "set" 和 "admin" 之后的第一个非 flag 参数
		newPassword := ""
		for i := 0; i < len(os.Args); i++ {
			if strings.EqualFold(os.Args[i], "admin") && i+1 < len(os.Args) {
				// "admin" 之后的参数如果是密码，不能以 - 开头（除非是负数密码，实际不会出现）
				potentialPwd := os.Args[i+1]
				if !strings.HasPrefix(potentialPwd, "-") {
					newPassword = potentialPwd
				}
				break
			}
		}

		if len(newPassword) < 6 {
			log.Fatalf("密码长度至少 6 位")
		}

		cfg := config.LoadWithConfig(configPath)
		database.Init(cfg)

		salt, err := utils.GenerateSalt()
		if err != nil {
			log.Fatalf("生成盐值失败: %v", err)
		}

		// 使用生成的 query.User 定位管理员；
		// WHERE is_admin=true 通过字段 Eq 表达，避免裸字符串列名。
		admin, findErr := query.User.Where(query.User.IsAdmin.Eq(true)).Take()
		if findErr != nil || admin == nil {
			log.Fatalf("未找到管理员账户，请先启动服务器以初始化数据库")
		}
		admin.Password = utils.HashPassword(newPassword, salt)
		admin.Salt = salt
		admin.TokenVersion++
		// Save 走主键更新；TokenVersion 已自增，确保旧 token 失效。
		if saveErr := database.DB.Save(admin).Error; saveErr != nil {
			log.Fatalf("保存管理员账户失败: %v", saveErr)
		}

		fmt.Println("管理员密码已更新")
		os.Exit(0)
	}

	cfg := config.Load()

	// Initialize database
	database.Init(cfg)

	// 启动后台维护任务（事件日志定期清理等）
	go runMaintenance()

	// Initialize handlers
	authHandler := handlers.NewAuthHandler(cfg)
	comicHandler := handlers.NewComicHandler()
	settingHandler := handlers.NewSettingHandler()
	tagHandler := handlers.NewTagHandler()
	eventHandler := handlers.NewEventHandler()

	// Gin 设为 release 模式（也可通过环境变量 GIN_MODE=release）
	gin.SetMode(gin.ReleaseMode)

	// Setup router
	r := gin.New()
	r.Use(gin.Recovery())
	r.Use(gin.Logger())

	// CORS middleware
	r.Use(func(c *gin.Context) {
		// Origin 白名单：CORS_ORIGINS 或 config.yaml 的 cors.origins 有值 → 精确匹配并回显；否则 "*"
		origin := c.GetHeader("Origin")
		allowOrigin := "*"
		if len(cfg.CORSOrigins) > 0 && origin != "" {
			ok := false
			for _, o := range cfg.CORSOrigins {
				if o == origin {
					ok = true
					break
				}
			}
			if ok {
				allowOrigin = origin
			} else {
				// 请求来源不在白名单：返回第一个配置的允许 Origin，浏览器将拒绝跨域访问（安全）
				allowOrigin = cfg.CORSOrigins[0]
			}
		}
		c.Header("Access-Control-Allow-Origin", allowOrigin)
		c.Header("Vary", "Origin")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS, HEAD")
		// 补上常见请求头：X-Requested-With/Accept*/Cache-Control/X-Client-Id/If-None-Match 等
		c.Header("Access-Control-Allow-Headers",
			"Origin, Content-Type, Authorization, Accept, Accept-Language, Accept-Encoding, "+
				"X-Requested-With, X-Client-Id, Cache-Control, Pragma, If-None-Match, If-Modified-Since")
		c.Header("Access-Control-Expose-Headers",
			"Content-Length, X-Request-Id, X-Latest-Event-Id, X-Server-Time")
		c.Header("Access-Control-Max-Age", "86400") // 预检缓存 1 天
		if c.Request.Method == "OPTIONS" {
			c.AbortWithStatus(204)
			return
		}
		c.Next()
	})

	// Health check
	r.GET("/api/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok", "service": "Cimoc Data Sync Server"})
	})

	// Auth routes (no token required)
	auth := r.Group("/api/auth")
	{
		auth.POST("/login", authHandler.Login)
	}

	// Token refresh (接受 7 天内过期的 token)
	refresh := r.Group("/api/auth")
	refresh.Use(middleware.AuthRefresh(cfg))
	{
		refresh.POST("/refresh", authHandler.RefreshToken)
	}

	// Protected routes (token required)
	api := r.Group("/api")
	api.Use(middleware.AuthRequired(cfg))
	{
		// Comics (history + favorites)
		api.GET("/comics", comicHandler.List)
		api.POST("/comics/sync", comicHandler.Sync)
		api.DELETE("/comics/:id", comicHandler.Delete)

		// Sync status (lightweight endpoint for checking sync state)
		api.GET("/sync/status", comicHandler.SyncStatus)

		// Event-based sync (核心同步机制)
		api.GET("/events/pull", eventHandler.PullEvents)
		api.POST("/events/push", eventHandler.PushEvents)
		api.GET("/events/status", eventHandler.EventStatus)

		// Settings
		api.GET("/settings", settingHandler.List)
		api.POST("/settings/sync", settingHandler.Sync)

		// Tags
		api.GET("/tags", tagHandler.List)
		api.POST("/tags/sync", tagHandler.Sync)
	}

	// Admin routes (admin token required)
	admin := r.Group("/api/admin")
	admin.Use(middleware.AdminRequired(cfg))
	{
		admin.GET("/users", authHandler.ListUsers)
		admin.POST("/users", authHandler.CreateUser)
		admin.POST("/password", authHandler.ChangePassword)
	}

	// Serve Vue admin frontend (embedded)
	r.GET("/admin", serveAdmin)
	r.GET("/admin/*filepath", func(c *gin.Context) {
		// 所有 /admin/* 子路径都返回同一份 HTML（SPA fallback）
		serveAdmin(c)
	})

	// 根路径和 /login 重定向到管理后台
	r.GET("/", func(c *gin.Context) {
		c.Redirect(http.StatusFound, "/admin")
	})
	r.GET("/login", func(c *gin.Context) {
		c.Redirect(http.StatusFound, "/admin")
	})

	r.GET("/favicon.ico", func(c *gin.Context) {
		c.Status(http.StatusNoContent)
	})

	// Start server
	addr := ":" + cfg.ServerPort
	log.Printf("Cimoc Data Sync Server starting on %s", addr)
	log.Printf("Admin panel: http://localhost%s/admin", addr)
	if err := r.Run(addr); err != nil {
		log.Fatalf("failed to start server: %v", err)
	}
}

// hasSetAdminCommand 检查命令行是否包含 set admin 命令
func hasSetAdminCommand() bool {
	for i, arg := range os.Args {
		if strings.EqualFold(arg, "set") && i+1 < len(os.Args) && strings.EqualFold(os.Args[i+1], "admin") {
			return true
		}
	}
	return false
}

// extractFlag 从 os.Args 中提取指定 flag 的值（支持 --key=val 和 --key val）
func extractFlag(name string) string {
	long := "--" + name
	for i, arg := range os.Args {
		if strings.HasPrefix(arg, long+"=") {
			return arg[len(long)+1:]
		}
		if arg == long && i+1 < len(os.Args) && !strings.HasPrefix(os.Args[i+1], "-") {
			return os.Args[i+1]
		}
	}
	return ""
}

func serveAdmin(c *gin.Context) {
	data, err := adminHTML.ReadFile("admin/index.html")
	if err != nil {
		c.String(http.StatusNotFound, "admin page not found")
		return
	}
	c.Data(http.StatusOK, "text/html; charset=utf-8", data)
}

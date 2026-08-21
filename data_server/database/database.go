package database

import (
	"crypto/rand"
	"log"
	"math/big"
	"os"
	"path/filepath"
	"strings"
	"time"

	"xcimoc-data-server/config"
	"xcimoc-data-server/models"
	"xcimoc-data-server/query"
	"xcimoc-data-server/utils"

	"gorm.io/driver/mysql"
	"gorm.io/driver/postgres"
	"gorm.io/driver/sqlite"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

var DB *gorm.DB

const DefaultAdminUsername = "admin"

func Init(cfg *config.Config) {
	switch cfg.DBType {
	case "mysql":
		if cfg.DBDSN == "" {
			log.Fatalf("使用 MySQL 时必须设置 --dbdsn 或 DB_DSN，例如: user:pass@tcp(127.0.0.1:3306)/cimoc?charset=utf8mb4&parseTime=True")
		}
		// 自动补充 parseTime=True（GORM 读写 time.Time 必需）
		if !strings.Contains(cfg.DBDSN, "parseTime=") {
			if strings.Contains(cfg.DBDSN, "?") {
				cfg.DBDSN += "&parseTime=True"
			} else {
				cfg.DBDSN += "?parseTime=True"
			}
		}
		openGorm(cfg, mysql.Open(cfg.DBDSN))

	case "postgres", "pgsql":
		if cfg.DBDSN == "" {
			log.Fatalf("使用 PostgreSQL 时必须设置 --dbdsn 或 DB_DSN，例如: host=localhost user=user password=pass dbname=cimoc port=5432 sslmode=disable")
		}
		openGorm(cfg, postgres.Open(cfg.DBDSN))

	default: // sqlite
		dbDir := filepath.Dir(cfg.DBPath)
		if err := os.MkdirAll(dbDir, 0755); err != nil {
			log.Fatalf("failed to create database directory %s: %v", dbDir, err)
		}
		openGorm(cfg, sqlite.Open(cfg.DBPath))
	}
}

// openGorm 封装 GORM 初始化 + 迁移 + 连接池 + 默认管理员
func openGorm(cfg *config.Config, dialector gorm.Dialector) {
	var err error
	DB, err = gorm.Open(dialector, &gorm.Config{
		// gorm Warn 级别默认会把“查询无结果”(record not found) 也打印成日志。
		// 全量同步时每个漫画都会查一次墓碑表，绝大多数没有墓碑，
		// 导致每次同步刷大量 record not found（正常现象、非错误）。
		// IgnoreRecordNotFoundError 只在日志层面忽略该场景，
		// 不影响任何查询返回的 error 值或业务错误处理。
		Logger: logger.New(log.New(os.Stdout, "\r\n", log.LstdFlags), logger.Config{
			SlowThreshold:             time.Second,
			LogLevel:                  logger.Warn,
			IgnoreRecordNotFoundError: true,
			Colorful:                  false,
		}),
	})
	if err != nil {
		log.Fatalf("failed to connect database: %v", err)
	}

	// 配置数据库连接池
	sqlDB, err := DB.DB()
	if err == nil {
		// SQLite 只支持单写入者，限制连接数避免 "database is locked"
		if cfg.DBType == "sqlite" {
			sqlDB.SetMaxOpenConns(1)
			sqlDB.SetMaxIdleConns(1)
		} else {
			sqlDB.SetMaxOpenConns(25)
			sqlDB.SetMaxIdleConns(10)
		}
		sqlDB.SetConnMaxLifetime(5 * time.Minute)
	}

	// MySQL 需要在迁移前关闭外键检查（TagRef 使用多表关联）
	if cfg.DBType == "mysql" {
		DB.Exec("SET FOREIGN_KEY_CHECKS = 0")
	}

	err = DB.AutoMigrate(
		&models.User{},
		&models.Comic{},
		&models.ComicDelete{},
		&models.SyncEvent{},
		&models.Setting{},
		&models.Tag{},
		&models.TagRef{},
	)
	if err != nil {
		log.Fatalf("failed to migrate database: %v", err)
	}

	if cfg.DBType == "mysql" {
		DB.Exec("SET FOREIGN_KEY_CHECKS = 1")
	}

	// 将已初始化的 *gorm.DB 绑定到 gorm.io/gen 生成的 query 单例。
	// 之后所有 handler/middleware/maintenance 都可以用 query.Q（即 query.User /
	// query.Comic / query.SyncEvent 等包级快捷）来进行类型安全的查询与写入，
	// 不必再手写 database.DB.Where("user_id = ? AND id > ?", ...) 这种字符串 Where，
	// 从编译期避免字段名拼写错误、重构不一致与潜在的拼接式 SQL 注入问题。
	// 原 database.DB 仍保留可用，作为复杂原生 SQL / 事务兜底。
	query.SetDefault(DB)

	// 首次启动自动创建默认管理员账户
	ensureAdminExists()

	log.Printf("database initialized successfully (type: %s)", cfg.DBType)
}

// generateRandomPassword 生成 12 位随机密码（字母+数字）
func generateRandomPassword() (string, error) {
	const charset = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
	password := make([]byte, 12)
	for i := range password {
		idx, err := rand.Int(rand.Reader, big.NewInt(int64(len(charset))))
		if err != nil {
			return "", err
		}
		password[i] = charset[idx.Int64()]
	}
	return string(password), nil
}

func ensureAdminExists() {
	// 用生成的 query.User 统计管理员数，避免裸字符串 "is_admin = ?"。
	count, err := query.User.Where(query.User.IsAdmin.Eq(true)).Count()
	if err != nil {
		log.Fatalf("failed to count admin users: %v", err)
	}
	if count > 0 {
		return // 管理员已存在
	}

	salt, err := utils.GenerateSalt()
	if err != nil {
		log.Fatalf("failed to generate salt for admin: %v", err)
	}

	adminPassword, err := generateRandomPassword()
	if err != nil {
		log.Fatalf("failed to generate admin password: %v", err)
	}

	admin := &models.User{
		Username: DefaultAdminUsername,
		Password: utils.HashPassword(adminPassword, salt),
		Salt:     salt,
		IsAdmin:  true,
	}

	// 使用生成 query 的 Create；如果后续 User 表字段结构变了，编译期会直接拦截。
	if err := query.User.Create(admin); err != nil {
		log.Fatalf("failed to create default admin: %v", err)
	}

	log.Printf("========================================")
	log.Printf("  首次启动，默认管理员已创建")
	log.Printf("  用户名: %s", DefaultAdminUsername)
	log.Printf("  密码: %s", adminPassword)
	log.Printf("  请立即登录管理后台修改密码！")
	log.Printf("  后台地址: http://<server>:<port>/admin")
	log.Printf("========================================")
}

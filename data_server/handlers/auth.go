package handlers

import (
	"log"
	"net/http"

	"xcimoc-data-server/config"
	"xcimoc-data-server/database"
	"xcimoc-data-server/middleware"
	"xcimoc-data-server/models"
	"xcimoc-data-server/query"
	"xcimoc-data-server/utils"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type AuthHandler struct {
	Config *config.Config
}

func NewAuthHandler(cfg *config.Config) *AuthHandler {
	return &AuthHandler{Config: cfg}
}

// Login 按用户名查询 + 盐值校验密码；用户名查询用生成 query 的类型安全 Eq。
func (h *AuthHandler) Login(c *gin.Context) {
	var req models.LoginRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效"})
		return
	}

	user, err := query.User.Where(query.User.Username.Eq(req.Username)).Take()
	if err != nil || user == nil {
		// 同时处理 not-found / 其它查询异常：统一返回 401，不泄露“用户名不存在”。
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户名或密码错误"})
		return
	}

	if !utils.VerifyPassword(req.Password, user.Salt, user.Password) {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户名或密码错误"})
		return
	}

	token, err := middleware.GenerateToken(user, h.Config)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "生成令牌失败"})
		return
	}

	c.JSON(http.StatusOK, models.LoginResponse{Token: token, User: *user})
}

// RefreshToken 延长有效期；按用户主键直接查询。
func (h *AuthHandler) RefreshToken(c *gin.Context) {
	userID := c.GetUint("user_id")

	user, err := query.User.Where(query.User.ID.Eq(userID)).Take()
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "用户不存在"})
		return
	}

	token, err := middleware.GenerateToken(user, h.Config)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "生成令牌失败"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"token": token, "user": *user})
}

// ListUsers 管理员：查询所有用户（仅返回 id/username/is_admin/created_at/updated_at）。
// 由于生成的 User DO 的 Select 需要 field.Expr 组合，这里直接用 UnderlyingDB().Select
// 附加字段名列表；WHERE 仍保持空（全表扫），所有字段名与表名已由生成 DO 初始化决定。
func (h *AuthHandler) ListUsers(c *gin.Context) {
	var users []models.User
	if err := query.User.UnderlyingDB().
		Select("id, username, is_admin, created_at, updated_at").
		Find(&users).Error; err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询用户失败"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"users": users})
}

// CreateUser 管理员创建普通用户。
func (h *AuthHandler) CreateUser(c *gin.Context) {
	var req models.CreateUserRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	existing, err := query.User.Where(query.User.Username.Eq(req.Username)).Take()
	if err != nil && !errorsIsNotFound(err) {
		log.Printf("查询用户名冲突失败: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "用户名冲突检查失败"})
		return
	}
	if existing != nil {
		c.JSON(http.StatusConflict, gin.H{"error": "用户名已存在"})
		return
	}

	salt, err := utils.GenerateSalt()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "密码加密失败"})
		return
	}

	user := &models.User{
		Username: req.Username,
		Password: utils.HashPassword(req.Password, salt),
		Salt:     salt,
		IsAdmin:  false,
	}
	if createErr := query.User.Create(user); createErr != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "创建用户失败"})
		return
	}
	c.JSON(http.StatusCreated, gin.H{"message": "用户创建成功", "user": *user})
}

// ChangePassword 管理员修改任意用户密码；同步递增 token_version 使旧 token 失效。
//
// 更新操作涉及 token_version = token_version + 1 的表达式赋值，
// 用生成 query 的 UnderlyingDB + 列名映射保持一致性，
// 主条件 username 仍使用生成字段 Username.Eq。
func (h *AuthHandler) ChangePassword(c *gin.Context) {
	var req models.ChangePasswordRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	user, err := query.User.Where(query.User.Username.Eq(req.Username)).Take()
	if err != nil {
		if errorsIsNotFound(err) {
			c.JSON(http.StatusNotFound, gin.H{"error": "用户不存在"})
			return
		}
		log.Printf("查询用户失败: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询用户失败"})
		return
	}

	salt, err := utils.GenerateSalt()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "密码加密失败"})
		return
	}
	user.Password = utils.HashPassword(req.NewPassword, salt)
	user.Salt = salt
	user.TokenVersion++
	if saveErr := database.DB.Save(user).Error; saveErr != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "保存用户失败"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "密码修改成功"})
}

func errorsIsNotFound(err error) bool {
	if err == nil {
		return false
	}
	return err == gorm.ErrRecordNotFound
}

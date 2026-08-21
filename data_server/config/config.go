package config

import (
	"crypto/rand"
	"encoding/hex"
	"flag"
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"gopkg.in/yaml.v3"
)

// ConfigFile 对应 YAML 配置文件结构
type ConfigFile struct {
	Server struct {
		Port string `yaml:"port"`
	} `yaml:"server"`
	Database struct {
		Type string `yaml:"type"` // sqlite / mysql / postgres
		Path string `yaml:"path"` // SQLite 文件路径
		DSN  string `yaml:"dsn"`  // MySQL/PostgreSQL 连接串
	} `yaml:"database"`
	JWT struct {
		Secret string `yaml:"secret"`
	} `yaml:"jwt"`
	// CORS 允许的 Origin 白名单，例如：
	//   cors:
	//     origins:
	//       - https://admin.example.com
	// 为空时回退到 "*"，但允许携带凭证的客户端将无法使用 CORS。
	CORS struct {
		Origins []string `yaml:"origins"`
	} `yaml:"cors"`
}

type Config struct {
	DBType      string
	DBPath      string
	DBDSN       string
	JWTSecret   string
	ServerPort  string
	CORSOrigins []string // CORS 允许的 Origin 白名单；空数组则使用 "*"
}

// LoadWithConfig 加载配置，可指定配置文件路径（用于 set admin 命令，无需 CLI flag 解析）
func LoadWithConfig(configPath string) *Config {
	cfg := defaultConfig()
	if configPath != "" {
		cfg.applyConfigFile(configPath)
	}
	applyEnvOverrides(cfg)
	ensureJWT(cfg)
	return cfg
}

func Load() *Config {
	// CLI 参数
	configPath := flag.String("config", "", "配置文件路径 (YAML 格式)")
	dbtype := flag.String("dbtype", "", "数据库类型: sqlite / mysql / postgres")
	data := flag.String("data", "", "SQLite 数据库路径")
	dbdsn := flag.String("dbdsn", "", "MySQL/PostgreSQL 连接串")
	jwt := flag.String("jwtsecret", "", "JWT 签名密钥")
	port := flag.String("port", "", "监听端口")
	flag.Parse()

	cfg := defaultConfig()

	// 从 YAML 配置文件加载
	if *configPath != "" {
		cfg.applyConfigFile(*configPath)
	}

	// 环境变量覆盖
	applyEnvOverrides(cfg)

	// CLI 参数覆盖（优先级最高）
	if *dbtype != "" {
		cfg.DBType = *dbtype
	}
	if *data != "" {
		cfg.DBPath = *data
	}
	if *dbdsn != "" {
		cfg.DBDSN = *dbdsn
	}
	if *jwt != "" {
		cfg.JWTSecret = *jwt
	}
	if *port != "" {
		cfg.ServerPort = *port
	}

	ensureJWT(cfg)
	return cfg
}

// defaultConfig 返回默认配置
func defaultConfig() *Config {
	return &Config{
		DBType:     "sqlite",
		DBPath:     "./data/cimoc.db",
		JWTSecret:  "",
		ServerPort: "8080",
	}
}

// applyEnvOverrides 用环境变量覆盖配置
func applyEnvOverrides(cfg *Config) {
	if v := os.Getenv("DB_TYPE"); v != "" {
		cfg.DBType = v
	}
	if v := os.Getenv("DB_PATH"); v != "" {
		cfg.DBPath = v
	}
	if v := os.Getenv("DB_DSN"); v != "" {
		cfg.DBDSN = v
	}
	if v := os.Getenv("JWT_SECRET"); v != "" {
		cfg.JWTSecret = v
	}
	if v := os.Getenv("SERVER_PORT"); v != "" {
		cfg.ServerPort = v
	}
	// CORS_ORIGINS 支持英文逗号分隔，例如：https://a.example.com,https://b.example.com
	if v := os.Getenv("CORS_ORIGINS"); v != "" {
		parts := strings.FieldsFunc(v, func(r rune) bool { return r == ',' || r == ' ' || r == ';' })
		for _, p := range parts {
			if p != "" {
				cfg.CORSOrigins = append(cfg.CORSOrigins, p)
			}
		}
	}
}

// ensureJWT 确保 JWT 密钥已设置，未设置则自动生成并持久化（重启后旧 token 仍有效）
func ensureJWT(cfg *Config) {
	if cfg.JWTSecret != "" {
		return
	}

	// 优先从持久化文件恢复上次生成的密钥，避免重启后所有已签发 token 失效
	if secret, err := loadPersistedJWTSecret(cfg); err == nil && secret != "" {
		cfg.JWTSecret = secret
		fmt.Println("[提示] 已从持久化文件恢复 JWT 密钥，重启不影响已签发 token")
		return
	}

	cfg.JWTSecret = generateRandomKey(32)
	if err := persistJWTSecret(cfg, cfg.JWTSecret); err != nil {
		fmt.Printf("[警告] JWT 密钥持久化失败: %v（重启后旧 token 将失效）\n", err)
		fmt.Printf("[提示] 本次密钥: %s\n", cfg.JWTSecret)
	} else {
		fmt.Println("[提示] 未设置 JWT_SECRET，已自动生成并持久化（重启后旧 token 仍有效）")
	}
}

// jwtSecretFile 返回 JWT 密钥持久化文件路径（与数据库同目录）
func jwtSecretFile(cfg *Config) string {
	dir := filepath.Dir(cfg.DBPath)
	if dir == "." || dir == "" {
		dir = "."
	}
	return filepath.Join(dir, ".jwt_secret")
}

// loadPersistedJWTSecret 从持久化文件读取 JWT 密钥
func loadPersistedJWTSecret(cfg *Config) (string, error) {
	data, err := os.ReadFile(jwtSecretFile(cfg))
	if err != nil {
		return "", err
	}
	return strings.TrimSpace(string(data)), nil
}

// persistJWTSecret 将 JWT 密钥写入持久化文件（0600 权限，仅本用户可读）
func persistJWTSecret(cfg *Config, secret string) error {
	dir := filepath.Dir(cfg.DBPath)
	if err := os.MkdirAll(dir, 0755); err != nil {
		return err
	}
	return os.WriteFile(jwtSecretFile(cfg), []byte(secret+"\n"), 0600)
}

// applyConfigFile 从 YAML 文件加载配置
func (cfg *Config) applyConfigFile(path string) {
	data, err := os.ReadFile(path)
	if err != nil {
		fmt.Fprintf(os.Stderr, "[警告] 读取配置文件失败 %s: %v\n", path, err)
		return
	}

	var cf ConfigFile
	if err := yaml.Unmarshal(data, &cf); err != nil {
		fmt.Fprintf(os.Stderr, "[警告] 解析配置文件失败 %s: %v\n", path, err)
		return
	}

	if cf.Server.Port != "" {
		cfg.ServerPort = cf.Server.Port
	}
	if cf.Database.Type != "" {
		cfg.DBType = cf.Database.Type
	}
	if cf.Database.Path != "" {
		cfg.DBPath = cf.Database.Path
	}
	if cf.Database.DSN != "" {
		cfg.DBDSN = cf.Database.DSN
	}
	if cf.JWT.Secret != "" {
		cfg.JWTSecret = cf.JWT.Secret
	}
	if len(cf.CORS.Origins) > 0 {
		cfg.CORSOrigins = append(cfg.CORSOrigins, cf.CORS.Origins...)
	}

	fmt.Printf("[配置] 已加载配置文件: %s\n", path)
}

func getEnv(key, fallback string) string {
	if value, ok := os.LookupEnv(key); ok {
		return value
	}
	return fallback
}

// generateRandomKey 生成随机十六进制密钥
func generateRandomKey(length int) string {
	b := make([]byte, length)
	if _, err := rand.Read(b); err != nil {
		return fmt.Sprintf("fallback-key-%d-%d", os.Getpid(), os.Getpid()*length)
	}
	return hex.EncodeToString(b)
}

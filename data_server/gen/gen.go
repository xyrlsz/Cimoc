// Package gen 是 gorm.io/gen 的代码生成入口。
//
// 只生成 query（不重复生成 model），复用 xcimoc-data-server/models
// 包中已手写维护的结构体（它们已经带好了 uniqueIndex 等数据库约束
// 以及 json 序列化标签，若让 gen 重新生成会覆盖这些信息）。
//
// 使用方式：
//
//	// 1) 在 data_server 目录执行：
//	go run ./gen --config ./config.example.yaml
//
//	// 2) 或者通过环境变量指定数据库类型和 DSN（无需 config.yaml）：
//	DB_TYPE=sqlite  DB_PATH=./data.db           go run ./gen
//	DB_TYPE=mysql   DB_DSN="user:pass@tcp(127.0.0.1:3306)/test" go run ./gen
//
// 生成结果会写到 ../../query/ 下，在 handler/database 中用
//
//	"xcimoc-data-server/query"
//	query.SetDefault(gormDB) // 在 database.Init() 之后调用
//	q := query.Q // 然后使用 q.User / q.Comic / q.SyncEvent / ...
//
// 直接进行类型安全的 SQL 构造，避免手写字符串 Where / Updates 造成的
// 拼写错误、字段名重构不一致、SQL 注入拼接等问题。
package main

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"

	"xcimoc-data-server/config"
	"xcimoc-data-server/models"

	"github.com/libtnb/sqlite" // 代码生成阶段专用：纯 Go 实现（modernc.org/sqlite），无需 gcc/CGO。
	"gorm.io/driver/mysql"
	"gorm.io/driver/postgres"
	"gorm.io/gen"
	"gorm.io/gorm"
	"gorm.io/gorm/logger"
)

// dataServerRoot 返回 data_server 模块根目录（gen/ 目录的上一级）。
// 无论 `go run ./gen` 还是从其它 cwd 执行生成，输出路径都正确落在
// <repo>/data_server/query/ 下。
func dataServerRoot() string {
	wd, err := os.Getwd()
	if err != nil {
		log.Fatalf("gen: 无法获取工作目录: %v", err)
	}
	// gen/gen.go 位于 data_server/gen/gen.go，所以模块根是 ../
	// 但若 cwd 已经是 data_server/，则 "." 即可。这里通过探测 go.mod
	// 得到最稳妥的路径。
	cur := wd
	for i := 0; i < 5; i++ {
		if _, err := os.Stat(filepath.Join(cur, "go.mod")); err == nil {
			if _, err2 := os.Stat(filepath.Join(cur, "main.go")); err2 == nil {
				return cur
			}
		}
		parent := filepath.Dir(cur)
		if parent == cur {
			break
		}
		cur = parent
	}
	return wd
}

func openDBFromConfig(cfg *config.Config) *gorm.DB {
	var dial gorm.Dialector
	switch cfg.DBType {
	case "mysql":
		if cfg.DBDSN == "" {
			log.Fatalf("gen: 代码生成仅需要连接一张空库建表，使用 mysql 必须配置 DB_DSN 或 --config 中的 database.dsn")
		}
		if !strings.Contains(cfg.DBDSN, "parseTime=") {
			if strings.Contains(cfg.DBDSN, "?") {
				cfg.DBDSN += "&parseTime=True"
			} else {
				cfg.DBDSN += "?parseTime=True"
			}
		}
		dial = mysql.Open(cfg.DBDSN)
	case "postgres", "pgsql":
		if cfg.DBDSN == "" {
			log.Fatalf("gen: 使用 postgres 必须配置 DB_DSN")
		}
		dial = postgres.Open(cfg.DBDSN)
	default: // sqlite
		// gen 只需要模型结构信息生成类型安全代码，不需要真实数据；
		// 始终在系统临时目录用空库，避免在构建/数据目录留下
		// data/cimoc.db、data/.jwt_secret 等文件污染项目。
		dbPath := filepath.Join(os.TempDir(), "xcimoc-gen.sqlite3")
		_ = os.Remove(dbPath) // 每次生成从干净表结构开始
		dial = sqlite.Open(dbPath)
	}

	db, err := gorm.Open(dial, &gorm.Config{
		Logger: logger.Default.LogMode(logger.Warn),
	})
	if err != nil {
		log.Fatalf("gen: 连接数据库失败（用于迁移生成查询结构体）: %v", err)
	}

	// gorm.io/gen 生成 Query 的结构体需要读取真实的模型定义，
	// 但表名/字段名通过结构体 tag 即可获得，实际不需要库里有东西。
	// 这里仍然 AutoMigrate 一下保证：
	//   - gen 工作在 "从数据库反向生成" 模式时，能得到一致的命名。
	//   - 同时提前发现模型 tag 与 GORM 不兼容的问题。
	if migrateErr := db.AutoMigrate(
		&models.User{},
		&models.Comic{},
		&models.ComicDelete{},
		&models.SyncEvent{},
		&models.Setting{},
		&models.Tag{},
		&models.TagRef{},
	); migrateErr != nil {
		log.Fatalf("gen: 迁移模型失败: %v", migrateErr)
	}
	return db
}

func main() {
	// 注意：config.Load() 内部会调用 flag.String("config") + flag.Parse()。
	// 为了避免与我们自定义的 flag 冲突（尤其是 --config 本身），我们不用
	// flag 包声明自己的参数，而是手工扫描 os.Args 提取三个 gen 专用参数，
	// 其余参数交由 config.Load() 的 flag.Parse 正常处理（这样用户依然
	// 可以用 --config=xxx 同时指定服务端配置文件）。
	configPath := ""
	outDir := ""
	modelPkg := "xcimoc-data-server/models"
	remainingArgs := make([]string, 0, len(os.Args))
	remainingArgs = append(remainingArgs, os.Args[0])
	for i := 1; i < len(os.Args); i++ {
		arg := os.Args[i]
		val, taken := "", false
		if val, taken = cutPrefix(arg, "--gen-out="); taken {
			outDir = val
			continue
		}
		if strings.HasPrefix(arg, "--gen-out") {
			if i+1 < len(os.Args) {
				outDir = os.Args[i+1]
				i++
			}
			continue
		}
		if val, taken = cutPrefix(arg, "--gen-model-pkg="); taken {
			modelPkg = val
			continue
		}
		if strings.HasPrefix(arg, "--gen-model-pkg") {
			if i+1 < len(os.Args) {
				modelPkg = os.Args[i+1]
				i++
			}
			continue
		}
		if val, taken = cutPrefix(arg, "--gen-config="); taken {
			configPath = val
			continue
		}
		if strings.HasPrefix(arg, "--gen-config") {
			if i+1 < len(os.Args) {
				configPath = os.Args[i+1]
				i++
			}
			continue
		}
		remainingArgs = append(remainingArgs, arg)
	}
	// 替换 os.Args 后再调用 config.Load，这样 config 包里的 --config/--dbtype/...
	// 仍然可用，且不会看到 --gen-* 开头的生成器参数。
	os.Args = remainingArgs

	// gen 阶段用不到 JWT；预置 JWT_SECRET 避免 config.ensureJWT 自动生成并
	// 持久化 .jwt_secret 到数据库目录（污染构建/数据目录，且属于敏感文件）。
	// 仅当环境变量未设置时预置，不影响用户显式传入的 JWT_SECRET。
	if os.Getenv("JWT_SECRET") == "" {
		os.Setenv("JWT_SECRET", "gen-temporary")
	}

	var cfg *config.Config
	if configPath != "" {
		// 用户明确传了 --gen-config 就绕过 config.Load 的 CLI flag 解析
		// （否则会跟它的 --config 命名重复且 Parse 读到错误值）。
		cfg = config.LoadWithConfig(configPath)
	} else {
		cfg = config.Load()
	}

	db := openDBFromConfig(cfg)

	root := dataServerRoot()
	if outDir == "" {
		outDir = filepath.Join(root, "query")
	}
	if err := os.MkdirAll(outDir, 0o755); err != nil {
		log.Fatalf("gen: 创建输出目录失败: %v", err)
	}

	g := gen.NewGenerator(gen.Config{
		OutPath:      outDir,
		OutFile:      "gen.go",
		ModelPkgPath: modelPkg,
		Mode:         gen.WithDefaultQuery | gen.WithoutContext,
		// 生成可空字段的字段查询器（Where/Cond/Update 接口）。
		FieldNullable:     true,
		FieldCoverable:    true,
		FieldSignable:     true,
		FieldWithIndexTag: true,
		FieldWithTypeTag:  true,
	})
	g.UseDB(db)

	// 为每个手写模型生成对应的 Query 对象：
	//   q.User.Where(q.User.Username.Eq("admin")).First() 等。
	// 通过 ApplyBasic + 自定义接口，所有原先手写的 database.DB.Where(...)
	// 字符串 Where 都能换成强类型字段。
	models := []interface{}{
		models.User{},
		models.Comic{},
		models.ComicDelete{},
		models.SyncEvent{},
		models.Setting{},
		models.Tag{},
		models.TagRef{},
	}
	for _, m := range models {
		g.ApplyBasic(m)
	}

	g.Execute()

	fmt.Println("gorm.io/gen 代码生成完成。")
	fmt.Println("  输出目录  :", outDir)
	fmt.Println("  模型来源  :", modelPkg)
	fmt.Println("  生成 Query: ", len(models), "个表：User/Comic/ComicDelete/SyncEvent/Setting/Tag/TagRef")
	fmt.Println("常用命令    :")
	fmt.Println("  # 用临时 sqlite 生成：")
	fmt.Println("  DB_TYPE=sqlite DB_PATH=./gen-temp.sqlite3 go run ./gen")
	fmt.Println("  # 用现有 config.yaml（可带 MySQL/PG DSN）：")
	fmt.Println("  go run ./gen --gen-config=./config.yaml")
}

// cutPrefix 是 strings.CutPrefix 的小封装：兼容老版本 Go。
func cutPrefix(s, prefix string) (string, bool) {
	if !strings.HasPrefix(s, prefix) {
		return "", false
	}
	return s[len(prefix):], true
}

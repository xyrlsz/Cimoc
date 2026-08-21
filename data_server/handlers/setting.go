package handlers

import (
	"log"
	"net/http"
	"time"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"
	"xcimoc-data-server/query"

	"github.com/gin-gonic/gin"
)

type SettingHandler struct{}

func NewSettingHandler() *SettingHandler { return &SettingHandler{} }

// List 返回当前用户的所有设置。
func (h *SettingHandler) List(c *gin.Context) {
	userID := c.GetUint("user_id")

	rows, err := query.Setting.Where(query.Setting.UserID.Eq(userID)).Find()
	if err != nil {
		log.Printf("获取设置失败 (user_id=%d): %v", userID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取设置失败"})
		return
	}
	settings := make([]models.Setting, 0, len(rows))
	for _, r := range rows {
		if r != nil {
			settings = append(settings, *r)
		}
	}

	c.JSON(http.StatusOK, gin.H{"settings": settings})
}

// Sync 合并客户端上传的设置。
// upsert 查找键为 (user_id, key)；不存在则创建，存在则覆盖 Value。
func (h *SettingHandler) Sync(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.SettingSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	synced := 0
	st := query.Setting
	now := time.Now()

	for _, item := range req.Settings {
		if item.Key == "" {
			continue
		}

		existing, err := st.Where(
			st.UserID.Eq(userID),
			st.Key.Eq(item.Key),
		).Take()
		if err != nil && !isNotFound(err) {
			log.Printf("查询设置失败 (user_id=%d, key=%s): %v", userID, item.Key, err)
			continue
		}

		if existing != nil {
			if existing.Value == item.Value {
				synced++
				continue
			}
			existing.Value = item.Value
			existing.UpdatedAt = now
			// 主键写回：复合唯一键 (user_id,key) 非主键，直接 Save 更稳
			if saveErr := database.DB.Save(existing).Error; saveErr != nil {
				log.Printf("更新设置失败 (user_id=%d, key=%s): %v", userID, item.Key, saveErr)
				continue
			}
		} else {
			if createErr := st.Create(&models.Setting{
				UserID:    userID,
				Key:       item.Key,
				Value:     item.Value,
				CreatedAt: now,
				UpdatedAt: now,
			}); createErr != nil {
				log.Printf("创建设置失败 (user_id=%d, key=%s): %v", userID, item.Key, createErr)
				continue
			}
		}
		synced++
	}

	c.JSON(http.StatusOK, gin.H{
		"synced":  synced,
		"message": "设置同步完成",
	})
}

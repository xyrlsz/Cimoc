package handlers

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"

	"github.com/gin-gonic/gin"
)

type ComicHandler struct{}

func NewComicHandler() *ComicHandler {
	return &ComicHandler{}
}

// List returns all comics for the authenticated user.
// Supports incremental pull via ?since=<unix_millis> query parameter.
func (h *ComicHandler) List(c *gin.Context) {
	userID := c.GetUint("user_id")

	var comics []models.Comic
	query := database.DB.Where("user_id = ?", userID)

	// 增量拉取：如果客户端传了 since 参数，只返回该时间之后更新过的漫画
	if sinceStr := c.Query("since"); sinceStr != "" {
		if sinceMillis, err := strconv.ParseInt(sinceStr, 10, 64); err == nil && sinceMillis > 0 {
			sinceTime := time.Unix(0, sinceMillis*int64(time.Millisecond))
			query = query.Where("updated_at > ?", sinceTime)
		}
	}

	result := query.Order("updated_at DESC").Find(&comics)
	if result.Error != nil {
		log.Printf("获取漫画列表失败 (user_id=%d): %v", userID, result.Error)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取漫画列表失败"})
		return
	}

	serverTime := time.Now().UnixMilli()

	// 增量拉取时，同时返回其他设备的删除记录
	var deletes []models.ComicDeleteItem
	if c.Query("since") != "" {
		var comicDeletes []models.ComicDelete
		if sinceMillis, err := strconv.ParseInt(c.Query("since"), 10, 64); err == nil && sinceMillis > 0 {
			sinceTime := time.Unix(0, sinceMillis*int64(time.Millisecond))
			database.DB.Where("user_id = ? AND created_at > ?", userID, sinceTime).
				Find(&comicDeletes)
		} else {
			database.DB.Where("user_id = ?", userID).Find(&comicDeletes)
		}
		deletes = make([]models.ComicDeleteItem, len(comicDeletes))
		for i, d := range comicDeletes {
			deletes[i] = models.ComicDeleteItem{
				Source:    d.Source,
				Cid:       d.Cid,
				DeleteFav: d.DeleteFav,
				DeleteHis: d.DeleteHis,
			}
		}
	}

	c.JSON(http.StatusOK, models.ComicListResponse{
		Comics:     comics,
		Deletes:    deletes,
		ServerTime: serverTime,
	})
}

// Sync merges uploaded comics with the server data.
// Merge strategy: match by (source, cid) per user.
// - If the comic exists on server: compare timestamps, keep newer version
// - If the comic doesn't exist: create
// - Check ComicDelete table to prevent re-uploading data deleted by another device
// - Returns server_time for client to store as last_synced_at
func (h *ComicHandler) Sync(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.ComicSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	synced := 0
	skipped := 0

	for _, item := range req.Comics {
		if item.Cid == "" {
			continue
		}

		// ---- 检查多端删除标记：如果其他设备已删除了此漫画的数据 ----
		var delRecord models.ComicDelete
		delResult := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
			userID, item.Source, item.Cid).Limit(1).Find(&delRecord)

		if delResult.RowsAffected > 0 {
			// 有其他设备删除了此漫画的数据，检查是否需要跳过
			if delRecord.DeleteFav && item.Favorite != nil && !item.ClearFavorite {
				// 其他设备已取消收藏 → 拒绝恢复，强制客户端清除
				item.Favorite = nil
				item.ClearFavorite = true
				log.Printf("多端冲突: 漫画 %d:%s 的收藏被其他设备删除，拒绝恢复", item.Source, item.Cid)
			}
			if delRecord.DeleteHis && item.History != nil && !item.ClearHistory {
				// 其他设备已清除历史 → 拒绝恢复
				item.History = nil
				item.ClearHistory = true
				log.Printf("多端冲突: 漫画 %d:%s 的历史被其他设备删除，拒绝恢复", item.Source, item.Cid)
			}
			// 如果两边都已清除，从删除记录表中移除（冲突已解决）
			if (delRecord.DeleteFav && item.ClearFavorite) && (delRecord.DeleteHis && item.ClearHistory) {
				database.DB.Delete(&delRecord)
			} else if delRecord.DeleteFav && item.ClearFavorite {
				delRecord.DeleteFav = false
				if !delRecord.DeleteHis {
					database.DB.Delete(&delRecord)
				} else {
					database.DB.Save(&delRecord)
				}
			} else if delRecord.DeleteHis && item.ClearHistory {
				delRecord.DeleteHis = false
				if !delRecord.DeleteFav {
					database.DB.Delete(&delRecord)
				} else {
					database.DB.Save(&delRecord)
				}
			}
		}

		// Look for existing comic by (source, cid)
		var existing models.Comic
		result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
			userID, item.Source, item.Cid).Limit(1).Find(&existing)

		if result.RowsAffected > 0 {
			// Comic exists — merge
			needsUpdate := false

			// Prefer newer history — also update metadata
			if item.History != nil && (existing.History == nil || *item.History > *existing.History) {
				existing.History = item.History
				existing.Last = item.Last
				existing.Page = item.Page
				existing.Chapter = item.Chapter
				existing.Title = item.Title
				existing.Cover = item.Cover
				existing.Update = item.Update
				existing.Finish = item.Finish
				if item.ChapterCount != nil {
					existing.ChapterCount = item.ChapterCount
				}
				needsUpdate = true
			}

			// Prefer newer favorite
			if item.Favorite != nil && (existing.Favorite == nil || *item.Favorite > *existing.Favorite) {
				existing.Favorite = item.Favorite
				needsUpdate = true
			}

			// Client explicit clear history — always honor and record deletion
			if item.ClearHistory {
				if existing.History != nil {
					existing.History = nil
					needsUpdate = true
				}
				h.recordDelete(userID, item.Source, item.Cid, false, true)
			}

			// Client explicit clear favorite — always honor and record deletion
			if item.ClearFavorite {
				if existing.Favorite != nil {
					existing.Favorite = nil
					needsUpdate = true
				}
				h.recordDelete(userID, item.Source, item.Cid, true, false)
			}

			if needsUpdate {
				if err := database.DB.Save(&existing).Error; err != nil {
					log.Printf("更新漫画失败 (user_id=%d, source=%d, cid=%s): %v", userID, item.Source, item.Cid, err)
					continue
				}
				synced++
			} else {
				skipped++
			}

		} else {
			// New comic — create (unless it's just a delete marker with no data)
			if item.Favorite == nil && item.History == nil {
				// 纯删除标记（clear_history/clear_favorite），没有实际数据，记录删除即可
				if item.ClearHistory {
					h.recordDelete(userID, item.Source, item.Cid, false, true)
				}
				if item.ClearFavorite {
					h.recordDelete(userID, item.Source, item.Cid, true, false)
				}
				skipped++
				continue
			}

			comic := models.Comic{
				UserID:       userID,
				Source:       item.Source,
				Cid:          item.Cid,
				Title:        item.Title,
				Cover:        item.Cover,
				Update:       item.Update,
				Finish:       item.Finish,
				Favorite:     item.Favorite,
				History:      item.History,
				Last:         item.Last,
				Page:         item.Page,
				Chapter:      item.Chapter,
				ChapterCount: item.ChapterCount,
			}
			if err := database.DB.Create(&comic).Error; err != nil {
				log.Printf("创建漫画失败 (user_id=%d, source=%d, cid=%s): %v", userID, item.Source, item.Cid, err)
				continue
			}
			synced++
		}
	}

	serverTime := time.Now().UnixMilli()

	// 如果客户端只想推送变更（push_only），不返回全量数据
	if req.PushOnly {
		c.JSON(http.StatusOK, models.ComicSyncResponse{
			Synced:     synced,
			Skipped:    skipped,
			Message:    "同步完成",
			ServerTime: serverTime,
		})
		return
	}

	// 全量模式：返回所有漫画 + 删除记录（兼容旧客户端）
	var comics []models.Comic
	database.DB.Where("user_id = ?", userID).Order("updated_at DESC").Find(&comics)

	var comicDeletes []models.ComicDelete
	database.DB.Where("user_id = ?", userID).Find(&comicDeletes)
	deletes := make([]models.ComicDeleteItem, len(comicDeletes))
	for i, d := range comicDeletes {
		deletes[i] = models.ComicDeleteItem{
			Source:    d.Source,
			Cid:       d.Cid,
			DeleteFav: d.DeleteFav,
			DeleteHis: d.DeleteHis,
		}
	}

	c.JSON(http.StatusOK, models.ComicSyncResponse{
		Synced:     synced,
		Skipped:    skipped,
		Message:    "同步完成",
		ServerTime: serverTime,
		Comics:     comics,
		Deletes:    deletes,
	})
}

// recordDelete 记录删除操作，用于多端传播
func (h *ComicHandler) recordDelete(userID uint, source int, cid string, deleteFav, deleteHis bool) {
	var existing models.ComicDelete
	result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
		userID, source, cid).Limit(1).Find(&existing)

	if result.RowsAffected > 0 {
		// 更新已有记录
		if deleteFav {
			existing.DeleteFav = true
		}
		if deleteHis {
			existing.DeleteHis = true
		}
		database.DB.Save(&existing)
	} else {
		// 新建记录
		database.DB.Create(&models.ComicDelete{
			UserID:    userID,
			Source:    source,
			Cid:       cid,
			DeleteFav: deleteFav,
			DeleteHis: deleteHis,
		})
	}
}

// Delete removes a specific comic sync record.
func (h *ComicHandler) Delete(c *gin.Context) {
	userID := c.GetUint("user_id")
	idStr := c.Param("id")
	id, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的ID"})
		return
	}

	result := database.DB.Where("id = ? AND user_id = ?", id, userID).Delete(&models.Comic{})
	if result.RowsAffected == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "记录不存在"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}

// SyncStatus returns the current server time and comic count for this user.
func (h *ComicHandler) SyncStatus(c *gin.Context) {
	userID := c.GetUint("user_id")

	var count int64
	database.DB.Model(&models.Comic{}).Where("user_id = ?", userID).Count(&count)

	c.JSON(http.StatusOK, models.SyncStatus{
		ServerTime: time.Now().UnixMilli(),
		ComicCount: int(count),
	})
}

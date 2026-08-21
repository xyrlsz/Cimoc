package handlers

import (
	"log"
	"net/http"
	"strconv"
	"time"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"
	"xcimoc-data-server/query"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type ComicHandler struct{}

func NewComicHandler() *ComicHandler { return &ComicHandler{} }

// List 返回当前用户的漫画列表；支持 ?since=millis 做增量拉取。
// 所有过滤条件（user_id / updated_at / 排序）统一通过 gorm.io/gen 生成的字段表达，
// 避免在代码中出现 "user_id = ?"、"updated_at > ?" 这类字符串 WHERE。
func (h *ComicHandler) List(c *gin.Context) {
	userID := c.GetUint("user_id")

	cm := query.Comic
	q := cm.Where(cm.UserID.Eq(userID))

	if sinceStr := c.Query("since"); sinceStr != "" {
		if sinceMillis, err := strconv.ParseInt(sinceStr, 10, 64); err == nil && sinceMillis > 0 {
			sinceTime := time.Unix(0, sinceMillis*int64(time.Millisecond))
			q = q.Where(cm.UpdatedAt.Gt(sinceTime))
		}
	}

	rows, err := q.Order(cm.UpdatedAt.Desc()).Find()
	if err != nil {
		log.Printf("获取漫画列表失败 (user_id=%d): %v", userID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "获取漫画列表失败"})
		return
	}
	comics := make([]models.Comic, 0, len(rows))
	for _, r := range rows {
		if r != nil {
			comics = append(comics, *r)
		}
	}

	serverTime := time.Now().UnixMilli()

	var deletes []models.ComicDeleteItem
	if c.Query("since") != "" {
		cd := query.ComicDelete
		dq := cd.Where(cd.UserID.Eq(userID))
		if sinceMillis, err := strconv.ParseInt(c.Query("since"), 10, 64); err == nil && sinceMillis > 0 {
			sinceTime := time.Unix(0, sinceMillis*int64(time.Millisecond))
			dq = dq.Where(cd.CreatedAt.Gt(sinceTime))
		}
		delRows, dErr := dq.Find()
		if dErr == nil {
			deletes = make([]models.ComicDeleteItem, 0, len(delRows))
			for _, d := range delRows {
				if d == nil {
					continue
				}
				deletes = append(deletes, models.ComicDeleteItem{
					Source:    d.Source,
					Cid:       d.Cid,
					DeleteFav: d.DeleteFav,
					DeleteHis: d.DeleteHis,
				})
			}
		}
	}

	c.JSON(http.StatusOK, models.ComicListResponse{
		Comics:     comics,
		Deletes:    deletes,
		ServerTime: serverTime,
	})
}

// Sync 按 (user_id, source, cid) 复合匹配合并客户端上传的漫画状态。
// 这里的比较/写入/删表操作都通过生成的 query 字段执行，保证字段名/类型在编译期一致。
func (h *ComicHandler) Sync(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.ComicSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	synced := 0
	skipped := 0

	cm := query.Comic
	cd := query.ComicDelete

	for _, item := range req.Comics {
		if item.Cid == "" {
			continue
		}

		// 1) 查询多端删除墓碑（按 (user_id, source, cid) 唯一键）
		delRecord, delErr := cd.Where(
			cd.UserID.Eq(userID),
			cd.Source.Eq(item.Source),
			cd.Cid.Eq(item.Cid),
		).Take()

		favResolved := false
		hisResolved := false

		if delErr == nil && delRecord != nil {
			if delRecord.DeleteFav && item.Favorite != nil && !item.ClearFavorite {
				item.Favorite = nil
				item.ClearFavorite = true
				log.Printf("多端冲突: 漫画 %d:%s 的收藏被其他设备删除，拒绝恢复", item.Source, item.Cid)
			}
			if delRecord.DeleteHis && item.History != nil && !item.ClearHistory {
				item.History = nil
				item.ClearHistory = true
				log.Printf("多端冲突: 漫画 %d:%s 的历史被其他设备删除，拒绝恢复", item.Source, item.Cid)
			}

			if delRecord.DeleteFav && item.ClearFavorite {
				delRecord.DeleteFav = false
				favResolved = true
			}
			if delRecord.DeleteHis && item.ClearHistory {
				delRecord.DeleteHis = false
				hisResolved = true
			}
			if !delRecord.DeleteFav && !delRecord.DeleteHis {
				// Save 语义在 gorm.io/gen 中等价于 ON CONFLICT UPDATE ALL，
				// 这里明确用 SELECT 后 Save，保持与原 GORM Save 的主键落库语义一致：
				// 记录已存在且主键已知，直接写回。
				if err := database.DB.Save(delRecord).Error; err != nil {
					log.Printf("清理漫画墓碑失败: %v", err)
				}
				// 原逻辑：当两个标志都清掉后整条删除。Save 成功后再显式删。
				if _, err := cd.Where(
					cd.ID.Eq(delRecord.ID),
				).Delete(); err != nil {
					log.Printf("删除空墓碑失败: %v", err)
				}
			} else {
				if err := database.DB.Save(delRecord).Error; err != nil {
					log.Printf("更新漫画墓碑失败: %v", err)
				}
			}
		}

		// 2) 查找现有漫画记录
		existing, findErr := cm.Where(
			cm.UserID.Eq(userID),
			cm.Source.Eq(item.Source),
			cm.Cid.Eq(item.Cid),
		).Take()

		if findErr == nil && existing != nil {
			needsUpdate := false

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
			if item.Favorite != nil && (existing.Favorite == nil || *item.Favorite > *existing.Favorite) {
				existing.Favorite = item.Favorite
				needsUpdate = true
			}
			if item.ClearHistory {
				if existing.History != nil {
					existing.History = nil
					needsUpdate = true
				}
				if !hisResolved {
					h.recordDelete(userID, item.Source, item.Cid, false, true)
				}
			}
			if item.ClearFavorite {
				if existing.Favorite != nil {
					existing.Favorite = nil
					needsUpdate = true
				}
				if !favResolved {
					h.recordDelete(userID, item.Source, item.Cid, true, false)
				}
			}

			if needsUpdate {
				// gen 的 Save(...) 会走 ON CONFLICT(主键) UPDATE ALL，这里直接复用 GORM Save
				// 避免额外的冲突列配置（Comic 的唯一键是复合唯一索引，非主键）。
				if err := database.DB.Save(existing).Error; err != nil {
					log.Printf("更新漫画失败 (user_id=%d, source=%d, cid=%s): %v",
						userID, item.Source, item.Cid, err)
					continue
				}
				synced++
			} else {
				skipped++
			}
			continue
		} else if findErr != nil && !isNotFound(findErr) {
			log.Printf("查询漫画失败 (user_id=%d, source=%d, cid=%s): %v",
				userID, item.Source, item.Cid, findErr)
			continue
		}

		// 3) 新建分支
		if item.Favorite == nil && item.History == nil {
			if item.ClearHistory && !hisResolved {
				h.recordDelete(userID, item.Source, item.Cid, false, true)
			}
			if item.ClearFavorite && !favResolved {
				h.recordDelete(userID, item.Source, item.Cid, true, false)
			}
			skipped++
			continue
		}

		newComic := &models.Comic{
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
		if err := cm.Create(newComic); err != nil {
			log.Printf("创建漫画失败 (user_id=%d, source=%d, cid=%s): %v",
				userID, item.Source, item.Cid, err)
			continue
		}
		synced++
	}

	serverTime := time.Now().UnixMilli()
	if req.PushOnly {
		c.JSON(http.StatusOK, models.ComicSyncResponse{
			Synced:     synced,
			Skipped:    skipped,
			Message:    "同步完成",
			ServerTime: serverTime,
		})
		return
	}

	cmRows, err := cm.Where(cm.UserID.Eq(userID)).Order(cm.UpdatedAt.Desc()).Find()
	if err != nil {
		log.Printf("全量读取漫画失败 (user_id=%d): %v", userID, err)
	}
	comics := make([]models.Comic, 0, len(cmRows))
	for _, r := range cmRows {
		if r != nil {
			comics = append(comics, *r)
		}
	}
	cdRows, dErr := query.ComicDelete.Where(query.ComicDelete.UserID.Eq(userID)).Find()
	deletes := []models.ComicDeleteItem{}
	if dErr == nil {
		deletes = make([]models.ComicDeleteItem, 0, len(cdRows))
		for _, d := range cdRows {
			if d == nil {
				continue
			}
			deletes = append(deletes, models.ComicDeleteItem{
				Source:    d.Source,
				Cid:       d.Cid,
				DeleteFav: d.DeleteFav,
				DeleteHis: d.DeleteHis,
			})
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

// recordDelete 按 (user_id, source, cid) 更新或创建 ComicDelete 墓碑。
func (h *ComicHandler) recordDelete(userID uint, source int, cid string, deleteFav, deleteHis bool) {
	cd := query.ComicDelete
	existing, err := cd.Where(
		cd.UserID.Eq(userID),
		cd.Source.Eq(source),
		cd.Cid.Eq(cid),
	).Take()
	if err == nil && existing != nil {
		changed := false
		if deleteFav && !existing.DeleteFav {
			existing.DeleteFav = true
			changed = true
		}
		if deleteHis && !existing.DeleteHis {
			existing.DeleteHis = true
			changed = true
		}
		if changed {
			// 复合唯一键不是主键，走 GORM Save 最稳。
			_ = database.DB.Save(existing).Error
		}
		return
	}
	if err != nil && !isNotFound(err) {
		log.Printf("查询删除墓碑失败: %v", err)
		return
	}
	if createErr := cd.Create(&models.ComicDelete{
		UserID:    userID,
		Source:    source,
		Cid:       cid,
		DeleteFav: deleteFav,
		DeleteHis: deleteHis,
	}); createErr != nil {
		log.Printf("创建删除墓碑失败: %v", createErr)
	}
}

// Delete 按漫画主键删除（仅允许本人）。
func (h *ComicHandler) Delete(c *gin.Context) {
	userID := c.GetUint("user_id")
	idStr := c.Param("id")
	id, err := strconv.ParseUint(idStr, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "无效的ID"})
		return
	}
	cm := query.Comic
	res, err := cm.Where(cm.ID.Eq(uint(id)), cm.UserID.Eq(userID)).Delete()
	if err != nil {
		log.Printf("删除漫画失败 (id=%s): %v", idStr, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "删除失败"})
		return
	}
	if res.RowsAffected == 0 {
		c.JSON(http.StatusNotFound, gin.H{"error": "记录不存在"})
		return
	}
	c.JSON(http.StatusOK, gin.H{"message": "删除成功"})
}

// SyncStatus 轻量接口：返回服务器时间与用户漫画总数。
func (h *ComicHandler) SyncStatus(c *gin.Context) {
	userID := c.GetUint("user_id")

	count, err := query.Comic.Where(query.Comic.UserID.Eq(userID)).Count()
	if err != nil {
		log.Printf("统计漫画数失败 (user_id=%d): %v", userID, err)
		count = 0
	}

	c.JSON(http.StatusOK, models.SyncStatus{
		ServerTime: time.Now().UnixMilli(),
		ComicCount: int(count),
	})
}

// isNotFound helper：同时兼容 gorm.ErrRecordNotFound 以及 gen 的 Take/First 找不到的情形。
func isNotFound(err error) bool {
	if err == nil {
		return false
	}
	return err == gorm.ErrRecordNotFound || err.Error() == gorm.ErrRecordNotFound.Error()
}

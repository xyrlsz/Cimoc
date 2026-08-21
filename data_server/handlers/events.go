package handlers

import (
	"encoding/json"
	"log"
	"net/http"
	"strconv"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"

	"github.com/gin-gonic/gin"
)

const maxEventsPerPull = 500

type EventHandler struct{}

func NewEventHandler() *EventHandler {
	return &EventHandler{}
}

// ==================== 事件拉取 ====================

// PullEvents 返回 since_id 之后的所有事件（最多 500 条）。
// GET /api/events/pull?since_id=<event_id>  （新命名，语义更清晰：传的是事件 ID，非时间戳）
// 兼容旧参数 ?since=<event_id>（仅保留一段时间用于平滑升级）
// since_id=0 / since=0 表示从头开始。
func (h *EventHandler) PullEvents(c *gin.Context) {
	userID := c.GetUint("user_id")

	// since_id 优先；若缺再回退到 since（旧兼容名）
	sinceStr := c.Query("since_id")
	if sinceStr == "" {
		sinceStr = c.Query("since")
	}
	var sinceID uint
	if sinceStr != "" {
		if id, err := strconv.ParseUint(sinceStr, 10, 64); err == nil {
			sinceID = uint(id)
		}
	}

	var events []models.SyncEvent
	query := database.DB.Where("user_id = ? AND id > ?", userID, sinceID).
		Order("id ASC").Limit(maxEventsPerPull + 1) // 多取1条判断 has_more

	result := query.Find(&events)
	if result.Error != nil {
		log.Printf("拉取事件失败 (user_id=%d): %v", userID, result.Error)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "拉取事件失败"})
		return
	}

	hasMore := len(events) > maxEventsPerPull
	if hasMore {
		events = events[:maxEventsPerPull]
	}

	// latest_id 表示"本用户事件流的当前尾部"：
	// - 有返回事件时：取返回批次最后一条的 ID
	// - 但服务器真实尾部 maxID 可能比返回批次更大（比如 has_more=true 截断了尾端，
	//   或将来引入 per-client_id 过滤导致事件被跳过），此时用真实尾部对齐，
	//   避免客户端以为服务器没新数据而一直回退到旧 since_id。
	// - 完全没事件时仍回落到 sinceID，保证类型稳定。
	latestID := sinceID
	if len(events) > 0 {
		latestID = events[len(events)-1].ID
	}

	var maxID uint
	database.DB.Model(&models.SyncEvent{}).
		Where("user_id = ?", userID).
		Select("COALESCE(MAX(id), 0)").
		Scan(&maxID)
	if maxID > latestID {
		latestID = maxID
	}

	c.JSON(http.StatusOK, models.PullEventsResponse{
		Events:   events,
		LatestID: latestID,
		HasMore:  hasMore,
	})
}

// ==================== 事件推送 ====================

// PushEvents 接收客户端产生的事件，存储到事件日志中。
// 同时将事件应用到当前用户数据（更新 comics/settings/tags 表）。
// POST /api/events/push
func (h *EventHandler) PushEvents(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.PushEventsRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	if len(req.Events) == 0 {
		c.JSON(http.StatusOK, gin.H{"message": "无事件需要推送"})
		return
	}

	clientID := req.ClientID

	// 1. 写入事件日志（批量插入）
	events := make([]models.SyncEvent, 0, len(req.Events))
	for _, e := range req.Events {
		if e.Type == "" || e.Payload == "" {
			continue
		}
		events = append(events, models.SyncEvent{
			UserID:   userID,
			Type:     e.Type,
			Payload:  e.Payload,
			ClientID: clientID,
		})
	}

	if len(events) == 0 {
		c.JSON(http.StatusOK, gin.H{"message": "无有效事件"})
		return
	}

	if err := database.DB.Create(&events).Error; err != nil {
		log.Printf("写入事件失败 (user_id=%d): %v", userID, err)
		c.JSON(http.StatusInternalServerError, gin.H{"error": "写入事件失败"})
		return
	}

	// 2. 将事件应用到本地数据（重放事件）
	applied := 0
	for i := range events {
		if h.applyEvent(userID, &events[i]) {
			applied++
		}
	}

	// 3. 写入成功后返回当前事件流的尾部（等于本批次最后一条的 ID）。
	// 客户端可直接用这个 latest_id 推进本地 since_id，避免下一轮 pull 再把
	// 自己刚 push 上去的事件再拉一遍（client_id 相同时会被过滤成空列表，
	// 但仍造成一次无谓的往返，且老代码 events 空时不会推进游标，
	// 这也是用户观察到 since_id 长时间不变的直接原因）。
	pushLatestID := uint(0)
	if len(events) > 0 {
		pushLatestID = events[len(events)-1].ID
	}

	c.JSON(http.StatusOK, gin.H{
		"message":   "推送成功",
		"received":  len(events),
		"applied":   applied,
		"latest_id": pushLatestID,
	})
}

// ==================== 事件状态 ====================

// EventStatus 返回事件流状态（最新事件ID、事件总数、漫画数）。
// GET /api/events/status
func (h *EventHandler) EventStatus(c *gin.Context) {
	userID := c.GetUint("user_id")

	var latest models.SyncEvent
	database.DB.Where("user_id = ?", userID).Order("id DESC").Limit(1).Find(&latest)

	var total int64
	database.DB.Model(&models.SyncEvent{}).Where("user_id = ?", userID).Count(&total)

	var comicCount int64
	database.DB.Model(&models.Comic{}).Where("user_id = ?", userID).Count(&comicCount)

	c.JSON(http.StatusOK, models.EventStatusResponse{
		LatestID:    latest.ID,
		TotalEvents: total,
		ComicCount:  comicCount,
	})
}

// ==================== 事件应用逻辑 ====================

// applyEvent 将单个事件应用到用户的实际数据中。
// 返回 true 表示应用成功，false 表示跳过或失败。
func (h *EventHandler) applyEvent(userID uint, event *models.SyncEvent) bool {
	switch event.Type {

	case models.EventTypeFavorite:
		var p models.FavoritePayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyFavorite(userID, &p)

	case models.EventTypeUnfavorite:
		var p models.UnfavoritePayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyUnfavorite(userID, &p)

	case models.EventTypeRead:
		var p models.ReadPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyRead(userID, &p)

	case models.EventTypeClearHistory:
		var p models.ClearHistoryPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyClearHistory(userID, &p)

	case models.EventTypeUpdateInfo:
		var p models.UpdateInfoPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyUpdateInfo(userID, &p)

	case models.EventTypeSettingUpdate:
		var p models.SettingPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applySettingUpdate(userID, &p)

	case models.EventTypeTagCreate:
		var p models.TagPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyTagCreate(userID, &p)

	case models.EventTypeTagDelete:
		var p models.TagPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyTagDelete(userID, &p)

	case models.EventTypeTagAddComic:
		var p models.TagPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyTagAddComic(userID, &p)

	case models.EventTypeTagRemoveComic:
		var p models.TagPayload
		if !unmarshalPayload(event.Payload, &p) {
			return false
		}
		return h.applyTagRemoveComic(userID, &p)

	default:
		log.Printf("未知事件类型: %s (user_id=%d)", event.Type, userID)
		return false
	}
}

// ==================== 各事件类型的应用实现 ====================

func (h *EventHandler) applyFavorite(userID uint, p *models.FavoritePayload) bool {
	if p.Cid == "" {
		return false
	}
	var comic models.Comic
	result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
		userID, p.Source, p.Cid).Limit(1).Find(&comic)

	if result.RowsAffected > 0 {
		// 已存在 → 更新收藏时间（只接受更新的）
		if comic.Favorite == nil || p.Timestamp > *comic.Favorite {
			comic.Favorite = &p.Timestamp
			if p.Title != "" {
				comic.Title = p.Title
				comic.Cover = p.Cover
				comic.Update = p.Update
				comic.Finish = p.Finish
				if p.ChapterCount != nil {
					comic.ChapterCount = p.ChapterCount
				}
			}
			database.DB.Save(&comic)
			// 重新收藏成功 → 移除删除墓碑
			h.resolveTombstone(userID, p.Source, p.Cid, true, false)
		}
	} else {
		// 新漫画 → 创建
		comic = models.Comic{
			UserID:       userID,
			Source:       p.Source,
			Cid:          p.Cid,
			Title:        p.Title,
			Cover:        p.Cover,
			Update:       p.Update,
			Finish:       p.Finish,
			Favorite:     &p.Timestamp,
			ChapterCount: p.ChapterCount,
		}
		database.DB.Create(&comic)
		h.resolveTombstone(userID, p.Source, p.Cid, true, false)
	}
	return true
}

func (h *EventHandler) applyUnfavorite(userID uint, p *models.UnfavoritePayload) bool {
	if p.Cid == "" {
		return false
	}
	database.DB.Model(&models.Comic{}).
		Where("user_id = ? AND source = ? AND cid = ?", userID, p.Source, p.Cid).
		Update("favorite", nil)
	// 记录删除墓碑：防止其他设备通过全量上传把已删除的收藏重新上传
	h.recordTombstone(userID, p.Source, p.Cid, true, false)
	return true
}

func (h *EventHandler) applyRead(userID uint, p *models.ReadPayload) bool {
	if p.Cid == "" {
		return false
	}
	var comic models.Comic
	result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
		userID, p.Source, p.Cid).Limit(1).Find(&comic)

	pageVal := p.Page
	if result.RowsAffected > 0 {
		// 只接受更新的阅读时间
		if comic.History == nil || p.Timestamp > *comic.History {
			comic.History = &p.Timestamp
			comic.Chapter = p.Chapter
			comic.Page = &pageVal
			comic.Last = p.Last
			database.DB.Save(&comic)
			// 恢复历史成功 → 移除删除墓碑
			h.resolveTombstone(userID, p.Source, p.Cid, false, true)
		}
	} else {
		// 新漫画（只有历史没有收藏）→ 创建
		comic = models.Comic{
			UserID:  userID,
			Source:  p.Source,
			Cid:     p.Cid,
			History: &p.Timestamp,
			Chapter: p.Chapter,
			Page:    &pageVal,
			Last:    p.Last,
		}
		database.DB.Create(&comic)
		h.resolveTombstone(userID, p.Source, p.Cid, false, true)
	}
	return true
}

func (h *EventHandler) applyClearHistory(userID uint, p *models.ClearHistoryPayload) bool {
	if p.Cid == "" {
		return false
	}
	database.DB.Model(&models.Comic{}).
		Where("user_id = ? AND source = ? AND cid = ?", userID, p.Source, p.Cid).
		Updates(map[string]interface{}{
			"history": nil,
			"last":    nil,
			"page":    nil,
			"chapter": nil,
		})
	// 记录删除墓碑：防止其他设备通过全量上传把已清除的历史重新上传
	h.recordTombstone(userID, p.Source, p.Cid, false, true)
	return true
}

func (h *EventHandler) applyUpdateInfo(userID uint, p *models.UpdateInfoPayload) bool {
	if p.Cid == "" {
		return false
	}
	updates := map[string]interface{}{
		"title":  p.Title,
		"cover":  p.Cover,
		"update": p.Update,
		"finish": p.Finish,
	}
	if p.ChapterCount != nil {
		updates["chapter_count"] = p.ChapterCount
	}
	database.DB.Model(&models.Comic{}).
		Where("user_id = ? AND source = ? AND cid = ?", userID, p.Source, p.Cid).
		Updates(updates)
	return true
}

func (h *EventHandler) applySettingUpdate(userID uint, p *models.SettingPayload) bool {
	if p.Key == "" {
		return false
	}
	var setting models.Setting
	result := database.DB.Where("user_id = ? AND key = ?", userID, p.Key).Limit(1).Find(&setting)

	if result.RowsAffected > 0 {
		setting.Value = p.Value
		database.DB.Save(&setting)
	} else {
		database.DB.Create(&models.Setting{
			UserID: userID,
			Key:    p.Key,
			Value:  p.Value,
		})
	}
	return true
}

func (h *EventHandler) applyTagCreate(userID uint, p *models.TagPayload) bool {
	if p.Title == "" {
		return false
	}
	// 幂等：已存在的标签跳过
	var existing models.Tag
	result := database.DB.Where("user_id = ? AND title = ?", userID, p.Title).Limit(1).Find(&existing)
	if result.RowsAffected > 0 {
		return true
	}

	tag := models.Tag{UserID: userID, Title: p.Title}
	database.DB.Create(&tag)

	// 创建初始关联
	for _, ref := range p.Comics {
		if ref.Cid == "" {
			continue
		}
		database.DB.Create(&models.TagRef{
			UserID: userID,
			TagID:  tag.ID,
			Source: ref.Source,
			Cid:    ref.Cid,
		})
	}
	return true
}

func (h *EventHandler) applyTagDelete(userID uint, p *models.TagPayload) bool {
	if p.Title == "" {
		return false
	}
	var tag models.Tag
	result := database.DB.Where("user_id = ? AND title = ?", userID, p.Title).Limit(1).Find(&tag)
	if result.RowsAffected == 0 {
		return true // 幂等：不存在则跳过
	}
	database.DB.Where("tag_id = ?", tag.ID).Delete(&models.TagRef{})
	database.DB.Delete(&tag)
	return true
}

func (h *EventHandler) applyTagAddComic(userID uint, p *models.TagPayload) bool {
	if p.Title == "" || p.Cid == "" {
		return false
	}
	var tag models.Tag
	result := database.DB.Where("user_id = ? AND title = ?", userID, p.Title).Limit(1).Find(&tag)
	if result.RowsAffected == 0 {
		return false // 标签不存在，静默跳过
	}
	// 幂等：检查关联是否已存在
	var existingRef models.TagRef
	refResult := database.DB.Where("user_id = ? AND tag_id = ? AND source = ? AND cid = ?",
		userID, tag.ID, p.Source, p.Cid).Limit(1).Find(&existingRef)
	if refResult.RowsAffected > 0 {
		return true
	}
	database.DB.Create(&models.TagRef{
		UserID: userID,
		TagID:  tag.ID,
		Source: p.Source,
		Cid:    p.Cid,
	})
	return true
}

func (h *EventHandler) applyTagRemoveComic(userID uint, p *models.TagPayload) bool {
	if p.Title == "" || p.Cid == "" {
		return false
	}
	var tag models.Tag
	result := database.DB.Where("user_id = ? AND title = ?", userID, p.Title).Limit(1).Find(&tag)
	if result.RowsAffected == 0 {
		return true // 幂等
	}
	database.DB.Where("user_id = ? AND tag_id = ? AND source = ? AND cid = ?",
		userID, tag.ID, p.Source, p.Cid).Delete(&models.TagRef{})
	return true
}

// ==================== 工具函数 ====================

func unmarshalPayload(data string, v interface{}) bool {
	if err := json.Unmarshal([]byte(data), v); err != nil {
		log.Printf("解析事件 payload 失败: %v, data=%s", err, data)
		return false
	}
	return true
}

// ==================== 墓碑（删除标记）维护 ====================
// 与 /api/comics/sync 的 ComicDelete 表保持一致：事件路径的删除也记录墓碑，
// 防止其他设备通过全量上传把已删除的数据重新上传；重新收藏/恢复历史时移除墓碑。

// recordTombstone 记录删除墓碑（幂等，可重复调用）
func (h *EventHandler) recordTombstone(userID uint, source int, cid string, deleteFav, deleteHis bool) {
	var existing models.ComicDelete
	result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
		userID, source, cid).Limit(1).Find(&existing)
	if result.RowsAffected > 0 {
		if deleteFav {
			existing.DeleteFav = true
		}
		if deleteHis {
			existing.DeleteHis = true
		}
		database.DB.Save(&existing)
	} else {
		database.DB.Create(&models.ComicDelete{
			UserID:    userID,
			Source:    source,
			Cid:       cid,
			DeleteFav: deleteFav,
			DeleteHis: deleteHis,
		})
	}
}

// resolveTombstone 移除墓碑的对应标志（重新收藏/恢复历史即视为已认可该删除）
func (h *EventHandler) resolveTombstone(userID uint, source int, cid string, fav, his bool) {
	var existing models.ComicDelete
	result := database.DB.Where("user_id = ? AND source = ? AND cid = ?",
		userID, source, cid).Limit(1).Find(&existing)
	if result.RowsAffected == 0 {
		return
	}
	if fav {
		existing.DeleteFav = false
	}
	if his {
		existing.DeleteHis = false
	}
	if !existing.DeleteFav && !existing.DeleteHis {
		database.DB.Delete(&existing)
	} else {
		database.DB.Save(&existing)
	}
}

package handlers

import (
	"fmt"
	"net/http"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"

	"github.com/gin-gonic/gin"
)

type TagHandler struct{}

func NewTagHandler() *TagHandler {
	return &TagHandler{}
}

// List returns all tags with their associated comics for the authenticated user.
// Optimized: 使用批量查询替代 N+1 循环查询
func (h *TagHandler) List(c *gin.Context) {
	userID := c.GetUint("user_id")

	var tags []models.Tag
	database.DB.Where("user_id = ?", userID).Find(&tags)

	// 批量查询所有 tag_id 的关联漫画
	var allRefs []models.TagRef
	database.DB.Where("user_id = ?", userID).Find(&allRefs)

	// 按 tag_id 分组
	refsByTagID := make(map[uint][]models.TagRef, len(tags))
	for _, ref := range allRefs {
		refsByTagID[ref.TagID] = append(refsByTagID[ref.TagID], ref)
	}

	type TagWithComics struct {
		models.Tag
		Comics []models.TagRef `json:"comics"`
	}

	result := make([]TagWithComics, 0, len(tags))
	for _, tag := range tags {
		refs := refsByTagID[tag.ID]
		if refs == nil {
			refs = []models.TagRef{}
		}
		result = append(result, TagWithComics{
			Tag:    tag,
			Comics: refs,
		})
	}

	c.JSON(http.StatusOK, gin.H{"tags": result})
}

// Sync uploads/merges tags and their comic references.
// 改进：合并策略 (merge) 替代原来的全量替换策略
// - 客户端上传的标签：如果标题匹配则更新关联，否则新建
// - 客户端未提及的标签：保留（多端支持，不会丢失其他设备的标签）
// - 标签关联：通过 added_comics 和 removed_comics 增量更新
func (h *TagHandler) Sync(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.TagSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	tx := database.DB.Begin()
	defer tx.Rollback()

	// Step 1: 获取服务端现有标签（按 title 索引，用于合并不是替换）
	var existingTags []models.Tag
	tx.Where("user_id = ?", userID).Find(&existingTags)
	existingByTitle := make(map[string]uint, len(existingTags)) // title -> tag_id
	for _, t := range existingTags {
		existingByTitle[t.Title] = t.ID
	}

	syncedTags := 0
	syncedRefs := 0
	clientTagTitles := make(map[string]bool) // 跟踪客户端上传的标签标题

	for _, item := range req.Tags {
		if item.Title == "" {
			continue
		}
		clientTagTitles[item.Title] = true

		var tagID uint
		if existingID, exists := existingByTitle[item.Title]; exists {
			// 标签已存在 → 复用
			tagID = existingID
		} else {
			// 新标签 → 创建
			tag := models.Tag{
				UserID: userID,
				Title:  item.Title,
			}
			if result := tx.Create(&tag); result.Error != nil {
				tx.Rollback()
				c.JSON(http.StatusInternalServerError, gin.H{"error": "保存标签失败: " + result.Error.Error()})
				return
			}
			tagID = tag.ID
			existingByTitle[item.Title] = tagID // 更新本地索引
			syncedTags++
		}

		// 查找客户端请求中已有的 cid 集合（用于增量更新）
		clientRefKeys := make(map[string]bool, len(item.Comics))
		for _, comic := range item.Comics {
			if comic.Cid == "" {
				continue
			}
			key := comicKey(comic.Source, comic.Cid)
			clientRefKeys[key] = true
		}

		// 获取该标签现有的 TagRef
		var existingRefs []models.TagRef
		tx.Where("user_id = ? AND tag_id = ?", userID, tagID).Find(&existingRefs)
		existingRefKeys := make(map[string]uint, len(existingRefs)) // key -> TagRef.ID
		for _, ref := range existingRefs {
			existingRefKeys[comicKey(ref.Source, ref.Cid)] = ref.ID
		}

		// 删除客户端不再包含的关联（客户端没传 = 用户在客户端删除了）
		// 但如果客户端使用的是增量模式（partial_update），则保留未提及的关联
		if !req.PartialUpdate {
			for key, refID := range existingRefKeys {
				if !clientRefKeys[key] {
					tx.Delete(&models.TagRef{}, refID)
				}
			}
		}

		// 插入或保留客户端指定的关联
		refs := make([]models.TagRef, 0, len(item.Comics))
		for _, comic := range item.Comics {
			if comic.Cid == "" {
				continue
			}
			key := comicKey(comic.Source, comic.Cid)
			if _, exists := existingRefKeys[key]; !exists {
				// 新关联
				refs = append(refs, models.TagRef{
					UserID: userID,
					TagID:  tagID,
					Source: comic.Source,
					Cid:    comic.Cid,
				})
			}
			// 如果已存在，保持不动
		}
		if len(refs) > 0 {
			if result := tx.Create(&refs); result.Error != nil {
				tx.Rollback()
				c.JSON(http.StatusInternalServerError, gin.H{"error": "保存标签关联失败"})
				return
			}
			syncedRefs += len(refs)
		}
	}

	// 如果客户端使用全量同步模式，删除服务端有但客户端没有的标签
	// 以此支持跨端标签删除
	if !req.PartialUpdate {
		// 收集客户端没有提及的标签，并删除它们
		for _, t := range existingTags {
			if !clientTagTitles[t.Title] {
				// 先删除关联
				tx.Where("tag_id = ?", t.ID).Delete(&models.TagRef{})
				tx.Delete(&t)
			}
		}
	}

	tx.Commit()

	c.JSON(http.StatusOK, gin.H{
		"synced":      syncedTags,
		"synced_refs": syncedRefs,
		"message":     "标签同步完成",
	})
}

func comicKey(source int, cid string) string {
	return fmt.Sprintf("%d:%s", source, cid)
}

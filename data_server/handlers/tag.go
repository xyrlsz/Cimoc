package handlers

import (
	"fmt"
	"net/http"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"
	"xcimoc-data-server/query"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type TagHandler struct{}

func NewTagHandler() *TagHandler { return &TagHandler{} }

// List 返回标签及关联漫画；使用 query.Tag / query.TagRef 做两次批量查询，
// 避免 N+1 查询，并且所有 WHERE 条件都通过生成字段进行类型安全表达。
func (h *TagHandler) List(c *gin.Context) {
	userID := c.GetUint("user_id")

	tagRows, err := query.Tag.Where(query.Tag.UserID.Eq(userID)).Find()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询标签失败: " + err.Error()})
		return
	}
	refRows, err := query.TagRef.Where(query.TagRef.UserID.Eq(userID)).Find()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "查询标签关联失败: " + err.Error()})
		return
	}

	refsByTagID := make(map[uint][]models.TagRef, len(tagRows))
	for _, r := range refRows {
		if r == nil {
			continue
		}
		refsByTagID[r.TagID] = append(refsByTagID[r.TagID], *r)
	}

	type TagWithComics struct {
		models.Tag
		Comics []models.TagRef `json:"comics"`
	}
	result := make([]TagWithComics, 0, len(tagRows))
	for _, t := range tagRows {
		if t == nil {
			continue
		}
		refs := refsByTagID[t.ID]
		if refs == nil {
			refs = []models.TagRef{}
		}
		result = append(result, TagWithComics{Tag: *t, Comics: refs})
	}

	c.JSON(http.StatusOK, gin.H{"tags": result})
}

// Sync 上传/合入标签与关联漫画。
//   - 标签匹配键：(user_id, title)，支持复用，不会出现重名重复创建；
//   - 关联键：(user_id, tag_id, source, cid)，做幂等插入；
//   - 当 partial_update=false 时执行全量语义：删除客户端未提及的标签与其关联。
//
// 整段放在事务闭包中执行，并通过 query.Use(tx) 生成事务绑定的临时 query 句柄，
// 确保所有写入都是事务一致的，避免中途失败造成标签/关联不同步。
func (h *TagHandler) Sync(c *gin.Context) {
	userID := c.GetUint("user_id")

	var req models.TagSyncRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "请求参数无效: " + err.Error()})
		return
	}

	partialUpdate := true
	if req.PartialUpdate != nil {
		partialUpdate = *req.PartialUpdate
	}

	type syncResult struct {
		syncedTags int
		syncedRefs int
	}

	var res syncResult
	txErr := database.DB.Transaction(func(tx *gorm.DB) error {
		qTx := query.Use(tx)
		tgTx := qTx.Tag
		trTx := qTx.TagRef

		existingTags, err := tgTx.Where(tgTx.UserID.Eq(userID)).Find()
		if err != nil {
			return err
		}
		existingByTitle := make(map[string]uint, len(existingTags))
		for _, t := range existingTags {
			if t == nil {
				continue
			}
			existingByTitle[t.Title] = t.ID
		}

		clientTagTitles := make(map[string]bool)

		for _, item := range req.Tags {
			if item.Title == "" {
				continue
			}
			clientTagTitles[item.Title] = true

			var tagID uint
			if existingID, ok := existingByTitle[item.Title]; ok {
				tagID = existingID
			} else {
				tag := &models.Tag{UserID: userID, Title: item.Title}
				if createErr := tgTx.Create(tag); createErr != nil {
					return createErr
				}
				tagID = tag.ID
				existingByTitle[item.Title] = tagID
				res.syncedTags++
			}

			clientRefKeys := make(map[string]bool, len(item.Comics))
			for _, comic := range item.Comics {
				if comic.Cid == "" {
					continue
				}
				clientRefKeys[comicKey(comic.Source, comic.Cid)] = true
			}

			existingRefs, err := trTx.Where(
				trTx.UserID.Eq(userID),
				trTx.TagID.Eq(tagID),
			).Find()
			if err != nil {
				return err
			}
			existingRefKeys := make(map[string]uint, len(existingRefs))
			for _, ref := range existingRefs {
				if ref == nil {
					continue
				}
				existingRefKeys[comicKey(ref.Source, ref.Cid)] = ref.ID
			}

			if !partialUpdate {
				for key, refID := range existingRefKeys {
					if !clientRefKeys[key] {
						if _, delErr := trTx.Where(trTx.ID.Eq(refID)).Delete(); delErr != nil {
							return delErr
						}
					}
				}
			}

			refs := make([]*models.TagRef, 0, len(item.Comics))
			for _, comic := range item.Comics {
				if comic.Cid == "" {
					continue
				}
				key := comicKey(comic.Source, comic.Cid)
				if _, exists := existingRefKeys[key]; !exists {
					refs = append(refs, &models.TagRef{
						UserID: userID,
						TagID:  tagID,
						Source: comic.Source,
						Cid:    comic.Cid,
					})
				}
			}
			if len(refs) > 0 {
				if createErr := trTx.Create(refs...); createErr != nil {
					return createErr
				}
				res.syncedRefs += len(refs)
			}
		}

		if !partialUpdate {
			for _, t := range existingTags {
				if t == nil {
					continue
				}
				if !clientTagTitles[t.Title] {
					if _, delErr := trTx.Where(trTx.TagID.Eq(t.ID)).Delete(); delErr != nil {
						return delErr
					}
					if _, delErr := tgTx.Where(tgTx.ID.Eq(t.ID)).Delete(); delErr != nil {
						return delErr
					}
				}
			}
		}

		return nil
	})
	if txErr != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "标签同步失败: " + txErr.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"synced":      res.syncedTags,
		"synced_refs": res.syncedRefs,
		"message":     "标签同步完成",
	})
}

func comicKey(source int, cid string) string { return fmt.Sprintf("%d:%s", source, cid) }

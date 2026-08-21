package models

import "time"

// Tag represents a tag/label.
type Tag struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	UserID    uint      `gorm:"index;not null" json:"user_id"`
	Title     string    `gorm:"size:128;not null" json:"title"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// TagRef links a tag to a comic (by source+cid).
type TagRef struct {
	ID     uint   `gorm:"primaryKey" json:"id"`
	UserID uint   `gorm:"index;not null;uniqueIndex:idx_user_tag_source_cid" json:"user_id"`
	TagID  uint   `gorm:"index;not null;uniqueIndex:idx_user_tag_source_cid" json:"tag_id"`
	Source int    `gorm:"not null;uniqueIndex:idx_user_tag_source_cid" json:"source"`
	Cid    string `gorm:"size:256;not null;uniqueIndex:idx_user_tag_source_cid" json:"cid"`
}

// TagSyncRequest is the payload for uploading/merging tags with their comic references.
// Tags 不使用 binding:"required"：允许上传空数组；PartialUpdate 默认 true，
// 客户端未传 partial_update 时按「增量同步」处理，避免误删服务端已存在的其它标签。
type TagSyncRequest struct {
	Tags          []TagSyncItem `json:"tags"`
	PartialUpdate *bool         `json:"partial_update"` // 非空 bool；nil = true（默认增量）
}

type TagSyncItem struct {
	Title  string        `json:"title"`
	Comics []TagComicRef `json:"comics"`
}

type TagComicRef struct {
	Source int    `json:"source"`
	Cid    string `json:"cid"`
}

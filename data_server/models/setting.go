package models

import "time"

// Setting stores app preference key-value pairs.
type Setting struct {
	ID        uint      `gorm:"primaryKey" json:"id"`
	UserID    uint      `gorm:"index;not null;uniqueIndex:idx_user_key" json:"user_id"`
	Key       string    `gorm:"size:128;not null;uniqueIndex:idx_user_key" json:"key"`
	Value     string    `gorm:"size:4096" json:"value"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
}

// SettingSyncRequest is the payload for uploading/merging settings.
// Settings 不使用 binding:"required"：允许空数组（客户端无变更时也能安全同步而不会 400）。
type SettingSyncRequest struct {
	Settings []SettingItem `json:"settings"`
}

type SettingItem struct {
	Key   string `json:"key"`
	Value string `json:"value"`
}

package main

import (
	"log"
	"time"

	"xcimoc-data-server/query"
)

const eventRetentionDays = 30
const maintenanceInterval = 24 * time.Hour

// runMaintenance 定期清理过期事件；WHERE 条件使用生成的 query.SyncEvent.CreatedAt.Lt(cutoff)，
// 不再手写字符串条件，字段重命名/类型变化都会在编译期暴露。
func runMaintenance() {
	for {
		time.Sleep(maintenanceInterval)

		cutoff := time.Now().AddDate(0, 0, -eventRetentionDays)
		res, err := query.SyncEvent.Where(query.SyncEvent.CreatedAt.Lt(cutoff)).Delete()
		if err != nil {
			log.Printf("清理过期事件失败: %v", err)
			continue
		}
		if res.RowsAffected > 0 {
			log.Printf("已清理 %d 条超过 %d 天的过期事件", res.RowsAffected, eventRetentionDays)
		}
	}
}

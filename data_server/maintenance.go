package main

import (
	"log"
	"time"

	"xcimoc-data-server/database"
	"xcimoc-data-server/models"
)

// eventRetentionDays 事件日志保留天数。
// 超过该时间的事件会被定期清理，避免 SyncEvent 表无限增长。
// 客户端每次前台全量同步都会做"全量对账"（拉取完整 Comic 表），
// 即使清理了历史事件，长期离线的设备也能通过对账恢复完整数据，
// 因此清理旧事件是安全的。
const eventRetentionDays = 30

// maintenanceInterval 维护任务执行间隔
const maintenanceInterval = 24 * time.Hour

// runMaintenance 启动后台维护任务（事件日志清理等）。
// 在 main() 中作为 goroutine 启动，进程生命周期内持续运行。
func runMaintenance() {
	for {
		time.Sleep(maintenanceInterval)

		cutoff := time.Now().AddDate(0, 0, -eventRetentionDays)
		result := database.DB.Where("created_at < ?", cutoff).Delete(&models.SyncEvent{})
		if result.Error != nil {
			log.Printf("清理过期事件失败: %v", result.Error)
		} else if result.RowsAffected > 0 {
			log.Printf("已清理 %d 条超过 %d 天的过期事件", result.RowsAffected, eventRetentionDays)
		}
	}
}

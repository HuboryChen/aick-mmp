package com.aick.mmp.central.task;

import com.aick.mmp.central.service.CdnNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * CDN节点健康检查定时任务
 * 
 * 职责：
 * 1. 每30秒检查所有CDN节点的心跳超时
 * 2. 每分钟执行一次节点连通性测试
 * 3. 更新节点状态
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CdnNodeHealthCheckTask {

    private final CdnNodeService cdnNodeService;

    /**
     * 心跳超时检查（每30秒）
     */
    @Scheduled(fixedRate = 30000)
    public void checkHeartbeatTimeout() {
        log.debug("Running CDN node heartbeat timeout check");
        try {
            cdnNodeService.checkHeartbeatTimeout();
        } catch (Exception e) {
            log.error("Error checking CDN node heartbeat timeout: {}", e.getMessage(), e);
        }
    }

    /**
     * 节点连通性测试（每分钟）
     */
    @Scheduled(fixedRate = 60000)
    public void performHealthCheck() {
        log.debug("Running CDN node connectivity check");
        long startTime = System.currentTimeMillis();
        
        try {
            Map<Long, ?> results = cdnNodeService.batchHealthCheck();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("CDN node health check completed: {} nodes checked in {}ms", 
                    results.size(), duration);
            
            // 统计健康状态
            long healthy = results.values().stream()
                    .filter(r -> {
                        try {
                            return r.getClass().getMethod("isSuccess").invoke(r).equals(true);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .count();
            
            log.info("CDN node health status: {}/{} healthy", healthy, results.size());
            
        } catch (Exception e) {
            log.error("Error performing CDN node health check: {}", e.getMessage(), e);
        }
    }

    /**
     * 清理过期负载历史数据（每天凌晨3点）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldLoadHistory() {
        log.info("Starting cleanup of old CDN node load history");
        // TODO: 实现历史数据清理逻辑
        // 可配置保留天数，默认保留30天
        log.info("Cleanup of old CDN node load history completed");
    }

    /**
     * 统计节点运行状态（每5分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void logNodeStatistics() {
        try {
            Map<String, Object> stats = cdnNodeService.getGlobalCdnStats();
            
            log.info("CDN Node Statistics - Total: {}, Online: {}, Offline: {}, Health Rate: {:.2f}%",
                    stats.get("totalNodes"),
                    stats.get("onlineNodes"),
                    stats.get("offlineNodes"),
                    stats.get("healthRate"));
            
            log.info("CDN Capacity - Total: {}, Load: {}, Load Percentage: {:.2f}%",
                    stats.get("totalCapacity"),
                    stats.get("totalLoad"),
                    stats.get("overallLoadPercentage"));
        } catch (Exception e) {
            log.error("Error logging CDN node statistics: {}", e.getMessage(), e);
        }
    }
}

package com.aick.mmp.central.config;

import com.aick.mmp.central.service.EdgeNodeFailoverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 待分配池定时处理调度器
 * 定期尝试将待分配池中的摄像头分配到可用的在线节点
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "edge.failover.enabled", havingValue = "true", matchIfMissing = true)
public class PendingAllocationScheduler {

    private final EdgeNodeFailoverService edgeNodeFailoverService;

    /**
     * 按配置的间隔定期处理待分配池
     */
    @Scheduled(fixedDelayString = "${edge.failover.retry-interval-seconds:300}000",
               initialDelayString = "${edge.failover.retry-interval-seconds:300}000")
    public void processPendingAllocationPool() {
        try {
            int allocatedCount = edgeNodeFailoverService.processPendingAllocationPool();
            if (allocatedCount > 0) {
                log.info("[待分配池调度] 成功分配 {} 个摄像头", allocatedCount);
            }
        } catch (Exception e) {
            log.error("[待分配池调度] 处理失败: {}", e.getMessage(), e);
        }
    }
}

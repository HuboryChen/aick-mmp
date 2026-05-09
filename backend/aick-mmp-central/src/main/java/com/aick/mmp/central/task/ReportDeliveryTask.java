package com.aick.mmp.central.task;

import com.aick.mmp.central.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 报表发送定时任务
 * 定期检查并发送订阅报表
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportDeliveryTask {

    private final ReportService reportService;

    /**
     * 每15分钟检查并发送到期订阅报表
     */
    @Scheduled(fixedRate = 900000) // 15分钟
    public void sendDueReports() {
        try {
            log.info("Starting scheduled report delivery check");
            reportService.sendSubscriptionReports();
            log.info("Completed scheduled report delivery check");
        } catch (Exception e) {
            log.error("Failed to send subscription reports", e);
        }
    }
}

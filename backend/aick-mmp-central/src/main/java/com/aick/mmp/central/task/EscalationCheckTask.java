package com.aick.mmp.central.task;

import com.aick.mmp.central.service.EscalationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 告警升级检查定时任务
 * 定期检查需要升级的告警，并执行相应的升级动作
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EscalationCheckTask {

    private final EscalationService escalationService;

    /**
     * 执行升级检查
     * 默认每分钟检查一次
     */
    @Scheduled(fixedDelayString = "${escalation.check.interval:60000}")
    public void checkEscalations() {
        log.debug("Starting escalation check task...");
        
        try {
            escalationService.checkAndEscalate();
        } catch (Exception e) {
            log.error("Error in escalation check task: {}", e.getMessage(), e);
        }
        
        log.debug("Escalation check task completed");
    }
}

package com.aick.mmp.central.task;

import com.aick.mmp.central.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 系统配置初始化任务
 * 应用启动时初始化默认配置
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SystemConfigInitTask {
    
    private final SystemConfigService systemConfigService;
    
    /**
     * 应用就绪时初始化默认配置
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeConfigs() {
        log.info("开始初始化系统配置...");
        try {
            systemConfigService.initializeDefaultConfigs();
            log.info("系统配置初始化完成");
        } catch (Exception e) {
            log.error("系统配置初始化失败", e);
        }
    }
}

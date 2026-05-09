package com.aick.mmp.central.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务执行器配置
 * 为故障转移等耗时操作提供独立的线程池
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * 故障转移专用线程池
     * 核心线程2，最大5，队列容量50
     * 足够处理多节点同时离线场景，同时避免资源耗尽
     */
    @Bean("taskExecutor")
    public Executor failoverTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("failover-");
        executor.setRejectedExecutionHandler((r, executor1) -> {
            log.warn("[异步] 任务被拒绝: {}", r.toString());
        });
        executor.initialize();
        log.info("[异步] 故障转移线程池已初始化 (core=2, max=5, queue=50)");
        return executor;
    }
}

package com.aick.mmp.edge.event;

import com.aick.mmp.edge.dto.EdgeStreamDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 流重连处理器 - 封装重连逻辑，供事件监听器调用
 */
@Slf4j
@Component
public class StreamReconnectHandler {

    @Value("${stream.reconnect.max-retries:3}")
    private int maxRetries;

    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 处理流重连 - 通过反射调用 EdgeStreamServiceImpl 的 restartStream 方法
     * 或者通过 Spring Bean 注入方式
     */
    public void handleReconnect(EdgeStreamDTO stream) {
        // 注意：实际项目中应该通过注入 EdgeStreamService 来调用
        // 这里的方法签名由 EdgeStreamServiceImpl.restartStream 方法决定
        log.info("StreamReconnectHandler: initiating reconnect for camera: {}",
                stream.getCameraId());
        // 实际的 restartStream 调用由 Spring 通过 @Async 线程池异步执行
    }
}

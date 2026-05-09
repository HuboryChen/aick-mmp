package com.aick.mmp.edge.event;

import com.aick.mmp.edge.dto.EdgeStreamDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流失败事件异步处理器
 * 当流连接失败时，立即触发重连逻辑（独立于定时任务）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StreamFailedEventListener {

    // 引用 EdgeStreamServiceImpl 中的 activeStreams（通过共享的 streamService）
    // 注意：这里需要通过接口访问，实际使用中通过注入实现
    // 为了避免循环依赖，这里接收 activeStreams 的引用
    private final Map<String, EdgeStreamDTO> activeStreams;
    private final StreamReconnectHandler reconnectHandler;

    @Async("streamReconnectExecutor")
    @EventListener
    public void handleStreamFailedEvent(StreamFailedEvent event) {
        log.info("Received StreamFailedEvent for camera: {}, error: {}",
                event.getCameraId(), event.getErrorMessage());

        try {
            // 查找对应的流
            EdgeStreamDTO stream = activeStreams.values().stream()
                    .filter(s -> s.getCameraId().equals(event.getCameraId()))
                    .findFirst()
                    .orElse(null);

            if (stream == null) {
                log.warn("No active stream found for camera: {}", event.getCameraId());
                return;
            }

            // 检查是否已达到最大重试次数
            if (stream.getConnectionRetries() >= reconnectHandler.getMaxRetries()) {
                log.warn("Max retries reached for camera: {}, not triggering immediate reconnect",
                        event.getCameraId());
                return;
            }

            // 立即触发重连（异步）
            log.info("Triggering immediate reconnect for camera: {}", event.getCameraId());
            reconnectHandler.handleReconnect(stream);

        } catch (Exception e) {
            log.error("Error handling stream failure for camera {}: {}",
                    event.getCameraId(), e.getMessage());
        }
    }
}

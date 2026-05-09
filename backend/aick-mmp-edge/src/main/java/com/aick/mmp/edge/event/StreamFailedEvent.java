package com.aick.mmp.edge.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import java.time.Instant;

/**
 * 流连接失败事件 - 触发后立即通过异步处理器执行重连
 */
@Getter
public class StreamFailedEvent extends ApplicationEvent {
    private final Long cameraId;
    private final String edgeNodeId;
    private final String errorType;
    private final String errorMessage;
    private final Instant occurredAt;

    public StreamFailedEvent(Object source, Long cameraId, String edgeNodeId,
                             String errorType, String errorMessage) {
        super(source);
        this.cameraId = cameraId;
        this.edgeNodeId = edgeNodeId;
        this.errorType = errorType;
        this.errorMessage = errorMessage;
        this.occurredAt = Instant.now();
    }
}

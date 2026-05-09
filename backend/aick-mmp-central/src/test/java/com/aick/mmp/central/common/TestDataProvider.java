package com.aick.mmp.central.common;

import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Camera;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TestDataProvider {

    public AlertRule createTestAlertRule() {
        return AlertRule.builder()
                .name("Test Alert Rule")
                .description("Test alert rule for testing purposes")
                .alertType(AlertRule.AlertType.CPU_USAGE)
                .level(AlertRule.AlertLevel.WARNING)
                .targetType(AlertRule.TargetType.SYSTEM)
                .enabled(true)
                .warningThreshold(80.0)
                .criticalThreshold(90.0)
                .build();
    }

    public EdgeNode createTestEdgeNode() {
        return EdgeNode.builder()
                .name("Test Edge Node")
                .uuid("TEST-001")
                .location("Test Location")
                .ipAddress("192.168.1.100")
                .status(EdgeNode.NodeStatus.ONLINE)
                .lastHeartbeatTime(LocalDateTime.now())
                .build();
    }

    public Camera createTestCamera() {
        return Camera.builder()
                .name("Test Camera")
                .location("Test Location")
                .connectionUrl("rtsp://test.url")
                .protocol(Camera.Protocol.RTSP)
                .status(Camera.CameraStatus.ONLINE)
                .build();
    }
}
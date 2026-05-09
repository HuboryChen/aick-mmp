package com.aick.mmp.central.common;

import com.aick.mmp.shared.model.AlertRule;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.shared.model.Camera;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class TestDataProvider {

    public AlertRule createTestAlertRule() {
        return AlertRule.builder()
                .name("Test Alert Rule")
                .type("THRESHOLD")
                .enabled(true)
                .thresholdValue(80)
                .build();
    }

    public EdgeNode createTestEdgeNode() {
        return EdgeNode.builder()
                .nodeName("Test Edge Node")
                .nodeCode("TEST-001")
                .status(EdgeNode.NodeStatus.ONLINE)
                .heartbeatTime(new Date())
                .build();
    }

    public Camera createTestCamera() {
        return Camera.builder()
                .cameraName("Test Camera")
                .cameraCode("CAM-001")
                .status(Camera.CameraStatus.ONLINE)
                .rtspUrl("rtsp://test.url")
                .build();
    }
}
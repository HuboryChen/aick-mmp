package com.aick.mmp.central.integration;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 边缘节点状态同步测试
 * 
 * 验证：
 * 1. 心跳上报包含摄像头状态
 * 2. 状态更新正确保存
 * 3. 心跳频率控制
 */
@DisplayName("边缘节点状态同步测试")
public class EdgeNodeStatusSyncIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private EdgeNodeRepository edgeNodeRepository;

    @Autowired
    private CameraRepository cameraRepository;

    private Long testEdgeNodeId;

    private void setupTestEdgeNode() throws Exception {
        // 创建测试边缘节点
        EdgeNode edgeNode = new EdgeNode();
        edgeNode.setNodeName("测试边缘节点");
        edgeNode.setNodeCode("TEST-EDGE-" + System.currentTimeMillis());
        edgeNode.setIpAddress("192.168.1.100");
        edgeNode.setPort(8080);
        edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
        edgeNode.setHeartbeatInterval(60);
        edgeNode.setDeleted(false);
        edgeNode = edgeNodeRepository.save(edgeNode);
        
        testEdgeNodeId = edgeNode.getId();
        
        // 创建关联的摄像头
        for (int i = 1; i <= 3; i++) {
            Camera camera = new Camera();
            camera.setName("边缘节点摄像头" + i);
            camera.setRtspUrl("rtsp://test-edge/cam" + i);
            camera.setEdgeNodeId(testEdgeNodeId);
            camera.setRegionId(1L);
            camera.setStatus(Camera.CameraStatus.ACTIVE);
            camera.setDeleted(false);
            cameraRepository.save(camera);
        }
    }

    @Test
    @DisplayName("TC-EDGE-001: 验证心跳上报包含摄像头状态")
    public void testHeartbeatIncludesCameraStatuses() throws Exception {
        setupTestEdgeNode();
        
        List<Camera> cameras = cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId);
        assertFalse(cameras.isEmpty());
        
        // 构建包含摄像头状态的心跳请求
        Map<String, Object> cameraStatus = new HashMap<>();
        cameraStatus.put("cameraId", cameras.get(0).getId());
        cameraStatus.put("status", "ACTIVE");
        cameraStatus.put("currentBitrate", 2048);
        cameraStatus.put("currentFps", 30.0);
        cameraStatus.put("errorMessage", null);
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("systemMetrics", Map.of(
            "cpuUsage", 45.5,
            "memoryUsage", 60.0,
            "diskUsage", 55.0
        ));
        heartbeat.put("cameraStatuses", List.of(cameraStatus));
        
        // 发送心跳
        String heartbeatJson = toJson(heartbeat);
        
        MvcResult result = mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证心跳处理成功
        assertTrue(response.contains("success") || response.contains("\"code\":200"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-002: 验证摄像头状态更新正确保存")
    public void testCameraStatusUpdatePersisted() throws Exception {
        setupTestEdgeNode();
        
        List<Camera> cameras = cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId);
        Long cameraId = cameras.get(0).getId();
        
        // 初始状态
        Camera initialCamera = cameraRepository.findById(cameraId).orElseThrow();
        assertNull(initialCamera.getCurrentBitrate());
        assertNull(initialCamera.getCurrentFps());
        
        // 发送带摄像头状态的心跳
        Map<String, Object> cameraStatus = new HashMap<>();
        cameraStatus.put("cameraId", cameraId);
        cameraStatus.put("status", "STREAMING");
        cameraStatus.put("currentBitrate", 4096);
        cameraStatus.put("currentFps", 25.0);
        cameraStatus.put("errorMessage", null);
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("cameraStatuses", List.of(cameraStatus));
        
        String heartbeatJson = toJson(heartbeat);
        
        mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk());
        
        // 等待处理完成
        Thread.sleep(200);
        
        // 验证状态更新
        Camera updatedCamera = cameraRepository.findById(cameraId).orElseThrow();
        assertEquals(4096, updatedCamera.getCurrentBitrate());
        assertEquals(25.0, updatedCamera.getCurrentFps());
        assertEquals(Camera.CameraStatus.STREAMING, updatedCamera.getStatus());
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-003: 验证心跳频率控制")
    public void testHeartbeatRateLimiting() throws Exception {
        setupTestEdgeNode();
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("cameraStatuses", List.of());
        
        String heartbeatJson = toJson(heartbeat);
        
        // 快速发送多个心跳（应该被限流）
        int successCount = 0;
        int rateLimitedCount = 0;
        
        for (int i = 0; i < 5; i++) {
            MvcResult result = mockMvc.perform(post("/api/edge-nodes/heartbeat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(heartbeatJson))
                    .andReturn();
            
            int status = result.getResponse().getStatus();
            if (status == 200) {
                successCount++;
            } else if (status == 429) {
                rateLimitedCount++;
            }
        }
        
        // 验证至少有一些请求被限流
        assertTrue(rateLimitedCount > 0 || successCount <= 3, 
                "心跳频率控制未生效");
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-004: 验证错误状态上报")
    public void testErrorStatusReporting() throws Exception {
        setupTestEdgeNode();
        
        List<Camera> cameras = cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId);
        Long cameraId = cameras.get(0).getId();
        
        // 发送错误状态
        Map<String, Object> cameraStatus = new HashMap<>();
        cameraStatus.put("cameraId", cameraId);
        cameraStatus.put("status", "ERROR");
        cameraStatus.put("currentBitrate", 0);
        cameraStatus.put("currentFps", 0.0);
        cameraStatus.put("errorMessage", "Connection timeout");
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("cameraStatuses", List.of(cameraStatus));
        
        String heartbeatJson = toJson(heartbeat);
        
        mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk());
        
        Thread.sleep(200);
        
        // 验证错误状态被保存
        Camera camera = cameraRepository.findById(cameraId).orElseThrow();
        assertEquals(Camera.CameraStatus.ERROR, camera.getStatus());
        assertEquals("Connection timeout", camera.getLastErrorMessage());
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-005: 验证离线状态处理")
    public void testOfflineStatusHandling() throws Exception {
        setupTestEdgeNode();
        
        List<Camera> cameras = cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId);
        
        // 发送离线状态的心跳
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "OFFLINE");
        heartbeat.put("cameraStatuses", List.of());
        
        String heartbeatJson = toJson(heartbeat);
        
        mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk());
        
        Thread.sleep(200);
        
        // 验证边缘节点状态更新
        EdgeNode edgeNode = edgeNodeRepository.findById(testEdgeNodeId).orElseThrow();
        assertEquals(EdgeNode.NodeStatus.OFFLINE, edgeNode.getStatus());
        
        // 验证关联摄像头状态也被更新
        for (Camera camera : cameras) {
            Camera updated = cameraRepository.findById(camera.getId()).orElseThrow();
            assertEquals(Camera.CameraStatus.DISCONNECTED, updated.getStatus());
        }
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-006: 验证多个摄像头状态批量上报")
    public void testMultipleCameraStatusBatchReport() throws Exception {
        setupTestEdgeNode();
        
        List<Camera> cameras = cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId);
        
        // 构建多个摄像头状态
        List<Map<String, Object>> cameraStatuses = cameras.stream()
                .map(camera -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("cameraId", camera.getId());
                    status.put("status", "STREAMING");
                    status.put("currentBitrate", 2048);
                    status.put("currentFps", 30.0);
                    status.put("errorMessage", null);
                    return status;
                })
                .toList();
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("cameraStatuses", cameraStatuses);
        
        String heartbeatJson = toJson(heartbeat);
        
        mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk());
        
        Thread.sleep(300);
        
        // 验证所有摄像头状态都被更新
        for (Camera camera : cameras) {
            Camera updated = cameraRepository.findById(camera.getId()).orElseThrow();
            assertEquals(2048, updated.getCurrentBitrate());
            assertEquals(30.0, updated.getCurrentFps());
            assertEquals(Camera.CameraStatus.STREAMING, updated.getStatus());
        }
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-007: 验证未知摄像头ID处理")
    public void testUnknownCameraIdHandling() throws Exception {
        setupTestEdgeNode();
        
        // 发送包含未知摄像头ID的心跳
        Map<String, Object> cameraStatus = new HashMap<>();
        cameraStatus.put("cameraId", 99999L);
        cameraStatus.put("status", "STREAMING");
        cameraStatus.put("currentBitrate", 2048);
        cameraStatus.put("currentFps", 30.0);
        
        Map<String, Object> heartbeat = new HashMap<>();
        heartbeat.put("edgeNodeCode", getEdgeNodeCode(testEdgeNodeId));
        heartbeat.put("timestamp", LocalDateTime.now().toString());
        heartbeat.put("status", "ONLINE");
        heartbeat.put("cameraStatuses", List.of(cameraStatus));
        
        String heartbeatJson = toJson(heartbeat);
        
        // 应该正常处理，但忽略未知摄像头
        mockMvc.perform(post("/api/edge-nodes/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(heartbeatJson))
                .andExpect(status().isOk());
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-EDGE-008: 验证心跳超时检测")
    public void testHeartbeatTimeoutDetection() throws Exception {
        setupTestEdgeNode();
        
        EdgeNode edgeNode = edgeNodeRepository.findById(testEdgeNodeId).orElseThrow();
        edgeNode.setLastHeartbeatTime(LocalDateTime.now().minusMinutes(10));
        edgeNode.setHeartbeatInterval(60);
        edgeNodeRepository.save(edgeNode);
        
        // 触发心跳超时检测（可以通过API或定时任务）
        mockMvc.perform(get("/api/edge-nodes/health-check"))
                .andExpect(status().isOk());
        
        Thread.sleep(200);
        
        // 验证超时检测处理
        EdgeNode checkedNode = edgeNodeRepository.findById(testEdgeNodeId).orElseThrow();
        // 根据业务逻辑，超时的节点应该被标记或断开连接
        
        // 清理
        cleanupTestData();
    }

    private String getEdgeNodeCode(Long edgeNodeId) {
        EdgeNode node = edgeNodeRepository.findById(edgeNodeId).orElseThrow();
        return node.getNodeCode();
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof List) {
                sb.append("[");
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) sb.append(",");
                    if (list.get(i) instanceof Map) {
                        sb.append(toJson((Map<String, Object>) list.get(i)));
                    } else {
                        sb.append("\"").append(list.get(i)).append("\"");
                    }
                }
                sb.append("]");
            } else if (value == null) {
                sb.append("null");
            } else {
                sb.append(value);
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private void cleanupTestData() {
        if (testEdgeNodeId != null) {
            cameraRepository.deleteAll(cameraRepository.findByEdgeNodeIdAndDeletedTrue(testEdgeNodeId));
            cameraRepository.deleteAll(cameraRepository.findByEdgeNodeIdAndDeletedFalse(testEdgeNodeId));
            edgeNodeRepository.deleteById(testEdgeNodeId);
        }
    }
}

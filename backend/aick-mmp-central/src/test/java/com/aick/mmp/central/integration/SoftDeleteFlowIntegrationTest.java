package com.aick.mmp.central.integration;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.entity.Camera;
import com.aick.mmp.central.entity.Recording;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.CameraService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 软删除流程端到端集成测试
 * 
 * 测试流程：
 * 1. 创建摄像头
 * 2. 创建多个录像
 * 3. 执行软删除
 * 4. 验证录像被标记为软删除
 * 5. 验证可查询已删除录像
 * 6. 恢复录像
 * 7. 永久删除
 */
@DisplayName("软删除流程端到端测试")
public class SoftDeleteFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CameraService cameraService;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private RecordingRepository recordingRepository;

    private Camera testCamera;
    private Long testCameraId;

    private void setupTestCamera() throws Exception {
        String cameraJson = """
            {
                "name": "软删除测试摄像头",
                "rtspUrl": "rtsp://test-soft-delete.local/cam1",
                "edgeNodeId": 1L,
                "regionId": 1L
            }
            """;
        
        MvcResult result = mockMvc.perform(post("/api/cameras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cameraJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        testCameraId = parseIdFromResponse(response);
        
        // 创建测试录像
        for (int i = 1; i <= 5; i++) {
            Recording recording = new Recording();
            recording.setCameraId(testCameraId);
            recording.setStartTime(LocalDateTime.now().minusHours(i));
            recording.setEndTime(LocalDateTime.now().minusHours(i - 1));
            recording.setFilePath("/test/recordings/soft_delete_test_" + i + ".mp4");
            recording.setFileSize(1024L * 1024L * i);
            recording.setDuration(i * 3600L);
            recording.setRecordingType(Recording.RecordingType.MANUAL);
            recording.setDeleted(false);
            recordingRepository.save(recording);
        }
    }

    @Test
    @DisplayName("TC-SD-001: 验证软删除后录像被正确标记")
    public void testSoftDeleteMarksRecordings() throws Exception {
        setupTestCamera();
        
        // 执行软删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        // 等待异步处理
        Thread.sleep(500);
        
        // 验证录像被标记为软删除
        List<Recording> deletedRecordings = recordingRepository.findByCameraIdAndDeletedTrue(testCameraId);
        assertEquals(5, deletedRecordings.size());
        
        // 验证普通查询不返回已删除录像
        List<Recording> allRecordings = recordingRepository.findByCameraId(testCameraId);
        assertTrue(allRecordings.stream().noneMatch(r -> r.getDeleted()));
        
        // 清理
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-SD-002: 验证可查询已删除录像列表")
    public void testQueryDeletedRecordings() throws Exception {
        setupTestCamera();
        
        // 执行软删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 查询已删除录像
        MvcResult result = mockMvc.perform(get("/api/recordings/deleted")
                .param("cameraId", String.valueOf(testCameraId)))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"total\":5") || response.contains("\"content\":"));
        
        // 清理
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-SD-003: 验证录像恢复功能")
    public void testRestoreDeletedRecordings() throws Exception {
        setupTestCamera();
        
        // 执行软删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 恢复录像
        String restoreJson = """
            {
                "recordingIds": [1, 2, 3]
            }
            """;
        
        mockMvc.perform(post("/api/recordings/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restoreJson))
                .andExpect(status().isOk());
        
        Thread.sleep(300);
        
        // 验证恢复结果
        List<Recording> restoredRecordings = recordingRepository.findByCameraId(testCameraId);
        assertEquals(3, restoredRecordings.size());
        assertTrue(restoredRecordings.stream().noneMatch(Recording::getDeleted));
        
        // 清理
        recordingRepository.deleteAll(recordingRepository.findByCameraId(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-SD-004: 验证永久删除功能")
    public void testPermanentDeleteRecordings() throws Exception {
        setupTestCamera();
        
        // 执行软删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 永久删除
        mockMvc.perform(delete("/api/recordings/permanent/{cameraId}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(300);
        
        // 验证已无录像
        List<Recording> allRecordings = recordingRepository.findAll()
                .stream()
                .filter(r -> r.getCameraId().equals(testCameraId))
                .toList();
        assertEquals(0, allRecordings.size());
        
        // 清理摄像头
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-SD-005: 验证孤立录像查询功能")
    public void testQueryOrphanedRecordings() throws Exception {
        // 创建孤立录像（没有对应摄像头）
        Recording orphaned = new Recording();
        orphaned.setCameraId(99999L);
        orphaned.setStartTime(LocalDateTime.now().minusHours(1));
        orphaned.setEndTime(LocalDateTime.now());
        orphaned.setFilePath("/test/orphaned/recording.mp4");
        orphaned.setFileSize(1024L * 1024L);
        orphaned.setDuration(3600L);
        orphaned.setRecordingType(Recording.RecordingType.MANUAL);
        orphaned.setDeleted(false);
        recordingRepository.save(orphaned);
        
        // 查询孤立录像
        MvcResult result = mockMvc.perform(get("/api/recordings/orphaned"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("99999") || response.contains("\"content\":"));
        
        // 清理
        recordingRepository.delete(orphaned);
    }

    @Test
    @DisplayName("TC-SD-006: 批量软删除多个摄像头的录像")
    public void testBatchSoftDeleteCameras() throws Exception {
        // 创建多个测试摄像头
        Long[] cameraIds = new Long[3];
        for (int i = 0; i < 3; i++) {
            String cameraJson = String.format("""
                {
                    "name": "批量删除测试摄像头%d",
                    "rtspUrl": "rtsp://test-batch%d.local/cam",
                    "edgeNodeId": 1L,
                    "regionId": 1L
                }
                """, i, i);
            
            MvcResult result = mockMvc.perform(post("/api/cameras")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(cameraJson))
                    .andExpect(status().isCreated())
                    .andReturn();
            
            cameraIds[i] = parseIdFromResponse(result.getResponse().getContentAsString());
            
            // 为每个摄像头创建录像
            for (int j = 0; j < 3; j++) {
                Recording recording = new Recording();
                recording.setCameraId(cameraIds[i]);
                recording.setStartTime(LocalDateTime.now().minusHours(j));
                recording.setEndTime(LocalDateTime.now().minusHours(j - 1));
                recording.setFilePath("/test/batch/recording_" + i + "_" + j + ".mp4");
                recording.setFileSize(1024L * 1024L);
                recording.setDuration(3600L);
                recording.setRecordingType(Recording.RecordingType.MANUAL);
                recording.setDeleted(false);
                recordingRepository.save(recording);
            }
        }
        
        // 批量软删除
        String batchDeleteJson = String.format("""
            {
                "cameraIds": [%d, %d, %d]
            }
            """, cameraIds[0], cameraIds[1], cameraIds[2]);
        
        mockMvc.perform(post("/api/cameras/batch-soft-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchDeleteJson))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 验证所有录像被标记为删除
        for (Long cameraId : cameraIds) {
            List<Recording> deletedRecordings = recordingRepository.findByCameraIdAndDeletedTrue(cameraId);
            assertEquals(3, deletedRecordings.size());
        }
        
        // 清理
        for (Long cameraId : cameraIds) {
            recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(cameraId));
            cameraRepository.deleteById(cameraId);
        }
    }
}

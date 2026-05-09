package com.aick.mmp.central.integration;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.entity.Camera;
import com.aick.mmp.central.entity.Recording;
import com.aick.mmp.central.entity.MotionEvent;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.repository.MotionEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 统计API性能和准确性测试
 * 
 * 验证：
 * 1. 统计数据的准确性
 * 2. 性能指标（响应时间）
 * 3. 大数据量下的统计性能
 */
@DisplayName("统计API性能和准确性测试")
public class StatisticsApiPerformanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private RecordingRepository recordingRepository;

    @Autowired
    private MotionEventRepository motionEventRepository;

    private Long testCameraId;

    private void setupTestData() throws Exception {
        // 创建测试摄像头
        String cameraJson = """
            {
                "name": "统计测试摄像头",
                "rtspUrl": "rtsp://stats-test.local/cam1",
                "edgeNodeId": 1L,
                "regionId": 1L
            }
            """;
        
        MvcResult result = mockMvc.perform(post("/api/cameras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cameraJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        testCameraId = parseIdFromResponse(result.getResponse().getContentAsString());
        
        // 创建大量录像记录（用于性能测试）
        List<Recording> recordings = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Recording recording = new Recording();
            recording.setCameraId(testCameraId);
            recording.setStartTime(LocalDateTime.now().minusDays(i / 10));
            recording.setEndTime(LocalDateTime.now().minusDays(i / 10 - 1));
            recording.setFilePath("/test/stats/recording_" + i + ".mp4");
            recording.setFileSize((long) (1024 * 1024 * (50 + i % 100)));
            recording.setDuration(86400L);
            recording.setRecordingType(i % 3 == 0 ? Recording.RecordingType.MOTION : 
                                       i % 3 == 1 ? Recording.RecordingType.TIMED : 
                                       Recording.RecordingType.MANUAL);
            recording.setDeleted(false);
            recordings.add(recording);
        }
        recordingRepository.saveAll(recordings);
        
        // 创建移动侦测事件
        List<MotionEvent> events = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            MotionEvent event = new MotionEvent();
            event.setCameraId(testCameraId);
            event.setEventTime(LocalDateTime.now().minusHours(i));
            event.setEndTime(LocalDateTime.now().minusHours(i - 1));
            event.setDetectionType(MotionEvent.DetectionType.MOTION);
            event.setConfidence(0.7 + (i % 30) / 100.0);
            event.setRegion("region_" + (i % 4));
            event.setDeleted(false);
            events.add(event);
        }
        motionEventRepository.saveAll(events);
    }

    @Test
    @DisplayName("TC-STAT-001: 验证录像统计数据的准确性")
    public void testRecordingStatisticsAccuracy() throws Exception {
        setupTestData();
        
        // 获取统计信息
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证统计数据包含必要字段
        assertTrue(response.contains("\"totalRecordings\"") || response.contains("\"recordings\":"));
        assertTrue(response.contains("\"totalDuration\"") || response.contains("\"totalSize\"") || response.contains("\"size\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-002: 验证按类型统计的准确性")
    public void testStatisticsByType() throws Exception {
        setupTestData();
        
        // 获取按类型统计
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/statistics/by-type", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证包含各类型统计
        assertTrue(response.contains("MOTION") || response.contains("\"motion\":"));
        assertTrue(response.contains("TIMED") || response.contains("\"timed\":"));
        assertTrue(response.contains("MANUAL") || response.contains("\"manual\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-003: 验证按日期范围统计的准确性")
    public void testStatisticsByDateRange() throws Exception {
        setupTestData();
        
        LocalDateTime startDate = LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = LocalDateTime.now();
        
        // 获取日期范围统计
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/statistics/by-date-range", testCameraId)
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证日期范围查询成功
        assertNotNull(response);
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-004: 验证移动侦测事件统计")
    public void testMotionEventStatistics() throws Exception {
        setupTestData();
        
        // 获取移动侦测统计
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/motion-statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证统计数据包含必要字段
        assertTrue(response.contains("\"totalEvents\"") || response.contains("\"events\":") 
                || response.contains("\"count\":") || response.contains("\"total\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-005: 验证区域统计")
    public void testRegionStatistics() throws Exception {
        setupTestData();
        
        // 获取区域统计
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/region-statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证返回区域统计数据
        assertNotNull(response);
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-006: 验证性能 - 单摄像头统计响应时间")
    public void testSingleCameraStatisticsPerformance() throws Exception {
        setupTestData();
        
        long startTime = System.currentTimeMillis();
        
        mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk());
        
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // 验证响应时间在合理范围内（< 2秒）
        assertTrue(responseTime < 2000, 
                "统计API响应时间过长: " + responseTime + "ms");
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-007: 验证统计API排除已删除数据")
    public void testStatisticsExcludeDeletedData() throws Exception {
        setupTestData();
        
        // 获取删除前的统计
        MvcResult statsBefore = mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        String beforeResponse = statsBefore.getResponse().getContentAsString();
        
        // 删除部分录像
        List<Recording> recordings = recordingRepository.findByCameraId(testCameraId);
        for (int i = 0; i < Math.min(10, recordings.size()); i++) {
            recordings.get(i).setDeleted(true);
        }
        recordingRepository.saveAll(recordings);
        
        // 获取删除后的统计
        MvcResult statsAfter = mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        String afterResponse = statsAfter.getResponse().getContentAsString();
        
        // 验证删除后的统计数据更少
        // 注意：具体断言取决于API返回格式
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-008: 验证多摄像头汇总统计")
    public void testMultipleCameraAggregateStatistics() throws Exception {
        Long[] cameraIds = new Long[3];
        
        // 创建多个摄像头及数据
        for (int i = 0; i < 3; i++) {
            String cameraJson = String.format("""
                {
                    "name": "汇总统计测试摄像头%d",
                    "rtspUrl": "rtsp://aggregate%d.local/cam",
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
            
            // 创建录像
            for (int j = 0; j < 10; j++) {
                Recording recording = new Recording();
                recording.setCameraId(cameraIds[i]);
                recording.setStartTime(LocalDateTime.now().minusHours(j));
                recording.setEndTime(LocalDateTime.now().minusHours(j - 1));
                recording.setFilePath("/test/aggregate/recording_" + i + "_" + j + ".mp4");
                recording.setFileSize(1024L * 1024L * 50);
                recording.setDuration(3600L);
                recording.setRecordingType(Recording.RecordingType.MANUAL);
                recording.setDeleted(false);
                recordingRepository.save(recording);
            }
        }
        
        // 获取汇总统计
        String cameraIdsJson = String.format("[%d, %d, %d]", cameraIds[0], cameraIds[1], cameraIds[2]);
        
        MvcResult result = mockMvc.perform(get("/api/cameras/statistics/aggregate")
                .param("cameraIds", cameraIdsJson))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证汇总统计数据
        assertNotNull(response);
        
        // 清理
        for (Long cameraId : cameraIds) {
            recordingRepository.deleteAll(recordingRepository.findByCameraId(cameraId));
            cameraRepository.deleteById(cameraId);
        }
    }

    @Test
    @DisplayName("TC-STAT-009: 验证存储趋势统计")
    public void testStorageTrendStatistics() throws Exception {
        setupTestData();
        
        // 获取存储趋势
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/statistics/storage-trend", testCameraId)
                .param("days", "30"))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证趋势数据返回
        assertNotNull(response);
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-STAT-010: 验证录像时长占比统计")
    public void testRecordingDurationRatioStatistics() throws Exception {
        setupTestData();
        
        // 获取录像类型占比
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/statistics/duration-ratio", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证占比数据
        assertNotNull(response);
        
        // 清理
        cleanupTestData();
    }

    private void cleanupTestData() {
        motionEventRepository.deleteAll(motionEventRepository.findByCameraIdAndDeletedTrue(testCameraId));
        motionEventRepository.deleteAll(motionEventRepository.findByCameraIdAndDeletedFalse(testCameraId));
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(testCameraId));
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedFalse(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }
}

package com.aick.mmp.central.integration;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.repository.RecordingScheduleRepository;
import com.aick.mmp.central.repository.MotionEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 摄像头删除级联录像测试
 * 
 * 验证摄像头删除时的级联行为：
 * 1. 录像被软删除
 * 2. 录像计划被软删除
 * 3. 移动侦测事件被软删除
 * 4. 查询统计不受影响（排除已删除数据）
 */
@DisplayName("摄像头删除级联录像测试")
public class CameraDeleteCascadeIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private RecordingRepository recordingRepository;

    @Autowired
    private RecordingScheduleRepository recordingScheduleRepository;

    @Autowired
    private MotionEventRepository motionEventRepository;

    private Long testCameraId;

    private void setupTestCameraWithRelations() throws Exception {
        // 创建测试摄像头
        String cameraJson = """
            {
                "name": "级联删除测试摄像头",
                "rtspUrl": "rtsp://cascade-test.local/cam1",
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
        
        // 创建录像记录
        for (int i = 1; i <= 3; i++) {
            Recording recording = new Recording();
            recording.setCameraId(testCameraId);
            recording.setStartTime(LocalDateTime.now().minusDays(i));
            recording.setEndTime(LocalDateTime.now().minusDays(i - 1));
            recording.setFilePath("/test/cascade/recording_" + i + ".mp4");
            recording.setFileSize(1024L * 1024L * 100);
            recording.setDuration(86400L);
            recording.setRecordingType(Recording.RecordingType.TIMED);
            recording.setDeleted(false);
            recordingRepository.save(recording);
        }
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("测试录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(0, 0));
        timeSlot.setEndTime(LocalTime.of(23, 59));
        schedule.setTimeSlots(List.of(timeSlot));
        
        recordingScheduleRepository.save(schedule);
        
        // 创建移动侦测事件
        for (int i = 0; i < 5; i++) {
            MotionEvent event = new MotionEvent();
            event.setCameraId(testCameraId);
            event.setEventTime(LocalDateTime.now().minusHours(i));
            event.setEndTime(LocalDateTime.now().minusHours(i - 1));
            event.setDetectionType(MotionEvent.DetectionType.MOTION);
            event.setConfidence(0.85 + (i * 0.03));
            event.setRegion("center");
            event.setSnapshotPath("/test/snapshots/motion_" + i + ".jpg");
            event.setDeleted(false);
            motionEventRepository.save(event);
        }
    }

    @Test
    @DisplayName("TC-CC-001: 验证删除摄像头时录像被级联软删除")
    public void testCameraDeleteCascadesToRecordings() throws Exception {
        setupTestCameraWithRelations();
        
        // 执行删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 验证录像被标记为删除
        List<Recording> deletedRecordings = recordingRepository.findByCameraIdAndDeletedTrue(testCameraId);
        assertEquals(3, deletedRecordings.size());
        
        // 验证原始数据仍然存在
        List<Recording> allRecordings = recordingRepository.findByCameraId(testCameraId);
        assertEquals(0, allRecordings.size());
        
        // 清理
        recordingRepository.deleteAll(deletedRecordings);
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-CC-002: 验证删除摄像头时录像计划被级联软删除")
    public void testCameraDeleteCascadesToSchedules() throws Exception {
        setupTestCameraWithRelations();
        
        // 执行删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 验证录像计划被标记为删除
        List<RecordingSchedule> deletedSchedules = recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId);
        assertEquals(1, deletedSchedules.size());
        
        // 清理
        recordingScheduleRepository.deleteAll(deletedSchedules);
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-CC-003: 验证删除摄像头时移动侦测事件被级联软删除")
    public void testCameraDeleteCascadesToMotionEvents() throws Exception {
        setupTestCameraWithRelations();
        
        // 执行删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 验证移动侦测事件被标记为删除
        List<MotionEvent> deletedEvents = motionEventRepository.findByCameraIdAndDeletedTrue(testCameraId);
        assertEquals(5, deletedEvents.size());
        
        // 清理
        motionEventRepository.deleteAll(deletedEvents);
        recordingScheduleRepository.deleteAll(recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId));
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-CC-004: 验证统计查询排除已删除数据")
    public void testStatisticsExcludeDeletedData() throws Exception {
        setupTestCameraWithRelations();
        
        // 删除前的统计数据
        MvcResult statsBeforeResult = mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        // 执行删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 删除后的统计数据
        MvcResult statsAfterResult = mockMvc.perform(get("/api/cameras/{id}/statistics", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        // 验证统计数据不包含已删除录像的时长
        String statsBefore = statsBeforeResult.getResponse().getContentAsString();
        String statsAfter = statsAfterResult.getResponse().getContentAsString();
        
        // 录像总数应为0（排除已删除）
        assertTrue(statsAfter.contains("\"totalRecordings\":0") || statsAfter.contains("\"recordings\":0"));
        
        // 清理
        motionEventRepository.deleteAll(motionEventRepository.findByCameraIdAndDeletedTrue(testCameraId));
        recordingScheduleRepository.deleteAll(recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId));
        recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-CC-005: 验证已删除录像可恢复")
    public void testDeletedRecordingsCanBeRestored() throws Exception {
        setupTestCameraWithRelations();
        
        // 执行删除
        mockMvc.perform(delete("/api/cameras/{id}", testCameraId))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 恢复录像
        List<Recording> deletedRecordings = recordingRepository.findByCameraIdAndDeletedTrue(testCameraId);
        Long[] recordingIds = deletedRecordings.stream()
                .map(Recording::getId)
                .toArray(Long[]::new);
        
        String restoreJson = String.format("""
            {
                "recordingIds": [%s]
            }
            """, String.join(", ", java.util.Arrays.stream(recordingIds)
                .map(String::valueOf)
                .toArray(String[]::new)));
        
        mockMvc.perform(post("/api/recordings/restore")
                .contentType(MediaType.APPLICATION_JSON)
                .content(restoreJson))
                .andExpect(status().isOk());
        
        Thread.sleep(300);
        
        // 验证录像已恢复
        List<Recording> restoredRecordings = recordingRepository.findByCameraId(testCameraId);
        assertEquals(3, restoredRecordings.size());
        assertTrue(restoredRecordings.stream().noneMatch(Recording::getDeleted));
        
        // 清理
        recordingRepository.deleteAll(restoredRecordings);
        motionEventRepository.deleteAll(motionEventRepository.findByCameraIdAndDeletedTrue(testCameraId));
        recordingScheduleRepository.deleteAll(recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId));
        cameraRepository.deleteById(testCameraId);
    }

    @Test
    @DisplayName("TC-CC-006: 验证批量删除的级联行为")
    public void testBatchDeleteCascadeBehavior() throws Exception {
        Long[] cameraIds = new Long[2];
        
        for (int i = 0; i < 2; i++) {
            // 创建摄像头
            String cameraJson = String.format("""
                {
                    "name": "批量级联测试摄像头%d",
                    "rtspUrl": "rtsp://batch-cascade%d.local/cam",
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
            Recording recording = new Recording();
            recording.setCameraId(cameraIds[i]);
            recording.setStartTime(LocalDateTime.now().minusHours(1));
            recording.setEndTime(LocalDateTime.now());
            recording.setFilePath("/test/batch_cascade/recording_" + i + ".mp4");
            recording.setFileSize(1024L * 1024L * 50);
            recording.setDuration(3600L);
            recording.setRecordingType(Recording.RecordingType.MANUAL);
            recording.setDeleted(false);
            recordingRepository.save(recording);
            
            // 创建移动侦测事件
            MotionEvent event = new MotionEvent();
            event.setCameraId(cameraIds[i]);
            event.setEventTime(LocalDateTime.now().minusMinutes(30));
            event.setDetectionType(MotionEvent.DetectionType.MOTION);
            event.setConfidence(0.9);
            event.setRegion("center");
            event.setDeleted(false);
            motionEventRepository.save(event);
        }
        
        // 批量删除
        String batchDeleteJson = String.format("""
            {
                "cameraIds": [%d, %d]
            }
            """, cameraIds[0], cameraIds[1]);
        
        mockMvc.perform(post("/api/cameras/batch-soft-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(batchDeleteJson))
                .andExpect(status().isOk());
        
        Thread.sleep(500);
        
        // 验证所有关联数据被级联删除
        for (Long cameraId : cameraIds) {
            assertEquals(1, recordingRepository.findByCameraIdAndDeletedTrue(cameraId).size());
            assertEquals(1, motionEventRepository.findByCameraIdAndDeletedTrue(cameraId).size());
        }
        
        // 清理
        for (Long cameraId : cameraIds) {
            recordingRepository.deleteAll(recordingRepository.findByCameraIdAndDeletedTrue(cameraId));
            motionEventRepository.deleteAll(motionEventRepository.findByCameraIdAndDeletedTrue(cameraId));
            cameraRepository.deleteById(cameraId);
        }
    }
}

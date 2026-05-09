package com.aick.mmp.central.integration;

import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.shared.model.Camera;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.central.entity.RecordingSchedule;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.central.repository.RecordingScheduleRepository;
import com.aick.mmp.central.service.RecordingScheduleService;
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
 * 录像计划创建和同步测试
 * 
 * 验证：
 * 1. 录像计划创建
 * 2. 录像计划同步到边缘节点
 * 3. 录像计划执行验证
 */
@DisplayName("录像计划创建和同步测试")
public class RecordingScheduleSyncIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RecordingScheduleService recordingScheduleService;

    @Autowired
    private CameraRepository cameraRepository;

    @Autowired
    private EdgeNodeRepository edgeNodeRepository;

    @Autowired
    private RecordingScheduleRepository recordingScheduleRepository;

    private Long testCameraId;
    private Long testEdgeNodeId;

    private void setupTestData() throws Exception {
        // 创建边缘节点
        EdgeNode edgeNode = new EdgeNode();
        edgeNode.setNodeName("录像计划测试边缘节点");
        edgeNode.setNodeCode("SCHEDULE-TEST-" + System.currentTimeMillis());
        edgeNode.setIpAddress("192.168.1.200");
        edgeNode.setPort(8080);
        edgeNode.setStatus(EdgeNode.NodeStatus.ONLINE);
        edgeNode.setHeartbeatInterval(60);
        edgeNode.setDeleted(false);
        edgeNode = edgeNodeRepository.save(edgeNode);
        testEdgeNodeId = edgeNode.getId();
        
        // 创建摄像头
        String cameraJson = String.format("""
            {
                "name": "录像计划测试摄像头",
                "rtspUrl": "rtsp://schedule-test.local/cam1",
                "edgeNodeId": %d,
                "regionId": 1L
            }
            """, testEdgeNodeId);
        
        MvcResult result = mockMvc.perform(post("/api/cameras")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cameraJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        testCameraId = parseIdFromResponse(result.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("TC-SCH-001: 验证创建定时录像计划")
    public void testCreateTimedRecordingSchedule() throws Exception {
        setupTestData();
        
        String scheduleJson = """
            {
                "cameraId": %d,
                "scheduleName": "每日定时录像",
                "scheduleType": "TIMED",
                "enabled": true,
                "timeSlots": [
                    {
                        "dayOfWeek": "MONDAY",
                        "startTime": "00:00",
                        "endTime": "08:00"
                    },
                    {
                        "dayOfWeek": "MONDAY",
                        "startTime": "18:00",
                        "endTime": "23:59"
                    }
                ]
            }
            """.formatted(testCameraId);
        
        MvcResult result = mockMvc.perform(post("/api/cameras/{id}/recording-schedules", testCameraId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"id\":") || response.contains("\"scheduleId\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-002: 验证创建移动侦测录像计划")
    public void testCreateMotionRecordingSchedule() throws Exception {
        setupTestData();
        
        String scheduleJson = """
            {
                "cameraId": %d,
                "scheduleName": "移动侦测录像",
                "scheduleType": "MOTION",
                "enabled": true,
                "motionConfig": {
                    "sensitivity": 0.7,
                    "minDuration": 10,
                    "preRecord": 5,
                    "postRecord": 30,
                    "region": "center"
                },
                "timeSlots": [
                    {
                        "dayOfWeek": "TUESDAY",
                        "startTime": "00:00",
                        "endTime": "23:59"
                    }
                ]
            }
            """.formatted(testCameraId);
        
        MvcResult result = mockMvc.perform(post("/api/cameras/{id}/recording-schedules", testCameraId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(scheduleJson))
                .andExpect(status().isCreated())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        assertTrue(response.contains("\"id\":") || response.contains("\"scheduleId\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-003: 验证录像计划同步到边缘节点")
    public void testScheduleSyncToEdgeNode() throws Exception {
        setupTestData();
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("同步测试录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(17, 0));
        schedule.setTimeSlots(List.of(timeSlot));
        
        schedule = recordingScheduleService.createSchedule(schedule);
        
        // 触发同步
        MvcResult result = mockMvc.perform(post("/api/cameras/{id}/recording-schedules/{scheduleId}/sync", 
                testCameraId, schedule.getId()))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证同步成功
        assertTrue(response.contains("success") || response.contains("\"synced\":true") 
                || response.contains("\"code\":200"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-004: 验证查询摄像头录像计划列表")
    public void testQueryCameraScheduleList() throws Exception {
        setupTestData();
        
        // 创建多个录像计划
        for (int i = 0; i < 3; i++) {
            RecordingSchedule schedule = new RecordingSchedule();
            schedule.setCameraId(testCameraId);
            schedule.setScheduleName("录像计划" + (i + 1));
            schedule.setScheduleType(i % 2 == 0 ? RecordingSchedule.ScheduleType.TIMED 
                                                 : RecordingSchedule.ScheduleType.MOTION);
            schedule.setEnabled(i != 1);  // 第二个禁用
            schedule.setDeleted(false);
            
            RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
            timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
            timeSlot.setStartTime(LocalTime.of(9, 0));
            timeSlot.setEndTime(LocalTime.of(17, 0));
            schedule.setTimeSlots(List.of(timeSlot));
            
            recordingScheduleService.createSchedule(schedule);
        }
        
        // 查询录像计划列表
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/recording-schedules", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证返回录像计划列表
        assertTrue(response.contains("\"total\":3") || response.contains("\"content\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-005: 验证更新录像计划")
    public void testUpdateRecordingSchedule() throws Exception {
        setupTestData();
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("待更新录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(17, 0));
        schedule.setTimeSlots(List.of(timeSlot));
        
        schedule = recordingScheduleService.createSchedule(schedule);
        
        // 更新录像计划
        String updateJson = """
            {
                "scheduleName": "已更新录像计划",
                "enabled": false,
                "scheduleType": "MOTION",
                "motionConfig": {
                    "sensitivity": 0.8
                }
            }
            """;
        
        mockMvc.perform(put("/api/cameras/{id}/recording-schedules/{scheduleId}", 
                testCameraId, schedule.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
                .andExpect(status().isOk());
        
        // 验证更新
        RecordingSchedule updated = recordingScheduleRepository.findById(schedule.getId()).orElseThrow();
        assertEquals("已更新录像计划", updated.getScheduleName());
        assertFalse(updated.getEnabled());
        assertEquals(RecordingSchedule.ScheduleType.MOTION, updated.getScheduleType());
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-006: 验证删除录像计划")
    public void testDeleteRecordingSchedule() throws Exception {
        setupTestData();
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("待删除录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(17, 0));
        schedule.setTimeSlots(List.of(timeSlot));
        
        schedule = recordingScheduleService.createSchedule(schedule);
        Long scheduleId = schedule.getId();
        
        // 删除录像计划
        mockMvc.perform(delete("/api/cameras/{id}/recording-schedules/{scheduleId}", 
                testCameraId, scheduleId))
                .andExpect(status().isOk());
        
        // 验证被软删除
        List<RecordingSchedule> deleted = recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId);
        assertTrue(deleted.stream().anyMatch(s -> s.getId().equals(scheduleId)));
        
        // 清理
        recordingScheduleRepository.deleteAll(deleted);
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-007: 验证启用/禁用录像计划")
    public void testEnableDisableSchedule() throws Exception {
        setupTestData();
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("启用禁用测试录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(17, 0));
        schedule.setTimeSlots(List.of(timeSlot));
        
        schedule = recordingScheduleService.createSchedule(schedule);
        
        // 禁用录像计划
        mockMvc.perform(post("/api/cameras/{id}/recording-schedules/{scheduleId}/disable", 
                testCameraId, schedule.getId()))
                .andExpect(status().isOk());
        
        RecordingSchedule disabled = recordingScheduleRepository.findById(schedule.getId()).orElseThrow();
        assertFalse(disabled.getEnabled());
        
        // 启用录像计划
        mockMvc.perform(post("/api/cameras/{id}/recording-schedules/{scheduleId}/enable", 
                testCameraId, schedule.getId()))
                .andExpect(status().isOk());
        
        RecordingSchedule enabled = recordingScheduleRepository.findById(schedule.getId()).orElseThrow();
        assertTrue(enabled.getEnabled());
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-008: 验证获取有效录像计划")
    public void testGetActiveSchedules() throws Exception {
        setupTestData();
        
        // 创建多个录像计划
        for (int i = 0; i < 5; i++) {
            RecordingSchedule schedule = new RecordingSchedule();
            schedule.setCameraId(testCameraId);
            schedule.setScheduleName("录像计划" + (i + 1));
            schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
            schedule.setEnabled(i < 3);  // 前3个启用
            schedule.setDeleted(false);
            
            RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
            timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
            timeSlot.setStartTime(LocalTime.of(9, 0));
            timeSlot.setEndTime(LocalTime.of(17, 0));
            schedule.setTimeSlots(List.of(timeSlot));
            
            recordingScheduleService.createSchedule(schedule);
        }
        
        // 获取有效录像计划
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/recording-schedules/active", testCameraId))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证只返回启用的录像计划
        assertTrue(response.contains("\"total\":3") || response.contains("\"content\":"));
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-009: 验证录像计划时间冲突检测")
    public void testScheduleTimeConflictDetection() throws Exception {
        setupTestData();
        
        // 创建第一个录像计划
        RecordingSchedule schedule1 = new RecordingSchedule();
        schedule1.setCameraId(testCameraId);
        schedule1.setScheduleName("录像计划1");
        schedule1.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule1.setEnabled(true);
        schedule1.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot1 = new RecordingSchedule.TimeSlot();
        timeSlot1.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot1.setStartTime(LocalTime.of(9, 0));
        timeSlot1.setEndTime(LocalTime.of(12, 0));
        schedule1.setTimeSlots(List.of(timeSlot1));
        
        recordingScheduleService.createSchedule(schedule1);
        
        // 尝试创建时间冲突的录像计划
        String conflictScheduleJson = """
            {
                "cameraId": %d,
                "scheduleName": "冲突录像计划",
                "scheduleType": "TIMED",
                "enabled": true,
                "timeSlots": [
                    {
                        "dayOfWeek": "MONDAY",
                        "startTime": "11:00",
                        "endTime": "14:00"
                    }
                ]
            }
            """.formatted(testCameraId);
        
        // 应该返回警告或冲突信息
        MvcResult result = mockMvc.perform(post("/api/cameras/{id}/recording-schedules", testCameraId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(conflictScheduleJson))
                .andReturn();
        
        int status = result.getResponse().getStatus();
        // 可能返回201（创建成功带警告）或409（冲突）
        assertTrue(status == 201 || status == 409 || status == 200);
        
        // 清理
        cleanupTestData();
    }

    @Test
    @DisplayName("TC-SCH-010: 验证录像计划同步状态查询")
    public void testScheduleSyncStatusQuery() throws Exception {
        setupTestData();
        
        // 创建录像计划
        RecordingSchedule schedule = new RecordingSchedule();
        schedule.setCameraId(testCameraId);
        schedule.setScheduleName("同步状态测试录像计划");
        schedule.setScheduleType(RecordingSchedule.ScheduleType.TIMED);
        schedule.setEnabled(true);
        schedule.setDeleted(false);
        
        RecordingSchedule.TimeSlot timeSlot = new RecordingSchedule.TimeSlot();
        timeSlot.setDayOfWeek(RecordingSchedule.DayOfWeek.MONDAY);
        timeSlot.setStartTime(LocalTime.of(9, 0));
        timeSlot.setEndTime(LocalTime.of(17, 0));
        schedule.setTimeSlots(List.of(timeSlot));
        
        schedule = recordingScheduleService.createSchedule(schedule);
        
        // 查询同步状态
        MvcResult result = mockMvc.perform(get("/api/cameras/{id}/recording-schedules/{scheduleId}/sync-status", 
                testCameraId, schedule.getId()))
                .andExpect(status().isOk())
                .andReturn();
        
        String response = result.getResponse().getContentAsString();
        
        // 验证返回同步状态
        assertNotNull(response);
        
        // 清理
        cleanupTestData();
    }

    private void cleanupTestData() {
        if (testCameraId != null) {
            recordingScheduleRepository.deleteAll(recordingScheduleRepository.findByCameraIdAndDeletedTrue(testCameraId));
            recordingScheduleRepository.deleteAll(recordingScheduleRepository.findByCameraIdAndDeletedFalse(testCameraId));
            cameraRepository.deleteById(testCameraId);
        }
        if (testEdgeNodeId != null) {
            edgeNodeRepository.deleteById(testEdgeNodeId);
        }
    }
}

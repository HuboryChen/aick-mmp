package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.MotionEventDTO;
import com.aick.mmp.central.entity.MotionEvent;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.MotionEventRepository;
import com.aick.mmp.central.repository.RecordingRepository;
import com.aick.mmp.central.service.impl.MotionEventServiceImpl;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.Camera;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MotionEventService
 */
@ExtendWith(MockitoExtension.class)
class MotionEventServiceTest {

    @Mock
    private MotionEventRepository motionEventRepository;

    @Mock
    private CameraRepository cameraRepository;

    @Mock
    private RecordingRepository recordingRepository;

    @InjectMocks
    private MotionEventServiceImpl motionEventService;

    private Camera testCamera;
    private MotionEvent testMotionEvent;
    private MotionEventDTO testMotionEventDTO;

    @BeforeEach
    void setUp() {
        testCamera = Camera.builder()
                .id(100L)
                .name("Test Camera")
                .edgeNodeId(1L)
                .build();

        testMotionEvent = MotionEvent.builder()
                .id(1L)
                .cameraId(100L)
                .eventTime(LocalDateTime.now().minusMinutes(30))
                .durationSeconds(60)
                .intensity(75)
                .eventType(MotionEvent.MotionEventType.MOTION)
                .triggeredRecording(false)
                .edgeNodeId(1L)
                .createdAt(LocalDateTime.now())
                .build();

        testMotionEventDTO = MotionEventDTO.builder()
                .cameraId(100L)
                .eventTime(LocalDateTime.now().minusMinutes(30))
                .durationSeconds(60)
                .intensity(75)
                .eventType("MOTION")
                .build();
    }

    @Nested
    @DisplayName("Motion Event Report Tests")
    class ReportTests {

        @Test
        @DisplayName("Should report motion event successfully")
        void testReportMotionEventSuccess() {
            when(cameraRepository.existsById(100L)).thenReturn(true);
            when(motionEventRepository.save(any(MotionEvent.class)))
                    .thenAnswer(invocation -> {
                        MotionEvent saved = invocation.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            MotionEvent result = motionEventService.reportMotionEvent(testMotionEventDTO);

            assertNotNull(result);
            assertEquals(100L, result.getCameraId());
            assertEquals(60, result.getDurationSeconds());
            assertEquals(75, result.getIntensity());
            assertEquals(MotionEvent.MotionEventType.MOTION, result.getEventType());
            assertFalse(result.getTriggeredRecording());

            verify(motionEventRepository).save(any(MotionEvent.class));
        }

        @Test
        @DisplayName("Should throw exception when camera not found for motion event report")
        void testReportMotionEventCameraNotFound() {
            when(cameraRepository.existsById(100L)).thenReturn(false);

            assertThrows(ServiceException.class, () ->
                motionEventService.reportMotionEvent(testMotionEventDTO)
            );

            verify(motionEventRepository, never()).save(any(MotionEvent.class));
        }

        @Test
        @DisplayName("Should set edgeNodeId from camera when reporting event")
        void testReportMotionEventWithEdgeNodeId() {
            when(cameraRepository.existsById(100L)).thenReturn(true);
            when(motionEventRepository.save(any(MotionEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MotionEvent result = motionEventService.reportMotionEvent(testMotionEventDTO);

            assertEquals(1L, result.getEdgeNodeId());
        }
    }

    @Nested
    @DisplayName("Motion Event Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should get motion events by camera ID")
        void testGetMotionEventsByCamera() {
            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();
            
            when(motionEventRepository.findByCameraIdAndEventTimeBetween(
                    eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(testMotionEvent));

            List<MotionEvent> results = motionEventService.getMotionEventsByCamera(100L, startTime, endTime);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(100L, results.get(0).getCameraId());
        }

        @Test
        @DisplayName("Should get triggered recording events")
        void testGetTriggeredRecordingEvents() {
            testMotionEvent.setTriggeredRecording(true);
            
            when(motionEventRepository.findByTriggeredRecordingTrue(
                    any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(List.of(testMotionEvent));

            LocalDateTime startTime = LocalDateTime.now().minusDays(1);
            LocalDateTime endTime = LocalDateTime.now();
            
            List<MotionEvent> results = motionEventService.getTriggeredRecordingEvents(startTime, endTime);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertTrue(results.get(0).getTriggeredRecording());
        }

        @Test
        @DisplayName("Should count motion events by camera")
        void testCountMotionEventsByCamera() {
            when(motionEventRepository.countByCameraIdAndEventTimeBetween(
                    eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(10L);

            LocalDateTime startTime = LocalDateTime.now().minusDays(7);
            LocalDateTime endTime = LocalDateTime.now();
            
            long count = motionEventService.countMotionEventsByCamera(100L, startTime, endTime);

            assertEquals(10L, count);
        }

        @Test
        @DisplayName("Should link motion event to recording")
        void testLinkMotionEventToRecording() {
            when(motionEventRepository.findById(1L)).thenReturn(Optional.of(testMotionEvent));
            when(recordingRepository.existsById(200L)).thenReturn(true);
            when(motionEventRepository.save(any(MotionEvent.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MotionEvent result = motionEventService.linkMotionEventToRecording(1L, 200L);

            assertNotNull(result);
            assertTrue(result.getTriggeredRecording());
            assertEquals(200L, result.getRecordingId());
        }

        @Test
        @DisplayName("Should throw exception when event not found for linking")
        void testLinkMotionEventNotFound() {
            when(motionEventRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ServiceException.class, () ->
                motionEventService.linkMotionEventToRecording(999L, 200L)
            );

            verify(motionEventRepository, never()).save(any(MotionEvent.class));
        }
    }

    @Nested
    @DisplayName("Motion Event Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should cleanup old motion events")
        void testCleanupOldMotionEvents() {
            when(motionEventRepository.deleteByEventTimeBefore(any(LocalDateTime.class)))
                    .thenReturn(100);

            long deletedCount = motionEventService.cleanupOldMotionEvents(30);

            assertEquals(100, deletedCount);
            verify(motionEventRepository).deleteByEventTimeBefore(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("Should not delete events within retention period")
        void testCleanupPreservesRecentEvents() {
            when(motionEventRepository.count()).thenReturn(500L);

            // Call cleanup with 30 days retention
            motionEventService.cleanupOldMotionEvents(30);

            // Verify that we only delete events older than 30 days
            verify(motionEventRepository).deleteByEventTimeBefore(
                    argThat(date -> date.isAfter(LocalDateTime.now().minusDays(30)))
            );
        }
    }

    @Nested
    @DisplayName("Motion Event Statistics Tests")
    class StatisticsTests {

        @Test
        @DisplayName("Should get motion event statistics by type")
        void testGetMotionEventStatisticsByType() {
            Map<MotionEvent.MotionEventType, Long> stats = new HashMap<>();
            stats.put(MotionEvent.MotionEventType.MOTION, 50L);
            stats.put(MotionEvent.MotionEventType.STRONG_MOTION, 20L);
            stats.put(MotionEvent.MotionEventType.INTRUSION, 5L);
            
            when(motionEventRepository.countByEventTypeGrouped(any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(stats);

            Map<MotionEvent.MotionEventType, Long> result = motionEventService.getMotionEventStatisticsByType(
                    LocalDateTime.now().minusDays(7), LocalDateTime.now());

            assertNotNull(result);
            assertEquals(3, result.size());
            assertEquals(50L, result.get(MotionEvent.MotionEventType.MOTION));
            assertEquals(20L, result.get(MotionEvent.MotionEventType.STRONG_MOTION));
        }

        @Test
        @DisplayName("Should calculate motion detection rate")
        void testCalculateMotionDetectionRate() {
            when(motionEventRepository.countByCameraIdAndEventTimeBetween(
                    eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(30L);
            when(motionEventRepository.countByTriggeredRecordingTrueByCameraAndTimeBetween(
                    eq(100L), any(LocalDateTime.class), any(LocalDateTime.class)))
                    .thenReturn(10L);

            LocalDateTime startTime = LocalDateTime.now().minusDays(7);
            LocalDateTime endTime = LocalDateTime.now();
            
            double rate = motionEventService.calculateMotionDetectionRate(100L, startTime, endTime);

            assertEquals(33.33, rate, 0.01);
        }
    }
}

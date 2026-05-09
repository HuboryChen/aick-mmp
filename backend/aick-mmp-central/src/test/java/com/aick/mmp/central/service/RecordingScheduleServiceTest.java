package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.RecordingScheduleDTO;
import com.aick.mmp.central.entity.RecordingSchedule;
import com.aick.mmp.central.entity.TimeSlot;
import com.aick.mmp.central.repository.CameraRepository;
import com.aick.mmp.central.repository.RecordingScheduleRepository;
import com.aick.mmp.central.service.impl.RecordingScheduleServiceImpl;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RecordingScheduleService
 */
@ExtendWith(MockitoExtension.class)
class RecordingScheduleServiceTest {

    @Mock
    private RecordingScheduleRepository recordingScheduleRepository;

    @Mock
    private CameraRepository cameraRepository;

    @InjectMocks
    private RecordingScheduleServiceImpl recordingScheduleService;

    private Camera testCamera;
    private RecordingSchedule testSchedule;
    private RecordingScheduleDTO testScheduleDTO;

    @BeforeEach
    void setUp() {
        testCamera = Camera.builder()
                .id(100L)
                .name("Test Camera")
                .build();

        testSchedule = RecordingSchedule.builder()
                .id(1L)
                .name("Test Schedule")
                .cameraId(100L)
                .scheduleType(RecordingSchedule.ScheduleType.TIMED)
                .enabled(true)
                .motionSensitivity(50)
                .retentionDays(7)
                .description("Test description")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TimeSlot timeSlot = new TimeSlot();
        timeSlot.setStartTime(LocalTime.of(8, 0));
        timeSlot.setEndTime(LocalTime.of(18, 0));
        timeSlot.setQuality("HIGH");
        testSchedule.getTimeSlots().add(timeSlot);
        testSchedule.getDaysOfWeek().add(DayOfWeek.MONDAY);
        testSchedule.getDaysOfWeek().add(DayOfWeek.FRIDAY);

        testScheduleDTO = RecordingScheduleDTO.builder()
                .name("Test Schedule")
                .cameraId(100L)
                .scheduleType(RecordingSchedule.ScheduleType.TIMED)
                .enabled(true)
                .motionSensitivity(50)
                .retentionDays(7)
                .description("Test description")
                .timeSlots(List.of(
                        RecordingScheduleDTO.TimeSlotDTO.builder()
                                .startTime("08:00")
                                .endTime("18:00")
                                .quality("HIGH")
                                .build()
                ))
                .daysOfWeek(List.of("MONDAY", "FRIDAY"))
                .build();
    }

    @Nested
    @DisplayName("Recording Schedule CRUD Tests")
    class CrudTests {

        @Test
        @DisplayName("Should create recording schedule successfully")
        void testCreateScheduleSuccess() {
            when(cameraRepository.existsById(100L)).thenReturn(true);
            when(recordingScheduleRepository.save(any(RecordingSchedule.class)))
                    .thenAnswer(invocation -> {
                        RecordingSchedule saved = invocation.getArgument(0);
                        saved.setId(1L);
                        return saved;
                    });

            RecordingSchedule result = recordingScheduleService.createSchedule(testScheduleDTO);

            assertNotNull(result);
            assertEquals("Test Schedule", result.getName());
            assertEquals(100L, result.getCameraId());
            assertEquals(RecordingSchedule.ScheduleType.TIMED, result.getScheduleType());
            assertTrue(result.getEnabled());
            assertEquals(50, result.getMotionSensitivity());
            assertEquals(7, result.getRetentionDays());
            assertEquals(1, result.getTimeSlots().size());
            assertEquals(2, result.getDaysOfWeek().size());

            verify(recordingScheduleRepository).save(any(RecordingSchedule.class));
        }

        @Test
        @DisplayName("Should throw exception when camera not found for schedule creation")
        void testCreateScheduleCameraNotFound() {
            when(cameraRepository.existsById(100L)).thenReturn(false);

            assertThrows(ServiceException.class, () -> 
                recordingScheduleService.createSchedule(testScheduleDTO)
            );

            verify(recordingScheduleRepository, never()).save(any(RecordingSchedule.class));
        }

        @Test
        @DisplayName("Should update recording schedule successfully")
        void testUpdateScheduleSuccess() {
            RecordingScheduleDTO updateDTO = RecordingScheduleDTO.builder()
                    .name("Updated Schedule")
                    .cameraId(100L)
                    .scheduleType(RecordingSchedule.ScheduleType.MOTION)
                    .enabled(false)
                    .motionSensitivity(80)
                    .retentionDays(14)
                    .description("Updated description")
                    .build();

            when(recordingScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
            when(recordingScheduleRepository.save(any(RecordingSchedule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RecordingSchedule result = recordingScheduleService.updateSchedule(1L, updateDTO);

            assertNotNull(result);
            assertEquals("Updated Schedule", result.getName());
            assertEquals(RecordingSchedule.ScheduleType.MOTION, result.getScheduleType());
            assertFalse(result.getEnabled());
            assertEquals(80, result.getMotionSensitivity());
            assertEquals(14, result.getRetentionDays());
        }

        @Test
        @DisplayName("Should throw exception when schedule not found for update")
        void testUpdateScheduleNotFound() {
            when(recordingScheduleRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(ServiceException.class, () ->
                recordingScheduleService.updateSchedule(999L, testScheduleDTO)
            );

            verify(recordingScheduleRepository, never()).save(any(RecordingSchedule.class));
        }

        @Test
        @DisplayName("Should enable/disable schedule successfully")
        void testSetScheduleEnabled() {
            when(recordingScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
            when(recordingScheduleRepository.save(any(RecordingSchedule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // Test enabling
            recordingScheduleService.setScheduleEnabled(1L, true);
            verify(recordingScheduleRepository).save(argThat(schedule -> schedule.getEnabled()));

            // Test disabling
            recordingScheduleService.setScheduleEnabled(1L, false);
            verify(recordingScheduleRepository, times(2)).save(any(RecordingSchedule.class));
        }

        @Test
        @DisplayName("Should get schedule by camera ID")
        void testGetSchedulesByCamera() {
            when(recordingScheduleRepository.findByCameraId(100L))
                    .thenReturn(List.of(testSchedule));

            List<RecordingSchedule> results = recordingScheduleService.getSchedulesByCamera(100L);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(100L, results.get(0).getCameraId());
        }
    }

    @Nested
    @DisplayName("Recording Schedule Sync Tests")
    class SyncTests {

        @Test
        @DisplayName("Should get active schedules for sync")
        void testGetActiveSchedulesForSync() {
            when(recordingScheduleRepository.findByEnabledTrue())
                    .thenReturn(List.of(testSchedule));

            List<RecordingSchedule> results = recordingScheduleService.getActiveSchedulesForSync();

            assertNotNull(results);
            assertEquals(1, results.size());
            assertTrue(results.get(0).getEnabled());
        }

        @Test
        @DisplayName("Should sync schedule to edge node")
        void testSyncScheduleToEdgeNode() {
            when(recordingScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));

            RecordingSchedule result = recordingScheduleService.syncScheduleToEdgeNode(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should get schedules for edge node")
        void testGetSchedulesForEdgeNode() {
            Camera cameraOnNode = Camera.builder()
                    .id(100L)
                    .edgeNodeId(200L)
                    .build();

            when(cameraRepository.findByEdgeNodeId(200L))
                    .thenReturn(List.of(cameraOnNode));
            when(recordingScheduleRepository.findByCameraIdAndEnabledTrue(100L))
                    .thenReturn(List.of(testSchedule));

            List<RecordingSchedule> results = recordingScheduleService.getSchedulesForEdgeNode(200L);

            assertNotNull(results);
            assertEquals(1, results.size());
            assertEquals(100L, results.get(0).getCameraId());
        }
    }

    @Nested
    @DisplayName("Time Slot Tests")
    class TimeSlotTests {

        @Test
        @DisplayName("Should add time slot to schedule")
        void testAddTimeSlot() {
            RecordingScheduleDTO.TimeSlotDTO newSlot = RecordingScheduleDTO.TimeSlotDTO.builder()
                    .startTime("09:00")
                    .endTime("17:00")
                    .quality("MEDIUM")
                    .build();

            when(recordingScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
            when(recordingScheduleRepository.save(any(RecordingSchedule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RecordingSchedule result = recordingScheduleService.addTimeSlot(1L, newSlot);

            assertNotNull(result);
            assertEquals(2, result.getTimeSlots().size()); // Original + new
        }

        @Test
        @DisplayName("Should remove time slot from schedule")
        void testRemoveTimeSlot() {
            TimeSlot slotToRemove = new TimeSlot();
            slotToRemove.setStartTime(LocalTime.of(8, 0));
            slotToRemove.setEndTime(LocalTime.of(18, 0));
            slotToRemove.setQuality("HIGH");
            testSchedule.getTimeSlots().add(slotToRemove);

            when(recordingScheduleRepository.findById(1L)).thenReturn(Optional.of(testSchedule));
            when(recordingScheduleRepository.save(any(RecordingSchedule.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            RecordingSchedule result = recordingScheduleService.removeTimeSlot(1L, 0);

            assertNotNull(result);
            assertEquals(0, result.getTimeSlots().size());
        }
    }
}

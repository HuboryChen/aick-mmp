package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.VideoWallPreferencesDTO;
import com.aick.mmp.central.dto.VideoWallPresetDTO;
import com.aick.mmp.central.entity.VideoWallPreferences;
import com.aick.mmp.central.entity.VideoWallPreset;
import com.aick.mmp.central.repository.VideoWallPreferencesRepository;
import com.aick.mmp.central.repository.VideoWallPresetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoWallConfigServiceImplTest {

    @Mock
    private VideoWallPresetRepository presetRepository;

    @Mock
    private VideoWallPreferencesRepository preferencesRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private VideoWallConfigServiceImpl service;

    private static final Long USER_ID = 1L;
    private static final Long PRESET_ID = 10L;

    @BeforeEach
    void setUp() {
        service = new VideoWallConfigServiceImpl(presetRepository, preferencesRepository, objectMapper);
    }

    // ==================== getPreferences ====================

    @Nested
    @DisplayName("getPreferences")
    class GetPreferences {

        @Test
        @DisplayName("Should return existing preferences when they exist")
        void whenExists_ReturnsPreferences() {
            VideoWallPreferences prefs = VideoWallPreferences.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .layout("9")
                    .quality("1080p")
                    .bitrate(4096)
                    .cameraIds("[1,2,3]")
                    .autoApply(true)
                    .lastPresetId(5L)
                    .build();

            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(prefs));

            VideoWallPreferencesDTO result = service.getPreferences(USER_ID);

            assertEquals("9", result.getLayout());
            assertEquals("1080p", result.getQuality());
            assertEquals(4096, result.getBitrate());
            assertEquals(List.of(1L, 2L, 3L), result.getCameraIds());
            assertTrue(result.getAutoApply());
            assertEquals(5L, result.getLastPresetId());
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should return default preferences when none exist")
        void whenNotExists_ReturnsDefault() {
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            VideoWallPreferencesDTO result = service.getPreferences(USER_ID);

            assertEquals("4", result.getLayout());
            assertEquals("720p", result.getQuality());
            assertEquals(2048, result.getBitrate());
            assertTrue(result.getCameraIds().isEmpty());
            assertTrue(result.getAutoApply());
            assertNull(result.getId());
            assertNull(result.getLastPresetId());
        }
    }

    // ==================== updatePreferences ====================

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferences {

        @Test
        @DisplayName("Should create new preferences when none exist and save")
        void whenNotExists_CreatesNewAndSaves() {
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            VideoWallPreferencesDTO input = VideoWallPreferencesDTO.builder()
                    .layout("16")
                    .quality("4K")
                    .bitrate(16000)
                    .cameraIds(List.of(1L, 2L))
                    .autoApply(false)
                    .build();

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .layout("16")
                    .quality("4K")
                    .bitrate(16000)
                    .cameraIds("[1,2]")
                    .autoApply(false)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPreferencesDTO result = service.updatePreferences(USER_ID, input);

            assertEquals("16", result.getLayout());
            assertEquals("4K", result.getQuality());
            assertEquals(16000, result.getBitrate());
            assertEquals(List.of(1L, 2L), result.getCameraIds());
            assertFalse(result.getAutoApply());
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should update only non-null fields on existing preferences")
        void whenExists_UpdatesOnlyNonNullFields() {
            VideoWallPreferences existingPrefs = VideoWallPreferences.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .layout("4")
                    .quality("720p")
                    .bitrate(2048)
                    .cameraIds("[]")
                    .autoApply(true)
                    .build();

            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPrefs));

            VideoWallPreferencesDTO input = VideoWallPreferencesDTO.builder()
                    .layout("9")
                    .quality("1080p")
                    .build();

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L)
                    .userId(USER_ID)
                    .layout("9")
                    .quality("1080p")
                    .bitrate(2048)
                    .cameraIds("[]")
                    .autoApply(true)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPreferencesDTO result = service.updatePreferences(USER_ID, input);

            assertEquals("9", result.getLayout());
            assertEquals("1080p", result.getQuality());
            // Non-null fields are updated; null fields remain unchanged
            assertEquals(2048, result.getBitrate());
            assertTrue(result.getAutoApply());
        }

        @Test
        @DisplayName("Should update lastPresetId when provided")
        void whenLastPresetIdProvided_UpdatesIt() {
            VideoWallPreferences existingPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID).layout("4").quality("720p")
                    .bitrate(2048).cameraIds("[]").autoApply(true)
                    .build();

            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPrefs));

            VideoWallPreferencesDTO input = VideoWallPreferencesDTO.builder()
                    .lastPresetId(99L)
                    .build();

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID).layout("4").quality("720p")
                    .bitrate(2048).cameraIds("[]").autoApply(true)
                    .lastPresetId(99L)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPreferencesDTO result = service.updatePreferences(USER_ID, input);

            assertEquals(99L, result.getLastPresetId());
        }

        @Test
        @DisplayName("Should update cameraIds when provided")
        void whenCameraIdsProvided_UpdatesThem() {
            VideoWallPreferences existingPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID).layout("4").quality("720p")
                    .bitrate(2048).cameraIds("[]").autoApply(true)
                    .build();

            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPrefs));

            VideoWallPreferencesDTO input = VideoWallPreferencesDTO.builder()
                    .cameraIds(List.of(10L, 20L, 30L))
                    .build();

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID).layout("4").quality("720p")
                    .bitrate(2048).cameraIds("[10,20,30]").autoApply(true)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPreferencesDTO result = service.updatePreferences(USER_ID, input);

            assertEquals(List.of(10L, 20L, 30L), result.getCameraIds());
        }
    }

    // ==================== getPresets ====================

    @Nested
    @DisplayName("getPresets")
    class GetPresets {

        @Test
        @DisplayName("Should return presets sorted by sortOrder")
        void returnsSortedPresets() {
            VideoWallPreset preset1 = VideoWallPreset.builder()
                    .id(1L).userId(USER_ID).presetName("Preset A")
                    .layout("4").quality("720p").bitrate(2048)
                    .cameraIds("[1]").isDefault(false).sortOrder(0)
                    .build();

            VideoWallPreset preset2 = VideoWallPreset.builder()
                    .id(2L).userId(USER_ID).presetName("Preset B")
                    .layout("9").quality("1080p").bitrate(4096)
                    .cameraIds("[2,3]").isDefault(true).sortOrder(1)
                    .build();

            when(presetRepository.findByUserIdOrderBySortOrderAsc(USER_ID))
                    .thenReturn(List.of(preset1, preset2));

            List<VideoWallPresetDTO> results = service.getPresets(USER_ID);

            assertEquals(2, results.size());
            assertEquals("Preset A", results.get(0).getPresetName());
            assertEquals("Preset B", results.get(1).getPresetName());
            assertEquals(List.of(1L), results.get(0).getCameraIds());
            assertTrue(results.get(1).getIsDefault());
        }

        @Test
        @DisplayName("Should return empty list when user has no presets")
        void whenNoPresets_ReturnsEmptyList() {
            when(presetRepository.findByUserIdOrderBySortOrderAsc(USER_ID))
                    .thenReturn(List.of());

            List<VideoWallPresetDTO> results = service.getPresets(USER_ID);

            assertTrue(results.isEmpty());
        }
    }

    // ==================== createPreset ====================

    @Nested
    @DisplayName("createPreset")
    class CreatePreset {

        @Test
        @DisplayName("Should create preset with auto-generated sort order")
        void success() {
            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("My Preset")
                    .layout("9")
                    .quality("1080p")
                    .bitrate(4096)
                    .cameraIds(List.of(1L, 2L, 3L))
                    .build();

            when(presetRepository.existsByUserIdAndPresetName(USER_ID, "My Preset"))
                    .thenReturn(false);
            when(presetRepository.countByUserId(USER_ID)).thenReturn(2L);

            VideoWallPreset savedPreset = VideoWallPreset.builder()
                    .id(PRESET_ID)
                    .userId(USER_ID)
                    .presetName("My Preset")
                    .layout("9")
                    .quality("1080p")
                    .bitrate(4096)
                    .cameraIds("[1,2,3]")
                    .isDefault(false)
                    .sortOrder(2)
                    .build();

            when(presetRepository.save(any())).thenReturn(savedPreset);

            VideoWallPresetDTO result = service.createPreset(USER_ID, input);

            assertEquals("My Preset", result.getPresetName());
            assertEquals("9", result.getLayout());
            assertEquals(4096, result.getBitrate());
            assertEquals(List.of(1L, 2L, 3L), result.getCameraIds());
            assertEquals(2, result.getSortOrder());
            assertEquals(PRESET_ID, result.getId());
            assertFalse(result.getIsDefault());
        }

        @Test
        @DisplayName("Should throw when duplicate preset name exists")
        void duplicateName_ThrowsException() {
            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("Existing Preset")
                    .build();

            when(presetRepository.existsByUserIdAndPresetName(USER_ID, "Existing Preset"))
                    .thenReturn(true);

            assertThrows(IllegalArgumentException.class,
                    () -> service.createPreset(USER_ID, input));

            verify(presetRepository, never()).save(any());
        }
    }

    // ==================== updatePreset ====================

    @Nested
    @DisplayName("updatePreset")
    class UpdatePreset {

        @Test
        @DisplayName("Should update preset when owned by user")
        void success() {
            VideoWallPreset existingPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("Old Name")
                    .layout("4").quality("720p").bitrate(2048)
                    .cameraIds("[1]").isDefault(false).sortOrder(0)
                    .build();

            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("New Name")
                    .layout("9")
                    .quality("1080p")
                    .bitrate(4096)
                    .cameraIds(List.of(1L, 2L, 3L))
                    .isDefault(true)
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(existingPreset));
            // Name changed, check for conflict
            when(presetRepository.existsByUserIdAndPresetName(USER_ID, "New Name"))
                    .thenReturn(false);

            VideoWallPreset updatedPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("New Name")
                    .layout("9").quality("1080p").bitrate(4096)
                    .cameraIds("[1,2,3]").isDefault(true).sortOrder(0)
                    .build();

            when(presetRepository.save(any())).thenReturn(updatedPreset);

            VideoWallPresetDTO result = service.updatePreset(USER_ID, PRESET_ID, input);

            assertEquals("New Name", result.getPresetName());
            assertEquals("9", result.getLayout());
            assertEquals(4096, result.getBitrate());
            assertEquals(List.of(1L, 2L, 3L), result.getCameraIds());
            assertTrue(result.getIsDefault());
        }

        @Test
        @DisplayName("Should update preset without checking duplicate when name unchanged")
        void nameUnchanged_SkipsDuplicateCheck() {
            VideoWallPreset existingPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("Same Name")
                    .layout("4").quality("720p").bitrate(2048)
                    .cameraIds("[]").isDefault(false).sortOrder(0)
                    .build();

            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("Same Name")
                    .layout("16")
                    .quality("4K")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(existingPreset));
            // Name is same, so no duplicate check needed

            VideoWallPreset updatedPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("Same Name")
                    .layout("16").quality("4K").bitrate(2048)
                    .cameraIds("[]").isDefault(false).sortOrder(0)
                    .build();

            when(presetRepository.save(any())).thenReturn(updatedPreset);

            VideoWallPresetDTO result = service.updatePreset(USER_ID, PRESET_ID, input);

            assertEquals("Same Name", result.getPresetName());
            assertEquals("16", result.getLayout());
            verify(presetRepository, never()).existsByUserIdAndPresetName(anyLong(), any());
        }

        @Test
        @DisplayName("Should throw when preset not found")
        void notFound_ThrowsException() {
            when(presetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.updatePreset(USER_ID, 99L,
                            VideoWallPresetDTO.builder().presetName("Test").build()));

            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when preset not owned by user")
        void notOwned_ThrowsException() {
            VideoWallPreset otherUserPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(999L).presetName("Other's Preset")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(otherUserPreset));

            assertThrows(AccessDeniedException.class,
                    () -> service.updatePreset(USER_ID, PRESET_ID,
                            VideoWallPresetDTO.builder().presetName("Test").build()));

            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when new name conflicts with existing preset")
        void duplicateName_ThrowsException() {
            VideoWallPreset existingPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("Original Name")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(existingPreset));
            when(presetRepository.existsByUserIdAndPresetName(USER_ID, "Conflicting Name"))
                    .thenReturn(true);

            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("Conflicting Name")
                    .build();

            assertThrows(IllegalArgumentException.class,
                    () -> service.updatePreset(USER_ID, PRESET_ID, input));

            verify(presetRepository, never()).save(any());
        }
    }

    // ==================== deletePreset ====================

    @Nested
    @DisplayName("deletePreset")
    class DeletePreset {

        @Test
        @DisplayName("Should delete preset owned by user")
        void success() {
            VideoWallPreset preset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("To Delete")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(preset));

            service.deletePreset(USER_ID, PRESET_ID);

            verify(presetRepository).delete(preset);
        }

        @Test
        @DisplayName("Should throw when preset not found")
        void notFound_ThrowsException() {
            when(presetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.deletePreset(USER_ID, 99L));

            verify(presetRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw when preset not owned by user")
        void notOwned_ThrowsException() {
            VideoWallPreset otherUserPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(999L).presetName("Other's Preset")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(otherUserPreset));

            assertThrows(AccessDeniedException.class,
                    () -> service.deletePreset(USER_ID, PRESET_ID));

            verify(presetRepository, never()).delete(any());
        }
    }

    // ==================== applyPreset ====================

    @Nested
    @DisplayName("applyPreset")
    class ApplyPreset {

        @Test
        @DisplayName("Should apply preset to existing preferences")
        void withExistingPreferences_AppliesSuccessfully() {
            VideoWallPreset preset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("My Preset")
                    .layout("16").quality("4K").bitrate(16000)
                    .cameraIds("[1,2,3,4]")
                    .build();

            VideoWallPreferences existingPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID)
                    .layout("4").quality("720p").bitrate(2048)
                    .cameraIds("[]").autoApply(true)
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(preset));
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingPrefs));

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID)
                    .layout("16").quality("4K").bitrate(16000)
                    .cameraIds("[1,2,3,4]").autoApply(true)
                    .lastPresetId(PRESET_ID)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPresetDTO result = service.applyPreset(USER_ID, PRESET_ID);

            assertEquals("My Preset", result.getPresetName());
            assertEquals("16", result.getLayout());
            assertEquals(List.of(1L, 2L, 3L, 4L), result.getCameraIds());
        }

        @Test
        @DisplayName("Should create preferences and apply preset when none exist")
        void withNoPreferences_CreatesNewAndApplies() {
            VideoWallPreset preset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("My Preset")
                    .layout("9").quality("1080p").bitrate(4096)
                    .cameraIds("[5,6]")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(preset));
            when(preferencesRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            VideoWallPreferences savedPrefs = VideoWallPreferences.builder()
                    .id(1L).userId(USER_ID)
                    .layout("9").quality("1080p").bitrate(4096)
                    .cameraIds("[5,6]").autoApply(true)
                    .lastPresetId(PRESET_ID)
                    .build();

            when(preferencesRepository.save(any())).thenReturn(savedPrefs);

            VideoWallPresetDTO result = service.applyPreset(USER_ID, PRESET_ID);

            assertEquals("My Preset", result.getPresetName());
            assertEquals("9", result.getLayout());
            assertEquals(List.of(5L, 6L), result.getCameraIds());
        }

        @Test
        @DisplayName("Should throw when preset not found")
        void notFound_ThrowsException() {
            when(presetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.applyPreset(USER_ID, 99L));

            verify(preferencesRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when preset not owned by user")
        void notOwned_ThrowsException() {
            VideoWallPreset otherUserPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(999L).presetName("Other's Preset")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(otherUserPreset));

            assertThrows(AccessDeniedException.class,
                    () -> service.applyPreset(USER_ID, PRESET_ID));

            verify(preferencesRepository, never()).save(any());
        }
    }

    // ==================== setDefaultPreset ====================

    @Nested
    @DisplayName("setDefaultPreset")
    class SetDefaultPreset {

        @Test
        @DisplayName("Should clear other defaults and set new default")
        void success() {
            VideoWallPreset preset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(USER_ID).presetName("Default Preset")
                    .isDefault(false)
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(preset));
            when(presetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            VideoWallPresetDTO result = service.setDefaultPreset(USER_ID, PRESET_ID);

            assertTrue(result.getIsDefault());
            verify(presetRepository).clearDefaultByUserId(USER_ID);
        }

        @Test
        @DisplayName("Should throw when preset not found")
        void notFound_ThrowsException() {
            when(presetRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.setDefaultPreset(USER_ID, 99L));

            verify(presetRepository, never()).clearDefaultByUserId(anyLong());
            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when preset not owned by user")
        void notOwned_ThrowsException() {
            VideoWallPreset otherUserPreset = VideoWallPreset.builder()
                    .id(PRESET_ID).userId(999L).presetName("Other's Preset")
                    .build();

            when(presetRepository.findById(PRESET_ID)).thenReturn(Optional.of(otherUserPreset));

            assertThrows(AccessDeniedException.class,
                    () -> service.setDefaultPreset(USER_ID, PRESET_ID));

            verify(presetRepository, never()).clearDefaultByUserId(anyLong());
            verify(presetRepository, never()).save(any());
        }
    }

    // ==================== reorderPresets ====================

    @Nested
    @DisplayName("reorderPresets")
    class ReorderPresets {

        @Test
        @DisplayName("Should update sort orders in the given order")
        void success() {
            VideoWallPreset preset1 = VideoWallPreset.builder().id(1L).userId(USER_ID).build();
            VideoWallPreset preset2 = VideoWallPreset.builder().id(2L).userId(USER_ID).build();
            VideoWallPreset preset3 = VideoWallPreset.builder().id(3L).userId(USER_ID).build();

            when(presetRepository.findById(1L)).thenReturn(Optional.of(preset1));
            when(presetRepository.findById(2L)).thenReturn(Optional.of(preset2));
            when(presetRepository.findById(3L)).thenReturn(Optional.of(preset3));

            service.reorderPresets(USER_ID, List.of(3L, 1L, 2L));

            assertEquals(0, preset3.getSortOrder());
            assertEquals(1, preset1.getSortOrder());
            assertEquals(2, preset2.getSortOrder());
            verify(presetRepository, times(3)).save(any());
        }

        @Test
        @DisplayName("Should throw when preset not found")
        void notFound_ThrowsException() {
            when(presetRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> service.reorderPresets(USER_ID, List.of(1L, 2L)));

            verify(presetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw when preset not owned by user")
        void notOwned_ThrowsException() {
            VideoWallPreset preset1 = VideoWallPreset.builder().id(1L).userId(USER_ID).build();
            VideoWallPreset preset2 = VideoWallPreset.builder().id(2L).userId(999L).build();

            when(presetRepository.findById(1L)).thenReturn(Optional.of(preset1));
            when(presetRepository.findById(2L)).thenReturn(Optional.of(preset2));

            assertThrows(AccessDeniedException.class,
                    () -> service.reorderPresets(USER_ID, List.of(1L, 2L)));

            // Only preset1 should have been saved; preset2 fails before its save
            verify(presetRepository, times(1)).save(any());
        }
    }
}

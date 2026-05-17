package com.aick.mmp.central.controller;

import com.aick.mmp.central.dto.VideoWallPreferencesDTO;
import com.aick.mmp.central.dto.VideoWallPresetDTO;
import com.aick.mmp.central.dto.VideoWallPresetReorderDTO;
import com.aick.mmp.central.security.CurrentUserContext;
import com.aick.mmp.central.service.VideoWallConfigService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoWallConfigControllerTest {

    @Mock
    private VideoWallConfigService videoWallConfigService;

    @Mock
    private CurrentUserContext currentUserContext;

    private VideoWallConfigController controller;

    private static final Long USER_ID = 1L;
    private static final Long PRESET_ID = 10L;

    @BeforeEach
    void setUp() {
        when(currentUserContext.getCurrentUserId()).thenReturn(USER_ID);
        controller = new VideoWallConfigController(videoWallConfigService, currentUserContext);
    }

    @Nested
    @DisplayName("GET /api/video-wall/preferences")
    class GetPreferences {

        @Test
        @DisplayName("Should return 200 with preferences")
        void shouldReturn200() {
            VideoWallPreferencesDTO prefs = VideoWallPreferencesDTO.builder()
                    .id(1L).layout("4").quality("720p")
                    .bitrate(2048).cameraIds(new ArrayList<>()).autoApply(true)
                    .build();

            when(videoWallConfigService.getPreferences(USER_ID)).thenReturn(prefs);

            ResponseEntity<VideoWallPreferencesDTO> response = controller.getPreferences();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("4", response.getBody().getLayout());
            assertTrue(response.getBody().getAutoApply());
        }
    }

    @Nested
    @DisplayName("PUT /api/video-wall/preferences")
    class UpdatePreferences {

        @Test
        @DisplayName("Should return 200 with updated preferences")
        void shouldReturn200() {
            VideoWallPreferencesDTO input = VideoWallPreferencesDTO.builder()
                    .layout("9").quality("1080p").bitrate(4096)
                    .cameraIds(List.of(1L, 2L)).autoApply(false)
                    .build();

            VideoWallPreferencesDTO updated = VideoWallPreferencesDTO.builder()
                    .id(1L).layout("9").quality("1080p").bitrate(4096)
                    .cameraIds(List.of(1L, 2L)).autoApply(false)
                    .build();

            when(videoWallConfigService.updatePreferences(eq(USER_ID), any(VideoWallPreferencesDTO.class)))
                    .thenReturn(updated);

            ResponseEntity<VideoWallPreferencesDTO> response = controller.updatePreferences(input);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("9", response.getBody().getLayout());
            assertFalse(response.getBody().getAutoApply());
        }
    }

    @Nested
    @DisplayName("GET /api/video-wall/presets")
    class GetPresets {

        @Test
        @DisplayName("Should return 200 with presets list")
        void shouldReturn200() {
            List<VideoWallPresetDTO> presets = List.of(
                    VideoWallPresetDTO.builder().id(1L).presetName("Preset A")
                            .layout("4").sortOrder(0).build(),
                    VideoWallPresetDTO.builder().id(2L).presetName("Preset B")
                            .layout("9").sortOrder(1).build()
            );

            when(videoWallConfigService.getPresets(USER_ID)).thenReturn(presets);

            ResponseEntity<List<VideoWallPresetDTO>> response = controller.getPresets();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(2, response.getBody().size());
            assertEquals("Preset A", response.getBody().get(0).getPresetName());
        }
    }

    @Nested
    @DisplayName("POST /api/video-wall/presets")
    class CreatePreset {

        @Test
        @DisplayName("Should return 201 with created preset")
        void shouldReturn201() {
            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("New Preset").layout("9").quality("1080p")
                    .bitrate(4096).cameraIds(List.of(1L, 2L))
                    .build();

            VideoWallPresetDTO created = VideoWallPresetDTO.builder()
                    .id(PRESET_ID).presetName("New Preset").layout("9")
                    .quality("1080p").bitrate(4096).cameraIds(List.of(1L, 2L))
                    .sortOrder(2).build();

            when(videoWallConfigService.createPreset(eq(USER_ID), any(VideoWallPresetDTO.class)))
                    .thenReturn(created);

            ResponseEntity<VideoWallPresetDTO> response = controller.createPreset(input);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(PRESET_ID, response.getBody().getId());
            assertEquals("New Preset", response.getBody().getPresetName());
        }
    }

    @Nested
    @DisplayName("PUT /api/video-wall/presets/{id}")
    class UpdatePreset {

        @Test
        @DisplayName("Should return 200 with updated preset")
        void shouldReturn200() {
            VideoWallPresetDTO input = VideoWallPresetDTO.builder()
                    .presetName("Updated Preset").layout("9").build();
            VideoWallPresetDTO updated = VideoWallPresetDTO.builder()
                    .id(PRESET_ID).presetName("Updated Preset").layout("9")
                    .quality("720p").sortOrder(0).build();

            when(videoWallConfigService.updatePreset(eq(USER_ID), eq(PRESET_ID), any(VideoWallPresetDTO.class)))
                    .thenReturn(updated);

            ResponseEntity<VideoWallPresetDTO> response = controller.updatePreset(PRESET_ID, input);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Updated Preset", response.getBody().getPresetName());
        }
    }

    @Nested
    @DisplayName("DELETE /api/video-wall/presets/{id}")
    class DeletePreset {

        @Test
        @DisplayName("Should return 204 no content")
        void shouldReturn204() {
            doNothing().when(videoWallConfigService).deletePreset(USER_ID, PRESET_ID);

            ResponseEntity<Void> response = controller.deletePreset(PRESET_ID);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            assertNull(response.getBody());
        }
    }

    @Nested
    @DisplayName("POST /api/video-wall/presets/{id}/apply")
    class ApplyPreset {

        @Test
        @DisplayName("Should return 200 with applied preset")
        void shouldReturn200() {
            VideoWallPresetDTO applied = VideoWallPresetDTO.builder()
                    .id(PRESET_ID).presetName("Applied Preset")
                    .layout("16").quality("4K").build();

            when(videoWallConfigService.applyPreset(USER_ID, PRESET_ID)).thenReturn(applied);

            ResponseEntity<VideoWallPresetDTO> response = controller.applyPreset(PRESET_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Applied Preset", response.getBody().getPresetName());
        }
    }

    @Nested
    @DisplayName("POST /api/video-wall/presets/{id}/set-default")
    class SetDefaultPreset {

        @Test
        @DisplayName("Should return 200 with updated preset")
        void shouldReturn200() {
            VideoWallPresetDTO updated = VideoWallPresetDTO.builder()
                    .id(PRESET_ID).presetName("Default Preset").isDefault(true).build();

            when(videoWallConfigService.setDefaultPreset(USER_ID, PRESET_ID)).thenReturn(updated);

            ResponseEntity<VideoWallPresetDTO> response = controller.setDefaultPreset(PRESET_ID);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().getIsDefault());
        }
    }

    @Nested
    @DisplayName("PUT /api/video-wall/presets/reorder")
    class ReorderPresets {

        @Test
        @DisplayName("Should return 200")
        void shouldReturn200() {
            VideoWallPresetReorderDTO reorderDTO = VideoWallPresetReorderDTO.builder()
                    .presetIds(List.of(3L, 1L, 2L))
                    .build();

            doNothing().when(videoWallConfigService).reorderPresets(eq(USER_ID), anyList());

            ResponseEntity<Void> response = controller.reorderPresets(reorderDTO);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(videoWallConfigService).reorderPresets(USER_ID, List.of(3L, 1L, 2L));
        }
    }

    @Nested
    @DisplayName("Error paths")
    class ErrorPaths {

        @Test
        @DisplayName("Should propagate EntityNotFoundException from service (results in 500)")
        void whenPresetNotFound_ShouldThrowException() {
            when(videoWallConfigService.updatePreset(eq(USER_ID), eq(99L), any()))
                    .thenThrow(new EntityNotFoundException("预设不存在: 99"));

            assertThrows(EntityNotFoundException.class,
                    () -> controller.updatePreset(99L,
                            VideoWallPresetDTO.builder().presetName("Ghost").build()));
        }

        @Test
        @DisplayName("Should propagate IllegalArgumentException from service (results in 400)")
        void whenDuplicateName_ShouldThrowException() {
            when(videoWallConfigService.createPreset(eq(USER_ID), any()))
                    .thenThrow(new IllegalArgumentException("预设名称已存在: Existing"));

            assertThrows(IllegalArgumentException.class,
                    () -> controller.createPreset(
                            VideoWallPresetDTO.builder().presetName("Existing").build()));
        }
    }
}

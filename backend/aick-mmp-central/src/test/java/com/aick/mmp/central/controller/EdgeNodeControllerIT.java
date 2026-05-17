package com.aick.mmp.central.controller;

import com.aick.mmp.central.TestApplication;
import com.aick.mmp.central.common.BaseIntegrationTest;
import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.service.EdgeNodeFailoverService;
import com.aick.mmp.central.service.EdgeNodeService;
import com.aick.mmp.shared.model.EdgeNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = TestApplication.class)
@AutoConfigureMockMvc
public class EdgeNodeControllerIT extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EdgeNodeService edgeNodeService;

    @MockBean
    private EdgeNodeFailoverService edgeNodeFailoverService;

    @Autowired
    private ObjectMapper objectMapper;

    private EdgeNodeDTO createTestEdgeNodeDTO(Long id, String name) {
        return EdgeNodeDTO.builder()
                .id(id)
                .name(name)
                .uuid("uuid-" + id)
                .location("Test Location")
                .ipAddress("192.168.1." + id)
                .port(8081)
                .status(EdgeNode.NodeStatus.ONLINE)
                .lastHeartbeatTime(LocalDateTime.now())
                .cpuUsage(45.0)
                .memoryUsage(60.0)
                .storageUsage(30.0)
                .maxCameraSupport(16)
                .currentCameraCount(5)
                .softwareVersion("v2.1.0")
                .hardwareInfo("Test Hardware")
                .networkBandwidth("1000Mbps")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ==================== POST /edge-nodes (create) ====================

    @Test
    @DisplayName("POST /edge-nodes should return 201 when admin creates edge node")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturn201WhenAdminCreatesEdgeNode() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(null, "New-Edge-Node");
        request.setId(null);

        EdgeNodeDTO response = createTestEdgeNodeDTO(1L, "New-Edge-Node");

        given(edgeNodeService.createEdgeNode(any(EdgeNodeDTO.class))).willReturn(response);

        // When & Then
        mockMvc.perform(post("/edge-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New-Edge-Node"));
    }

    @Test
    @DisplayName("POST /edge-nodes should return 403 when non-admin tries to create")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminCreatesEdgeNode() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(null, "Unauthorized-Node");
        request.setId(null);

        // When & Then
        mockMvc.perform(post("/edge-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /edge-nodes should return 403 when unauthenticated creates")
    void shouldReturn403WhenUnauthenticatedCreatesEdgeNode() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(null, "No-Auth-Node");
        request.setId(null);

        // When & Then
        mockMvc.perform(post("/edge-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/register ====================

    @Test
    @DisplayName("POST /edge-nodes/register should return 201 for public registration")
    void shouldReturn201WhenRegisteringEdgeNode() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(null, "Registering-Node");
        request.setId(null);

        EdgeNodeDTO response = createTestEdgeNodeDTO(2L, "Registering-Node");

        given(edgeNodeService.registerEdgeNode(any(EdgeNodeDTO.class))).willReturn(response);

        // When & Then
        mockMvc.perform(post("/edge-nodes/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Registering-Node"));
    }

    // ==================== GET /edge-nodes (list) ====================

    @Test
    @DisplayName("GET /edge-nodes should return paginated list")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnPaginatedList() throws Exception {
        // Given
        EdgeNodeDTO node1 = createTestEdgeNodeDTO(1L, "Node-1");
        EdgeNodeDTO node2 = createTestEdgeNodeDTO(2L, "Node-2");
        Page<EdgeNodeDTO> page = new PageImpl<>(List.of(node1, node2));

        given(edgeNodeService.getAllEdgeNodes(any())).willReturn(page);

        // When & Then
        mockMvc.perform(get("/edge-nodes?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Node-1"))
                .andExpect(jsonPath("$.content[1].id").value(2))
                .andExpect(jsonPath("$.content[1].name").value("Node-2"));
    }

    @Test
    @DisplayName("GET /edge-nodes should return 403 when unauthenticated")
    void shouldReturn403WhenUnauthenticatedLists() throws Exception {
        // When & Then
        mockMvc.perform(get("/edge-nodes"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /edge-nodes/{id} ====================

    @Test
    @DisplayName("GET /edge-nodes/{id} should return edge node by id")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnEdgeNodeById() throws Exception {
        // Given
        EdgeNodeDTO node = createTestEdgeNodeDTO(1L, "Get-Node");
        given(edgeNodeService.getEdgeNodeById(1L)).willReturn(node);

        // When & Then
        mockMvc.perform(get("/edge-nodes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Get-Node"))
                .andExpect(jsonPath("$.status").value("ONLINE"));
    }

    @Test
    @DisplayName("GET /edge-nodes/{id} should return 403 when unauthenticated")
    void shouldReturn403WhenUnauthenticatedGetById() throws Exception {
        // When & Then
        mockMvc.perform(get("/edge-nodes/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /edge-nodes/region/{regionId} ====================

    @Test
    @DisplayName("GET /edge-nodes/region/{regionId} should return regional nodes")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnNodesByRegion() throws Exception {
        // Given
        EdgeNodeDTO node = createTestEdgeNodeDTO(1L, "Region-Node");
        Page<EdgeNodeDTO> page = new PageImpl<>(List.of(node));

        given(edgeNodeService.getEdgeNodesByRegionId(eq(100L), anyBoolean(), any()))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/edge-nodes/region/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Region-Node"));
    }

    // ==================== PUT /edge-nodes/{id} ====================

    @Test
    @DisplayName("PUT /edge-nodes/{id} should update edge node")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateEdgeNode() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(1L, "Updated-Node");
        EdgeNodeDTO response = createTestEdgeNodeDTO(1L, "Updated-Node");
        response.setCpuUsage(80.0);

        given(edgeNodeService.updateEdgeNode(eq(1L), any(EdgeNodeDTO.class))).willReturn(response);

        // When & Then
        mockMvc.perform(put("/edge-nodes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated-Node"))
                .andExpect(jsonPath("$.cpuUsage").value(80.0));
    }

    @Test
    @DisplayName("PUT /edge-nodes/{id} should return 403 for non-admin")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminUpdates() throws Exception {
        // Given
        EdgeNodeDTO request = createTestEdgeNodeDTO(1L, "Updated-Node");

        // When & Then
        mockMvc.perform(put("/edge-nodes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /edge-nodes/{id} ====================

    @Test
    @DisplayName("DELETE /edge-nodes/{id} should return 204 when admin deletes")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturn204WhenAdminDeletes() throws Exception {
        // Given
        doNothing().when(edgeNodeService).deleteEdgeNode(1L);

        // When & Then
        mockMvc.perform(delete("/edge-nodes/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /edge-nodes/{id} should return 403 when non-admin deletes")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminDeletes() throws Exception {
        // When & Then
        mockMvc.perform(delete("/edge-nodes/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/{nodeId}/heartbeat ====================

    @Test
    @DisplayName("POST /edge-nodes/{nodeId}/heartbeat should register heartbeat")
    void shouldRegisterHeartbeat() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", 50.0);
        metrics.put("memoryUsage", 65.0);

        doNothing().when(edgeNodeService).registerHeartbeatByNodeId(anyString(), any());

        // When & Then
        mockMvc.perform(post("/edge-nodes/node-1/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(metrics)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /edge-nodes/{nodeId}/heartbeat should return 500 when service fails")
    void shouldReturn500WhenHeartbeatFails() throws Exception {
        // Given
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", 50.0);

        doThrow(new RuntimeException("Connection refused"))
                .when(edgeNodeService).registerHeartbeatByNodeId(anyString(), any());

        // When & Then
        mockMvc.perform(post("/edge-nodes/node-1/heartbeat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(metrics)))
                .andExpect(status().isInternalServerError());
    }

    // ==================== PATCH /edge-nodes/{id}/status ====================

    @Test
    @DisplayName("PATCH /edge-nodes/{id}/status should update status")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldUpdateEdgeNodeStatus() throws Exception {
        // Given
        EdgeNodeStatusUpdateDTO statusUpdate = new EdgeNodeStatusUpdateDTO();
        statusUpdate.setId(1L);
        statusUpdate.setStatus("MAINTENANCE");

        doNothing().when(edgeNodeService).updateEdgeNodeStatus(eq(1L), any(EdgeNodeStatusUpdateDTO.class));

        // When & Then
        mockMvc.perform(patch("/edge-nodes/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PATCH /edge-nodes/{id}/status should return 403 for viewer")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturn403WhenViewerUpdatesStatus() throws Exception {
        // Given
        EdgeNodeStatusUpdateDTO statusUpdate = new EdgeNodeStatusUpdateDTO();
        statusUpdate.setId(1L);
        statusUpdate.setStatus("MAINTENANCE");

        // When & Then
        mockMvc.perform(patch("/edge-nodes/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusUpdate)))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /edge-nodes/{id}/details ====================

    @Test
    @DisplayName("GET /edge-nodes/{id}/details should return details")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnEdgeNodeDetails() throws Exception {
        // Given
        Map<String, Object> details = new HashMap<>();
        details.put("id", 1L);
        details.put("name", "Detail-Node");
        details.put("status", EdgeNode.NodeStatus.ONLINE);

        given(edgeNodeService.getEdgeNodeStatistics(1L)).willReturn(details);

        // When & Then
        mockMvc.perform(get("/edge-nodes/1/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Detail-Node"));
    }

    // ==================== POST /edge-nodes/{id}/test-connection ====================

    @Test
    @DisplayName("POST /edge-nodes/{id}/test-connection should test connection")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldTestConnection() throws Exception {
        // Given
        given(edgeNodeService.testEdgeNodeConnection(1L)).willReturn(true);

        // When & Then
        mockMvc.perform(post("/edge-nodes/1/test-connection"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    @DisplayName("POST /edge-nodes/{id}/test-connection should return 403 for viewer")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturn403WhenViewerTestsConnection() throws Exception {
        // When & Then
        mockMvc.perform(post("/edge-nodes/1/test-connection"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/{id}/restart ====================

    @Test
    @DisplayName("POST /edge-nodes/{id}/restart should restart service")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldRestartEdgeNodeService() throws Exception {
        // Given
        doNothing().when(edgeNodeService).restartEdgeNodeService(1L);

        // When & Then
        mockMvc.perform(post("/edge-nodes/1/restart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restart command sent successfully"));
    }

    @Test
    @DisplayName("POST /edge-nodes/{id}/restart should return 403 for non-admin")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminRestarts() throws Exception {
        // When & Then
        mockMvc.perform(post("/edge-nodes/1/restart"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /edge-nodes/{id}/credentials ====================

    @Test
    @DisplayName("PUT /edge-nodes/{id}/credentials should update credentials")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldUpdateCredentials() throws Exception {
        // Given
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "new-username");
        credentials.put("password", "new-password");

        doNothing().when(edgeNodeService).updateEdgeNodeCredentials(eq(1L), eq("new-username"), eq("new-password"));

        // When & Then
        mockMvc.perform(put("/edge-nodes/1/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Credentials updated successfully"));
    }

    @Test
    @DisplayName("PUT /edge-nodes/{id}/credentials should return 403 for non-admin")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminUpdatesCredentials() throws Exception {
        // Given
        Map<String, String> credentials = new HashMap<>();
        credentials.put("username", "test");
        credentials.put("password", "test");

        // When & Then
        mockMvc.perform(put("/edge-nodes/1/credentials")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/batch-delete ====================

    @Test
    @DisplayName("POST /edge-nodes/batch-delete should batch delete")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBatchDelete() throws Exception {
        // Given
        doNothing().when(edgeNodeService).batchDeleteEdgeNodes(any());

        // When & Then
        mockMvc.perform(post("/edge-nodes/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L, 3L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("批量删除成功"))
                .andExpect(jsonPath("$.deletedCount").value(3));
    }

    @Test
    @DisplayName("POST /edge-nodes/batch-delete should return 403 for non-admin")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminBatchDeletes() throws Exception {
        // When & Then
        mockMvc.perform(post("/edge-nodes/batch-delete")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L))))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/batch-enable ====================

    @Test
    @DisplayName("POST /edge-nodes/batch-enable should batch enable")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBatchEnable() throws Exception {
        // Given
        doNothing().when(edgeNodeService).batchEnableEdgeNodes(any(), eq(true));

        // When & Then
        mockMvc.perform(post("/edge-nodes/batch-enable?enabled=true")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("批量启用成功"))
                .andExpect(jsonPath("$.affectedCount").value(2))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST /edge-nodes/batch-enable should batch disable")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBatchDisable() throws Exception {
        // Given
        doNothing().when(edgeNodeService).batchEnableEdgeNodes(any(), eq(false));

        // When & Then
        mockMvc.perform(post("/edge-nodes/batch-enable?enabled=false")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("批量禁用成功"));
    }

    // ==================== POST /edge-nodes/batch-update-status ====================

    @Test
    @DisplayName("POST /edge-nodes/batch-update-status should batch update status")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldBatchUpdateStatus() throws Exception {
        // Given
        doNothing().when(edgeNodeService).batchUpdateEdgeNodeStatus(any(), eq(EdgeNode.NodeStatus.MAINTENANCE));

        // When & Then
        mockMvc.perform(post("/edge-nodes/batch-update-status?status=MAINTENANCE")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(List.of(1L, 2L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("批量更新状态成功"))
                .andExpect(jsonPath("$.affectedCount").value(2))
                .andExpect(jsonPath("$.status").value("MAINTENANCE"));
    }

    // ==================== GET /edge-nodes/search ====================

    @Test
    @DisplayName("GET /edge-nodes/search should search by keyword")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldSearchEdgeNodes() throws Exception {
        // Given
        EdgeNodeDTO node = createTestEdgeNodeDTO(1L, "Search-Result");
        Page<EdgeNodeDTO> page = new PageImpl<>(List.of(node));

        given(edgeNodeService.searchEdgeNodes(eq("Search"), any(), any(), any(), anyBoolean(), any()))
                .willReturn(page);

        // When & Then
        mockMvc.perform(get("/edge-nodes/search?keyword=Search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Search-Result"));
    }

    @Test
    @DisplayName("GET /edge-nodes/search should return 403 when unauthenticated")
    void shouldReturn403WhenUnauthenticatedSearch() throws Exception {
        // When & Then
        mockMvc.perform(get("/edge-nodes/search?keyword=test"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /edge-nodes/{id}/health-status ====================

    @Test
    @DisplayName("GET /edge-nodes/{id}/health-status should return health status")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnHealthStatus() throws Exception {
        // Given
        given(edgeNodeService.checkNodeHealthStatus(1L)).willReturn("HEALTHY");

        // When & Then
        mockMvc.perform(get("/edge-nodes/1/health-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value(1))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ==================== GET /edge-nodes/{id}/health-details ====================

    @Test
    @DisplayName("GET /edge-nodes/{id}/health-details should return health details")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturnHealthDetails() throws Exception {
        // Given
        Map<String, Object> healthDetails = new HashMap<>();
        healthDetails.put("nodeId", 1L);
        healthDetails.put("nodeName", "Health-Node");
        healthDetails.put("healthStatus", "HEALTHY");
        healthDetails.put("healthScore", 100);

        given(edgeNodeService.getNodeHealthDetails(1L)).willReturn(healthDetails);

        // When & Then
        mockMvc.perform(get("/edge-nodes/1/health-details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodeId").value(1))
                .andExpect(jsonPath("$.nodeName").value("Health-Node"))
                .andExpect(jsonPath("$.healthStatus").value("HEALTHY"))
                .andExpect(jsonPath("$.healthScore").value(100));
    }

    // ==================== POST /edge-nodes/{id}/trigger-failover ====================

    @Test
    @DisplayName("POST /edge-nodes/{id}/trigger-failover should trigger failover and return 202")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldTriggerFailover() throws Exception {
        // Given
        given(edgeNodeFailoverService.triggerFailover(eq(1L), any())).willReturn(100L);

        // When & Then
        mockMvc.perform(post("/edge-nodes/1/trigger-failover"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.message").value("故障转移已触发"))
                .andExpect(jsonPath("$.nodeId").value(1))
                .andExpect(jsonPath("$.eventId").value(100));
    }

    @Test
    @DisplayName("POST /edge-nodes/{id}/trigger-failover should return 403 for non-admin")
    @WithMockUser(username = "operator", roles = {"OPERATOR"})
    void shouldReturn403WhenNonAdminTriggersFailover() throws Exception {
        // When & Then
        mockMvc.perform(post("/edge-nodes/1/trigger-failover"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /edge-nodes/{nodeId}/camera-statuses ====================

    @Test
    @DisplayName("POST /edge-nodes/{nodeId}/camera-statuses should process camera statuses")
    void shouldReportCameraStatuses() throws Exception {
        // Given
        List<Map<String, Object>> cameraStatuses = List.of(
                Map.of("cameraId", 100, "status", "ONLINE"));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("processedCount", 1);
        response.put("totalCount", 1);
        response.put("errorCount", 0);

        given(edgeNodeService.processCameraStatusReports(eq("node-1"), any())).willReturn(response);

        // When & Then
        mockMvc.perform(post("/edge-nodes/node-1/camera-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cameraStatuses)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.processedCount").value(1));
    }

    // ==================== Error scenarios ====================

    @Test
    @DisplayName("GET /edge-nodes/{id} should propagate service exception as 500")
    @WithMockUser(username = "viewer", roles = {"VIEWER"})
    void shouldReturn500WhenServiceThrowsException() throws Exception {
        // Given
        given(edgeNodeService.getEdgeNodeById(99L))
                .willThrow(new RuntimeException("Edge node not found"));

        // When & Then
        mockMvc.perform(get("/edge-nodes/99"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /edge-nodes should return 400 when request body is invalid")
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void shouldReturn400WhenRequestBodyIsInvalid() throws Exception {
        // When & Then - sending empty JSON
        mockMvc.perform(post("/edge-nodes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }
}

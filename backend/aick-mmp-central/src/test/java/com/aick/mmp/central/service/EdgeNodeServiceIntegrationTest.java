package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.EdgeNodeDTO;
import com.aick.mmp.central.dto.EdgeNodeStatusUpdateDTO;
import com.aick.mmp.central.repository.EdgeNodeRepository;
import com.aick.mmp.shared.exception.ServiceException;
import com.aick.mmp.shared.model.EdgeNode;
import com.aick.test.ServiceIntegrationTestConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = ServiceIntegrationTestConfig.class)
@ActiveProfiles("test")
@Transactional
public class EdgeNodeServiceIntegrationTest {

    @Autowired
    private EdgeNodeService edgeNodeService;

    @Autowired
    private EdgeNodeRepository edgeNodeRepository;

    private EdgeNodeDTO createValidEdgeNodeDTO(String name, String ipAddress) {
        return EdgeNodeDTO.builder()
                .name(name)
                .uuid("test-uuid-" + System.nanoTime())
                .location("Test Location")
                .ipAddress(ipAddress)
                .port(8081)
                .cpuUsage(45.0)
                .memoryUsage(60.0)
                .storageUsage(30.0)
                .maxCameraSupport(16)
                .currentCameraCount(5)
                .softwareVersion("v2.1.0")
                .hardwareInfo("Test Hardware")
                .networkBandwidth("1000Mbps")
                .enabled(true)
                .build();
    }

    @Test
    @DisplayName("should create edge node with valid data")
    void shouldCreateEdgeNodeWithValidData() {
        // Given
        EdgeNodeDTO request = createValidEdgeNodeDTO("New-Edge-Node", "10.100.1.1");

        // When
        EdgeNodeDTO result = edgeNodeService.createEdgeNode(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("New-Edge-Node");
        assertThat(result.getIpAddress()).isEqualTo("10.100.1.1");
        assertThat(result.getPort()).isEqualTo(8081);
        assertThat(result.getStatus()).isEqualTo(EdgeNode.NodeStatus.ONLINE);
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getUuid()).isNotNull();

        // Verify database state
        EdgeNode saved = edgeNodeRepository.findById(result.getId()).orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getName()).isEqualTo("New-Edge-Node");
        assertThat(saved.getStatus()).isEqualTo(EdgeNode.NodeStatus.ONLINE);
        assertThat(saved.getLastHeartbeatTime()).isNotNull();
        assertThat(saved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should get edge node by id")
    void shouldGetEdgeNodeById() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Get-ById-Node", "10.100.2.1"));

        // When
        EdgeNodeDTO result = edgeNodeService.getEdgeNodeById(created.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getName()).isEqualTo("Get-ById-Node");
        assertThat(result.getIpAddress()).isEqualTo("10.100.2.1");
    }

    @Test
    @DisplayName("should throw exception when getting non-existing edge node")
    void shouldThrowExceptionWhenGettingNonExistingEdgeNode() {
        // When & Then
        assertThatThrownBy(() -> edgeNodeService.getEdgeNodeById(99999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Edge node not found with id: 99999");
    }

    @Test
    @DisplayName("should update edge node")
    void shouldUpdateEdgeNode() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Update-Target-Node", "10.100.3.1"));

        EdgeNodeDTO updateRequest = EdgeNodeDTO.builder()
                .name("Updated-Node-Name")
                .location("Updated Location")
                .ipAddress("10.100.3.1") // same IP to avoid duplicate check
                .port(9090)
                .cpuUsage(80.0)
                .memoryUsage(75.0)
                .maxCameraSupport(32)
                .enabled(false)
                .build();

        // When
        EdgeNodeDTO result = edgeNodeService.updateEdgeNode(created.getId(), updateRequest);

        // Then
        assertThat(result.getId()).isEqualTo(created.getId());
        assertThat(result.getName()).isEqualTo("Updated-Node-Name");
        assertThat(result.getLocation()).isEqualTo("Updated Location");
        assertThat(result.getPort()).isEqualTo(9090);
        assertThat(result.getCpuUsage()).isEqualTo(80.0);
        assertThat(result.getMemoryUsage()).isEqualTo(75.0);
        assertThat(result.isEnabled()).isFalse();

        // Verify database state
        EdgeNode updated = edgeNodeRepository.findById(created.getId()).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getName()).isEqualTo("Updated-Node-Name");
        assertThat(updated.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should throw exception when updating non-existing edge node")
    void shouldThrowExceptionWhenUpdatingNonExistingEdgeNode() {
        // Given
        EdgeNodeDTO updateRequest = createValidEdgeNodeDTO("Non-Existent", "10.100.99.1");

        // When & Then
        assertThatThrownBy(() -> edgeNodeService.updateEdgeNode(99999L, updateRequest))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Edge node not found with id: 99999");
    }

    @Test
    @DisplayName("should delete edge node")
    void shouldDeleteEdgeNode() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Delete-Target-Node", "10.100.4.1"));

        // When
        edgeNodeService.deleteEdgeNode(created.getId());

        // Then
        assertThat(edgeNodeRepository.findById(created.getId())).isEmpty();
    }

    @Test
    @DisplayName("should throw exception when deleting non-existing edge node")
    void shouldThrowExceptionWhenDeletingNonExistingEdgeNode() {
        // When & Then
        assertThatThrownBy(() -> edgeNodeService.deleteEdgeNode(99999L))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Edge node not found with id: 99999");
    }

    @Test
    @DisplayName("should get all edge nodes with pagination")
    void shouldGetAllEdgeNodesWithPagination() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Page-Node-1", "10.100.5.1"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Page-Node-2", "10.100.5.2"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Page-Node-3", "10.100.5.3"));

        // When
        Page<EdgeNodeDTO> page = edgeNodeService.getAllEdgeNodes(PageRequest.of(0, 10));

        // Then
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent().size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("should get edge nodes by location")
    void shouldGetEdgeNodesByLocation() {
        // Given
        EdgeNodeDTO node1 = createValidEdgeNodeDTO("Loc-Node-1", "10.100.6.1");
        node1.setLocation("DataCenter-East");
        edgeNodeService.createEdgeNode(node1);

        EdgeNodeDTO node2 = createValidEdgeNodeDTO("Loc-Node-2", "10.100.6.2");
        node2.setLocation("DataCenter-West");
        edgeNodeService.createEdgeNode(node2);

        // When
        Page<EdgeNodeDTO> eastNodes = edgeNodeService.getEdgeNodesByLocation("DataCenter-East",
                PageRequest.of(0, 10));

        // Then
        assertThat(eastNodes.getContent()).hasSize(1);
        assertThat(eastNodes.getContent().get(0).getLocation()).isEqualTo("DataCenter-East");
    }

    @Test
    @DisplayName("should get edge nodes by status")
    void shouldGetEdgeNodesByStatus() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Status-Node-1", "10.100.7.1"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Status-Node-2", "10.100.7.2"));

        // When
        Page<EdgeNodeDTO> onlineNodes = edgeNodeService.getEdgeNodesByStatus(
                EdgeNode.NodeStatus.ONLINE, PageRequest.of(0, 10));

        // Then
        assertThat(onlineNodes.getContent()).isNotEmpty();
        assertThat(onlineNodes.getContent())
                .allMatch(node -> node.getStatus() == EdgeNode.NodeStatus.ONLINE);
    }

    @Test
    @DisplayName("should register heartbeat by node id")
    void shouldRegisterHeartbeatByNodeId() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Heartbeat-Node", "10.100.8.1"));
        LocalDateTime beforeHeartbeat = LocalDateTime.now();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", 50.0);
        metrics.put("memoryUsage", 65.0);
        metrics.put("storageUsage", 40.0);

        // When
        edgeNodeService.registerHeartbeat(created.getId(), metrics);

        // Then
        EdgeNode updated = edgeNodeRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getLastHeartbeatTime()).isNotNull();
        assertThat(updated.getLastHeartbeatTime()).isAfter(beforeHeartbeat);
        assertThat(updated.getStatus()).isEqualTo(EdgeNode.NodeStatus.ONLINE);
        assertThat(updated.getCpuUsage()).isEqualTo(50.0);
        assertThat(updated.getMemoryUsage()).isEqualTo(65.0);
        assertThat(updated.getStorageUsage()).isEqualTo(40.0);
    }

    @Test
    @DisplayName("should register heartbeat by node name (string id)")
    void shouldRegisterHeartbeatByNodeName() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("HB-Name-Node", "10.100.9.1"));
        LocalDateTime beforeHeartbeat = LocalDateTime.now();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("cpuUsage", 30.0);
        metrics.put("memoryUsage", 45.0);

        // When - use node name as string id
        edgeNodeService.registerHeartbeatByNodeId("HB-Name-Node", metrics);

        // Then
        EdgeNode updated = edgeNodeRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getLastHeartbeatTime()).isAfter(beforeHeartbeat);
        assertThat(updated.getCpuUsage()).isEqualTo(30.0);
        assertThat(updated.getMemoryUsage()).isEqualTo(45.0);
    }

    @Test
    @DisplayName("should register heartbeat by node uuid (string id)")
    void shouldRegisterHeartbeatByNodeUuid() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("HB-Uuid-Node", "10.100.10.1"));
        String nodeUuid = created.getUuid();

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("softwareVersion", "v3.0.0");

        // When - use node uuid as string id
        edgeNodeService.registerHeartbeatByNodeId(nodeUuid, metrics);

        // Then
        EdgeNode updated = edgeNodeRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getSoftwareVersion()).isEqualTo("v3.0.0");
    }

    @Test
    @DisplayName("should throw exception when registering heartbeat for non-existing node id")
    void shouldThrowExceptionWhenRegisteringHeartbeatForNonExistingNode() {
        // When & Then
        assertThatThrownBy(() -> edgeNodeService.registerHeartbeat(99999L, new HashMap<>()))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("Edge node not found with id: 99999");
    }

    @Test
    @DisplayName("should update edge node status")
    void shouldUpdateEdgeNodeStatus() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Status-Update-Node", "10.100.11.1"));

        EdgeNodeStatusUpdateDTO statusUpdate = new EdgeNodeStatusUpdateDTO();
        statusUpdate.setId(created.getId());
        statusUpdate.setStatus("MAINTENANCE");

        // When
        edgeNodeService.updateEdgeNodeStatus(created.getId(), statusUpdate);

        // Then
        EdgeNode updated = edgeNodeRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(EdgeNode.NodeStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("should get online edge nodes")
    void shouldGetOnlineEdgeNodes() {
        // Given
        EdgeNodeDTO onlineNode = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Online-Node-Get", "10.100.12.1"));

        EdgeNodeDTO offlineNodeDTO = createValidEdgeNodeDTO("Offline-Node-Get", "10.100.12.2");
        EdgeNodeDTO createdOffline = edgeNodeService.createEdgeNode(offlineNodeDTO);
        // Manually set status to OFFLINE via repository
        EdgeNode entity = edgeNodeRepository.findById(createdOffline.getId()).orElseThrow();
        entity.setStatus(EdgeNode.NodeStatus.OFFLINE);
        edgeNodeRepository.save(entity);

        // When
        List<EdgeNodeDTO> onlineNodes = edgeNodeService.getOnlineEdgeNodes();

        // Then
        assertThat(onlineNodes).isNotEmpty();
        assertThat(onlineNodes).allMatch(node -> node.getStatus() == EdgeNode.NodeStatus.ONLINE);
        assertThat(onlineNodes).extracting(EdgeNodeDTO::getName)
                .contains("Online-Node-Get");
    }

    @Test
    @DisplayName("should get edge node count by status")
    void shouldGetEdgeNodeCountByStatus() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Count-Node-A", "10.100.13.1"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Count-Node-B", "10.100.13.2"));

        // When
        long onlineCount = edgeNodeService.getEdgeNodeCountByStatus(EdgeNode.NodeStatus.ONLINE);
        long totalCount = edgeNodeService.getEdgeNodeCount();

        // Then
        assertThat(onlineCount).isGreaterThanOrEqualTo(2);
        assertThat(totalCount).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("should search edge nodes by keyword")
    void shouldSearchEdgeNodesByKeyword() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Search-Node-Alpha", "10.100.14.1"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Search-Node-Beta", "10.100.14.2"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Other-Node", "10.100.14.3"));

        // When
        Page<EdgeNodeDTO> result = edgeNodeService.searchEdgeNodes(
                "Alpha", null, null, null, false, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Search-Node-Alpha");
    }

    @Test
    @DisplayName("should search edge nodes by ip address keyword")
    void shouldSearchEdgeNodesByIpAddress() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Ip-Search-1", "192.168.50.1"));
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Ip-Search-2", "10.100.15.1"));

        // When
        Page<EdgeNodeDTO> result = edgeNodeService.searchEdgeNodes(
                "192.168", null, null, null, false, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getIpAddress()).isEqualTo("192.168.50.1");
    }

    @Test
    @DisplayName("should search edge nodes by status filter")
    void shouldSearchEdgeNodesByStatusFilter() {
        // Given
        edgeNodeService.createEdgeNode(createValidEdgeNodeDTO("Filter-Online", "10.100.16.1"));
        // Create and set OFFLINE
        EdgeNodeDTO offlineDTO = createValidEdgeNodeDTO("Filter-Offline", "10.100.16.2");
        EdgeNodeDTO createdOffline = edgeNodeService.createEdgeNode(offlineDTO);
        EdgeNode entity = edgeNodeRepository.findById(createdOffline.getId()).orElseThrow();
        entity.setStatus(EdgeNode.NodeStatus.OFFLINE);
        edgeNodeRepository.save(entity);

        // When
        Page<EdgeNodeDTO> onlineOnly = edgeNodeService.searchEdgeNodes(
                null, EdgeNode.NodeStatus.ONLINE, null, null, false, PageRequest.of(0, 10));

        // Then
        assertThat(onlineOnly.getContent()).isNotEmpty();
        assertThat(onlineOnly.getContent())
                .allMatch(node -> node.getStatus() == EdgeNode.NodeStatus.ONLINE);
        assertThat(onlineOnly.getContent())
                .extracting(EdgeNodeDTO::getName)
                .contains("Filter-Online");
    }

    @Test
    @DisplayName("should get edge node statistics")
    void shouldGetEdgeNodeStatistics() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Stats-Node", "10.100.17.1"));

        // When
        Map<String, Object> stats = edgeNodeService.getEdgeNodeStatistics(created.getId());

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.get("id")).isEqualTo(created.getId());
        assertThat(stats.get("name")).isEqualTo("Stats-Node");
        assertThat(stats.get("location")).isEqualTo("Test Location");
    }

    @Test
    @DisplayName("should check node health status")
    void shouldCheckNodeHealthStatus() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Health-Node", "10.100.18.1"));

        // When
        String healthStatus = edgeNodeService.checkNodeHealthStatus(created.getId());

        // Then
        assertThat(healthStatus).isNotNull();
        assertThat(healthStatus).isNotEmpty();
    }

    @Test
    @DisplayName("should get node health details")
    void shouldGetNodeHealthDetails() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Health-Detail-Node", "10.100.19.1"));

        // When
        Map<String, Object> healthDetails = edgeNodeService.getNodeHealthDetails(created.getId());

        // Then
        assertThat(healthDetails).isNotNull();
        assertThat(healthDetails.get("nodeId")).isEqualTo(created.getId());
        assertThat(healthDetails.get("nodeName")).isEqualTo("Health-Detail-Node");
        assertThat(healthDetails.get("healthStatus")).isNotNull();
        assertThat(healthDetails.get("healthScore")).isNotNull();
    }

    @Test
    @DisplayName("should register edge node")
    void shouldRegisterEdgeNode() {
        // Given
        EdgeNodeDTO request = createValidEdgeNodeDTO("Register-New-Node", "10.100.20.1");
        request.setUuid("custom-register-uuid");

        // When
        EdgeNodeDTO result = edgeNodeService.registerEdgeNode(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Register-New-Node");
        assertThat(result.getStatus()).isEqualTo(EdgeNode.NodeStatus.ONLINE);

        // Verify
        EdgeNode saved = edgeNodeRepository.findById(result.getId()).orElseThrow();
        assertThat(saved.getUuid()).isEqualTo("custom-register-uuid");
    }

    @Test
    @DisplayName("should register edge node updating existing one by name")
    void shouldRegisterEdgeNodeUpdateExistingByName() {
        // Given
        EdgeNodeDTO existing = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("ReRegister-Node", "10.100.21.1"));

        EdgeNodeDTO reRegisterRequest = EdgeNodeDTO.builder()
                .name("ReRegister-Node")
                .ipAddress("10.100.21.2") // new IP
                .port(8081)
                .cpuUsage(90.0)
                .build();

        // When
        EdgeNodeDTO result = edgeNodeService.registerEdgeNode(reRegisterRequest);

        // Then - should update existing node, not create a new one
        assertThat(result.getId()).isEqualTo(existing.getId());
        assertThat(result.getIpAddress()).isEqualTo("10.100.21.2");
        assertThat(result.getCpuUsage()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("should batch delete edge nodes")
    void shouldBatchDeleteEdgeNodes() {
        // Given
        EdgeNodeDTO node1 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Del-1", "10.100.22.1"));
        EdgeNodeDTO node2 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Del-2", "10.100.22.2"));

        // When
        edgeNodeService.batchDeleteEdgeNodes(List.of(node1.getId(), node2.getId()));

        // Then
        assertThat(edgeNodeRepository.findById(node1.getId())).isEmpty();
        assertThat(edgeNodeRepository.findById(node2.getId())).isEmpty();
    }

    @Test
    @DisplayName("should batch enable edge nodes")
    void shouldBatchEnableEdgeNodes() {
        // Given
        EdgeNodeDTO node1 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Enable-1", "10.100.23.1"));
        EdgeNodeDTO node2 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Enable-2", "10.100.23.2"));

        // When - disable them
        edgeNodeService.batchEnableEdgeNodes(
                List.of(node1.getId(), node2.getId()), false);

        // Then
        EdgeNode disabled1 = edgeNodeRepository.findById(node1.getId()).orElseThrow();
        EdgeNode disabled2 = edgeNodeRepository.findById(node2.getId()).orElseThrow();
        assertThat(disabled1.isEnabled()).isFalse();
        assertThat(disabled2.isEnabled()).isFalse();

        // When - re-enable them
        edgeNodeService.batchEnableEdgeNodes(
                List.of(node1.getId(), node2.getId()), true);

        // Then
        EdgeNode enabled1 = edgeNodeRepository.findById(node1.getId()).orElseThrow();
        EdgeNode enabled2 = edgeNodeRepository.findById(node2.getId()).orElseThrow();
        assertThat(enabled1.isEnabled()).isTrue();
        assertThat(enabled2.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should batch update edge node status")
    void shouldBatchUpdateEdgeNodeStatus() {
        // Given
        EdgeNodeDTO node1 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Status-1", "10.100.24.1"));
        EdgeNodeDTO node2 = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Batch-Status-2", "10.100.24.2"));

        // When
        edgeNodeService.batchUpdateEdgeNodeStatus(
                List.of(node1.getId(), node2.getId()), EdgeNode.NodeStatus.MAINTENANCE);

        // Then
        EdgeNode updated1 = edgeNodeRepository.findById(node1.getId()).orElseThrow();
        EdgeNode updated2 = edgeNodeRepository.findById(node2.getId()).orElseThrow();
        assertThat(updated1.getStatus()).isEqualTo(EdgeNode.NodeStatus.MAINTENANCE);
        assertThat(updated2.getStatus()).isEqualTo(EdgeNode.NodeStatus.MAINTENANCE);
    }

    @Test
    @DisplayName("should test edge node connection")
    void shouldTestEdgeNodeConnection() {
        // Given
        EdgeNodeDTO created = edgeNodeService.createEdgeNode(
                createValidEdgeNodeDTO("Connection-Test-Node", "10.100.25.1"));

        // When
        boolean isConnected = edgeNodeService.testEdgeNodeConnection(created.getId());

        // Then
        assertThat(isConnected).isTrue();
    }

    @Test
    @DisplayName("should get edge nodes by region id")
    void shouldGetEdgeNodesByRegionId() {
        // Given
        EdgeNodeDTO nodeDTO = createValidEdgeNodeDTO("Region-Service-1", "10.100.26.1");
        nodeDTO.setRegionId(500L);
        edgeNodeService.createEdgeNode(nodeDTO);

        EdgeNodeDTO nodeDTO2 = createValidEdgeNodeDTO("Region-Service-2", "10.100.26.2");
        nodeDTO2.setRegionId(500L);
        edgeNodeService.createEdgeNode(nodeDTO2);

        // When
        Page<EdgeNodeDTO> regionNodes = edgeNodeService.getEdgeNodesByRegionId(
                500L, false, PageRequest.of(0, 10));

        // Then
        assertThat(regionNodes.getContent()).hasSize(2);
        assertThat(regionNodes.getContent()).extracting(EdgeNodeDTO::getName)
                .containsExactlyInAnyOrder("Region-Service-1", "Region-Service-2");
    }
}

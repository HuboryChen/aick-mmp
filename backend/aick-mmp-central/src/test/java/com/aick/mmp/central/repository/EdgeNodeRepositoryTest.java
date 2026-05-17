package com.aick.mmp.central.repository;

import com.aick.test.DataJpaTestConfig;
import com.aick.mmp.shared.model.EdgeNode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = DataJpaTestConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class EdgeNodeRepositoryTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private EdgeNodeRepository repository;

    private EdgeNode createTestEdgeNode(String name, String uuid, String ipAddress, EdgeNode.NodeStatus status) {
        return EdgeNode.builder()
                .name(name)
                .uuid(uuid)
                .location("Test Location")
                .ipAddress(ipAddress)
                .port(8081)
                .status(status)
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
                .systemMetrics(null)
                .build();
    }

    @Test
    @DisplayName("should save and retrieve edge node by id")
    void shouldSaveAndRetrieveEdgeNodeById() {
        // Given
        EdgeNode node = createTestEdgeNode("Test-Node-1", "UUID-001", "192.168.1.1", EdgeNode.NodeStatus.ONLINE);

        // When
        EdgeNode saved = repository.save(node);
        em.flush();
        em.clear();

        EdgeNode retrieved = repository.findById(saved.getId()).orElse(null);

        // Then
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(saved.getId());
        assertThat(retrieved.getName()).isEqualTo("Test-Node-1");
        assertThat(retrieved.getUuid()).isEqualTo("UUID-001");
        assertThat(retrieved.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(retrieved.getPort()).isEqualTo(8081);
        assertThat(retrieved.getStatus()).isEqualTo(EdgeNode.NodeStatus.ONLINE);
        assertThat(retrieved.getLocation()).isEqualTo("Test Location");
        assertThat(retrieved.getCpuUsage()).isEqualTo(45.0);
        assertThat(retrieved.getMemoryUsage()).isEqualTo(60.0);
        assertThat(retrieved.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should find edge nodes by status")
    void shouldFindEdgeNodesByStatus() {
        // Given
        EdgeNode onlineNode = createTestEdgeNode("Online-Node", "UUID-ON-1", "10.0.0.1", EdgeNode.NodeStatus.ONLINE);
        EdgeNode offlineNode = createTestEdgeNode("Offline-Node", "UUID-OFF-1", "10.0.0.2", EdgeNode.NodeStatus.OFFLINE);
        repository.save(onlineNode);
        repository.save(offlineNode);
        em.flush();
        em.clear();

        // When - findByStatus with pagination
        Page<EdgeNode> onlinePage = repository.findByStatus(EdgeNode.NodeStatus.ONLINE, PageRequest.of(0, 10));
        Page<EdgeNode> offlinePage = repository.findByStatus(EdgeNode.NodeStatus.OFFLINE, PageRequest.of(0, 10));

        // Then
        assertThat(onlinePage.getContent()).hasSize(1);
        assertThat(onlinePage.getContent().get(0).getName()).isEqualTo("Online-Node");
        assertThat(offlinePage.getContent()).hasSize(1);
        assertThat(offlinePage.getContent().get(0).getStatus()).isEqualTo(EdgeNode.NodeStatus.OFFLINE);
    }

    @Test
    @DisplayName("should find edge nodes by status without pagination")
    void shouldFindEdgeNodesByStatusNoPagination() {
        // Given
        repository.save(createTestEdgeNode("Node-A", "UUID-A", "10.0.0.10", EdgeNode.NodeStatus.ONLINE));
        repository.save(createTestEdgeNode("Node-B", "UUID-B", "10.0.0.11", EdgeNode.NodeStatus.ONLINE));
        repository.save(createTestEdgeNode("Node-C", "UUID-C", "10.0.0.12", EdgeNode.NodeStatus.OFFLINE));
        em.flush();
        em.clear();

        // When
        List<EdgeNode> onlineNodes = repository.findByStatus(EdgeNode.NodeStatus.ONLINE);

        // Then
        assertThat(onlineNodes).hasSize(2);
        assertThat(onlineNodes).extracting(EdgeNode::getStatus)
                .allMatch(status -> status == EdgeNode.NodeStatus.ONLINE);
    }

    @Test
    @DisplayName("should find edge nodes by status in list")
    void shouldFindEdgeNodesByStatusIn() {
        // Given
        repository.save(createTestEdgeNode("Node-Online", "UUID-IN-1", "10.0.1.1", EdgeNode.NodeStatus.ONLINE));
        repository.save(createTestEdgeNode("Node-Connecting", "UUID-IN-2", "10.0.1.2", EdgeNode.NodeStatus.CONNECTING));
        repository.save(createTestEdgeNode("Node-Offline", "UUID-IN-3", "10.0.1.3", EdgeNode.NodeStatus.OFFLINE));
        em.flush();
        em.clear();

        // When
        List<EdgeNode> nodes = repository.findByStatusIn(
                List.of(EdgeNode.NodeStatus.ONLINE, EdgeNode.NodeStatus.CONNECTING));

        // Then
        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(EdgeNode::getStatus)
                .containsExactlyInAnyOrder(EdgeNode.NodeStatus.ONLINE, EdgeNode.NodeStatus.CONNECTING);
    }

    @Test
    @DisplayName("should find edge node by location")
    void shouldFindEdgeNodeByLocation() {
        // Given
        EdgeNode node = createTestEdgeNode("Location-Node", "UUID-LOC-1", "10.0.2.1", EdgeNode.NodeStatus.ONLINE);
        node.setLocation("DataCenter-A");
        repository.save(node);
        em.flush();
        em.clear();

        // When
        Page<EdgeNode> result = repository.findByLocation("DataCenter-A", PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getLocation()).isEqualTo("DataCenter-A");
    }

    @Test
    @DisplayName("should find edge node by uuid")
    void shouldFindEdgeNodeByUuid() {
        // Given
        EdgeNode node = createTestEdgeNode("Uuid-Node", "TARGET-UUID-123", "10.0.3.1", EdgeNode.NodeStatus.ONLINE);
        repository.save(node);
        em.flush();
        em.clear();

        // When
        EdgeNode result = repository.findByUuid("TARGET-UUID-123").orElse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Uuid-Node");
        assertThat(result.getUuid()).isEqualTo("TARGET-UUID-123");
    }

    @Test
    @DisplayName("should return empty when finding by non-existing uuid")
    void shouldReturnEmptyWhenFindingByNonExistingUuid() {
        // When
        EdgeNode result = repository.findByUuid("NON-EXISTENT-UUID").orElse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should find edge node by name")
    void shouldFindEdgeNodeByName() {
        // Given
        EdgeNode node = createTestEdgeNode("Target-Name-Node", "UUID-NAME-1", "10.0.4.1", EdgeNode.NodeStatus.ONLINE);
        repository.save(node);
        em.flush();
        em.clear();

        // When
        EdgeNode result = repository.findByName("Target-Name-Node").orElse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUuid()).isEqualTo("UUID-NAME-1");
    }

    @Test
    @DisplayName("should return empty when finding by non-existing name")
    void shouldReturnEmptyWhenFindingByNonExistingName() {
        // When
        EdgeNode result = repository.findByName("Non-Existent-Node").orElse(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("should count edge nodes by status")
    void shouldCountEdgeNodesByStatus() {
        // Given
        repository.save(createTestEdgeNode("Count-Node-1", "UUID-COUNT-1", "10.0.5.1", EdgeNode.NodeStatus.ONLINE));
        repository.save(createTestEdgeNode("Count-Node-2", "UUID-COUNT-2", "10.0.5.2", EdgeNode.NodeStatus.ONLINE));
        repository.save(createTestEdgeNode("Count-Node-3", "UUID-COUNT-3", "10.0.5.3", EdgeNode.NodeStatus.ERROR));
        em.flush();
        em.clear();

        // When
        long onlineCount = repository.countByStatus(EdgeNode.NodeStatus.ONLINE);
        long errorCount = repository.countByStatus(EdgeNode.NodeStatus.ERROR);
        long offlineCount = repository.countByStatus(EdgeNode.NodeStatus.OFFLINE);

        // Then
        assertThat(onlineCount).isEqualTo(2);
        assertThat(errorCount).isEqualTo(1);
        assertThat(offlineCount).isEqualTo(0);
    }

    @Test
    @DisplayName("should check if edge node exists by name")
    void shouldCheckExistsByName() {
        // Given
        repository.save(createTestEdgeNode("Exists-Node", "UUID-EXISTS-1", "10.0.6.1", EdgeNode.NodeStatus.ONLINE));
        em.flush();
        em.clear();

        // When
        boolean exists = repository.existsByName("Exists-Node");
        boolean notExists = repository.existsByName("No-Such-Node");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should check if edge node exists by ip and port")
    void shouldCheckExistsByIpAndPort() {
        // Given
        repository.save(createTestEdgeNode("IpPort-Node", "UUID-IPPORT-1", "10.0.7.1", EdgeNode.NodeStatus.ONLINE));
        em.flush();
        em.clear();

        // When
        boolean exists = repository.existsByIpAddressAndPort("10.0.7.1", 8081);
        boolean notExists = repository.existsByIpAddressAndPort("10.0.7.99", 8081);

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("should find edge node by ip address")
    void shouldFindEdgeNodeByIpAddress() {
        // Given
        EdgeNode node = createTestEdgeNode("Ip-Node", "UUID-IP-1", "10.0.8.1", EdgeNode.NodeStatus.ONLINE);
        repository.save(node);
        em.flush();
        em.clear();

        // When
        EdgeNode result = repository.findByIpAddress("10.0.8.1").orElse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Ip-Node");
    }

    @Test
    @DisplayName("should find edge nodes by status and enabled flag")
    void shouldFindEdgeNodesByStatusAndEnabled() {
        // Given
        EdgeNode enabledNode = createTestEdgeNode("Enabled-Node", "UUID-EN-1", "10.0.9.1", EdgeNode.NodeStatus.ONLINE);
        enabledNode.setEnabled(true);
        EdgeNode disabledNode = createTestEdgeNode("Disabled-Node", "UUID-DIS-1", "10.0.9.2", EdgeNode.NodeStatus.ONLINE);
        disabledNode.setEnabled(false);
        repository.save(enabledNode);
        repository.save(disabledNode);
        em.flush();
        em.clear();

        // When
        List<EdgeNode> enabledOnline = repository.findByStatusAndEnabled(EdgeNode.NodeStatus.ONLINE, true);
        List<EdgeNode> disabledOnline = repository.findByStatusAndEnabled(EdgeNode.NodeStatus.ONLINE, false);

        // Then
        assertThat(enabledOnline).hasSize(1);
        assertThat(enabledOnline.get(0).getName()).isEqualTo("Enabled-Node");
        assertThat(disabledOnline).hasSize(1);
        assertThat(disabledOnline.get(0).getName()).isEqualTo("Disabled-Node");
    }

    @Test
    @DisplayName("should update last heartbeat time")
    void shouldUpdateLastHeartbeatTime() {
        // Given
        LocalDateTime oldHeartbeat = LocalDateTime.now().minusHours(1);
        EdgeNode node = createTestEdgeNode("Heartbeat-Node", "UUID-HB-1", "10.0.10.1", EdgeNode.NodeStatus.ONLINE);
        node.setLastHeartbeatTime(oldHeartbeat);
        EdgeNode saved = repository.save(node);
        em.flush();
        em.clear();

        // When - reload and update heartbeat, keeping systemMetrics null to avoid H2 JSON round-trip
        EdgeNode toUpdate = repository.findById(saved.getId()).orElseThrow();
        LocalDateTime newHeartbeat = LocalDateTime.now();
        toUpdate.setLastHeartbeatTime(newHeartbeat);
        toUpdate.setSystemMetrics(null);
        EdgeNode reSaved = repository.save(toUpdate);
        em.flush();
        em.clear();

        // Then
        EdgeNode updated = repository.findById(reSaved.getId()).orElseThrow();
        assertThat(updated.getLastHeartbeatTime()).isNotNull();
        assertThat(updated.getLastHeartbeatTime()).isAfter(oldHeartbeat);
    }

    @Test
    @DisplayName("should delete edge node")
    void shouldDeleteEdgeNode() {
        // Given
        EdgeNode node = createTestEdgeNode("Delete-Node", "UUID-DEL-1", "10.0.11.1", EdgeNode.NodeStatus.OFFLINE);
        EdgeNode saved = repository.save(node);
        em.flush();
        em.clear();

        // When
        repository.deleteById(saved.getId());
        em.flush();
        em.clear();

        // Then
        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    @DisplayName("should find edge nodes by region id")
    void shouldFindEdgeNodesByRegionId() {
        // Given
        EdgeNode node1 = createTestEdgeNode("Region-Node-1", "UUID-REG-1", "10.0.12.1", EdgeNode.NodeStatus.ONLINE);
        node1.setRegionId(100L);
        EdgeNode node2 = createTestEdgeNode("Region-Node-2", "UUID-REG-2", "10.0.12.2", EdgeNode.NodeStatus.ONLINE);
        node2.setRegionId(100L);
        EdgeNode node3 = createTestEdgeNode("Region-Node-3", "UUID-REG-3", "10.0.12.3", EdgeNode.NodeStatus.ONLINE);
        node3.setRegionId(200L);
        repository.saveAll(List.of(node1, node2, node3));
        em.flush();
        em.clear();

        // When
        List<EdgeNode> region100Nodes = repository.findByRegionId(100L);
        long countRegion100 = repository.countByRegionId(100L);

        // Then
        assertThat(region100Nodes).hasSize(2);
        assertThat(region100Nodes).extracting(EdgeNode::getName)
                .containsExactlyInAnyOrder("Region-Node-1", "Region-Node-2");
        assertThat(countRegion100).isEqualTo(2);
    }

    @Test
    @DisplayName("should find edge nodes by region id with pagination")
    void shouldFindEdgeNodesByRegionIdWithPagination() {
        // Given
        EdgeNode node = createTestEdgeNode("Region-Page-1", "UUID-RP-1", "10.0.13.1", EdgeNode.NodeStatus.ONLINE);
        node.setRegionId(300L);
        repository.save(node);
        em.flush();
        em.clear();

        // When
        Page<EdgeNode> result = repository.findByRegionId(300L, PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getRegionId()).isEqualTo(300L);
    }

    @Test
    @DisplayName("should find edge nodes by region ids in list")
    void shouldFindEdgeNodesByRegionIdIn() {
        // Given
        EdgeNode node1 = createTestEdgeNode("Region-In-1", "UUID-RI-1", "10.0.14.1", EdgeNode.NodeStatus.ONLINE);
        node1.setRegionId(10L);
        EdgeNode node2 = createTestEdgeNode("Region-In-2", "UUID-RI-2", "10.0.14.2", EdgeNode.NodeStatus.ONLINE);
        node2.setRegionId(20L);
        EdgeNode node3 = createTestEdgeNode("Region-In-3", "UUID-RI-3", "10.0.14.3", EdgeNode.NodeStatus.ONLINE);
        node3.setRegionId(30L);
        repository.saveAll(List.of(node1, node2, node3));
        em.flush();
        em.clear();

        // When
        List<EdgeNode> nodes = repository.findByRegionIdIn(List.of(10L, 30L));
        long count = repository.countByRegionIdIn(List.of(10L, 30L));

        // Then
        assertThat(nodes).hasSize(2);
        assertThat(nodes).extracting(EdgeNode::getRegionId)
                .containsExactlyInAnyOrder(10L, 30L);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("should find edge node by ip address and port")
    void shouldFindEdgeNodeByIpAddressAndPort() {
        // Given
        EdgeNode node = createTestEdgeNode("IpPort-Find", "UUID-IPF-1", "10.0.15.1", EdgeNode.NodeStatus.ONLINE);
        node.setPort(9090);
        repository.save(node);
        em.flush();
        em.clear();

        // When
        EdgeNode result = repository.findByIpAddressAndPort("10.0.15.1", 9090).orElse(null);
        EdgeNode notFound = repository.findByIpAddressAndPort("10.0.15.1", 9999).orElse(null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("IpPort-Find");
        assertThat(notFound).isNull();
    }

    @Test
    @DisplayName("should return empty page when no location matches")
    void shouldReturnEmptyPageWhenNoLocationMatches() {
        // When
        Page<EdgeNode> result = repository.findByLocation("NonExistentLocation", PageRequest.of(0, 10));

        // Then
        assertThat(result.getContent()).isEmpty();
    }
}

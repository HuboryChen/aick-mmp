package com.aick.mmp.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cdn_nodes")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CdnNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ipAddress;

    public enum NodeStatus {
        ONLINE, OFFLINE, MAINTENANCE, DEGRADED
    }

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NodeStatus status;

    private String location;

    private Integer capacity;

    private Integer currentLoad;

    private LocalDateTime lastHeartbeat;
}
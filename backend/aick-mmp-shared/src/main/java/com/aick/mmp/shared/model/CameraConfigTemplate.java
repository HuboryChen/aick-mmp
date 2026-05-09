package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camera_config_templates", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"brand", "model", "is_deleted"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraConfigTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false, length = 20)
    private String protocol;

    @Column(name = "default_port", nullable = false)
    private Integer defaultPort;

    @Column(name = "url_path_template", nullable = false, length = 500)
    private String urlPathTemplate;

    @Column(columnDefinition = "JSON")
    private String presetParameters;

    @Column(name = "is_preset")
    @Builder.Default
    private Boolean isPreset = false;

    @Column(name = "usage_count")
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isPreset == null) isPreset = false;
        if (usageCount == null) usageCount = 0;
        if (isDeleted == null) isDeleted = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

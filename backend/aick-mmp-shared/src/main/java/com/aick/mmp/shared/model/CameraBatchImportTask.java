package com.aick.mmp.shared.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "camera_batch_import_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraBatchImportTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "total_records")
    @Builder.Default
    private Integer totalRecords = 0;

    @Column(name = "success_count")
    @Builder.Default
    private Integer successCount = 0;

    @Column(name = "fail_count")
    @Builder.Default
    private Integer failCount = 0;

    @Column(name = "error_details", columnDefinition = "JSON")
    private String errorDetails;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (totalRecords == null) totalRecords = 0;
        if (successCount == null) successCount = 0;
        if (failCount == null) failCount = 0;
    }
}

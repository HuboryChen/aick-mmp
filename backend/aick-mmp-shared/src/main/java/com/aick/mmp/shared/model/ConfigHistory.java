package com.aick.mmp.shared.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 配置变更历史实体
 * 记录配置项的变更历史，支持回滚
 */
@Entity
@Table(name = "config_history",
       indexes = {
           @Index(name = "idx_history_config_key", columnList = "config_key"),
           @Index(name = "idx_history_config_id", columnList = "config_id"),
           @Index(name = "idx_history_created_at", columnList = "created_at")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 配置ID
     */
    @Column(name = "config_id")
    private Long configId;
    
    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;
    
    /**
     * 操作类型: CREATE, UPDATE, DELETE, RESET
     */
    @Column(name = "operation_type", nullable = false, length = 20)
    private String operationType;
    
    /**
     * 变更前的值
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;
    
    /**
     * 变更后的值
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
    
    /**
     * 变更描述
     */
    @Column(name = "change_description", length = 500)
    private String changeDescription;
    
    /**
     * 操作者ID
     */
    @Column(name = "operator_id")
    private Long operatorId;
    
    /**
     * 操作者名称
     */
    @Column(name = "operator_name", length = 100)
    private String operatorName;
    
    /**
     * 操作者IP
     */
    @Column(name = "operator_ip", length = 50)
    private String operatorIp;
    
    /**
     * 是否可回滚
     */
    @Builder.Default
    @Column(name = "rollbackable")
    private Boolean rollbackable = true;
    
    /**
     * 是否已回滚
     */
    @Builder.Default
    @Column(name = "rolled_back")
    private Boolean rolledBack = false;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

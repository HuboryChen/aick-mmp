package com.aick.mmp.shared.model;

import com.aick.mmp.shared.model.enums.ConfigCategory;
import com.aick.mmp.shared.model.enums.ConfigValueType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 系统配置实体
 * 存储系统的各种配置项
 */
@Entity
@Table(name = "system_configs",
       indexes = {
           @Index(name = "idx_config_key", columnList = "config_key", unique = true),
           @Index(name = "idx_config_category", columnList = "category"),
           @Index(name = "idx_config_group", columnList = "config_group")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 配置键（唯一标识）
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;
    
    /**
     * 配置名称（显示用）
     */
    @Column(name = "config_name", nullable = false, length = 100)
    private String configName;
    
    /**
     * 配置描述
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    /**
     * 配置值
     */
    @Column(name = "config_value", columnDefinition = "TEXT")
    private String configValue;
    
    /**
     * 默认值
     */
    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;
    
    /**
     * 配置值类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", length = 20)
    private ConfigValueType valueType;
    
    /**
     * 所属分类
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private ConfigCategory category;
    
    /**
     * 配置分组（同一组配置可一起操作）
     */
    @Column(name = "config_group", length = 50)
    private String configGroup;
    
    /**
     * 选项值（用于SELECT类型，多个选项用逗号分隔）
     */
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;
    
    /**
     * 最小值（用于NUMBER类型）
     */
    @Column(name = "min_value")
    private Double minValue;
    
    /**
     * 最大值（用于NUMBER类型）
     */
    @Column(name = "max_value")
    private Double maxValue;
    
    /**
     * 是否必填
     */
    @Builder.Default
    @Column(name = "required")
    private Boolean required = false;
    
    /**
     * 是否可编辑
     */
    @Builder.Default
    @Column(name = "editable")
    private Boolean editable = true;
    
    /**
     * 是否敏感配置（不返回具体值）
     */
    @Builder.Default
    @Column(name = "sensitive")
    private Boolean sensitive = false;
    
    /**
     * 验证规则（正则表达式）
     */
    @Column(name = "validation_rule", length = 500)
    private String validationRule;
    
    /**
     * 显示顺序
     */
    @Builder.Default
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    /**
     * 是否启用
     */
    @Builder.Default
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 创建者ID
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    /**
     * 更新者ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;
}

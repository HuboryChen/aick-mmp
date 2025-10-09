package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegionDTO {
    private Long id;

    @NotBlank(message = "地区编码不能为空")
    @Size(max = 50, message = "地区编码长度不能超过50个字符")
    private String code;

    @NotBlank(message = "地区名称不能为空")
    @Size(max = 100, message = "地区名称长度不能超过100个字符")
    private String name;

    @Size(max = 500, message = "地区描述长度不能超过500个字符")
    private String description;

    private Long parentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
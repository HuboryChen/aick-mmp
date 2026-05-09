package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CameraImportDTO {
    private String cameraName;
    private String brand;
    private String model;
    private String ip;
    private Integer port;
    private String regionName;
    private String username;
    private String password;
    private String resolution;
    private String description;
}

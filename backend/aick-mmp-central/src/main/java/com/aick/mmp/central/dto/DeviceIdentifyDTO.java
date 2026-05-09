package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceIdentifyDTO {
    private String ip;
    private Integer port;
    private String brand;
    private String model;
    private String protocol;
    private boolean isIdentified;
}

package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectivityResultDTO {
    private String ip;
    private Integer port;
    private boolean connected;
    private Long responseTimeMs;
    private String errorMessage;
}

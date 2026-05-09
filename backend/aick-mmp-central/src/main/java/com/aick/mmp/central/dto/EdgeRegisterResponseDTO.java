package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for Edge node registration response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EdgeRegisterResponseDTO {
    
    private Long id;
    private String uuid;
    private String name;
    private String status;
    private LocalDateTime registeredAt;
    private String message;
}

package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebRTC Offer/Answer 交换请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebRtcRequest {
    
    /**
     * SDP Offer 或 Answer
     */
    private String sdp;
    
    /**
     * SDP类型 (offer/answer/pranswer/rollback)
     */
    private String type;
    
    /**
     * ICE候选
     */
    private String candidate;
    
    /**
     * SDP媒体行索引
     */
    private Integer sdpMLineIndex;
    
    /**
     * SDP媒体名称
     */
    private String sdpMid;
}

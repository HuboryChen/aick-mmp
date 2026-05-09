package com.aick.mmp.central.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CDN节点连通性测试结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdnNodeConnectivityTestDTO {
    
    /**
     * 节点ID
     */
    private Long nodeId;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点IP
     */
    private String ipAddress;
    
    /**
     * 测试是否成功
     */
    private boolean success;
    
    /**
     * 连接状态
     */
    private String status;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 测试时间戳
     */
    private Long testTimestamp;
    
    /**
     * 建议
     */
    private String suggestion;
    
    /**
     * 测试详情
     */
    private TestDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestDetails {
        /**
         * DNS解析时间（毫秒）
         */
        private Long dnsLookupMs;
        
        /**
         * TCP连接时间（毫秒）
         */
        private Long tcpConnectMs;
        
        /**
         * SSL握手时间（毫秒）
         */
        private Long sslHandshakeMs;
        
        /**
         * 首字节时间（毫秒）
         */
        private Long ttfbMs;
        
        /**
         * 总传输时间（毫秒）
         */
        private Long totalTransferMs;
        
        /**
         * HTTP状态码
         */
        private Integer httpStatusCode;
        
        /**
         * 响应内容长度
         */
        private Long contentLength;
    }
}

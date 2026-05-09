package com.aick.mmp.edge.controller;

import com.aick.mmp.edge.config.ApiKeyConfig;
import com.aick.mmp.edge.config.EdgeNodeConfig;
import com.aick.mmp.edge.dto.MotionEventReport;
import com.aick.mmp.edge.service.MotionDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 移动侦测事件上报控制器
 * 负责将移动侦测事件上报到中心服务器
 */
@RestController
@RequestMapping("/api/motion")
public class MotionEventReportController {

    private static final Logger logger = LoggerFactory.getLogger(MotionEventReportController.class);

    @Autowired
    private MotionDetectionService motionDetectionService;

    @Autowired
    private EdgeNodeConfig edgeNodeConfig;

    @Autowired
    private ApiKeyConfig apiKeyConfig;

    @Autowired
    private RestTemplate restTemplate;

    private String centralServerUrl;

    @PostConstruct
    public void init() {
        this.centralServerUrl = edgeNodeConfig.getCentralServerUrl();
        logger.info("MotionEventReportController initialized, central server: {}", centralServerUrl);
    }

    /**
     * 手动触发事件上报
     */
    @PostMapping("/report")
    public Map<String, Object> reportEvents() {
        List<MotionEventReport> pendingEvents = motionDetectionService.getPendingEvents();
        
        if (pendingEvents.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "No pending events to report");
            response.put("count", 0);
            return response;
        }

        return reportEventsToServer(pendingEvents);
    }

    /**
     * 获取待上报事件数量
     */
    @GetMapping("/pending/count")
    public Map<String, Object> getPendingCount() {
        List<MotionEventReport> pendingEvents = motionDetectionService.getPendingEvents();
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", pendingEvents.size());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 获取活跃移动事件状态
     */
    @GetMapping("/status/{cameraId}")
    public Map<String, Object> getMotionStatus(@PathVariable Long cameraId) {
        boolean hasActive = motionDetectionService.hasActiveMotion(cameraId);
        var activeEvent = motionDetectionService.getActiveMotionEvent(cameraId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("cameraId", cameraId);
        response.put("hasActiveMotion", hasActive);
        response.put("activeEvent", activeEvent.orElse(null));
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    /**
     * 配置移动侦测灵敏度
     */
    @PostMapping("/config/sensitivity")
    public Map<String, Object> setSensitivity(@RequestParam double sensitivity) {
        motionDetectionService.setSensitivity(sensitivity);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("sensitivity", motionDetectionService.getSensitivity());
        return response;
    }

    /**
     * 获取统计信息
     */
    @GetMapping("/statistics")
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sensitivity", motionDetectionService.getSensitivity());
        stats.put("minMotionDuration", motionDetectionService.getMinMotionDuration());
        stats.put("pendingEvents", motionDetectionService.getPendingEvents().size());
        return stats;
    }

    /**
     * 定时上报事件到中心服务器
     */
    @Scheduled(fixedRateString = "${edge.motion.report-interval:30000}") // 默认30秒
    public void scheduledReportEvents() {
        List<MotionEventReport> pendingEvents = motionDetectionService.getPendingEvents();
        
        if (!pendingEvents.isEmpty()) {
            logger.info("Scheduled reporting {} motion events", pendingEvents.size());
            reportEventsToServer(pendingEvents);
        }
    }

    /**
     * 上报事件到中心服务器
     */
    private Map<String, Object> reportEventsToServer(List<MotionEventReport> events) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String url = centralServerUrl + "/api/cameras/motion-events/batch";
            
            // 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", apiKeyConfig.getAccessKey());
            headers.set("X-Node-Id", edgeNodeConfig.getNodeId());
            headers.set("X-Timestamp", String.valueOf(System.currentTimeMillis()));
            
            // 构建请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("edgeNodeId", edgeNodeConfig.getNodeId());
            requestBody.put("events", events);
            requestBody.put("timestamp", LocalDateTime.now().toString());
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 发送请求
            ResponseEntity<String> result = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    String.class
            );
            
            if (result.getStatusCode().is2xxSuccessful()) {
                motionDetectionService.clearPendingEvents(events);
                logger.info("Successfully reported {} motion events", events.size());
                
                response.put("success", true);
                response.put("reported", events.size());
                response.put("message", "Events reported successfully");
            } else {
                logger.warn("Failed to report events: HTTP {}", result.getStatusCode().value());
                
                response.put("success", false);
                response.put("message", "Failed to report events: HTTP " + result.getStatusCode().value());
            }
        } catch (Exception e) {
            logger.error("Error reporting motion events: {}", e.getMessage(), e);
            
            response.put("success", false);
            response.put("message", "Error reporting events: " + e.getMessage());
        }
        
        return response;
    }
}

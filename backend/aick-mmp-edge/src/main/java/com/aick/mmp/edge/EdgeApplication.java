package com.aick.mmp.edge;

import com.aick.mmp.edge.service.EdgeCameraService;
import com.aick.mmp.edge.service.EdgeNetworkMonitorService;
import com.aick.mmp.edge.service.EdgeStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.aick.mmp.shared.repository",
    "com.aick.mmp.repository"
})
@EntityScan(basePackages = {
    "com.aick.mmp.shared.model",
    "com.aick.mmp.model"
})
@ComponentScan(basePackages = {
    "com.aick.mmp.edge",
    "com.aick.mmp.shared",
    "com.aick.mmp.exception",
    "com.aick.mmp.util"
})
@Profile("edge")
@RequiredArgsConstructor
@Slf4j
public class EdgeApplication {

    private final EdgeCameraService edgeCameraService;
    private final EdgeStreamService edgeStreamService;
    private final EdgeNetworkMonitorService edgeNetworkMonitorService;

    public static void main(String[] args) {
        // Set the active profile to edge if not already set
//        System.setProperty("spring.profiles.active", "edge");
        
        log.info("Starting AICK-MMP Edge Node Application");
        SpringApplication.run(EdgeApplication.class, args);
    }

    @PostConstruct
    public void initializeEdgeNode() {
        log.info("Initializing Edge Node Services");
        
        try {
            // Initialize camera service
            log.info("Initializing Camera Service");
            edgeCameraService.initializeCameras();
            
            // Initialize streaming service
            log.info("Initializing Streaming Service");
            edgeStreamService.initializeStreaming();
            
            // Start network monitoring
            log.info("Starting Network Monitoring Service");
            edgeNetworkMonitorService.startMonitoring();
            
            log.info("Edge Node Services initialized successfully");
            
        } catch (Exception e) {
            log.error("Failed to initialize Edge Node Services", e);
            throw new RuntimeException("Edge Node initialization failed", e);
        }
    }

    @PreDestroy
    public void shutdownEdgeNode() {
        log.info("Shutting down Edge Node Services");
        
        try {
            // Stop network monitoring
            log.info("Stopping Network Monitoring Service");
            edgeNetworkMonitorService.stopMonitoring();
            
            // Shutdown streaming service
            log.info("Shutting down Streaming Service");
            edgeStreamService.shutdownAllStreams();
            
            // Shutdown camera service
            log.info("Shutting down Camera Service");
            edgeCameraService.shutdownCameras();
            
            log.info("Edge Node Services shutdown completed");
            
        } catch (Exception e) {
            log.error("Error during Edge Node shutdown", e);
        }
    }
}
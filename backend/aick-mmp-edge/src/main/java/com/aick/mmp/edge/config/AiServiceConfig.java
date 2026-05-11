package com.aick.mmp.edge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceConfig {
    private String host = "localhost";
    private int grpcPort = 50051;
    private Map<String, CameraAnalysisConfig> cameras;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getGrpcPort() { return grpcPort; }
    public void setGrpcPort(int grpcPort) { this.grpcPort = grpcPort; }

    public Map<String, CameraAnalysisConfig> getCameras() { return cameras; }
    public void setCameras(Map<String, CameraAnalysisConfig> cameras) { this.cameras = cameras; }

    public String getTargetAddress() {
        return host + ":" + grpcPort;
    }

    public static class CameraAnalysisConfig {
        private double fps = 1.0;
        private java.util.List<String> analysisTypes;

        public double getFps() { return fps; }
        public void setFps(double fps) { this.fps = fps; }

        public java.util.List<String> getAnalysisTypes() { return analysisTypes; }
        public void setAnalysisTypes(java.util.List<String> analysisTypes) { this.analysisTypes = analysisTypes; }
    }
}

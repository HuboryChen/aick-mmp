package com.aick.mmp.central.service.recording;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 录像存储配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "recording.storage")
public class StorageProperties {

    /**
     * 本地存储根路径
     */
    private String localPath = "/data/recordings";

    /**
     * 录像保留天数
     */
    private int retentionDays = 30;

    /**
     * 磁盘使用率阈值 (达到此值时触发清理)
     */
    private double diskUsageThreshold = 0.8;

    /**
     * 清理后保留比例 (清理后保留到此比例)
     */
    private double cleanupRetentionRatio = 0.7;

    /**
     * 存储路径模板 {date} 会被替换为日期
     */
    private String pathTemplate = "{localPath}/{date}/";

    /**
     * 文件名模板 {cameraId} 会被替换为摄像头ID
     */
    private String fileNameTemplate = "cam_{cameraId}.mp4";

    /**
     * 下载最大并发数
     */
    private int maxDownloadConcurrency = 3;

    /**
     * 下载最大带宽 (MB/s)
     */
    private int maxDownloadBandwidthMB = 10;

    /**
     * 获取实际存储路径
     */
    public String getStoragePath(String date, Long cameraId) {
        String path = pathTemplate
                .replace("{localPath}", localPath)
                .replace("{date}", date);
        String fileName = fileNameTemplate.replace("{cameraId}", String.valueOf(cameraId));
        return path + fileName;
    }

    /**
     * 计算清理后应保留的最大容量 (字节)
     */
    public long getCleanupTargetBytes(long totalDiskBytes) {
        return (long) (totalDiskBytes * cleanupRetentionRatio);
    }
}

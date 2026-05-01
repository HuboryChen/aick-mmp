package com.aick.mmp.central.service.recording;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 本地文件存储服务
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final StorageProperties storageProperties;

    public FileStorageService(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 获取录像文件的绝对路径
     */
    public String getAbsolutePath(String date, Long cameraId) {
        String storagePath = storageProperties.getStoragePath(date, cameraId);
        return storagePath.startsWith("/") ? storagePath : 
               storageProperties.getLocalPath() + "/" + storagePath;
    }

    /**
     * 获取录像文件的相对路径 (用于数据库存储)
     */
    public String getRelativePath(String date, Long cameraId) {
        return storageProperties.getStoragePath(date, cameraId);
    }

    /**
     * 检查文件是否存在
     */
    public boolean fileExists(String relativePath) {
        String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
        return Files.exists(Paths.get(absolutePath));
    }

    /**
     * 获取文件大小
     */
    public Optional<Long> getFileSize(String relativePath) {
        try {
            String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
            long size = Files.size(Paths.get(absolutePath));
            return Optional.of(size);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * 计算文件的MD5校验码
     */
    public Optional<String> calculateMd5(String relativePath) {
        try {
            String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
            Path path = Paths.get(absolutePath);
            
            if (!Files.exists(path)) {
                return Optional.empty();
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            
            try (InputStream is = Files.newInputStream(path);
                 BufferedInputStream bis = new BufferedInputStream(is)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = bis.read(buffer)) != -1) {
                    md.update(buffer, 0, bytesRead);
                }
            }

            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return Optional.of(sb.toString());

        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 验证文件完整性
     */
    public boolean verifyIntegrity(String relativePath, String expectedMd5) {
        return calculateMd5(relativePath)
                .map(calculated -> calculated.equalsIgnoreCase(expectedMd5))
                .orElse(false);
    }

    /**
     * 创建存储目录
     */
    public void ensureDirectoryExists(String relativePath) throws IOException {
        String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
        Path path = Paths.get(absolutePath);
        
        // 获取父目录
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
    }

    /**
     * 删除文件
     */
    public boolean deleteFile(String relativePath) {
        try {
            String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
            return Files.deleteIfExists(Paths.get(absolutePath));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 获取输入流用于下载
     */
    public Optional<InputStream> getInputStream(String relativePath) {
        try {
            String absolutePath = storageProperties.getLocalPath() + "/" + relativePath;
            return Optional.of(Files.newInputStream(Paths.get(absolutePath)));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * 获取日期格式的路径
     */
    public String getDatePath(LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 获取磁盘总空间 (字节)
     */
    public long getTotalDiskSpace() {
        try {
            java.io.File root = new java.io.File(storageProperties.getLocalPath());
            if (!root.exists()) {
                // 如果目录不存在，使用根目录
                root = new java.io.File("/");
            }
            return root.getTotalSpace();
        } catch (Exception e) {
            log.error("获取磁盘总空间失败", e);
            return 0;
        }
    }

    /**
     * 获取磁盘已使用空间 (字节)
     */
    public long getUsedDiskSpace() {
        try {
            java.io.File root = new java.io.File(storageProperties.getLocalPath());
            if (!root.exists()) {
                return 0;
            }
            return root.getTotalSpace() - root.getFreeSpace();
        } catch (Exception e) {
            log.error("获取磁盘已使用空间失败", e);
            return 0;
        }
    }

    /**
     * 获取磁盘可用空间 (字节)
     */
    public long getAvailableDiskSpace() {
        try {
            java.io.File root = new java.io.File(storageProperties.getLocalPath());
            if (!root.exists()) {
                return 0;
            }
            return root.getUsableSpace();
        } catch (Exception e) {
            log.error("获取磁盘可用空间失败", e);
            return 0;
        }
    }
}

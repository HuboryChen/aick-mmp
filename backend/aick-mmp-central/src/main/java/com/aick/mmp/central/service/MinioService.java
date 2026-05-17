package com.aick.mmp.central.service;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class MinioService {
    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private final MinioClient minioClient;
    private final com.aick.mmp.central.config.MinioConfig config;

    public MinioService(MinioClient minioClient, com.aick.mmp.central.config.MinioConfig config) {
        this.minioClient = minioClient;
        this.config = config;
        ensureBucketExists();
    }

    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(config.getBucketName()).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder()
                        .bucket(config.getBucketName()).build());
                log.info("Created MinIO bucket: {}", config.getBucketName());
            }
        } catch (Exception e) {
            log.warn("Failed to ensure MinIO bucket: {}", e.getMessage());
        }
    }

    public String uploadSnapshot(MultipartFile file, String prefix) {
        try {
            String objectName = prefix + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("Uploaded snapshot: {}", objectName);
            return objectName;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO", e);
        }
    }

    public InputStream getSnapshot(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get file from MinIO", e);
        }
    }

    public void deleteSnapshot(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(config.getBucketName())
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from MinIO", e);
        }
    }

    public String getPresignedUrl(String objectName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(config.getBucketName())
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }
}

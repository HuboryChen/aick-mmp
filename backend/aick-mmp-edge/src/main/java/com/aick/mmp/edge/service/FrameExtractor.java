package com.aick.mmp.edge.service;

import com.aick.mmp.edge.config.AiServiceConfig;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class FrameExtractor {
    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    private final FrameAnalysisGrpcClient grpcClient;
    private final AiServiceConfig config;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> extractions = new ConcurrentHashMap<>();

    public FrameExtractor(FrameAnalysisGrpcClient grpcClient, AiServiceConfig config,
                          ScheduledExecutorService scheduler) {
        this.grpcClient = grpcClient;
        this.config = config;
        this.scheduler = scheduler;
    }

    public void startExtraction(String cameraId, String streamUrl) {
        if (extractions.containsKey(cameraId)) {
            return;
        }

        AiServiceConfig.CameraAnalysisConfig camConfig =
            config.getCameras() != null ? config.getCameras().get(cameraId) : null;
        double fps = camConfig != null ? camConfig.getFps() : 1.0;
        java.util.List<String> types = camConfig != null ? camConfig.getAnalysisTypes() : java.util.List.of("passenger");

        long periodMs = (long) (1000.0 / fps);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(
            () -> captureAndSend(cameraId, streamUrl, types),
            0, periodMs, TimeUnit.MILLISECONDS
        );
        extractions.put(cameraId, future);
        log.info("Started frame extraction for camera {} at {} fps", cameraId, fps);
    }

    public void stopExtraction(String cameraId) {
        ScheduledFuture<?> future = extractions.remove(cameraId);
        if (future != null) {
            future.cancel(false);
            log.info("Stopped frame extraction for camera {}", cameraId);
        }
    }

    private void captureAndSend(String cameraId, String streamUrl, java.util.List<String> types) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(streamUrl)) {
            grabber.start();
            Frame frame = grabber.grabImage();
            if (frame != null) {
                try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    java.awt.image.BufferedImage bi = converter.convert(frame);
                    if (bi != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bi, "jpg", baos);
                        baos.flush();
                        byte[] jpegData = baos.toByteArray();
                        grpcClient.sendFrame(cameraId, jpegData, System.currentTimeMillis(), types);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to capture frame for camera {}: {}", cameraId, e.getMessage());
        }
    }
}

package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.ConnectivityResultDTO;
import com.aick.mmp.central.dto.DeviceIdentifyDTO;
import com.aick.mmp.central.dto.DiscoveryTaskDTO;
import com.aick.mmp.central.dto.ScanProgressDTO;
import com.aick.mmp.central.repository.CameraDiscoveryTaskRepository;
import com.aick.mmp.central.service.CameraDiscoveryService;
import com.aick.mmp.shared.model.CameraDiscoveryTask;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CameraDiscoveryServiceImpl implements CameraDiscoveryService {

    private final CameraDiscoveryTaskRepository repository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${camera-discovery.scan-batch-size:50}")
    private int scanBatchSize;

    @Value("${camera-discovery.scan-timeout-ms:2000}")
    private int scanTimeoutMs;

    @Value("${camera-discovery.common-ports:554,80,8080,8554}")
    private String commonPortsStr;

    private final ConcurrentHashMap<Long, Boolean> cancellationFlags = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public Long startScan(String networkSegment, Long userId) {
        CameraDiscoveryTask task = CameraDiscoveryTask.builder()
                .userId(userId)
                .networkSegment(networkSegment)
                .status("PENDING")
                .progress(0)
                .totalIps(0)
                .build();

        CameraDiscoveryTask saved = repository.save(task);
        executeScan(saved.getId());
        return saved.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public ScanProgressDTO getScanProgress(Long taskId) {
        CameraDiscoveryTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Scan task not found: " + taskId));
        return toScanProgressDTO(task);
    }

    @Override
    @Transactional
    public void cancelScan(Long taskId) {
        CameraDiscoveryTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Scan task not found: " + taskId));

        if (!"RUNNING".equals(task.getStatus()) && !"PENDING".equals(task.getStatus())) {
            throw new IllegalStateException("Cannot cancel task with status: " + task.getStatus());
        }

        cancellationFlags.put(taskId, true);
        task.setStatus("CANCELLED");
        task.setCompletedAt(LocalDateTime.now());
        repository.save(task);

        pushProgress(toScanProgressDTO(task));
        log.info("Cancelled scan task: {}", taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectivityResultDTO testConnectivity(String ip, Integer port) {
        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), scanTimeoutMs);
            long responseTime = System.currentTimeMillis() - start;
            return ConnectivityResultDTO.builder()
                    .ip(ip)
                    .port(port)
                    .connected(true)
                    .responseTimeMs(responseTime)
                    .build();
        } catch (IOException e) {
            return ConnectivityResultDTO.builder()
                    .ip(ip)
                    .port(port)
                    .connected(false)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceIdentifyDTO identifyDevice(String ip, Integer port) {
        String brand = "Unknown";
        String model = "Unknown";
        String protocol = "Unknown";
        boolean identified = false;

        try {
            if (port == 554 || port == 8554) {
                protocol = "RTSP";
                String serverBanner = probeRtsp(ip, port);
                if (serverBanner != null) {
                    if (serverBanner.contains("Hikvision")) {
                        brand = "海康威视";
                        identified = true;
                    } else if (serverBanner.contains("Dahua")) {
                        brand = "大华";
                        identified = true;
                    } else if (serverBanner.contains("Uniview")) {
                        brand = "宇视";
                        identified = true;
                    }
                }
            } else if (port == 80 || port == 8080) {
                protocol = "HTTP";
                String serverHeader = probeHttp(ip, port);
                if (serverHeader != null) {
                    if (serverHeader.contains("Hikvision")) {
                        brand = "海康威视";
                        identified = true;
                    } else if (serverHeader.contains("Dahua")) {
                        brand = "大华";
                        identified = true;
                    } else if (serverHeader.contains("Uniview")) {
                        brand = "宇视";
                        identified = true;
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to identify device {}:{}: {}", ip, port, e.getMessage());
        }

        return DeviceIdentifyDTO.builder()
                .ip(ip)
                .port(port)
                .brand(brand)
                .model(model)
                .protocol(protocol)
                .isIdentified(identified)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscoveryTaskDTO> getScanHistory(Pageable pageable, Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toDiscoveryTaskDTO);
    }

    @Async
    public void executeScan(Long taskId) {
        CameraDiscoveryTask task = repository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Scan task not found: " + taskId));

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        repository.save(task);

        try {
            List<String> ips = generateIpList(task.getNetworkSegment());
            task.setTotalIps(ips.size());
            repository.save(task);

            List<Integer> commonPorts = parseCommonPorts();
            List<Map<String, Object>> foundDevices = parseFoundDevices(task.getFoundDevices());
            int scanned = 0;

            for (int i = 0; i < ips.size(); i += scanBatchSize) {
                if (cancellationFlags.getOrDefault(taskId, false)) {
                    cancellationFlags.remove(taskId);
                    return;
                }

                int batchEnd = Math.min(i + scanBatchSize, ips.size());
                List<String> batch = ips.subList(i, batchEnd);

                for (String ip : batch) {
                    for (int port : commonPorts) {
                        ConnectivityResultDTO result = testConnectivity(ip, port);
                        if (result.isConnected()) {
                            DeviceIdentifyDTO device = identifyDevice(ip, port);
                            Map<String, Object> deviceMap = new HashMap<>();
                            deviceMap.put("ip", ip);
                            deviceMap.put("port", port);
                            deviceMap.put("brand", device.getBrand());
                            deviceMap.put("model", device.getModel());
                            deviceMap.put("protocol", device.getProtocol());
                            deviceMap.put("isIdentified", device.isIdentified());
                            foundDevices.add(deviceMap);
                        }
                    }
                    scanned++;
                }

                task.setProgress(scanned * 100 / ips.size());
                task.setFoundDevices(serializeFoundDevices(foundDevices));
                repository.save(task);

                pushProgress(ScanProgressDTO.builder()
                        .taskId(taskId)
                        .progress(task.getProgress())
                        .totalIps(ips.size())
                        .scannedIps(scanned)
                        .status("RUNNING")
                        .build());

                Thread.sleep(100);
            }

            task.setStatus("COMPLETED");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            repository.save(task);

            pushProgress(ScanProgressDTO.builder()
                    .taskId(taskId)
                    .progress(100)
                    .totalIps(ips.size())
                    .scannedIps(scanned)
                    .status("COMPLETED")
                    .build());

            log.info("Scan task {} completed: found {} devices", taskId, foundDevices.size());
        } catch (Exception e) {
            log.error("Scan task {} failed: {}", taskId, e.getMessage(), e);
            task.setStatus("FAILED");
            task.setCompletedAt(LocalDateTime.now());
            repository.save(task);

            pushProgress(ScanProgressDTO.builder()
                    .taskId(taskId)
                    .progress(task.getProgress())
                    .totalIps(task.getTotalIps())
                    .scannedIps(0)
                    .status("FAILED")
                    .build());
        } finally {
            cancellationFlags.remove(taskId);
        }
    }

    private void pushProgress(ScanProgressDTO progress) {
        try {
            messagingTemplate.convertAndSend(
                    "/topic/discovery/" + progress.getTaskId(),
                    objectMapper.writeValueAsString(progress)
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize scan progress: {}", e.getMessage());
        }
    }

    private List<String> generateIpList(String networkSegment) {
        List<String> ips = new ArrayList<>();

        try {
            if (networkSegment.contains("/")) {
                String[] parts = networkSegment.split("/");
                String baseIp = parts[0];
                int prefix = Integer.parseInt(parts[1]);

                String[] octets = baseIp.split("\\.");
                int base = (Integer.parseInt(octets[0]) << 24)
                        | (Integer.parseInt(octets[1]) << 16)
                        | (Integer.parseInt(octets[2]) << 8)
                        | Integer.parseInt(octets[3]);

                int mask = prefix == 0 ? 0 : 0xFFFFFFFF << (32 - prefix);
                int networkStart = base & mask;
                int hostCount = (int) Math.pow(2, 32 - prefix) - 2;

                for (int i = 1; i <= hostCount && i < 65536; i++) {
                    int ipInt = networkStart | i;
                    if ((ipInt & 0xFF) == 0 || (ipInt & 0xFF) == 255) continue;
                    ips.add(String.format("%d.%d.%d.%d",
                            (ipInt >> 24) & 0xFF,
                            (ipInt >> 16) & 0xFF,
                            (ipInt >> 8) & 0xFF,
                            ipInt & 0xFF));
                }
            } else {
                ips.add(networkSegment);
            }
        } catch (Exception e) {
            log.error("Failed to generate IP list from segment: {}", networkSegment, e);
        }

        return ips;
    }

    private String probeRtsp(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), scanTimeoutMs);
            socket.getOutputStream().write((
                    "OPTIONS rtsp://" + ip + " RTSP/1.0\r\n" +
                            "CSeq: 1\r\n" +
                            "User-Agent: AICK-MMP-Discovery\r\n" +
                            "\r\n"
            ).getBytes());
            socket.getOutputStream().flush();

            byte[] buffer = new byte[1024];
            int read = socket.getInputStream().read(buffer);
            if (read > 0) {
                return new String(buffer, 0, read);
            }
        } catch (IOException e) {
            log.trace("RTSP probe failed for {}:{}: {}", ip, port, e.getMessage());
        }
        return null;
    }

    private String probeHttp(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), scanTimeoutMs);
            socket.getOutputStream().write((
                    "GET / HTTP/1.0\r\n" +
                            "Host: " + ip + "\r\n" +
                            "User-Agent: AICK-MMP-Discovery\r\n" +
                            "\r\n"
            ).getBytes());
            socket.getOutputStream().flush();

            byte[] buffer = new byte[2048];
            int read = socket.getInputStream().read(buffer);
            if (read > 0) {
                String response = new String(buffer, 0, read);
                for (String line : response.split("\r\n")) {
                    if (line.toLowerCase().startsWith("server:")) {
                        return line.substring(7).trim();
                    }
                }
            }
        } catch (IOException e) {
            log.trace("HTTP probe failed for {}:{}: {}", ip, port, e.getMessage());
        }
        return null;
    }

    private List<Integer> parseCommonPorts() {
        List<Integer> ports = new ArrayList<>();
        for (String s : commonPortsStr.split(",")) {
            try {
                ports.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                log.warn("Invalid port in config: {}", s);
            }
        }
        return ports;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseFoundDevices(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.error("Failed to parse found devices JSON", e);
            return new ArrayList<>();
        }
    }

    private String serializeFoundDevices(List<Map<String, Object>> devices) {
        try {
            return objectMapper.writeValueAsString(devices);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize found devices", e);
            return "[]";
        }
    }

    private ScanProgressDTO toScanProgressDTO(CameraDiscoveryTask task) {
        return ScanProgressDTO.builder()
                .taskId(task.getId())
                .progress(task.getProgress())
                .totalIps(task.getTotalIps())
                .scannedIps(task.getTotalIps() > 0
                        ? task.getProgress() * task.getTotalIps() / 100
                        : 0)
                .status(task.getStatus())
                .build();
    }

    private DiscoveryTaskDTO toDiscoveryTaskDTO(CameraDiscoveryTask entity) {
        return DiscoveryTaskDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .networkSegment(entity.getNetworkSegment())
                .status(entity.getStatus())
                .progress(entity.getProgress())
                .totalIps(entity.getTotalIps())
                .foundDevices(entity.getFoundDevices())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

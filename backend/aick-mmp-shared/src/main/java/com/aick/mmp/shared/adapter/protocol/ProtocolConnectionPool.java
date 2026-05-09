package com.aick.mmp.shared.adapter.protocol;

import com.aick.mmp.shared.model.Camera;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 协议连接池管理器
 * 用于管理协议适配器的连接池，实现连接复用和资源清理
 */
@Component
@Slf4j
public class ProtocolConnectionPool {

    private static final int DEFAULT_MAX_CONNECTIONS = 100;
    private static final int DEFAULT_IDLE_TIMEOUT_SECONDS = 300;
    private static final int CLEANUP_INTERVAL_SECONDS = 60;

    private final int maxConnections;
    private final int idleTimeoutSeconds;
    private final ScheduledExecutorService cleanupExecutor;
    
    // 存储活跃连接信息
    private final Map<String, ConnectionInfo> activeConnections;
    // 摄像头ID到连接会话的映射（用于连接复用）
    private final Map<Long, String> cameraToSession;
    // 锁用于并发控制
    private final ReentrantLock lock;

    public ProtocolConnectionPool() {
        this(DEFAULT_MAX_CONNECTIONS, DEFAULT_IDLE_TIMEOUT_SECONDS);
    }

    public ProtocolConnectionPool(int maxConnections, int idleTimeoutSeconds) {
        this.maxConnections = maxConnections;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
        this.activeConnections = new ConcurrentHashMap<>();
        this.cameraToSession = new ConcurrentHashMap<>();
        this.lock = new ReentrantLock();
        
        // 启动定时清理任务
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "protocol-connection-pool-cleanup");
            t.setDaemon(true);
            return t;
        });
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanupIdleConnections,
            CLEANUP_INTERVAL_SECONDS,
            CLEANUP_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );
        
        log.info("Protocol connection pool initialized: maxConnections={}, idleTimeout={}s", 
                maxConnections, idleTimeoutSeconds);
    }

    /**
     * 注册一个新连接
     */
    public void registerConnection(String sessionId, Camera camera) {
        lock.lock();
        try {
            if (activeConnections.size() >= maxConnections) {
                throw new RuntimeException("Connection pool is full. Max connections: " + maxConnections);
            }
            
            ConnectionInfo info = new ConnectionInfo(sessionId, camera);
            activeConnections.put(sessionId, info);
            
            if (camera != null && camera.getId() != null) {
                cameraToSession.put(camera.getId(), sessionId);
            }
            
            log.debug("Registered connection: sessionId={}, cameraId={}", sessionId, 
                    camera != null ? camera.getId() : "null");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取摄像头已有的会话（用于连接复用）
     */
    public Optional<String> getExistingSession(Long cameraId) {
        return Optional.ofNullable(cameraToSession.get(cameraId));
    }

    /**
     * 检查连接是否存在
     */
    public boolean hasConnection(String sessionId) {
        return activeConnections.containsKey(sessionId);
    }

    /**
     * 更新连接的最后活跃时间
     */
    public void touchConnection(String sessionId) {
        ConnectionInfo info = activeConnections.get(sessionId);
        if (info != null) {
            info.setLastActiveTime(Instant.now());
        }
    }

    /**
     * 移除连接
     */
    public void removeConnection(String sessionId) {
        lock.lock();
        try {
            ConnectionInfo info = activeConnections.remove(sessionId);
            if (info != null && info.getCamera() != null && info.getCamera().getId() != null) {
                cameraToSession.remove(info.getCamera().getId());
            }
            log.debug("Removed connection: sessionId={}", sessionId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取连接信息
     */
    public Optional<ConnectionInfo> getConnectionInfo(String sessionId) {
        return Optional.ofNullable(activeConnections.get(sessionId));
    }

    /**
     * 获取连接池统计
     */
    public Map<String, Object> getStats() {
        lock.lock();
        try {
            return Map.of(
                "activeConnections", activeConnections.size(),
                "maxConnections", maxConnections,
                "availableSlots", maxConnections - activeConnections.size(),
                "idleTimeout", idleTimeoutSeconds
            );
        } finally {
            lock.unlock();
        }
    }

    /**
     * 清理空闲连接
     */
    private void cleanupIdleConnections() {
        lock.lock();
        try {
            Instant cutoff = Instant.now().minusSeconds(idleTimeoutSeconds);
            int cleanedCount = 0;
            
            for (Map.Entry<String, ConnectionInfo> entry : activeConnections.entrySet()) {
                if (entry.getValue().getLastActiveTime().isBefore(cutoff)) {
                    log.info("Cleaning up idle connection: sessionId={}, idleTime={}s", 
                            entry.getKey(), idleTimeoutSeconds);
                    activeConnections.remove(entry.getKey());
                    
                    ConnectionInfo info = entry.getValue();
                    if (info.getCamera() != null && info.getCamera().getId() != null) {
                        cameraToSession.remove(info.getCamera().getId());
                    }
                    cleanedCount++;
                }
            }
            
            if (cleanedCount > 0) {
                log.info("Cleaned up {} idle connections", cleanedCount);
            }
        } catch (Exception e) {
            log.error("Error during connection pool cleanup", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 关闭连接池
     */
    public void shutdown() {
        log.info("Shutting down protocol connection pool");
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        lock.lock();
        try {
            activeConnections.clear();
            cameraToSession.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 连接信息内部类
     */
    public static class ConnectionInfo {
        private final String sessionId;
        private final Camera camera;
        private final Instant createdAt;
        private Instant lastActiveTime;
        private final Map<String, Object> metadata;

        public ConnectionInfo(String sessionId, Camera camera) {
            this.sessionId = sessionId;
            this.camera = camera;
            this.createdAt = Instant.now();
            this.lastActiveTime = Instant.now();
            this.metadata = new ConcurrentHashMap<>();
        }

        public String getSessionId() {
            return sessionId;
        }

        public Camera getCamera() {
            return camera;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public Instant getLastActiveTime() {
            return lastActiveTime;
        }

        public void setLastActiveTime(Instant lastActiveTime) {
            this.lastActiveTime = lastActiveTime;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public void putMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }
}

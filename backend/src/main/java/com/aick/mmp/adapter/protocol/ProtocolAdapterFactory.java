package com.aick.mmp.adapter.protocol;

import com.aick.mmp.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 协议适配器工厂，用于根据协议类型动态获取对应的协议适配器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProtocolAdapterFactory {

    private final List<ProtocolAdapter> adapters;
    private final Map<String, ProtocolAdapter> adapterCache = new ConcurrentHashMap<>();

    /**
     * 根据协议类型获取对应的适配器
     * @param protocol 协议类型（如RTSP、ONVIF、GB28181）
     * @return 对应的协议适配器
     * @throws ServiceException 如果不支持该协议
     */
    public ProtocolAdapter getAdapter(String protocol) {
        // 从缓存获取适配器
        ProtocolAdapter adapter = adapterCache.get(protocol);

        if (adapter != null) {
            return adapter;
        }

        // 查找支持该协议的适配器
        adapter = adapters.stream()
                .filter(a -> a.getProtocol().equalsIgnoreCase(protocol))
                .findFirst()
                .orElseThrow(() -> new ServiceException("Unsupported protocol: " + protocol));

        // 缓存适配器实例
        adapterCache.put(protocol, adapter);
        log.info("Loaded protocol adapter for: {}", protocol);

        return adapter;
    }

    /**
     * 获取所有支持的协议类型
     * @return 支持的协议类型列表
     */
    public List<String> getSupportedProtocols() {
        return adapters.stream()
                .map(ProtocolAdapter::getProtocol)
                .collect(Collectors.toList());
    }
}
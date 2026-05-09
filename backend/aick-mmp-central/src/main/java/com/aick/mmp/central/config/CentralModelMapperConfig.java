package com.aick.mmp.central.config;

import com.aick.mmp.shared.model.EdgeNode;
import com.aick.mmp.central.dto.EdgeNodeDTO;
import org.modelmapper.ModelMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class CentralModelMapperConfig {

    @Autowired
    private ModelMapper modelMapper;

    @PostConstruct
    public void configureModelMapper() {
        // 配置EdgeNode到EdgeNodeDTO的映射
        modelMapper.createTypeMap(EdgeNode.class, EdgeNodeDTO.class)
            .addMapping(EdgeNode::getId, EdgeNodeDTO::setId)
            .addMapping(EdgeNode::getUuid, EdgeNodeDTO::setUuid)
            .addMapping(EdgeNode::getName, EdgeNodeDTO::setName)
            .addMapping(EdgeNode::getIpAddress, EdgeNodeDTO::setIpAddress)
            .addMapping(EdgeNode::getLocation, EdgeNodeDTO::setLocation)
            .addMapping(EdgeNode::getStatus, EdgeNodeDTO::setStatus)
            .addMapping(EdgeNode::getLastHeartbeatTime, EdgeNodeDTO::setLastHeartbeatTime)
            .addMapping(EdgeNode::getCpuUsage, EdgeNodeDTO::setCpuUsage)
            .addMapping(EdgeNode::getMemoryUsage, EdgeNodeDTO::setMemoryUsage)
            .addMapping(EdgeNode::getStorageUsage, EdgeNodeDTO::setStorageUsage)
            .addMapping(EdgeNode::getMaxCameraSupport, EdgeNodeDTO::setMaxCameraSupport)
            .addMapping(EdgeNode::getCurrentCameraCount, EdgeNodeDTO::setCurrentCameraCount)
            .addMapping(EdgeNode::getSoftwareVersion, EdgeNodeDTO::setSoftwareVersion)
            .addMapping(EdgeNode::getHardwareInfo, EdgeNodeDTO::setHardwareInfo)
            .addMapping(EdgeNode::getNetworkBandwidth, EdgeNodeDTO::setNetworkBandwidth)
            .addMapping(EdgeNode::isEnabled, EdgeNodeDTO::setEnabled)
            .addMapping(EdgeNode::getCreatedAt, EdgeNodeDTO::setCreatedAt)
            .addMapping(EdgeNode::getUpdatedAt, EdgeNodeDTO::setUpdatedAt)
            .addMapping(EdgeNode::getPort, EdgeNodeDTO::setPort);
    }
}
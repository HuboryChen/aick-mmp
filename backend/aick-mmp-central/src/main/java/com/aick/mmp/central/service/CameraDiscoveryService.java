package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ConnectivityResultDTO;
import com.aick.mmp.central.dto.DeviceIdentifyDTO;
import com.aick.mmp.central.dto.DiscoveryTaskDTO;
import com.aick.mmp.central.dto.ScanProgressDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CameraDiscoveryService {

    Long startScan(String networkSegment, Long userId);

    ScanProgressDTO getScanProgress(Long taskId);

    void cancelScan(Long taskId);

    ConnectivityResultDTO testConnectivity(String ip, Integer port);

    DeviceIdentifyDTO identifyDevice(String ip, Integer port);

    Page<DiscoveryTaskDTO> getScanHistory(Pageable pageable, Long userId);
}

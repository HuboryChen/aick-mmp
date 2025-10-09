package com.aick.mmp.central.service.impl;

import com.aick.mmp.central.dto.RecordingDTO;
import com.aick.mmp.central.service.RecordingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecordingServiceImpl implements RecordingService {

    @Override
    public Page<RecordingDTO> getRecordings(Long cameraId, String location, LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        // 模拟数据 - 实际项目中应该从数据库查询
        List<RecordingDTO> recordings = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            recordings.add(RecordingDTO.builder()
                    .id((long) i)
                    .cameraId((long) (i % 5 + 1))
                    .location("Location " + (i % 3 + 1))
                    .startTime(LocalDateTime.now().minusDays(i))
                    .endTime(LocalDateTime.now().minusDays(i).plusMinutes(30))
                    .size((long) (100 + i * 10) * 1024 * 1024) // 转换为字节
                    .build());
        }

        // 根据参数过滤数据
        List<RecordingDTO> filtered = recordings.stream()
                .filter(r -> cameraId == null || r.getCameraId().equals(cameraId))
                .filter(r -> location == null || r.getLocation().contains(location))
                .filter(r -> startTime == null || r.getEndTime().isAfter(startTime))
                .filter(r -> endTime == null || r.getStartTime().isBefore(endTime))
                .collect(Collectors.toList());

        // 简单实现分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<RecordingDTO> paged = filtered.subList(start, end);
        
        return new PageImpl<>(paged, pageable, filtered.size());
    }

    @Override
    public RecordingDTO getRecordingById(Long id) {
        // 模拟数据 - 实际项目中应该从数据库查询
        return RecordingDTO.builder()
                .id(id)
                .cameraId(1L)
                .location("Location 1")
                .startTime(LocalDateTime.now().minusHours(2))
                .endTime(LocalDateTime.now().minusHours(1))
                .size(500L * 1024 * 1024) // 转换为字节
                .build();
    }

    @Override
    public String getRecordingUrl(Long recordingId) {
        // 模拟数据 - 实际项目中应该返回真实的录像URL
        return "/api/recordings/" + recordingId + "/stream";
    }

    @Override
    public void deleteRecording(Long recordingId) {
        // 模拟数据 - 实际项目中应该从数据库删除录像记录并删除文件
    }

    @Override
    public List<RecordingDTO> getRecordingsByCameraId(Long cameraId) {
        // 模拟数据 - 实际项目中应该从数据库查询
        List<RecordingDTO> recordings = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            recordings.add(RecordingDTO.builder()
                    .id((long) i)
                    .cameraId(cameraId)
                    .location("Location " + (i % 3 + 1))
                    .startTime(LocalDateTime.now().minusDays(i))
                    .endTime(LocalDateTime.now().minusDays(i).plusMinutes(30))
                    .size((long) (100 + i * 10) * 1024 * 1024) // 转换为字节
                    .build());
        }
        return recordings;
    }

    @Override
    public long getTotalRecordingSize() {
        // 模拟数据 - 实际项目中应该从数据库查询
        return 1024L * 1024 * 1024; // 1GB
    }
}
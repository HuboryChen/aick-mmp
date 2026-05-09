package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.CameraFailoverEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CameraFailoverEventRepository extends JpaRepository<CameraFailoverEvent, Long> {

    /**
     * 按源节点ID查询故障转移事件
     */
    Page<CameraFailoverEvent> findBySourceEdgeNodeId(Long sourceEdgeNodeId, Pageable pageable);

    /**
     * 按触发类型查询
     */
    Page<CameraFailoverEvent> findByTriggerType(CameraFailoverEvent.FailoverTriggerType triggerType, Pageable pageable);

    /**
     * 按状态查询
     */
    Page<CameraFailoverEvent> findByStatus(CameraFailoverEvent.FailoverStatus status, Pageable pageable);

    /**
     * 按源节点和触发类型查询
     */
    List<CameraFailoverEvent> findBySourceEdgeNodeIdAndTriggerType(
            Long sourceEdgeNodeId,
            CameraFailoverEvent.FailoverTriggerType triggerType);

    /**
     * 查询进行中的事件
     */
    List<CameraFailoverEvent> findByStatus(CameraFailoverEvent.FailoverStatus status);

    /**
     * 综合筛选查询
     */
    @Query("SELECT e FROM CameraFailoverEvent e WHERE " +
           "(:sourceEdgeNodeId IS NULL OR e.sourceEdgeNodeId = :sourceEdgeNodeId) AND " +
           "(:triggerType IS NULL OR e.triggerType = :triggerType) AND " +
           "(:status IS NULL OR e.status = :status)")
    Page<CameraFailoverEvent> findByConditions(
            @Param("sourceEdgeNodeId") Long sourceEdgeNodeId,
            @Param("triggerType") CameraFailoverEvent.FailoverTriggerType triggerType,
            @Param("status") CameraFailoverEvent.FailoverStatus status,
            Pageable pageable);
}

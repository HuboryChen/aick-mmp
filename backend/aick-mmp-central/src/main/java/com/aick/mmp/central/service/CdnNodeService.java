package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.*;
import com.aick.mmp.shared.model.CdnNode;
import com.aick.mmp.shared.model.CdnNodeLoad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CDN节点服务接口
 */
public interface CdnNodeService {

    // ==================== 基础CRUD操作 ====================
    
    /**
     * 获取所有CDN节点（分页）
     */
    Page<CdnNodeDTO> getAllCdnNodes(Pageable pageable);

    /**
     * 获取所有未删除的CDN节点
     */
    List<CdnNodeDTO> getAllActiveCdnNodes();

    /**
     * 根据区域获取CDN节点（支持递归查询）
     * region参数可以是regionId或regionCode
     */
    Page<CdnNodeDTO> getCdnNodesByRegion(String region, Pageable pageable);

    /**
     * 根据区域ID获取CDN节点（支持递归查询）
     */
    Page<CdnNodeDTO> getCdnNodesByRegionId(Long regionId, boolean recursive, Pageable pageable);

    /**
     * 根据状态获取CDN节点
     */
    Page<CdnNodeDTO> getCdnNodesByStatus(CdnNode.NodeStatus status, Pageable pageable);

    /**
     * 根据ID获取CDN节点
     */
    CdnNodeDTO getCdnNodeById(Long id);

    /**
     * 根据节点标识符获取CDN节点
     */
    CdnNodeDTO getCdnNodeByNodeId(String nodeId);

    /**
     * 创建CDN节点
     */
    CdnNodeDTO createCdnNode(CdnNodeDTO cdnNodeDTO);

    /**
     * 更新CDN节点
     */
    CdnNodeDTO updateCdnNode(Long id, CdnNodeDTO cdnNodeDTO);

    /**
     * 更新CDN节点状态
     */
    void updateCdnNodeStatus(Long id, String status, String message);

    /**
     * 删除CDN节点（软删除）
     */
    void deleteCdnNode(Long id);

    /**
     * 恢复已删除的CDN节点
     */
    CdnNodeDTO restoreCdnNode(Long id);

    /**
     * 启用CDN节点
     */
    void enableCdnNode(Long id);

    /**
     * 禁用CDN节点
     */
    void disableCdnNode(Long id);

    // ==================== 负载相关操作 ====================

    /**
     * 注册心跳
     */
    void registerHeartbeat(String nodeId, Map<String, Object> metrics);

    /**
     * 上报节点负载
     */
    void reportLoad(CdnNodeReportDTO report);

    /**
     * 获取节点负载历史
     */
    List<CdnNodeLoadDTO> getLoadHistory(Long nodeId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取节点最新负载
     */
    CdnNodeLoadDTO getLatestLoad(Long nodeId);

    /**
     * 获取节点统计信息
     */
    CdnNodeStatsDTO getCdnNodeStatistics(Long nodeId);

    // ==================== 负载均衡相关操作 ====================

    /**
     * 获取最佳CDN节点（基础：按区域和负载）
     */
    List<CdnNodeDTO> getBestCdnNodesForRegion(String region, int count);

    /**
     * 使用WLC算法获取最佳CDN节点
     */
    List<CdnNodeDTO> getBestCdnNodesByWlc(int count);

    /**
     * 使用地理邻近性+WLC混合算法获取最佳CDN节点
     */
    List<CdnNodeDTO> getBestCdnNodesByGeoAndWlc(String regionCode, int count);

    /**
     * 选择单个最优CDN节点
     */
    CdnNodeDTO selectOptimalNode();

    // ==================== 健康检查相关操作 ====================

    /**
     * 执行连通性测试
     */
    CdnNodeConnectivityTestDTO testConnectivity(Long nodeId);

    /**
     * 批量执行健康检查
     */
    Map<Long, CdnNodeConnectivityTestDTO> batchHealthCheck();

    /**
     * 更新节点心跳超时状态
     */
    void checkHeartbeatTimeout();

    // ==================== 统计相关操作 ====================

    /**
     * 获取全局CDN节点统计
     */
    Map<String, Object> getGlobalCdnStats();

    /**
     * 获取区域CDN节点统计
     */
    Map<String, Object> getRegionCdnStats(String region);

    /**
     * 获取健康节点列表
     */
    List<CdnNodeDTO> getHealthyNodes();
    
    /**
     * 获取所有CDN节点实体列表（用于分析统计）
     */
    List<CdnNode> getAllNodes();
}

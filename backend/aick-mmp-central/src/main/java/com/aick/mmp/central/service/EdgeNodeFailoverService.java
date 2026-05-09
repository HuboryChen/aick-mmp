package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.CameraFailoverEvent;

/**
 * 边缘节点故障转移服务接口
 * 负责在边缘节点离线时自动将其上的摄像头迁移到其他健康节点
 */
public interface EdgeNodeFailoverService {

    /**
     * 触发指定节点的故障转移
     *
     * @param sourceNodeId 源（离线）节点ID
     * @param triggerType  触发类型：AUTO（自动）或 MANUAL（手动）
     * @return 创建的故障转移事件ID
     */
    Long triggerFailover(Long sourceNodeId, CameraFailoverEvent.FailoverTriggerType triggerType);

    /**
     * 处理待分配池中的摄像头
     * 尝试将 PENDING_ALLOCATION 状态的摄像头分配到在线的健康节点
     *
     * @return 成功分配的摄像头数量
     */
    int processPendingAllocationPool();
}

package com.aick.mmp.central.service;

import com.aick.mmp.shared.model.AlertEscalation;
import com.aick.mmp.shared.model.AlertRecord;

import java.util.List;

/**
 * 告警升级服务接口
 * 负责管理告警升级逻辑，包括升级条件检查、升级执行等
 */
public interface EscalationService {

    /**
     * 执行升级检查
     * 由定时任务调用，检查所有待处理的告警是否需要升级
     */
    void checkAndEscalate();

    /**
     * 为告警记录创建升级配置
     * 当告警规则包含升级配置时，为每个新告警创建对应的升级配置
     *
     * @param alertRecord 告警记录
     * @return 创建的升级配置列表
     */
    List<AlertEscalation> createEscalationsForAlert(AlertRecord alertRecord);

    /**
     * 手动触发升级
     *
     * @param escalationId 升级配置ID
     */
    void triggerEscalation(Long escalationId);

    /**
     * 手动触发告警的所有升级
     *
     * @param alertRecordId 告警记录ID
     */
    void triggerAllEscalations(Long alertRecordId);

    /**
     * 跳过某个升级
     *
     * @param escalationId 升级配置ID
     */
    void skipEscalation(Long escalationId);

    /**
     * 获取告警的升级历史
     *
     * @param alertRecordId 告警记录ID
     * @return 升级历史列表
     */
    List<AlertEscalation> getEscalationHistory(Long alertRecordId);

    /**
     * 检查升级条件是否满足
     *
     * @param escalation 升级配置
     * @param alertRecord 告警记录
     * @return 是否满足条件
     */
    boolean checkCondition(AlertEscalation escalation, AlertRecord alertRecord);

    /**
     * 执行升级动作
     *
     * @param escalation 升级配置
     * @param alertRecord 告警记录
     */
    void executeEscalationAction(AlertEscalation escalation, AlertRecord alertRecord);

    /**
     * 获取待处理的升级数量
     *
     * @return 待处理升级数量
     */
    long countPendingEscalations();
}

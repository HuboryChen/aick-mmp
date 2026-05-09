package com.aick.mmp.central.service;

import com.aick.mmp.central.dto.ReportRequestDTO;
import com.aick.mmp.central.dto.ReportSubscriptionDTO;

import java.io.OutputStream;
import java.util.List;

/**
 * 报表服务接口
 */
public interface ReportService {

    /**
     * 生成报表
     */
    byte[] generateReport(ReportRequestDTO request);

    /**
     * 生成报表并写入输出流
     */
    void generateReportToStream(ReportRequestDTO request, OutputStream outputStream);

    /**
     * 获取订阅列表
     */
    List<ReportSubscriptionDTO> getSubscriptions(Long userId);

    /**
     * 获取订阅详情
     */
    ReportSubscriptionDTO getSubscription(Long id);

    /**
     * 创建订阅
     */
    ReportSubscriptionDTO createSubscription(ReportSubscriptionDTO subscription, Long userId);

    /**
     * 更新订阅
     */
    ReportSubscriptionDTO updateSubscription(Long id, ReportSubscriptionDTO subscription);

    /**
     * 删除订阅
     */
    void deleteSubscription(Long id);

    /**
     * 启用/禁用订阅
     */
    ReportSubscriptionDTO toggleSubscription(Long id, boolean enabled);

    /**
     * 手动触发报表生成
     */
    byte[] triggerReport(Long subscriptionId);

    /**
     * 发送订阅报表
     */
    void sendSubscriptionReports();

    /**
     * 获取支持的数据维度
     */
    List<String> getAvailableDimensions();

    /**
     * 获取报表模板列表
     */
    List<String> getReportTemplates();
}

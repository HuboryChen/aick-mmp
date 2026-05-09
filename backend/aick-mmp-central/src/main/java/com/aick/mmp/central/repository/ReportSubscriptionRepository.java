package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.ReportSubscription;
import com.aick.mmp.shared.model.enums.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表订阅仓库接口
 */
@Repository
public interface ReportSubscriptionRepository extends JpaRepository<ReportSubscription, Long> {

    /**
     * 按创建人查询
     */
    List<ReportSubscription> findByCreatedBy(Long createdBy);

    /**
     * 查询启用的订阅
     */
    List<ReportSubscription> findByEnabledTrue();

    /**
     * 查询需要发送的订阅
     */
    @Query("SELECT r FROM ReportSubscription r WHERE r.enabled = true " +
           "AND r.nextSendTime <= :now")
    List<ReportSubscription> findDueSubscriptions(@Param("now") LocalDateTime now);

    /**
     * 按报表类型查询
     */
    List<ReportSubscription> findByReportType(ReportType reportType);

    /**
     * 统计用户订阅数量
     */
    long countByCreatedBy(Long createdBy);

    /**
     * 检查名称是否存在
     */
    boolean existsByNameAndCreatedBy(String name, Long createdBy);

    /**
     * 检查名称是否存在（排除指定ID）
     */
    boolean existsByNameAndCreatedByAndIdNot(String name, Long createdBy, Long excludeId);
}

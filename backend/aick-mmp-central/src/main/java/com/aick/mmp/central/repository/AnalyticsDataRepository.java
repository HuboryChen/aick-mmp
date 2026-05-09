package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.AnalyticsData;
import com.aick.mmp.shared.model.enums.AggregationLevel;
import com.aick.mmp.shared.model.enums.AnalyticsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分析数据仓库接口
 */
@Repository
public interface AnalyticsDataRepository extends JpaRepository<AnalyticsData, Long> {

    /**
     * 按类型和时间范围查询
     */
    List<AnalyticsData> findByAnalyticsTypeAndPeriodStartBetween(
            AnalyticsType type, LocalDateTime start, LocalDateTime end);

    /**
     * 按类型、维度和时间范围查询
     */
    List<AnalyticsData> findByAnalyticsTypeAndDimensionAndDimensionValueAndPeriodStartBetween(
            AnalyticsType type, String dimension, String dimensionValue, LocalDateTime start, LocalDateTime end);

    /**
     * 按类型和聚合粒度查询
     */
    List<AnalyticsData> findByAnalyticsTypeAndAggregationLevelAndPeriodStartBetween(
            AnalyticsType type, AggregationLevel level, LocalDateTime start, LocalDateTime end);

    /**
     * 分页查询
     */
    Page<AnalyticsData> findByAnalyticsType(AnalyticsType type, Pageable pageable);

    /**
     * 按维度查询最新数据
     */
    List<AnalyticsData> findByDimensionAndDimensionValueAndPeriodStartBetweenOrderByPeriodStartDesc(
            String dimension, String dimensionValue, LocalDateTime start, LocalDateTime end);

    /**
     * 统计指定时间范围内的数据点数量
     */
    @Query("SELECT COUNT(a) FROM AnalyticsData a WHERE a.analyticsType = :type " +
           "AND a.periodStart BETWEEN :start AND :end")
    long countByTypeAndPeriod(@Param("type") AnalyticsType type,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /**
     * 获取指定维度的最新记录
     */
    @Query("SELECT a FROM AnalyticsData a WHERE a.dimension = :dimension " +
           "AND a.dimensionValue = :dimensionValue " +
           "ORDER BY a.periodStart DESC LIMIT 1")
    AnalyticsData findLatestByDimension(@Param("dimension") String dimension,
                                        @Param("dimensionValue") String dimensionValue);

    /**
     * 按指标名称查询
     */
    List<AnalyticsData> findByMetricNameAndPeriodStartBetweenOrderByPeriodStartAsc(
            String metricName, LocalDateTime start, LocalDateTime end);

    /**
     * 删除过期数据
     */
    void deleteByPeriodEndBefore(LocalDateTime before);

    /**
     * 按多个维度查询
     */
    @Query("SELECT a FROM AnalyticsData a WHERE a.analyticsType = :type " +
           "AND a.dimension IN :dimensions AND a.periodStart BETWEEN :start AND :end " +
           "ORDER BY a.periodStart ASC")
    List<AnalyticsData> findByTypeAndDimensions(@Param("type") AnalyticsType type,
                                                 @Param("dimensions") List<String> dimensions,
                                                 @Param("start") LocalDateTime start,
                                                 @Param("end") LocalDateTime end);
}

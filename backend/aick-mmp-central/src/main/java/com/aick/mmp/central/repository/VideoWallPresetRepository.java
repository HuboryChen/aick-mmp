package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.VideoWallPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 视频墙预设数据访问接口
 */
@Repository
public interface VideoWallPresetRepository extends JpaRepository<VideoWallPreset, Long> {

    /**
     * 根据用户ID查询所有预设
     */
    List<VideoWallPreset> findByUserId(Long userId);

    /**
     * 根据用户ID查询预设，按排序顺序排序
     */
    List<VideoWallPreset> findByUserIdOrderBySortOrderAsc(Long userId);

    /**
     * 根据用户ID和预设名称查询
     */
    Optional<VideoWallPreset> findByUserIdAndPresetName(Long userId, String presetName);

    /**
     * 根据用户ID查询默认预设
     */
    Optional<VideoWallPreset> findByUserIdAndIsDefaultTrue(Long userId);

    /**
     * 检查预设是否存在
     */
    boolean existsByUserIdAndPresetName(Long userId, String presetName);

    /**
     * 统计用户的预设数量
     */
    long countByUserId(Long userId);

    /**
     * 清除用户的所有默认标记
     */
    @Modifying
    @Query("UPDATE VideoWallPreset p SET p.isDefault = false WHERE p.userId = :userId")
    int clearDefaultByUserId(@Param("userId") Long userId);

    /**
     * 删除用户的指定预设
     */
    void deleteByUserIdAndId(Long userId, Long id);
}

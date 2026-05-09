package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.VideoWallPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 视频墙偏好设置数据访问接口
 */
@Repository
public interface VideoWallPreferencesRepository extends JpaRepository<VideoWallPreferences, Long> {

    /**
     * 根据用户ID查询偏好设置
     */
    Optional<VideoWallPreferences> findByUserId(Long userId);

    /**
     * 检查用户偏好设置是否存在
     */
    boolean existsByUserId(Long userId);

    /**
     * 删除用户的偏好设置
     */
    void deleteByUserId(Long userId);
}

package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.CameraConfigTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CameraConfigTemplateRepository extends JpaRepository<CameraConfigTemplate, Long> {

    Page<CameraConfigTemplate> findByIsDeletedFalse(Pageable pageable);

    List<CameraConfigTemplate> findByBrandAndIsDeletedFalse(String brand);

    Optional<CameraConfigTemplate> findByBrandAndModelAndIsDeletedFalse(String brand, String model);

    List<CameraConfigTemplate> findByIsPresetAndIsDeletedFalse(boolean isPreset);

    List<CameraConfigTemplate> findByProtocolAndIsDeletedFalse(String protocol);

    @Query("SELECT DISTINCT c.brand FROM CameraConfigTemplate c WHERE c.isDeleted = false")
    List<String> findDistinctBrandByIsDeletedFalse();
}

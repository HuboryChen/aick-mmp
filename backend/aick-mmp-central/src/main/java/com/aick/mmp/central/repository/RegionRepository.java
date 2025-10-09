package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    Optional<Region> findByCode(String code);
    
    List<Region> findByParentId(Long parentId);
    
    List<Region> findByParentIdIsNull();
}
package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiVehicleWhitelist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiVehicleWhitelistRepository extends JpaRepository<AiVehicleWhitelist, Long> {
    Optional<AiVehicleWhitelist> findByPlateNumber(String plateNumber);
    boolean existsByPlateNumber(String plateNumber);
}

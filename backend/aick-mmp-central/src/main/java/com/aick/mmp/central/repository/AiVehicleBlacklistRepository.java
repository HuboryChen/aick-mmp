package com.aick.mmp.central.repository;

import com.aick.mmp.central.entity.AiVehicleBlacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiVehicleBlacklistRepository extends JpaRepository<AiVehicleBlacklist, Long> {
    Optional<AiVehicleBlacklist> findByPlateNumber(String plateNumber);
}

package com.aick.mmp.central.repository;

import com.aick.mmp.shared.model.CameraBatchImportTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CameraBatchImportTaskRepository extends JpaRepository<CameraBatchImportTask, Long> {

    Page<CameraBatchImportTask> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<CameraBatchImportTask> findByStatus(String status);
}

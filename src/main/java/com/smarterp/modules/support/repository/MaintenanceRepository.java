package com.smarterp.modules.support.repository;

import com.smarterp.modules.support.entity.Maintenance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, String> {
    List<Maintenance> findByBusinessId(String businessId);
}
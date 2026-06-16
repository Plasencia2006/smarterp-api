package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.PhysicalCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhysicalCountRepository extends JpaRepository<PhysicalCount, String> {
}
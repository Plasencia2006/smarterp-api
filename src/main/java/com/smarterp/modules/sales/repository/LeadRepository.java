package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {
    List<Lead> findBySellerId(String sellerId);
}
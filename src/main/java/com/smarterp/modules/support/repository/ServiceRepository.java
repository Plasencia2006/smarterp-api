package com.smarterp.modules.support.repository;

import com.smarterp.modules.support.entity.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, String> {
    List<Service> findByBusinessId(String businessId);
}
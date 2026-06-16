package com.smarterp.modules.support.repository;

import com.smarterp.modules.support.entity.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ServiceOrderRepository extends JpaRepository<ServiceOrder, String> {
    List<ServiceOrder> findByBusinessId(String businessId);
}
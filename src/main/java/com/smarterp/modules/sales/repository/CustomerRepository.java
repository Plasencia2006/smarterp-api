package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {
    List<Customer> findByBusinessId(String businessId);
}
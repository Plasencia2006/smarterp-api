package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    List<Customer> findByBusinessIdOrderByCreatedAtDesc(String businessId);

    Optional<Customer> findByDocumentNumberAndBusinessId(String documentNumber, String businessId);

    @Query("SELECT c FROM Customer c WHERE c.businessId = :businessId AND " +
            "(LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.documentNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(c.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Customer> searchCustomers(@Param("businessId") String businessId, @Param("search") String search);

    List<Customer> findByBusinessIdAndIsFrequentTrue(String businessId);
}
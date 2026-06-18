package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, String> {

    List<Supplier> findByBusinessId(String businessId);

    @Query("SELECT s FROM Supplier s WHERE s.businessId = :businessId AND " +
            "(LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(s.ruc) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Supplier> searchSuppliers(@Param("businessId") String businessId,
            @Param("search") String search);
}
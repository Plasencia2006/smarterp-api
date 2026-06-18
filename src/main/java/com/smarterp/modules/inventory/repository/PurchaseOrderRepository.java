package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {

    // ✅ Fetch Join para cargar items en una sola query
    @Query("SELECT DISTINCT p FROM PurchaseOrder p LEFT JOIN FETCH p.items WHERE p.businessId = :businessId ORDER BY p.createdAt DESC")
    List<PurchaseOrder> findByBusinessIdWithItems(@Param("businessId") String businessId);

    // ✅ Con items para búsqueda por ID
    @Query("SELECT p FROM PurchaseOrder p LEFT JOIN FETCH p.items WHERE p.id = :id")
    Optional<PurchaseOrder> findByIdWithItems(@Param("id") String id);

    // ✅ Búsqueda con filtros
    @Query("SELECT DISTINCT p FROM PurchaseOrder p LEFT JOIN FETCH p.items WHERE p.businessId = :businessId AND " +
            "(LOWER(p.supplierName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(p.orderNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<PurchaseOrder> searchPurchases(@Param("businessId") String businessId,
            @Param("search") String search);

    // ✅ Método simple (sin items)
    List<PurchaseOrder> findByBusinessIdOrderByCreatedAtDesc(String businessId);
}
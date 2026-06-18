package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.PurchaseItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, String> {
    List<PurchaseItem> findByPurchaseOrderId(String purchaseOrderId);

    void deleteByPurchaseOrderId(String purchaseOrderId);
}
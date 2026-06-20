package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.SalePOSItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalePOSItemRepository extends JpaRepository<SalePOSItem, String> {
    List<SalePOSItem> findBySalePOSId(String salePOSId);

    void deleteBySalePOSId(String salePOSId);
}
package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {

    List<ProductCategory> findByBusinessId(String businessId);

    List<ProductCategory> findByBusinessIdAndIsActive(String businessId, Boolean isActive);

    @Query("SELECT pc FROM ProductCategory pc WHERE pc.businessId = :businessId AND " +
            "(LOWER(pc.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(pc.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<ProductCategory> searchCategories(@Param("businessId") String businessId,
            @Param("search") String search);

    List<ProductCategory> findByParentId(String parentId);

    boolean existsByNameAndBusinessId(String name, String businessId);
}
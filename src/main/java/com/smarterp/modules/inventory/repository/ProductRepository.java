package com.smarterp.modules.inventory.repository;

import com.smarterp.modules.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByBusinessId(String businessId);
}
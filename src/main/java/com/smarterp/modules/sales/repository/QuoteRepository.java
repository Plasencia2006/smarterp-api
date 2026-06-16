package com.smarterp.modules.sales.repository;

import com.smarterp.modules.sales.entity.Quote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, String> {
    List<Quote> findBySellerId(String sellerId);
}
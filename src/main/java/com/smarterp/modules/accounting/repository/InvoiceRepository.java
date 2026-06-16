package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    List<Invoice> findByBusinessId(String businessId);
}
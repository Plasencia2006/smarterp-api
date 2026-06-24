package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.ProductSerial;
import com.smarterp.modules.cashier.entity.SerialStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSerialRepository extends JpaRepository<ProductSerial, String> {

    Optional<ProductSerial> findBySerialNumber(String serialNumber);

    Optional<ProductSerial> findByImei(String imei);

    List<ProductSerial> findByProductIdAndBusinessId(String productId, String businessId);

    List<ProductSerial> findByProductIdAndBusinessIdAndStatus(
            String productId, String businessId, SerialStatus status);

    List<ProductSerial> findByQuoteId(String quoteId);

    List<ProductSerial> findByCustomerName(String customerName);
}
package com.smarterp.modules.cashier.repository;

import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.entity.CashRegisterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, String> {

    Optional<CashRegister> findByUserIdAndStatus(String userId, CashRegisterStatus status);

    List<CashRegister> findByBusinessIdAndUserIdOrderByOpeningTimeDesc(String businessId, String userId);

    List<CashRegister> findByBusinessIdOrderByOpeningTimeDesc(String businessId);
}
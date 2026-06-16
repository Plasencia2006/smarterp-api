package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.AccountReceivable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountReceivableRepository extends JpaRepository<AccountReceivable, String> {
    List<AccountReceivable> findByBusinessId(String businessId);
}
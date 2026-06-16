package com.smarterp.modules.accounting.repository;

import com.smarterp.modules.accounting.entity.AccountPayable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AccountPayableRepository extends JpaRepository<AccountPayable, String> {
    List<AccountPayable> findByBusinessId(String businessId);
}
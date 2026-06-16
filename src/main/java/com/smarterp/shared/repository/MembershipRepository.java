package com.smarterp.shared.repository;

import com.smarterp.shared.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, String> {
    List<Membership> findByUserId(String userId);

    List<Membership> findByBusinessId(String businessId);
}
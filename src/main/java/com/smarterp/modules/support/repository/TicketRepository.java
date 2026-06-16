package com.smarterp.modules.support.repository;

import com.smarterp.modules.support.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByBusinessId(String businessId);

    List<Ticket> findByAssignedToId(String assignedToId);
}
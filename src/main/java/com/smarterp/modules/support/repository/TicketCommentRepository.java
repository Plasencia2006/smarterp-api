package com.smarterp.modules.support.repository;

import com.smarterp.modules.support.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, String> {
    List<TicketComment> findByTicketId(String ticketId);
}
package com.smarterp.modules.support.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.support.entity.Ticket;
import com.smarterp.modules.support.entity.TicketComment;
import com.smarterp.modules.support.entity.TicketStatus;
import com.smarterp.modules.support.repository.TicketRepository;
import com.smarterp.modules.support.repository.TicketCommentRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.entity.User;
import com.smarterp.shared.repository.BusinessRepository;
import com.smarterp.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Ticket>>> getTickets() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(ticketRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Ticket>> createTicket(@RequestBody Ticket ticket) {
        String businessId = userContext.getCurrentBusinessId();
        String userId = userContext.getCurrentUserId();

        Business business = businessRepository.findById(businessId).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        ticket.setBusiness(business);
        ticket.setCreatedBy(user);

        return ResponseEntity.ok(ApiResponse.success("Ticket creado", ticketRepository.save(ticket)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> getTicket(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(ticketRepository.findById(id).orElseThrow()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Ticket>> updateTicket(@PathVariable String id, @RequestBody Ticket ticket) {
        Ticket existing = ticketRepository.findById(id).orElseThrow();
        existing.setTitle(ticket.getTitle());
        existing.setDescription(ticket.getDescription());
        existing.setStatus(ticket.getStatus());
        existing.setPriority(ticket.getPriority());
        return ResponseEntity.ok(ApiResponse.success("Ticket actualizado", ticketRepository.save(existing)));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<Ticket>> assignTicket(@PathVariable String id, @RequestParam String userId) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();
        ticket.setAssignedTo(user);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        return ResponseEntity.ok(ApiResponse.success("Ticket asignado", ticketRepository.save(ticket)));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<ApiResponse<Ticket>> closeTicket(@PathVariable String id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        ticket.setStatus(TicketStatus.CLOSED);
        return ResponseEntity.ok(ApiResponse.success("Ticket cerrado", ticketRepository.save(ticket)));
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<List<TicketComment>>> getComments(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(commentRepository.findByTicketId(id)));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<TicketComment>> addComment(@PathVariable String id,
            @RequestBody TicketComment comment) {
        String userId = userContext.getCurrentUserId();

        Ticket ticket = ticketRepository.findById(id).orElseThrow();
        User user = userRepository.findById(userId).orElseThrow();

        comment.setTicket(ticket);
        comment.setCreatedBy(user);

        return ResponseEntity.ok(ApiResponse.success("Comentario agregado", commentRepository.save(comment)));
    }
}
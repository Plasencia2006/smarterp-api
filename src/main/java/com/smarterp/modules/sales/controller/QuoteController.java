package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.entity.Quote;
import com.smarterp.modules.sales.entity.QuoteStatus;
import com.smarterp.modules.sales.repository.QuoteRepository;
import com.smarterp.shared.entity.User;
import com.smarterp.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/quotes")
@RequiredArgsConstructor
public class QuoteController {

    private final QuoteRepository quoteRepository;
    private final UserRepository userRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Quote>>> getMyQuotes() {
        String userId = userContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(quoteRepository.findBySellerId(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Quote>> createQuote(@RequestBody Quote quote) {
        String userId = userContext.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();
        quote.setSeller(user);
        return ResponseEntity.ok(ApiResponse.success("Cotización creada", quoteRepository.save(quote)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Quote>> updateQuote(@PathVariable String id, @RequestBody Quote quote) {
        Quote existing = quoteRepository.findById(id).orElseThrow();
        existing.setSubtotal(quote.getSubtotal());
        existing.setTax(quote.getTax());
        existing.setTotal(quote.getTotal());
        return ResponseEntity.ok(ApiResponse.success("Cotización actualizada", quoteRepository.save(existing)));
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<ApiResponse<Quote>> convertToSale(@PathVariable String id) {
        Quote quote = quoteRepository.findById(id).orElseThrow();
        quote.setStatus(QuoteStatus.CONVERTED);
        return ResponseEntity.ok(ApiResponse.success("Cotización convertida a venta", quoteRepository.save(quote)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteQuote(@PathVariable String id) {
        quoteRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Cotización eliminada", null));
    }
}
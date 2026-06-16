package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.accounting.entity.Invoice;
import com.smarterp.modules.accounting.entity.InvoiceStatus;
import com.smarterp.modules.accounting.repository.InvoiceRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounting/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoices() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(invoiceRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Invoice>> createInvoice(@RequestBody Invoice invoice) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        invoice.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Factura creada", invoiceRepository.save(invoice)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Invoice>> getInvoice(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(invoiceRepository.findById(id).orElseThrow()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Invoice>> cancelInvoice(@PathVariable String id) {
        Invoice invoice = invoiceRepository.findById(id).orElseThrow();
        invoice.setStatus(InvoiceStatus.CANCELLED);
        return ResponseEntity.ok(ApiResponse.success("Factura anulada", invoiceRepository.save(invoice)));
    }
}
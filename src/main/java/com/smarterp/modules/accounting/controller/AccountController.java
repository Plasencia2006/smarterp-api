package com.smarterp.modules.accounting.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.accounting.entity.AccountReceivable;
import com.smarterp.modules.accounting.entity.AccountPayable;
import com.smarterp.modules.accounting.entity.AccountStatus;
import com.smarterp.modules.accounting.repository.AccountReceivableRepository;
import com.smarterp.modules.accounting.repository.AccountPayableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounting")
@RequiredArgsConstructor
public class AccountController {

    private final AccountReceivableRepository receivableRepository;
    private final AccountPayableRepository payableRepository;
    private final UserContext userContext;

    @GetMapping("/receivables")
    public ResponseEntity<ApiResponse<List<AccountReceivable>>> getReceivables() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(receivableRepository.findByBusinessId(businessId)));
    }

    @GetMapping("/payables")
    public ResponseEntity<ApiResponse<List<AccountPayable>>> getPayables() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(payableRepository.findByBusinessId(businessId)));
    }

    @PostMapping("/receivables/{id}/pay")
    public ResponseEntity<ApiResponse<AccountReceivable>> payReceivable(@PathVariable String id,
            @RequestParam BigDecimal amount) {
        AccountReceivable receivable = receivableRepository.findById(id).orElseThrow();
        receivable.setPaidAmount(receivable.getPaidAmount().add(amount));

        if (receivable.getPaidAmount().compareTo(receivable.getAmount()) >= 0) {
            receivable.setStatus(AccountStatus.PAID);
        } else {
            receivable.setStatus(AccountStatus.PARTIAL);
        }

        return ResponseEntity.ok(ApiResponse.success("Pago registrado", receivableRepository.save(receivable)));
    }

    @PostMapping("/payables/{id}/pay")
    public ResponseEntity<ApiResponse<AccountPayable>> payPayable(@PathVariable String id,
            @RequestParam BigDecimal amount) {
        AccountPayable payable = payableRepository.findById(id).orElseThrow();
        payable.setPaidAmount(payable.getPaidAmount().add(amount));

        if (payable.getPaidAmount().compareTo(payable.getAmount()) >= 0) {
            payable.setStatus(AccountStatus.PAID);
        } else {
            payable.setStatus(AccountStatus.PARTIAL);
        }

        return ResponseEntity.ok(ApiResponse.success("Pago registrado", payableRepository.save(payable)));
    }
}
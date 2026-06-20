package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.entity.Customer;
import com.smarterp.modules.sales.service.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final UserContext userContext;

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody Customer customer) {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Customer saved = customerService.createCustomer(businessId, customer);
            return ResponseEntity.ok(ApiResponse.success("Cliente creado", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(
            @PathVariable String id, @RequestBody Customer customer) {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Customer updated = customerService.updateCustomer(id, businessId, customer);
            return ResponseEntity.ok(ApiResponse.success("Cliente actualizado", updated));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        try {
            customerService.deleteCustomer(id, businessId);
            return ResponseEntity.ok(ApiResponse.success("Cliente eliminado", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getAllCustomers() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getAllCustomers(businessId)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<Customer>>> searchCustomers(
            @RequestParam String search) {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                customerService.searchCustomers(businessId, search)));
    }

    @GetMapping("/frequent")
    public ResponseEntity<ApiResponse<List<Customer>>> getFrequentCustomers() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getFrequentCustomers(businessId)));
    }

    @GetMapping("/document/{documentNumber}")
    public ResponseEntity<ApiResponse<Customer>> getByDocument(
            @PathVariable String documentNumber) {
        String businessId = userContext.getCurrentBusinessId();
        try {
            Customer customer = customerService.getByDocument(businessId, documentNumber);
            return ResponseEntity.ok(ApiResponse.success(customer));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
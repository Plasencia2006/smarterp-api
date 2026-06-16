package com.smarterp.modules.sales.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.sales.entity.Customer;
import com.smarterp.modules.sales.repository.CustomerRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Customer>>> getMyCustomers() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(customerRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@RequestBody Customer customer) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        customer.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Cliente creado", customerRepository.save(customer)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Customer>> updateCustomer(@PathVariable String id,
            @RequestBody Customer customer) {
        Customer existing = customerRepository.findById(id).orElseThrow();
        existing.setName(customer.getName());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        return ResponseEntity.ok(ApiResponse.success("Cliente actualizado", customerRepository.save(existing)));
    }
}
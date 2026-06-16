package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Supplier;
import com.smarterp.modules.inventory.repository.SupplierRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Supplier>>> getSuppliers() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(supplierRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        supplier.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Proveedor creado", supplierRepository.save(supplier)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(@PathVariable String id,
            @RequestBody Supplier supplier) {
        Supplier existing = supplierRepository.findById(id).orElseThrow();
        existing.setName(supplier.getName());
        existing.setRuc(supplier.getRuc());
        existing.setPhone(supplier.getPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());
        return ResponseEntity.ok(ApiResponse.success("Proveedor actualizado", supplierRepository.save(existing)));
    }
}
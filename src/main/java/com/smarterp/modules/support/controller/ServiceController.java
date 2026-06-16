package com.smarterp.modules.support.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.support.entity.Service;
import com.smarterp.modules.support.repository.ServiceRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Service>>> getServices() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(serviceRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Service>> createService(@RequestBody Service service) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        service.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Servicio creado", serviceRepository.save(service)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Service>> updateService(@PathVariable String id, @RequestBody Service service) {
        Service existing = serviceRepository.findById(id).orElseThrow();
        existing.setName(service.getName());
        existing.setDescription(service.getDescription());
        existing.setPrice(service.getPrice());
        return ResponseEntity.ok(ApiResponse.success("Servicio actualizado", serviceRepository.save(existing)));
    }
}
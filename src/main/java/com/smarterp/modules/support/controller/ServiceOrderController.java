package com.smarterp.modules.support.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.support.entity.ServiceOrder;
import com.smarterp.modules.support.entity.ServiceOrderStatus;
import com.smarterp.modules.support.repository.ServiceOrderRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/service-orders")
@RequiredArgsConstructor
public class ServiceOrderController {

    private final ServiceOrderRepository serviceOrderRepository;
    private final BusinessRepository businessRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceOrder>>> getServiceOrders() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(serviceOrderRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ServiceOrder>> createServiceOrder(@RequestBody ServiceOrder serviceOrder) {
        String businessId = userContext.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId).orElseThrow();
        serviceOrder.setBusiness(business);
        return ResponseEntity.ok(ApiResponse.success("Orden creada", serviceOrderRepository.save(serviceOrder)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceOrder>> updateServiceOrder(@PathVariable String id,
            @RequestBody ServiceOrder serviceOrder) {
        ServiceOrder existing = serviceOrderRepository.findById(id).orElseThrow();
        existing.setStatus(serviceOrder.getStatus());
        existing.setAssignedTo(serviceOrder.getAssignedTo());
        return ResponseEntity.ok(ApiResponse.success("Orden actualizada", serviceOrderRepository.save(existing)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<ServiceOrder>> completeServiceOrder(@PathVariable String id) {
        ServiceOrder serviceOrder = serviceOrderRepository.findById(id).orElseThrow();
        serviceOrder.setStatus(ServiceOrderStatus.COMPLETED);
        return ResponseEntity.ok(ApiResponse.success("Orden completada", serviceOrderRepository.save(serviceOrder)));
    }
}
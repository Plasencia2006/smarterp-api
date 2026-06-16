package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.inventory.entity.PhysicalCount;
import com.smarterp.modules.inventory.repository.PhysicalCountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory/physical-count")
@RequiredArgsConstructor
public class PhysicalCountController {

    private final PhysicalCountRepository physicalCountRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<PhysicalCount>> startPhysicalCount(@RequestBody PhysicalCount physicalCount) {
        return ResponseEntity.ok(ApiResponse.success(physicalCountRepository.save(physicalCount)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PhysicalCount>> getPhysicalCount(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(physicalCountRepository.findById(id).orElseThrow()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<PhysicalCount>> completePhysicalCount(@PathVariable String id) {
        PhysicalCount physicalCount = physicalCountRepository.findById(id).orElseThrow();
        physicalCount.setStatus(com.smarterp.modules.inventory.entity.PhysicalCountStatus.COMPLETED);
        return ResponseEntity.ok(ApiResponse.success(physicalCountRepository.save(physicalCount)));
    }
}
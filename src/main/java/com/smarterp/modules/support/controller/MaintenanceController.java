package com.smarterp.modules.support.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.modules.support.entity.Maintenance;
import com.smarterp.modules.support.repository.MaintenanceRepository;
import com.smarterp.shared.entity.User;
import com.smarterp.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/support/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceRepository maintenanceRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<Maintenance>>> getMaintenances(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        var business = user.getMemberships().stream().filter(m -> m.getIsActive()).findFirst().map(m -> m.getBusiness())
                .orElseThrow();
        return ResponseEntity.ok(ApiResponse.success(maintenanceRepository.findByBusinessId(business.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Maintenance>> scheduleMaintenance(@RequestBody Maintenance maintenance,
            Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        maintenance.setBusiness(user.getMemberships().stream().filter(m -> m.getIsActive()).findFirst()
                .map(m -> m.getBusiness()).orElseThrow());
        return ResponseEntity.ok(ApiResponse.success(maintenanceRepository.save(maintenance)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Maintenance>> updateMaintenance(@PathVariable String id,
            @RequestBody Maintenance maintenance) {
        Maintenance existing = maintenanceRepository.findById(id).orElseThrow();
        existing.setDescription(maintenance.getDescription());
        existing.setScheduledDate(maintenance.getScheduledDate());
        existing.setStatus(maintenance.getStatus());
        return ResponseEntity.ok(ApiResponse.success(maintenanceRepository.save(existing)));
    }
}
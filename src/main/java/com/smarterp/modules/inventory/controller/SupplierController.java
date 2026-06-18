package com.smarterp.modules.inventory.controller;

import com.smarterp.common.response.ApiResponse;
import com.smarterp.common.utils.UserContext;
import com.smarterp.modules.inventory.entity.Supplier;
import com.smarterp.modules.inventory.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierRepository supplierRepository;
    private final UserContext userContext;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Supplier>>> getSuppliers() {
        String businessId = userContext.getCurrentBusinessId();
        return ResponseEntity.ok(ApiResponse.success(supplierRepository.findByBusinessId(businessId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Supplier>> createSupplier(@RequestBody Supplier supplier) {
        String businessId = userContext.getCurrentBusinessId();

        // ✅ Solo asignar businessId (String), NO buscar en BD
        supplier.setBusinessId(businessId);

        return ResponseEntity.ok(ApiResponse.success("Proveedor creado", supplierRepository.save(supplier)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Supplier>> updateSupplier(
            @PathVariable String id,
            @RequestBody Supplier supplier) {

        String businessId = userContext.getCurrentBusinessId();

        System.out.println("🔄 Actualizando proveedor ID: " + id);
        System.out.println("📝 Datos recibidos: " + supplier);
        System.out.println("📝 Nombre: " + supplier.getName());
        System.out.println("📝 RUC: " + supplier.getRuc());

        Supplier existing = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Proveedor no encontrado"));

        if (!existing.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        // ✅ Actualizar campos EXPLÍCITAMENTE
        existing.setName(supplier.getName());
        existing.setRuc(supplier.getRuc());
        existing.setContactName(supplier.getContactName());
        existing.setPhone(supplier.getPhone());
        existing.setEmail(supplier.getEmail());
        existing.setAddress(supplier.getAddress());

        System.out.println("💾 Guardando proveedor actualizado...");
        Supplier updated = supplierRepository.save(existing);
        System.out.println("✅ Proveedor actualizado: " + updated.getId());

        return ResponseEntity.ok(ApiResponse.success("Proveedor actualizado", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(@PathVariable String id) {
        String businessId = userContext.getCurrentBusinessId();
        Supplier existing = supplierRepository.findById(id).orElseThrow();

        if (!existing.getBusinessId().equals(businessId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("No tiene permisos"));
        }

        supplierRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Proveedor eliminado", null));
    }
}
package com.smarterp.modules.inventory.controller;

import com.smarterp.modules.inventory.service.ProductImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory/products")
@RequiredArgsConstructor
@Slf4j
public class ProductImageController {

    private final ProductImageService imageService;

    /**
     * 📥 OBTENER IMAGEN
     * GET /api/inventory/products/images/{filename}
     */
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable String filename) {
        log.info("📥 Solicitando imagen: {}", filename);
        return imageService.getImage(filename);
    }

    /**
     * 🗑️ ELIMINAR IMAGEN
     * DELETE /api/inventory/products/{id}/delete-image
     */
    @DeleteMapping("/{id}/delete-image")
    public ResponseEntity<?> deleteImage(
            @PathVariable String id,
            @RequestParam String filename) {

        try {
            boolean deleted = imageService.deleteImage(filename);
            if (deleted) {
                return ResponseEntity.ok().body(java.util.Map.of(
                        "success", true,
                        "message", "Imagen eliminada correctamente"));
            } else {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "message", "No se pudo eliminar la imagen"));
            }
        } catch (Exception e) {
            log.error("❌ Error al eliminar imagen: {}", e.getMessage());
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}
package com.smarterp.modules.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductImageService {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    private Path productsDir;

    @PostConstruct
    public void init() {
        try {
            // ✅ Crear directorio principal uploads/
            Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            // ✅ Crear subdirectorio products/
            productsDir = uploadDir.resolve("products").normalize();
            Files.createDirectories(productsDir);

            log.info("📁 Directorio de uploads: {}", uploadDir);
            log.info("📁 Directorio de productos: {}", productsDir);
        } catch (IOException e) {
            log.error("❌ Error al crear directorio de uploads", e);
        }
    }

    /**
     * 📤 SUBIR IMAGEN DE PRODUCTO
     */
    public String uploadImage(MultipartFile file, String productId) throws IOException {
        // Validar archivo
        if (file.isEmpty()) {
            throw new IOException("El archivo está vacío");
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Solo se permiten archivos de imagen");
        }

        // Validar tamaño (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IOException("La imagen no puede superar los 5MB");
        }

        // Generar nombre único
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = productId + "_" + UUID.randomUUID() + extension;

        // ✅ Guardar en uploads/products/ (NO en uploads/)
        Path filePath = productsDir.resolve(filename).normalize();
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("✅ Imagen subida: {} en {}", filename, filePath);
        return filename;
    }

    /**
     * 📥 OBTENER IMAGEN
     */
    public ResponseEntity<Resource> getImage(String filename) {
        try {
            // ✅ Buscar en uploads/products/
            Path filePath = productsDir.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType)
                        : MediaType.IMAGE_JPEG;

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.error("❌ Imagen no encontrada: {}", filePath);
                throw new RuntimeException("No se pudo leer la imagen: " + filename);
            }
        } catch (MalformedURLException e) {
            log.error("❌ Error de URL al obtener imagen: {}", filename, e);
            throw new RuntimeException("Error al obtener la imagen", e);
        } catch (IOException e) {
            log.error("❌ Error de IO al leer tipo de contenido", e);
            throw new RuntimeException("Error al leer el tipo de contenido", e);
        }
    }

    /**
     * 🗑️ ELIMINAR IMAGEN
     */
    public boolean deleteImage(String filename) {
        try {
            // ✅ Eliminar de uploads/products/
            Path filePath = productsDir.resolve(filename).normalize();
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("✅ Imagen eliminada: {}", filename);
            } else {
                log.warn("⚠️ Imagen no encontrada para eliminar: {}", filename);
            }
            return deleted;
        } catch (IOException e) {
            log.error("❌ Error al eliminar imagen: {}", filename, e);
            return false;
        }
    }

    /**
     * 🌐 OBTENER URL DE IMAGEN
     */
    public String getImageUrl(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        return "/api/inventory/products/images/" + filename;
    }
}
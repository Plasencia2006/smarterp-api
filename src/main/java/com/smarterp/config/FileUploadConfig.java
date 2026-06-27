package com.smarterp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class FileUploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // ✅ Obtener ruta absoluta del directorio de uploads
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path productsDir = uploadDir.resolve("products").normalize();

        System.out.println("📁 [FileUploadConfig] Directorio de uploads: " + uploadDir);
        System.out.println(" [FileUploadConfig] Directorio de productos: " + productsDir);
        System.out.println("📁 [FileUploadConfig] ¿Existe products? " + productsDir.toFile().exists());

        // ✅ Registrar handler para servir imágenes de productos
        // URL: http://localhost:8080/api/inventory/products/images/filename.jpg
        registry.addResourceHandler("/api/inventory/products/images/**")
                .addResourceLocations("file:" + productsDir + "/")
                .setCachePeriod(0); // Sin caché para desarrollo

        // ✅ También permitir acceso directo desde /uploads/products/
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:" + productsDir + "/")
                .setCachePeriod(0);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
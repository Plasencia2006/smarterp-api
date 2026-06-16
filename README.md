# SMART ERP API

Sistema de gestión empresarial multi-tenant desarrollado con Spring Boot 4.0.6

## 🚀 Características

- **Autenticación JWT** con integración Django
- **Multi-tenant**: Aislamiento de datos por negocio
- **5 Módulos principales**:
  - 💵 Cajero (Punto de venta)
  - 💰 Vendedor (Ventas y cotizaciones)
  - 📦 Inventario (Gestión de stock)
  - 📊 Contador (Facturación y reportes)
  - 🔧 Soporte (Tickets y servicios)

##  Requisitos

- Java 17+
- MySQL 8.0+
- Maven 3.8+

## ⚙️ Configuración

1. Crear base de datos:
```sql
CREATE DATABASE microservicios_smarterp 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;
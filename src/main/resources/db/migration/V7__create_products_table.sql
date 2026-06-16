-- V7: Crear tabla products
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2) NULL,
    category_id VARCHAR(36) NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_products_business 
        FOREIGN KEY (business_id) REFERENCES businesses(id) 
        ON DELETE CASCADE,
    CONSTRAINT fk_products_category 
        FOREIGN KEY (category_id) REFERENCES product_categories(id) 
        ON DELETE SET NULL,
    
    INDEX idx_products_business (business_id),
    INDEX idx_products_sku (sku)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
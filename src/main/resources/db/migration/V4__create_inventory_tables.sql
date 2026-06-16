CREATE TABLE product_categories (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(500),
    FOREIGN KEY (business_id) REFERENCES businesses(id)
);

CREATE TABLE products (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(50) UNIQUE NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    cost_price DECIMAL(10,2),
    category_id VARCHAR(36),
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    FOREIGN KEY (category_id) REFERENCES product_categories(id)
);

CREATE TABLE stocks (
    id VARCHAR(36) PRIMARY KEY,
    product_id VARCHAR(36) NOT NULL,
    business_id VARCHAR(36) NOT NULL,
    quantity INT DEFAULT 0,
    min_stock INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    UNIQUE KEY unique_product_business (product_id, business_id)
);

CREATE TABLE stock_movements (
    id VARCHAR(36) PRIMARY KEY,
    stock_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity INT NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (stock_id) REFERENCES stocks(id)
);

CREATE TABLE suppliers (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    ruc VARCHAR(20),
    phone VARCHAR(20),
    email VARCHAR(255),
    address VARCHAR(255),
    FOREIGN KEY (business_id) REFERENCES businesses(id)
);

CREATE TABLE purchase_orders (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    supplier_id VARCHAR(36) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

CREATE TABLE purchase_items (
    id VARCHAR(36) PRIMARY KEY,
    purchase_order_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (purchase_order_id) REFERENCES purchase_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
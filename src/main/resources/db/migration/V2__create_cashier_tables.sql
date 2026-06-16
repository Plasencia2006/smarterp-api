CREATE TABLE cash_registers (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    opened_by VARCHAR(36) NOT NULL,
    closed_by VARCHAR(36),
    initial_amount DECIMAL(10,2) NOT NULL,
    final_amount DECIMAL(10,2),
    status VARCHAR(20) NOT NULL,
    opened_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    FOREIGN KEY (opened_by) REFERENCES users(id),
    FOREIGN KEY (closed_by) REFERENCES users(id)
);

CREATE TABLE sales (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    customer_id VARCHAR(36),
    seller_id VARCHAR(36),
    cashier_id VARCHAR(36) NOT NULL,
    cash_register_id VARCHAR(36),
    subtotal DECIMAL(10,2) NOT NULL,
    tax DECIMAL(10,2) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    FOREIGN KEY (cashier_id) REFERENCES users(id),
    FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id)
);

CREATE TABLE sale_items (
    id VARCHAR(36) PRIMARY KEY,
    sale_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    subtotal DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (sale_id) REFERENCES sales(id) ON DELETE CASCADE
);

CREATE TABLE payments (
    id VARCHAR(36) PRIMARY KEY,
    sale_id VARCHAR(36) NOT NULL,
    method VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    reference VARCHAR(100),
    FOREIGN KEY (sale_id) REFERENCES sales(id)
);

CREATE TABLE cash_transactions (
    id VARCHAR(36) PRIMARY KEY,
    cash_register_id VARCHAR(36) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (cash_register_id) REFERENCES cash_registers(id)
);

CREATE TABLE returns (
    id VARCHAR(36) PRIMARY KEY,
    business_id VARCHAR(36) NOT NULL,
    sale_id VARCHAR(36) NOT NULL,
    cashier_id VARCHAR(36) NOT NULL,
    total DECIMAL(10,2) NOT NULL,
    reason VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (business_id) REFERENCES businesses(id),
    FOREIGN KEY (sale_id) REFERENCES sales(id),
    FOREIGN KEY (cashier_id) REFERENCES users(id)
);

CREATE TABLE return_items (
    id VARCHAR(36) PRIMARY KEY,
    return_id VARCHAR(36) NOT NULL,
    product_id VARCHAR(36) NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (return_id) REFERENCES returns(id) ON DELETE CASCADE
);
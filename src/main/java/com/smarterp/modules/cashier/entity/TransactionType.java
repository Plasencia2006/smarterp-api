package com.smarterp.modules.cashier.entity;

public enum TransactionType {
    INGRESO, // Venta, ingreso de dinero
    EGRESO, // Gasto, retiro de dinero
    APERTURA, // Dinero inicial de apertura
    CIERRE // Dinero final de cierre
}
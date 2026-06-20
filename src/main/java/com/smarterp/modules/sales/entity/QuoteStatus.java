package com.smarterp.modules.sales.entity;

public enum QuoteStatus {
    PENDIENTE, // Vendedor creó, esperando pago
    PAGADA, // Cajero cobró
    CANCELADA // Cliente canceló
}
package com.smarterp.modules.sales.entity;

public enum QuoteStatus {
    PENDIENTE, // Vendedor creó, esperando pago
    PAGADA,  
    FACTURADA, //  NUEVO - Cajero cobró y facturó
    CANCELADA, // Cliente canceló
    EXPIRADA //  NUEVO - Cotización fuera de vigencia
}
package com.smarterp.modules.cashier.entity;

public enum AuditStatus {
    PENDIENTE, // Arqueo iniciado, esperando conteo
    CONCORDANTE, // El conteo coincide con lo esperado
    DISCORDANTE, // Hay diferencia en el conteo
    CANCELADO // Arqueo cancelado
}
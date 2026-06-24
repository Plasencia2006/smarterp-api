package com.smarterp.modules.cashier.entity;

public enum WithdrawalStatus {
    SOLICITADO, // Cajero solicitó retiro
    APROBADO, // Supervisor aprobó
    RECHAZADO, // Supervisor rechazó
    COMPLETADO, // Dinero entregado a caja fuerte
    CANCELADO // Retiro cancelado
}
package com.smarterp.modules.admin.service;

import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.entity.CashTransaction;
import com.smarterp.modules.cashier.entity.CashRegisterStatus;
import com.smarterp.modules.cashier.entity.CashWithdrawal;
import com.smarterp.modules.cashier.entity.CashAudit;
import com.smarterp.modules.cashier.entity.WithdrawalStatus;
import com.smarterp.modules.cashier.entity.AuditStatus;
import com.smarterp.modules.cashier.repository.CashRegisterRepository;
import com.smarterp.modules.cashier.repository.CashTransactionRepository;
import com.smarterp.modules.cashier.repository.CashWithdrawalRepository;
import com.smarterp.modules.cashier.repository.CashAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCashService {

    private final CashRegisterRepository registerRepository;
    private final CashTransactionRepository transactionRepository;
    private final CashWithdrawalRepository withdrawalRepository;
    private final CashAuditRepository auditRepository;

    /**
     * 📊 DASHBOARD DE CAJAS
     */
    public Map<String, Object> getCashDashboard(String businessId) {
        log.info("📊 [ADMIN] Dashboard de cajas - Business: {}", businessId);

        Map<String, Object> dashboard = new HashMap<>();

        List<CashRegister> allRegisters = registerRepository.findByBusinessIdOrderByOpeningTimeDesc(businessId);

        long cajasAbiertas = allRegisters.stream()
                .filter(r -> r.getStatus() == CashRegisterStatus.ABIERTO)
                .count();

        long cajasCerradasHoy = allRegisters.stream()
                .filter(r -> r.getStatus() == CashRegisterStatus.CERRADO)
                .filter(r -> r.getClosingTime() != null &&
                        r.getClosingTime().toLocalDate().equals(LocalDateTime.now().toLocalDate()))
                .count();

        BigDecimal totalIngresos = allRegisters.stream()
                .filter(r -> r.getExpectedCash() != null)
                .map(r -> {
                    BigDecimal inicial = r.getInitialAmount() != null ? r.getInitialAmount() : BigDecimal.ZERO;
                    BigDecimal diferencia = r.getCashDifference() != null ? r.getCashDifference() : BigDecimal.ZERO;
                    return inicial.add(diferencia).subtract(inicial);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dashboard.put("cajasAbiertas", cajasAbiertas);
        dashboard.put("cajasCerradasHoy", cajasCerradasHoy);
        dashboard.put("totalRegistros", allRegisters.size());
        dashboard.put("totalIngresosHoy", totalIngresos);
        dashboard.put("totalEgresosHoy", BigDecimal.ZERO);
        dashboard.put("balanceHoy", totalIngresos);

        return dashboard;
    }

    /**
     * 📋 HISTORIAL DE CAJAS
     */
    public Map<String, Object> getCashRegistersHistory(String businessId, int page, int size) {
        log.info("📋 [ADMIN] Historial de cajas - Business: {} - Page: {}", businessId, page);

        List<CashRegister> allRegisters = registerRepository.findByBusinessIdOrderByOpeningTimeDesc(businessId);

        int start = page * size;
        int end = Math.min(start + size, allRegisters.size());

        List<CashRegister> paginatedRegisters = start < allRegisters.size()
                ? allRegisters.subList(start, end)
                : new ArrayList<>();

        List<Map<String, Object>> registersData = new ArrayList<>();

        for (CashRegister register : paginatedRegisters) {
            Map<String, Object> registerMap = new HashMap<>();
            registerMap.put("id", register.getId());
            registerMap.put("userId", register.getUserId());
            registerMap.put("userName", formatCajeroName(register.getUserName(), register.getUserId()));
            registerMap.put("status", register.getStatus());
            registerMap.put("openingTime", register.getOpeningTime());
            registerMap.put("closingTime", register.getClosingTime());
            registerMap.put("initialAmount", register.getInitialAmount());
            registerMap.put("finalCash", register.getFinalCash());
            registerMap.put("expectedCash", register.getExpectedCash());
            registerMap.put("cashDifference", register.getCashDifference());
            registerMap.put("openingNotes", register.getOpeningNotes());
            registerMap.put("closingNotes", register.getClosingNotes());

            registersData.add(registerMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", registersData);
        response.put("totalElements", allRegisters.size());
        response.put("totalPages", (int) Math.ceil((double) allRegisters.size() / size));
        response.put("currentPage", page);

        return response;
    }

    /**
     * 💵 TRANSACCIONES DE UNA CAJA
     */
    public Map<String, Object> getRegisterTransactions(String registerId) {
        log.info("💵 [ADMIN] Transacciones de caja: {}", registerId);

        List<CashTransaction> transactions = transactionRepository.findByRegisterIdOrderByCreatedAtDesc(registerId);

        List<Map<String, Object>> transactionsData = transactions.stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.getId());
                    map.put("type", t.getType());
                    map.put("amount", t.getAmount());
                    map.put("paymentMethod", t.getPaymentMethod());
                    map.put("description", t.getDescription());
                    map.put("customerName", t.getCustomerName());
                    map.put("createdBy", t.getCreatedBy());
                    map.put("createdAt", t.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("content", transactionsData);
        response.put("totalElements", transactions.size());

        return response;
    }

    /**
     * 🔍 ARQUEOS PENDIENTES - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> getPendingAudits(String businessId) {
        log.info("🔍 [ADMIN] Arqueos pendientes - Business: {}", businessId);

        List<CashAudit> allAudits = auditRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        log.info("🔍 [ADMIN] Total arqueos encontrados: {}", allAudits.size());

        // Filtrar solo los pendientes
        List<CashAudit> pendingAudits = allAudits.stream()
                .filter(a -> a.getStatus() == AuditStatus.PENDIENTE)
                .collect(Collectors.toList());

        log.info("🔍 [ADMIN] Arqueos pendientes: {}", pendingAudits.size());

        // ✅ Cache de cajas para obtener info del cajero
        Map<String, CashRegister> registerCache = new HashMap<>();

        List<Map<String, Object>> auditsData = new ArrayList<>();

        for (CashAudit audit : pendingAudits) {
            Map<String, Object> auditMap = new HashMap<>();
            auditMap.put("id", audit.getId());

            // Número de arqueo
            String auditNumber = "ARQ-" + audit.getId().substring(0, Math.min(8, audit.getId().length())).toUpperCase();
            try {
                if (audit.getAuditNumber() != null && !audit.getAuditNumber().isEmpty()) {
                    auditNumber = audit.getAuditNumber();
                }
            } catch (Exception e) {
                log.debug("No se pudo obtener auditNumber");
            }

            auditMap.put("auditNumber", auditNumber);
            auditMap.put("registerId", audit.getRegisterId());
            auditMap.put("status", audit.getStatus());
            auditMap.put("createdAt", audit.getCreatedAt());
            auditMap.put("expectedCash", audit.getExpectedCash());
            auditMap.put("countedCash", audit.getCountedCash());
            auditMap.put("difference", audit.getDifference());

            // ✅ Obtener info del cajero desde la caja asociada
            CashRegister register = getRegisterCached(audit.getRegisterId(), registerCache);
            if (register != null) {
                auditMap.put("userId", register.getUserId());
                auditMap.put("userName", formatCajeroName(register.getUserName(), register.getUserId()));
            } else {
                auditMap.put("userId", "N/A");
                auditMap.put("userName", "Cajero Desconocido");
            }

            // Detalles del conteo (solo si existen)
            try {
                auditMap.put("bills200", audit.getBills200());
            } catch (Exception e) {
                auditMap.put("bills200", 0);
            }
            try {
                auditMap.put("bills100", audit.getBills100());
            } catch (Exception e) {
                auditMap.put("bills100", 0);
            }
            try {
                auditMap.put("bills50", audit.getBills50());
            } catch (Exception e) {
                auditMap.put("bills50", 0);
            }
            try {
                auditMap.put("bills20", audit.getBills20());
            } catch (Exception e) {
                auditMap.put("bills20", 0);
            }
            try {
                auditMap.put("bills10", audit.getBills10());
            } catch (Exception e) {
                auditMap.put("bills10", 0);
            }
            try {
                auditMap.put("coins", audit.getCoins());
            } catch (Exception e) {
                auditMap.put("coins", 0);
            }
            try {
                auditMap.put("vouchers", audit.getVouchers());
            } catch (Exception e) {
                auditMap.put("vouchers", 0);
            }

            auditMap.put("notes", "");

            auditsData.add(auditMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", auditsData);
        response.put("totalElements", auditsData.size());

        log.info("✅ [ADMIN] Arqueos pendientes enviados al frontend: {}", auditsData.size());

        return response;
    }

    /**
     * 💰 RETIROS PENDIENTES - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> getPendingWithdrawals(String businessId) {
        log.info("💰 [ADMIN] Retiros pendientes - Business: {}", businessId);

        List<CashWithdrawal> allWithdrawals = withdrawalRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
        log.info("💰 [ADMIN] Total retiros encontrados: {}", allWithdrawals.size());

        // Filtrar solo los SOLICITADOS
        List<CashWithdrawal> pendingWithdrawals = allWithdrawals.stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.SOLICITADO)
                .collect(Collectors.toList());

        log.info("💰 [ADMIN] Retiros pendientes: {}", pendingWithdrawals.size());

        // ✅ Cache de cajas para obtener info del cajero
        Map<String, CashRegister> registerCache = new HashMap<>();

        List<Map<String, Object>> withdrawalsData = new ArrayList<>();

        for (CashWithdrawal withdrawal : pendingWithdrawals) {
            Map<String, Object> withdrawalMap = new HashMap<>();
            withdrawalMap.put("id", withdrawal.getId());

            // Número de retiro generado
            String withdrawalNumber = "RET-"
                    + withdrawal.getId().substring(0, Math.min(8, withdrawal.getId().length())).toUpperCase();
            withdrawalMap.put("withdrawalNumber", withdrawalNumber);

            withdrawalMap.put("registerId", withdrawal.getRegisterId());
            withdrawalMap.put("amount", withdrawal.getAmount());
            withdrawalMap.put("reason", withdrawal.getReason());
            withdrawalMap.put("status", withdrawal.getStatus());
            withdrawalMap.put("requestedAt", withdrawal.getRequestedAt());
            withdrawalMap.put("notes", "");

            // ✅ Obtener info del cajero desde la caja asociada
            CashRegister register = getRegisterCached(withdrawal.getRegisterId(), registerCache);
            if (register != null) {
                withdrawalMap.put("userId", register.getUserId());
                withdrawalMap.put("userName", formatCajeroName(register.getUserName(), register.getUserId()));
            } else {
                withdrawalMap.put("userId", "N/A");
                withdrawalMap.put("userName", "Cajero Desconocido");
            }

            withdrawalsData.add(withdrawalMap);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", withdrawalsData);
        response.put("totalElements", withdrawalsData.size());

        log.info("✅ [ADMIN] Retiros pendientes enviados al frontend: {}", withdrawalsData.size());

        return response;
    }

    /**
     * ✅ APROBAR RETIRO - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> approveWithdrawal(String withdrawalId, String adminId, String notes) {
        log.info("✅ [ADMIN] Aprobando retiro: {} - Admin: {}", withdrawalId, adminId);

        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        withdrawal.setStatus(WithdrawalStatus.APROBADO);
        withdrawal.setApprovedAt(LocalDateTime.now());

        withdrawalRepository.save(withdrawal);
        log.info("✅ Retiro aprobado correctamente: {}", withdrawalId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Retiro aprobado correctamente");

        return response;
    }

    /**
     * ❌ RECHAZAR RETIRO - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> rejectWithdrawal(String withdrawalId, String adminId, String notes) {
        log.info("❌ [ADMIN] Rechazando retiro: {} - Admin: {}", withdrawalId, adminId);

        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        withdrawal.setStatus(WithdrawalStatus.RECHAZADO);
        withdrawal.setApprovedAt(LocalDateTime.now());

        withdrawalRepository.save(withdrawal);
        log.info("❌ Retiro rechazado: {}", withdrawalId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Retiro rechazado");

        return response;
    }

    /**
     * ✅ APROBAR ARQUEO - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> approveAudit(String auditId, String adminId, String notes) {
        log.info("✅ [ADMIN] Aprobando arqueo: {} - Admin: {}", auditId, adminId);

        CashAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        audit.setStatus(AuditStatus.CONCORDANTE);
        audit.setCompletedAt(LocalDateTime.now());

        auditRepository.save(audit);
        log.info("✅ Arqueo aprobado correctamente: {}", auditId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Arqueo aprobado correctamente");

        return response;
    }

    /**
     * ❌ RECHAZAR ARQUEO - ✅ CONEXIÓN REAL A BD
     */
    public Map<String, Object> rejectAudit(String auditId, String adminId, String notes) {
        log.info("❌ [ADMIN] Rechazando arqueo: {} - Admin: {}", auditId, adminId);

        CashAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        audit.setStatus(AuditStatus.DISCORDANTE);
        audit.setCompletedAt(LocalDateTime.now());

        auditRepository.save(audit);
        log.info("❌ Arqueo rechazado: {}", auditId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Arqueo rechazado");

        return response;
    }

    /**
     * 🔍 Helper: Obtener caja con cache para evitar múltiples consultas
     */
    private CashRegister getRegisterCached(String registerId, Map<String, CashRegister> cache) {
        if (registerId == null)
            return null;

        if (cache.containsKey(registerId)) {
            return cache.get(registerId);
        }

        try {
            CashRegister register = registerRepository.findById(registerId).orElse(null);
            cache.put(registerId, register);
            return register;
        } catch (Exception e) {
            log.warn("⚠️ No se pudo obtener caja {}: {}", registerId, e.getMessage());
            return null;
        }
    }

    /**
     * ✅ FORMATEAR NOMBRE DEL CAJERO
     */
    private String formatCajeroName(String userName, String userId) {
        String name = (userName != null && !userName.isEmpty()) ? userName : userId;

        if (name == null || name.isEmpty()) {
            return "Cajero Desconocido";
        }

        if (name.contains("@")) {
            String emailPart = name.split("@")[0];
            if (emailPart.startsWith("user-")) {
                String number = emailPart.replace("user-", "");
                return "Cajero " + number;
            }
            return emailPart;
        }

        if (name.equals("temp-user-id") || name.equals("anonymous")) {
            return "Cajero Desconocido";
        }

        if (name.equals("user@example.com")) {
            return "Usuario Demo";
        }

        return name;
    }
}
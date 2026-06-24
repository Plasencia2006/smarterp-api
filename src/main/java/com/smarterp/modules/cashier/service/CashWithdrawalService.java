package com.smarterp.modules.cashier.service;

import com.smarterp.modules.cashier.dto.*;
import com.smarterp.modules.cashier.entity.*;
import com.smarterp.modules.cashier.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashWithdrawalService {

    private final CashWithdrawalRepository withdrawalRepository;
    private final CashRegisterRepository registerRepository;
    private final CashTransactionRepository transactionRepository;
    private final CashAuditRepository auditRepository;

    //  LÍMITE DE EFECTIVO EN CAJA (configurable)
    private static final BigDecimal MAX_CASH_LIMIT = new BigDecimal("10000.00");

    /**
     *  SOLICITAR RETIRO DE EFECTIVO
     */
    @Transactional
    public CashWithdrawalResponse requestWithdrawal(String businessId, String registerId,
            String cashierId, String cashierName,
            CashWithdrawalRequest request) {
        log.info(" Solicitando retiro - Turno: {} - Monto: S/ {}", registerId, request.getAmount());

        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para este turno");
        }

        if (register.getStatus() != CashRegisterStatus.ABIERTO) {
            throw new RuntimeException("El turno debe estar abierto");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto debe ser mayor a cero");
        }

        if (request.getReason() == null || request.getReason().isBlank()) {
            throw new RuntimeException("Debe indicar el motivo del retiro");
        }

        BigDecimal currentCash = calculateCurrentCash(register);
        if (request.getAmount().compareTo(currentCash) > 0) {
            throw new RuntimeException("No hay suficiente efectivo en caja. " +
                    "Disponible: S/ " + currentCash + ", Solicitado: S/ " + request.getAmount());
        }

        CashWithdrawal withdrawal = CashWithdrawal.builder()
                .businessId(businessId)
                .registerId(registerId)
                .cashierId(cashierId)
                .cashierName(cashierName)
                .status(WithdrawalStatus.SOLICITADO)
                .amount(request.getAmount())
                .reason(request.getReason())
                .destination(request.getDestination() != null ? request.getDestination() : "Caja Fuerte")
                .requestedAt(LocalDateTime.now())
                .build();

        CashWithdrawal saved = withdrawalRepository.save(withdrawal);

        log.info("✅ Retiro {} solicitado por {}", saved.getReferenceNumber(), cashierName);

        return mapToResponse(saved);
    }

    /**
     * ✅ APROBAR RETIRO (por supervisor)
     */
    @Transactional
    public CashWithdrawalResponse approveWithdrawal(String withdrawalId, String businessId,
            String supervisorId, String supervisorName,
            String approvalNotes) {
        log.info("✅ Aprobando retiro: {} por {}", withdrawalId, supervisorName);

        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        if (!withdrawal.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (withdrawal.getStatus() != WithdrawalStatus.SOLICITADO) {
            throw new RuntimeException("El retiro ya fue procesado");
        }

        if (withdrawal.getCashierId().equals(supervisorId)) {
            throw new RuntimeException("Un cajero no puede aprobar su propio retiro");
        }

        withdrawal.setStatus(WithdrawalStatus.APROBADO);
        withdrawal.setSupervisorId(supervisorId);
        withdrawal.setSupervisorName(supervisorName);
        withdrawal.setApprovalNotes(approvalNotes);
        withdrawal.setApprovedAt(LocalDateTime.now());

        CashWithdrawal saved = withdrawalRepository.save(withdrawal);

        log.info(" Retiro {} aprobado por {}", withdrawal.getReferenceNumber(), supervisorName);

        return mapToResponse(saved);
    }

    /**
     *  RECHAZAR RETIRO
     */
    @Transactional
    public CashWithdrawalResponse rejectWithdrawal(String withdrawalId, String businessId,
            String supervisorId, String supervisorName,
            String rejectionNotes) {
        log.info(" Rechazando retiro: {}", withdrawalId);

        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        if (!withdrawal.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (withdrawal.getStatus() != WithdrawalStatus.SOLICITADO) {
            throw new RuntimeException("El retiro ya fue procesado");
        }

        withdrawal.setStatus(WithdrawalStatus.RECHAZADO);
        withdrawal.setSupervisorId(supervisorId);
        withdrawal.setSupervisorName(supervisorName);
        withdrawal.setApprovalNotes("RECHAZADO: " + (rejectionNotes != null ? rejectionNotes : ""));
        withdrawal.setApprovedAt(LocalDateTime.now());

        CashWithdrawal saved = withdrawalRepository.save(withdrawal);
        log.info(" Retiro {} rechazado", withdrawal.getReferenceNumber());

        return mapToResponse(saved);
    }

    /**
     *  COMPLETAR RETIRO (el dinero se entrega a caja fuerte)
     */
    @Transactional
    public CashWithdrawalResponse completeWithdrawal(String withdrawalId, String businessId,
            String userId, String userName) {
        log.info(" Completando retiro: {}", withdrawalId);

        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        if (!withdrawal.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (withdrawal.getStatus() != WithdrawalStatus.APROBADO) {
            throw new RuntimeException("El retiro debe estar aprobado antes de completarse");
        }

        CashTransaction transaction = CashTransaction.builder()
                .businessId(businessId)
                .registerId(withdrawal.getRegisterId())
                .type(TransactionType.EGRESO)
                .amount(withdrawal.getAmount())
                .description("RETIRO " + withdrawal.getReferenceNumber() + " - " + withdrawal.getReason() +
                        " → " + withdrawal.getDestination())
                .createdBy(userName)
                .build();

        transactionRepository.save(transaction);

        withdrawal.setStatus(WithdrawalStatus.COMPLETADO);
        withdrawal.setCompletedAt(LocalDateTime.now());

        CashWithdrawal saved = withdrawalRepository.save(withdrawal);

        log.info("📦 Retiro {} completado - S/ {} enviados a {}",
                withdrawal.getReferenceNumber(), withdrawal.getAmount(), withdrawal.getDestination());

        return mapToResponse(saved);
    }

    /**
     * 📋 LISTAR RETIROS DE UN TURNO
     */
    public List<CashWithdrawalResponse> getWithdrawalsByRegister(String registerId, String businessId) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        return withdrawalRepository.findByRegisterIdOrderByCreatedAtDesc(registerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 🔍 OBTENER DETALLE DE UN RETIRO
     */
    public CashWithdrawalResponse getWithdrawalDetail(String withdrawalId, String businessId) {
        CashWithdrawal withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new RuntimeException("Retiro no encontrado"));

        if (!withdrawal.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        return mapToResponse(withdrawal);
    }

    /**
     * 📊 RESUMEN DE FLUJO DE EFECTIVO
     */
    public CashFlowSummaryResponse getCashFlowSummary(String registerId, String businessId) {
        log.info("📊 Generando resumen de flujo de efectivo para turno {}", registerId);

        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        List<CashTransaction> transactions = transactionRepository.findByRegisterIdOrderByCreatedAtDesc(registerId);

        BigDecimal totalIngresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawals = withdrawalRepository
                .sumApprovedWithdrawalsByRegisterId(registerId);

        BigDecimal currentCash = register.getInitialAmount() // ✅ CAMBIADO
                .add(totalIngresos)
                .subtract(totalEgresos);

        // Estadísticas de arqueos
        List<CashAudit> audits = auditRepository.findByRegisterIdOrderByCreatedAtDesc(registerId);
        Long totalAudits = (long) audits.size();
        Long concordantAudits = audits.stream()
                .filter(a -> a.getStatus() == AuditStatus.CONCORDANTE)
                .count();
        Long discrepantAudits = audits.stream()
                .filter(a -> a.getStatus() == AuditStatus.DISCORDANTE)
                .count();
        BigDecimal totalDiscrepancy = auditRepository.sumAbsoluteDifferencesByRegisterId(registerId);

        // Estadísticas de retiros
        List<CashWithdrawal> withdrawals = withdrawalRepository.findByRegisterIdOrderByCreatedAtDesc(registerId);
        Long totalWithdrawalsCount = (long) withdrawals.size();
        Long pendingWithdrawals = withdrawals.stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.SOLICITADO)
                .count();

        // ✅ ALERTAS
        List<String> alerts = new ArrayList<>();

        if (currentCash.compareTo(MAX_CASH_LIMIT) > 0) {
            alerts.add("⚠️ Exceso de efectivo en caja: S/ " + currentCash +
                    " (Límite: S/ " + MAX_CASH_LIMIT + "). Considere realizar un retiro.");
        }

        if (pendingWithdrawals > 0) {
            alerts.add("📋 Hay " + pendingWithdrawals + " retiro(s) pendiente(s) de aprobación.");
        }

        if (discrepantAudits > 0) {
            alerts.add("⚠️ Se detectaron " + discrepantAudits + " discrepancia(s) en arqueos.");
        }

        // Últimos movimientos
        List<CashFlowSummaryResponse.RecentMovement> recentMovements = new ArrayList<>();

        transactions.stream().limit(5).forEach(t -> {
            recentMovements.add(CashFlowSummaryResponse.RecentMovement.builder()
                    .type(t.getType().name())
                    .description(t.getDescription())
                    .amount(t.getAmount())
                    .dateTime(t.getCreatedAt().toString())
                    .status("COMPLETADO")
                    .build());
        });

        withdrawals.stream().limit(3).forEach(w -> {
            recentMovements.add(CashFlowSummaryResponse.RecentMovement.builder()
                    .type("RETIRO")
                    .description(w.getReason() + " → " + w.getDestination())
                    .amount(w.getAmount())
                    .dateTime(w.getRequestedAt().toString())
                    .status(w.getStatus().name())
                    .build());
        });

        return CashFlowSummaryResponse.builder()
                .registerId(registerId)
                .registerStatus(register.getStatus().name())
                .initialCash(register.getInitialAmount()) // ✅ CAMBIADO
                .totalIngresos(totalIngresos)
                .totalEgresos(totalEgresos)
                .totalWithdrawals(totalWithdrawals)
                .currentCash(currentCash)
                .totalAudits(totalAudits)
                .concordantAudits(concordantAudits)
                .discrepantAudits(discrepantAudits)
                .totalDiscrepancy(totalDiscrepancy)
                .totalWithdrawalsCount(totalWithdrawalsCount)
                .totalWithdrawnAmount(totalWithdrawals)
                .pendingWithdrawals(pendingWithdrawals)
                .alerts(alerts)
                .recentMovements(recentMovements)
                .build();
    }

    /**
     * 🔍 VERIFICAR SI SE SUPERA EL LÍMITE
     */
    public boolean exceedsCashLimit(String registerId, String businessId) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        BigDecimal currentCash = calculateCurrentCash(register);
        return currentCash.compareTo(MAX_CASH_LIMIT) > 0;
    }

    /**
     * 🧮 Calcular efectivo actual en caja
     */
    private BigDecimal calculateCurrentCash(CashRegister register) {
        List<CashTransaction> transactions = transactionRepository
                .findByRegisterIdOrderByCreatedAtDesc(register.getId());

        BigDecimal totalIngresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalEgresos = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EGRESO)
                .map(CashTransaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return register.getInitialAmount().add(totalIngresos).subtract(totalEgresos); // ✅ CAMBIADO
    }

    /**
     * 🔧 Mapear entidad a respuesta
     */
    private CashWithdrawalResponse mapToResponse(CashWithdrawal withdrawal) {
        return CashWithdrawalResponse.builder()
                .id(withdrawal.getId())
                .referenceNumber(withdrawal.getReferenceNumber())
                .registerId(withdrawal.getRegisterId())
                .cashierName(withdrawal.getCashierName())
                .supervisorName(withdrawal.getSupervisorName())
                .status(withdrawal.getStatus().name())
                .amount(withdrawal.getAmount())
                .reason(withdrawal.getReason())
                .destination(withdrawal.getDestination())
                .approvalNotes(withdrawal.getApprovalNotes())
                .requestedAt(withdrawal.getRequestedAt())
                .approvedAt(withdrawal.getApprovedAt())
                .completedAt(withdrawal.getCompletedAt())
                .build();
    }
}
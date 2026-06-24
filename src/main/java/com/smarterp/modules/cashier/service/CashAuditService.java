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
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashAuditService {

    private final CashAuditRepository auditRepository;
    private final CashRegisterRepository registerRepository;
    private final CashTransactionRepository transactionRepository;

    /**
     * 🔍 INICIAR ARQUEO PARCIAL (a ciegas)
     * El supervisor NO ve el monto esperado hasta completar el conteo
     */
    @Transactional
    public CashAuditResponse startAudit(String businessId, String registerId,
            String supervisorId, String supervisorName,
            String auditType) {
        log.info("🔍 Iniciando arqueo {} en turno {}", auditType, registerId);

        // Validar que el turno esté abierto
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para este turno");
        }

        if (register.getStatus() != CashRegisterStatus.ABIERTO) {
            throw new RuntimeException("El turno debe estar abierto para realizar un arqueo");
        }

        // Verificar que no haya otro arqueo pendiente
        auditRepository.findTopByRegisterIdAndStatusOrderByStartedAtDesc(
                registerId, AuditStatus.PENDIENTE)
                .ifPresent(audit -> {
                    throw new RuntimeException("Ya hay un arqueo pendiente. Complete el arqueo " +
                            audit.getAuditNumber() + " antes de iniciar uno nuevo.");
                });

        // ✅ Calcular efectivo esperado (SIN mostrarlo al supervisor aún)
        BigDecimal expectedCash = calculateExpectedCash(register);

        CashAudit audit = CashAudit.builder()
                .businessId(businessId)
                .registerId(registerId)
                .cashierId(register.getUserId())
                .cashierName(register.getUserName())
                .supervisorId(supervisorId)
                .supervisorName(supervisorName)
                .status(AuditStatus.PENDIENTE)
                .expectedCash(expectedCash) // Se guarda pero NO se muestra aún
                .auditType(auditType != null ? auditType : "PARCIAL")
                .startedAt(LocalDateTime.now())
                .build();

        CashAudit saved = auditRepository.save(audit);

        log.info("✅ Arqueo {} iniciado. Esperado: S/ {} (oculto al supervisor)",
                saved.getAuditNumber(), expectedCash);

        return mapToResponse(saved, true); // hideExpected = true
    }

    /**
     * ✅ COMPLETAR ARQUEO (el supervisor ingresa el conteo)
     */
    @Transactional
    public CashAuditResponse completeAudit(String auditId, String businessId,
            String supervisorId, CashAuditRequest request) {
        log.info("✅ Completando arqueo: {}", auditId);

        CashAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        if (!audit.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos para este arqueo");
        }

        if (audit.getStatus() != AuditStatus.PENDIENTE) {
            throw new RuntimeException("El arqueo ya fue completado o cancelado");
        }

        // Guardar detalles del conteo
        audit.setBills200(request.getBills200() != null ? request.getBills200() : BigDecimal.ZERO);
        audit.setBills100(request.getBills100() != null ? request.getBills100() : BigDecimal.ZERO);
        audit.setBills50(request.getBills50() != null ? request.getBills50() : BigDecimal.ZERO);
        audit.setBills20(request.getBills20() != null ? request.getBills20() : BigDecimal.ZERO);
        audit.setBills10(request.getBills10() != null ? request.getBills10() : BigDecimal.ZERO);
        audit.setCoins(request.getCoins() != null ? request.getCoins() : BigDecimal.ZERO);
        audit.setVouchers(request.getVouchers() != null ? request.getVouchers() : BigDecimal.ZERO);

        // Calcular total contado
        BigDecimal countedCash;
        if (request.getCountedCash() != null && request.getCountedCash().compareTo(BigDecimal.ZERO) > 0) {
            countedCash = request.getCountedCash();
        } else {
            countedCash = audit.calculateCountedFromDetails();
        }
        audit.setCountedCash(countedCash);

        // Calcular diferencia y determinar estado
        audit.calculateDifference();

        if (audit.getDifference().compareTo(BigDecimal.ZERO) == 0) {
            audit.setStatus(AuditStatus.CONCORDANTE);
        } else {
            audit.setStatus(AuditStatus.DISCORDANTE);
        }

        audit.setCompletedAt(LocalDateTime.now());
        audit.setNotes(request.getNotes());

        CashAudit saved = auditRepository.save(audit);

        log.info(" Arqueo completado: {} - Estado: {} - Diferencia: S/ {}",
                audit.getAuditNumber(), audit.getStatus(), audit.getDifference());

        if (audit.getStatus() == AuditStatus.DISCORDANTE) {
            log.warn(" DISCORDANCIA en arqueo {}: Esperado S/ {}, Contado S/ {}, Diferencia S/ {}",
                    audit.getAuditNumber(), audit.getExpectedCash(),
                    audit.getCountedCash(), audit.getDifference());
        }

        return mapToResponse(saved, false); // hideExpected = false (ya se puede ver)
    }

    /**
     * ❌ CANCELAR ARQUEO
     */
    @Transactional
    public CashAuditResponse cancelAudit(String auditId, String businessId, String reason) {
        CashAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        if (!audit.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        if (audit.getStatus() != AuditStatus.PENDIENTE) {
            throw new RuntimeException("Solo se pueden cancelar arqueos pendientes");
        }

        audit.setStatus(AuditStatus.CANCELADO);
        audit.setNotes("CANCELADO: " + (reason != null ? reason : "Sin motivo"));
        audit.setCompletedAt(LocalDateTime.now());

        CashAudit saved = auditRepository.save(audit);
        log.info("❌ Arqueo {} cancelado", audit.getAuditNumber());

        return mapToResponse(saved, false);
    }

    /**
     * 📋 LISTAR ARQUEOS DE UN TURNO
     */
    public List<CashAuditResponse> getAuditsByRegister(String registerId, String businessId) {
        CashRegister register = registerRepository.findById(registerId)
                .orElseThrow(() -> new RuntimeException("Turno no encontrado"));

        if (!register.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        return auditRepository.findByRegisterIdOrderByCreatedAtDesc(registerId)
                .stream()
                .map(a -> mapToResponse(a, false))
                .collect(Collectors.toList());
    }

    /**
     * 🔍 OBTENER DETALLE DE UN ARQUEO
     */
    public CashAuditResponse getAuditDetail(String auditId, String businessId) {
        CashAudit audit = auditRepository.findById(auditId)
                .orElseThrow(() -> new RuntimeException("Arqueo no encontrado"));

        if (!audit.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        // Si está pendiente, ocultar el monto esperado
        boolean hideExpected = audit.getStatus() == AuditStatus.PENDIENTE;
        return mapToResponse(audit, hideExpected);
    }

    /**
     * 🧮 Calcular efectivo esperado del turno
     */
    private BigDecimal calculateExpectedCash(CashRegister register) {
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
    private CashAuditResponse mapToResponse(CashAudit audit, boolean hideExpected) {
        BigDecimal expectedCash = hideExpected ? null : audit.getExpectedCash();
        Boolean isConcordant = audit.getStatus() == AuditStatus.CONCORDANTE;

        return CashAuditResponse.builder()
                .id(audit.getId())
                .auditNumber(audit.getAuditNumber())
                .registerId(audit.getRegisterId())
                .cashierName(audit.getCashierName())
                .supervisorName(audit.getSupervisorName())
                .status(audit.getStatus().name())
                .auditType(audit.getAuditType())
                .expectedCash(expectedCash)
                .countedCash(audit.getCountedCash())
                .difference(audit.getDifference())
                .isConcordant(isConcordant)
                .bills200(audit.getBills200())
                .bills100(audit.getBills100())
                .bills50(audit.getBills50())
                .bills20(audit.getBills20())
                .bills10(audit.getBills10())
                .coins(audit.getCoins())
                .vouchers(audit.getVouchers())
                .startedAt(audit.getStartedAt())
                .completedAt(audit.getCompletedAt())
                .notes(audit.getNotes())
                .build();
    }
}
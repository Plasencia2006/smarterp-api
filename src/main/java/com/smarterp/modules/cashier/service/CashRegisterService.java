package com.smarterp.modules.cashier.service;

import com.smarterp.common.exceptions.BusinessException;
import com.smarterp.common.exceptions.ResourceNotFoundException;
import com.smarterp.modules.cashier.dto.CashRegisterCloseRequest;
import com.smarterp.modules.cashier.dto.CashRegisterOpenRequest;
import com.smarterp.modules.cashier.dto.CashRegisterSummaryResponse;
import com.smarterp.modules.cashier.entity.CashRegister;
import com.smarterp.modules.cashier.entity.CashRegisterStatus;
import com.smarterp.modules.cashier.entity.Sale;
import com.smarterp.modules.cashier.repository.CashRegisterRepository;
import com.smarterp.modules.cashier.repository.SaleRepository;
import com.smarterp.shared.entity.Business;
import com.smarterp.shared.entity.User;
import com.smarterp.shared.repository.BusinessRepository;
import com.smarterp.shared.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashRegisterService {

        private final CashRegisterRepository cashRegisterRepository;
        private final SaleRepository saleRepository;
        private final BusinessRepository businessRepository;
        private final UserRepository userRepository;

        /**
         * Abre una nueva caja para el negocio
         * 
         * @param businessId ID del negocio (del token JWT)
         * @param userId     ID del usuario (del token JWT)
         * @param request    Datos de apertura
         */
        @Transactional
        public CashRegister openCashRegister(String businessId, String userId, CashRegisterOpenRequest request) {
                // Verificar que el negocio existe
                Business business = businessRepository.findById(businessId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Negocio no encontrado con ID: " + businessId));

                // Verificar que el usuario existe
                User cashier = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con ID: " + userId));

                // Verificar que no haya una caja abierta para este negocio
                Optional<CashRegister> existingOpen = cashRegisterRepository
                                .findByBusinessIdAndStatus(businessId, CashRegisterStatus.OPEN);

                if (existingOpen.isPresent()) {
                        throw new BusinessException("Ya existe una caja abierta para este negocio");
                }

                CashRegister cashRegister = CashRegister.builder()
                                .business(business)
                                .openedBy(cashier)
                                .initialAmount(request.getInitialAmount())
                                .status(CashRegisterStatus.OPEN)
                                .build();

                log.info("Caja abierta para negocio: {} por usuario: {}", businessId, userId);
                return cashRegisterRepository.save(cashRegister);
        }

        /**
         * Obtiene el resumen de una caja específica
         * 
         * @param id         ID de la caja
         * @param businessId ID del negocio (para validación multi-tenant)
         */
        public CashRegisterSummaryResponse getSummary(String id, String businessId) {
                CashRegister cr = cashRegisterRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Caja no encontrada con ID: " + id));

                // Validar que la caja pertenezca al negocio del usuario
                if (!cr.getBusiness().getId().equals(businessId)) {
                        throw new BusinessException("No tiene permisos para ver esta caja");
                }

                // Calcular totales de ventas
                List<Sale> sales = saleRepository.findByCashRegisterId(cr.getId());

                BigDecimal totalSales = sales.stream()
                                .map(Sale::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return CashRegisterSummaryResponse.builder()
                                .id(cr.getId())
                                .cashierName(cr.getOpenedBy().getFirstName() + " " + cr.getOpenedBy().getLastName())
                                .initialAmount(cr.getInitialAmount())
                                .finalAmount(cr.getFinalAmount())
                                .totalSales(totalSales)
                                .totalReturns(BigDecimal.ZERO)
                                .salesCount(sales.size())
                                .status(cr.getStatus().name())
                                .openedAt(cr.getOpenedAt())
                                .closedAt(cr.getClosedAt())
                                .build();
        }

        /**
         * Cierra la caja con el monto final del arqueo
         * 
         * @param cashRegisterId ID de la caja
         * @param businessId     ID del negocio (para validación multi-tenant)
         * @param userId         ID del usuario que cierra
         * @param request        Datos de cierre
         */
        @Transactional
        public CashRegister closeCashRegister(String cashRegisterId, String businessId, String userId,
                        CashRegisterCloseRequest request) {
                CashRegister cr = cashRegisterRepository.findById(cashRegisterId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Caja no encontrada con ID: " + cashRegisterId));

                // Validar que la caja pertenezca al negocio
                if (!cr.getBusiness().getId().equals(businessId)) {
                        throw new BusinessException("No tiene permisos para cerrar esta caja");
                }

                if (cr.getStatus() == CashRegisterStatus.CLOSED) {
                        throw new BusinessException("La caja ya está cerrada");
                }

                User cashier = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Usuario no encontrado con ID: " + userId));

                cr.setClosedBy(cashier);
                cr.setFinalAmount(request.getFinalAmount());
                cr.setStatus(CashRegisterStatus.CLOSED);
                cr.setClosedAt(LocalDateTime.now());

                log.info("Caja cerrada: {} por usuario: {}", cashRegisterId, userId);
                return cashRegisterRepository.save(cr);
        }

        /**
         * Obtiene la caja abierta actual para el negocio
         * 
         * @param businessId ID del negocio (del token JWT)
         */
        public CashRegister getOpenCashRegister(String businessId) {
                return cashRegisterRepository.findByBusinessIdAndStatus(businessId, CashRegisterStatus.OPEN)
                                .orElseThrow(() -> new BusinessException(
                                                "No hay caja abierta para este negocio. Debe abrir caja primero"));
        }

        /**
         * Obtiene todas las ventas de una caja específica
         */
        public List<Sale> getSalesByCashRegister(String cashRegisterId) {
                return saleRepository.findByCashRegisterId(cashRegisterId);
        }

        /**
         * Verifica si existe una caja abierta para un negocio
         */
        public boolean isOpenCashRegisterExists(String businessId) {
                return cashRegisterRepository.findByBusinessIdAndStatus(businessId, CashRegisterStatus.OPEN)
                                .isPresent();
        }
}
package com.smarterp.modules.sales.service;

import com.smarterp.modules.sales.entity.Customer;
import com.smarterp.modules.sales.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(String businessId, Customer customer) {
        customer.setBusinessId(businessId);
        Customer saved = customerRepository.save(customer);
        log.info("✅ Cliente creado: {} ({})", saved.getName(), saved.getDocumentNumber());
        return saved;
    }

    public Customer updateCustomer(String customerId, String businessId, Customer customer) {
        Customer existing = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!existing.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        existing.setName(customer.getName());
        existing.setDocumentType(customer.getDocumentType());
        existing.setDocumentNumber(customer.getDocumentNumber());
        existing.setEmail(customer.getEmail());
        existing.setPhone(customer.getPhone());
        existing.setAddress(customer.getAddress());
        existing.setNotes(customer.getNotes());
        existing.setIsFrequent(customer.getIsFrequent());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(String customerId, String businessId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (!customer.getBusinessId().equals(businessId)) {
            throw new RuntimeException("No tiene permisos");
        }

        customerRepository.delete(customer);
        log.info("🗑️ Cliente eliminado: {}", customer.getName());
    }

    public List<Customer> getAllCustomers(String businessId) {
        return customerRepository.findByBusinessIdOrderByCreatedAtDesc(businessId);
    }

    public List<Customer> searchCustomers(String businessId, String search) {
        return customerRepository.searchCustomers(businessId, search);
    }

    public List<Customer> getFrequentCustomers(String businessId) {
        return customerRepository.findByBusinessIdAndIsFrequentTrue(businessId);
    }

    public Customer getByDocument(String businessId, String documentNumber) {
        return customerRepository.findByDocumentNumberAndBusinessId(documentNumber, businessId)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }
}
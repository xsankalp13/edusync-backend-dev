package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.PaymentGateway;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link PaymentGateway} entity.
 */
@Repository
public interface PaymentGatewayRepository extends JpaRepository<PaymentGateway, Long> {

    /**
     * Finds a payment gateway by its unique provider name.
     *
     * @param providerName The name (e.g., "Stripe", "PayPal").
     * @return An Optional containing the found gateway.
     */
    Optional<PaymentGateway> findByProviderName(String providerName);

    /**
     * Finds all active (or inactive) payment gateways.
     *
     * @param isActive The active status.
     * @return A list of matching gateways.
     */
    List<PaymentGateway> findByIsActive(boolean isActive);
}
package com.project.edusync.finance.model.entity;


import com.project.edusync.finance.model.enums.GatewayEnvironment;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_gateways")
public class PaymentGateway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gateway_id")
    private Integer gatewayId;

    @Column(name = "provider_name", nullable = false, unique = true, length = 50)
    private String providerName;

    @Column(name = "api_key", length = 255)
    private String apiKey;

    @Column(name = "api_secret", length = 255)
    private String apiSecret;

    @Column(name = "is_active", nullable = false)
    @ColumnDefault("true")
    private boolean isActive;

    /**
     * --- FIXED ---
     * Removed 'columnDefinition' from the @Column annotation.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false)
    @ColumnDefault("'SANDBOX'")
    private GatewayEnvironment environment;
}

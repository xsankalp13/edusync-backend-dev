package com.project.edusync.uis.model.entity;

import com.project.edusync.common.converter.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "staff_sensitive_info")
@Getter
@Setter
@NoArgsConstructor
public class StaffSensitiveInfo {

    @Id
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "id")
    private Staff staff;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "aadhaar_number", length = 64)
    private String aadhaarNumber;

    @Column(name = "pan_number", length = 20)
    private String panNumber;

    @Column(name = "passport_number", length = 20)
    private String passportNumber;

    @Column(name = "apaar_id", length = 30)
    private String apaarId;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "bank_account_number", length = 64)
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code", length = 20)
    private String bankIfscCode;

    @Column(name = "emergency_contact_name", length = 100)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;
}


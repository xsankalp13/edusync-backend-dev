package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_address_contact")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AddressContactDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    @Column(nullable = false)
    private String residentialAddress;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String pinCode;

    private String permanentAddress;

    @Column(nullable = false)
    private String primaryMobile;

    private String alternateMobile;

    @Column(nullable = false)
    private String emailId;
}

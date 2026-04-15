package com.project.edusync.admission.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admission_transport_details")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransportDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private AdmissionApplication application;

    private String pickupLocation;
    private String routeOrStop;
    private String distanceFromSchool;
}

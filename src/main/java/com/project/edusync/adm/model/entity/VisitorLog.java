package com.project.edusync.adm.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "visitor_logs")
@Getter
@Setter
@NoArgsConstructor
public class VisitorLog extends AuditableEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 20)
    private String gender;

    /**
     * Stored masked by the service before saving to DB, per requirements.
     */
    @Column(name = "aadhaar_no", length = 20)
    private String aadhaarNo;

    @Column(name = "phone_no", nullable = false, length = 20)
    private String phoneNo;

    @Column(nullable = false, length = 255)
    private String purpose;

    @Column(name = "whom_to_meet", nullable = false, length = 150)
    private String whomToMeet;

}

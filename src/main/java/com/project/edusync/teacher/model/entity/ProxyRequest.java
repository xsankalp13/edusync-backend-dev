package com.project.edusync.teacher.model.entity;

import com.project.edusync.iam.model.entity.User;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class ProxyRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "requested_by", nullable = false)
    private User requestedBy;

    @ManyToOne
    @JoinColumn(name = "requested_to", nullable = false)
    private User requestedTo;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private Boolean isAccepted;
}
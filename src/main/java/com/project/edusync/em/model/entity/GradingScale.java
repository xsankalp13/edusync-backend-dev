package com.project.edusync.em.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grading_scales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GradingScale {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer minMarks;

    @Column(nullable = false)
    private Integer maxMarks;

    @Column(nullable = false, length = 10)
    private String grade;
}


package com.project.edusync.teacher.model.entity;

import com.project.edusync.adm.model.entity.Schedule;
import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.iam.model.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import java.util.List;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lecture_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LectureLog extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "lecture_log_documents", joinColumns = @JoinColumn(name = "lecture_log_id"))
    @Column(name = "document_url", length = 1024)
    @Builder.Default
    private List<String> documentUrls = new ArrayList<>();

    @Column(name = "has_taken_test", nullable = false)
    @Builder.Default
    private boolean hasTakenTest = false;
}

package com.project.edusync.adm.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a time block in the schedule.
 *
 * This entity extends AuditableEntity to gain ID (Long), UUID,
 * and audit timestamp fields.
 *
 * Relationships will be joined using the inherited 'id' (Long) primary key.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true) // Includes inherited 'id' and 'uuid'
@ToString(callSuper = true, exclude = {"constraints", "schedules"}) // Exclude lazy relationships
@Entity
@Table(name = "timeslots")
public class Timeslot extends AuditableEntity {

    // The @Id (Long id) and 'uuid' are inherited from AuditableEntity.

    @Column(name = "day_of_week", nullable = false)
    private Short dayOfWeek; // Using Short to match SMALLINT

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_label", length = 100)
    private String slotLabel;

    @Column(name = "is_break")
    private Boolean isBreak = false;

    @Column(name = "is_non_teaching")
    private Boolean isNonTeachingSlot = false;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // --- Relationships ---

    /**
     * All constraints that are associated with this timeslot.
     */
    @OneToMany(mappedBy = "timeslot", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<AcademicConstraint> academicConstraints = new HashSet<>();

    /**
     * All schedule entries assigned to this timeslot.
     */
    @OneToMany(mappedBy = "timeslot", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Schedule> schedules = new HashSet<>();

}

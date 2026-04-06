package com.project.edusync.adm.model.entity;


import com.project.edusync.common.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a physical room where classes are held.
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
@EqualsAndHashCode(callSuper = true, exclude = {"schedules", "building"})
@ToString(callSuper = true, exclude = {"schedules", "building"})
@Entity
@Table(name = "rooms")
public class Room extends AuditableEntity {
    /**
     * Number of benches (exam seat units): rowCount × columnsPerRow
     */
    @Column(name = "exam_seat_units")
    private Integer examSeatUnits;

    // The @Id (Long id) and 'uuid' are inherited from AuditableEntity.

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "room_type", length = 100)
    private String roomType;


    @Column(name = "capacity", nullable = false)
    private Integer capacity;
    // Kept nullable at DB level for backward compatibility with existing rooms.
    @Column(name = "row_count")
    private Integer rowCount;

    @Column(name = "columns_per_row")
    private Integer columnsPerRow;

    @Column(name = "seating_type", length = 50)
    private String seatingType;

    @Column(name = "seats_per_unit")
    private Integer seatsPerUnit;

    @Column(name = "total_capacity")
    private Integer totalCapacity;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(name = "has_projector")
    private Boolean hasProjector = false;

    @Column(name = "has_ac")
    private Boolean hasAC = false;

    @Column(name = "has_whiteboard")
    private Boolean hasWhiteboard = true;

    @Column(name = "is_accessible")
    private Boolean isAccessible = false;

    @Column(name = "other_amenities", length = 500)
    private String otherAmenities;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Only @PreUpdate here. Do NOT add @PrePersist to avoid duplicate entity listener error.
    @PreUpdate
    public void calculateCapacity() {
        if (rowCount != null && columnsPerRow != null) {
            int seatUnits = rowCount * columnsPerRow;
            this.examSeatUnits = seatUnits;
            if (seatsPerUnit != null) {
                int calculated = seatUnits * seatsPerUnit;
                this.totalCapacity = calculated;
                this.capacity = calculated;
            }
        }
    }
    // If you want to ensure capacity is set on insert, call calculateCapacity() from the superclass's @PrePersist method.

    // --- Relationships ---

    /**
     * All schedule entries that are assigned to this room.
     */
    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<Schedule> schedules = new HashSet<>();

}

package com.project.edusync.em.model.entity;

import com.project.edusync.adm.model.entity.Room;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a physical seat within a room.
 * Seats are pre-generated based on room dimensions (rowCount × columnsPerRow).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_seat_room_row_col",
            columnNames = {"room_id", "row_number", "column_number"})
    },
    indexes = {
        @Index(name = "idx_seat_room", columnList = "room_id")
    })
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "column_number", nullable = false)
    private Integer columnNumber;

    @Column(name = "label", nullable = false, length = 20)
    private String label;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.label == null) {
            this.label = "R" + rowNumber + "-C" + columnNumber;
        }
    }
}

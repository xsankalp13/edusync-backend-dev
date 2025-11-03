package com.project.edusync.uis.model.entity;

import com.project.edusync.common.model.AuditableEntity;
import com.project.edusync.iam.model.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile extends AuditableEntity {

    // id (as profile_id), uuid, createdAt, updatedAt, createdBy, updatedBy
    // are all INHERITED from AuditableEntity.

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;

    @Column(name = "middle_name", length = 50)
    private String middleName;

    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(name = "preferred_name", length = 50)
    private String preferredName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String bio;

    // --- Relationships ---

    /**
     * This links the Profile (person data) to the User (login data).
     * This is the one-to-one link described in your UIS_Schema document.
     * The 'user_id' column in this table must be unique, as one
     * login can only be for one person.
+
     * We make this link 'optional = true' (the default) because a
     * UserProfile might be created (e.g., for a student) before
     * a login account (User) is provisioned for them.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", unique = true)
    private User user;

    /*
     * NOTE: We do not have fields like 'private Student student' here.
     * The "Role" entities (Student, Staff) will link *back to* this
     * UserProfile. This keeps the central "Person" entity clean
     * and decoupled from the many roles it can have.
     */
}
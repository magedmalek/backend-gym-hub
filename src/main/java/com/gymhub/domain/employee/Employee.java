package com.gymhub.domain.employee;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * An employee is a {@link User} who works at a specific {@link Gym}.
 * Each employee has a job title, salary information, and a set of
 * operational permissions that determine what they can do in the system.
 *
 * Every operation performed by an employee (selling a subscription,
 * activating it, registering attendance, etc.) is stored with a reference
 * back to the responsible employee.
 */
@Entity
@Table(name = "employees",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gym_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Identity links ───────────────────────────────────────────────────────

    /** The platform identity behind this employee. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** The gym this employee belongs to. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    // ── Work details ─────────────────────────────────────────────────────────

    @Column(nullable = false, length = 100)
    private String jobTitle;

    @Column(precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(length = 10)
    @Builder.Default
    private String salaryCurrency = "EGP";

    @Column(length = 500)
    private String notes;

    // ── Permissions ─────────────────────────────────────────────────────────

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_permissions",
            joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "permission")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<EmployeePermission> permissions = new HashSet<>();

    /**
     * Hierarchy level — higher number = more authority.
     * Level 100 = gym owner / super admin (cannot be modified by employees).
     * Level 50  = admin employee.
     * Level 1   = regular employee (default).
     *
     * An employee can only manage employees with a strictly lower hierarchyLevel.
     */
    @Builder.Default
    private int hierarchyLevel = 1;

    // ── Status ───────────────────────────────────────────────────────────────

    @Builder.Default
    private boolean active = true;

    // ── Audit ────────────────────────────────────────────────────────────────

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Helpers ──────────────────────────────────────────────────────────────

    public boolean hasPermission(EmployeePermission permission) {
        return permissions.contains(permission) || permissions.contains(EmployeePermission.ADMIN);
    }

    public void addPermission(EmployeePermission permission) {
        this.permissions.add(permission);
    }
}

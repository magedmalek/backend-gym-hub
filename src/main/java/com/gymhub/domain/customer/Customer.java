package com.gymhub.domain.customer;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a member / customer relationship between a {@link User} and a {@link Gym}.
 *
 * The same User can be a customer of multiple gyms —
 * each gym relationship is a separate Customer record.
 *
 * The memberCode field stores the value embedded in the member's
 * barcode card or printed QR code for attendance scanning.
 */
@Entity
@Table(name = "customers",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gym_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    /**
     * Unique scannable code for this member inside this gym
     * (printed QR or barcode value).
     */
    @Column(unique = true, length = 100)
    private String memberCode;

    /** The employee who registered this customer. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_employee_id")
    private Employee registeredBy;

    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    private LocalDateTime joinedAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

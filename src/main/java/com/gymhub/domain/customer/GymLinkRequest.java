package com.gymhub.domain.customer;

import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A request from an existing platform user to be linked as a customer of a specific gym.
 *
 * Created by the customer app. Approved (or ignored) by gym employees from the dashboard.
 * On approval: a Customer record is created for this user in this gym.
 */
@Entity
@Table(name = "gym_link_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "gym_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymLinkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private GymLinkRequestStatus status = GymLinkRequestStatus.PENDING;

    @Column(length = 500)
    private String notes;

    @CreationTimestamp
    private LocalDateTime requestedAt;

    private LocalDateTime resolvedAt;
}

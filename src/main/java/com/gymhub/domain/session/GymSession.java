package com.gymhub.domain.session;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A scheduled gym session / class that customers can book (GD-09 Calendar, Sessions & Bookings;
 * CA-07 Customer Booking Calendar).
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>endTime must be after startTime.</li>
 *   <li>bookedCount is maintained by the booking flow and never exceeds capacity.</li>
 *   <li>Only SCHEDULED, future sessions can be booked.</li>
 * </ul>
 */
@Entity
@Table(name = "gym_sessions", indexes = {
        @Index(name = "idx_session_gym_start", columnList = "gym_id, start_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GymSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(length = 200)
    private String instructorName;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    @Builder.Default
    private int bookedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private Employee createdBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public boolean hasFreeSlot() {
        return bookedCount < capacity;
    }
}

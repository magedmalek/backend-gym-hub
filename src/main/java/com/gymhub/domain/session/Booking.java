package com.gymhub.domain.session;

import com.gymhub.domain.customer.Customer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A customer's booking of a {@link GymSession} (CA-07).
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>A customer may hold only one CONFIRMED booking per session.</li>
 *   <li>Cancelling frees a slot (decrements the session's bookedCount).</li>
 * </ul>
 */
@Entity
@Table(name = "session_bookings", indexes = {
        @Index(name = "idx_booking_session", columnList = "session_id"),
        @Index(name = "idx_booking_customer", columnList = "customer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private GymSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.CONFIRMED;

    @CreationTimestamp
    private LocalDateTime bookedAt;

    private LocalDateTime cancelledAt;
}

package com.gymhub.domain.attendance;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.gym.EntranceMethod;
import com.gymhub.domain.subscription.Subscription;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Records a single visit to the gym by a member or guest.
 *
 * Supported entry methods:
 *  - BARCODE: physical card scanned at the gate device
 *  - DYNAMIC_QR: one-time QR code shown on the receptionist's screen
 *  - PRINTED_QR: static QR printed on the member's card
 *
 * (Fingerprint is outside the current scope.)
 *
 * The linked subscription indicates which active subscription authorised
 * this entry. For guest visits the subscription field is null —
 * the invitation record holds that link instead.
 */
@Entity
@Table(name = "attendance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceType type;

    @Enumerated(EnumType.STRING)
    private EntranceMethod entranceMethod;

    /**
     * The subscription under which this visit was authorised.
     * Null for guest visits (use Invitation.guestAttendance instead).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    /**
     * Employee who recorded the entry (for DYNAMIC_QR or manual override).
     * Null for fully automated BARCODE entries.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_employee_id")
    private Employee recordedBy;

    @CreationTimestamp
    private LocalDateTime visitedAt;
}

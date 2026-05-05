package com.gymhub.domain.invitation;

import com.gymhub.domain.customer.Customer;
import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import com.gymhub.domain.subscription.Subscription;
import com.gymhub.domain.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents the use of one invitation slot from a member's subscription.
 *
 * Business rules:
 *  - A gym employee registers the guest's details (not an anonymous entry).
 *  - If the guest already exists in the system (existing User) the invitation
 *    is linked to that User; otherwise a new User account is created (with no
 *    gym or subscription linked — just an identity record).
 *  - The invitation record preserves the full relationship:
 *      who invited (host customer), who was invited (guest user), which gym.
 *  - Whether the same guest can visit again depends on
 *    {@link com.gymhub.domain.gympackage.GymPackage#isAllowGuestRepeatVisit()}.
 */
@Entity
@Table(name = "invitations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Participants ──────────────────────────────────────────────────────────

    /** The subscribing member who is using one of their invitation slots. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_customer_id", nullable = false)
    private Customer host;

    /** The subscription from which the invitation slot is consumed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    /**
     * The invited person's User record.
     * Either an existing user or a newly created one for the first-time guest.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_user_id", nullable = false)
    private User guestUser;

    /** The gym where the invitation is being used. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    /** Employee who registered this invitation entry. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_employee_id")
    private Employee recordedBy;

    // ── Guest account flag ────────────────────────────────────────────────────

    /**
     * True when guestUser was freshly created for this invitation
     * (not an existing platform user).
     * Their account has no gym or subscription link.
     */
    @Builder.Default
    private boolean newGuestAccount = false;

    // ── Repeat-visit tracking ─────────────────────────────────────────────────

    /**
     * Cached from the package setting at the time of the invitation so that
     * later package changes do not retroactively affect stored records.
     */
    private boolean guestRepeatVisitAllowed;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @CreationTimestamp
    private LocalDateTime usedAt;
}

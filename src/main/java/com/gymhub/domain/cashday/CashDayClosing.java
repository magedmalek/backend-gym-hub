package com.gymhub.domain.cashday;

import com.gymhub.domain.employee.Employee;
import com.gymhub.domain.gym.Gym;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A daily cash-day closing record for a gym (GD-07 Cash Payments & Daily Closing).
 *
 * <p>At close time the system computes {@code expectedCashTotal} as the sum of CASH payments
 * recorded for the gym on {@code businessDate}; the operator enters {@code countedCashTotal}
 * (the physical drawer count). {@code variance} = counted − expected.</p>
 *
 * <p>Business rules enforced by the service layer:
 * <ul>
 *   <li>One closing per gym per business date (re-closing is blocked).</li>
 *   <li>Only the owner or an employee holding {@code CLOSE_CASH_DAY} may close the day.</li>
 *   <li>Closing is a financial record — it is never deleted (audit integrity).</li>
 * </ul>
 */
@Entity
@Table(name = "cash_day_closings",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cash_closing_gym_date", columnNames = {"gym_id", "business_date"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashDayClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(name = "business_date", nullable = false)
    private LocalDate businessDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedCashTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal countedCashTotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal variance;

    @Column(nullable = false)
    private int paymentCount;

    @Column(length = 1000)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by_employee_id")
    private Employee closedBy;

    @CreationTimestamp
    private LocalDateTime closedAt;
}

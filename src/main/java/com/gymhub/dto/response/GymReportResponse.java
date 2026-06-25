package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Operational summary for a gym over a date range (GD-15 Reports & Analytics).
 */
@Data
@Builder
public class GymReportResponse {
    private Long gymId;
    private LocalDate fromDate;
    private LocalDate toDate;

    // Revenue
    private BigDecimal totalRevenueInRange;
    private BigDecimal cashRevenueInRange;

    // Subscriptions
    private long newSubscriptionsInRange;
    private long activeSubscriptions;
    private long pendingActivationSubscriptions;
    private long expiredSubscriptions;

    // Customers
    private long totalCustomers;
    private long activeCustomers;

    // Attendance
    private long attendanceInRange;
}

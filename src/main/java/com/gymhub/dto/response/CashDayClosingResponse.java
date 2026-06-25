package com.gymhub.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class CashDayClosingResponse {
    private Long id;
    private Long gymId;
    private LocalDate businessDate;
    private BigDecimal expectedCashTotal;
    private BigDecimal countedCashTotal;
    private BigDecimal variance;
    private int paymentCount;
    private String notes;
    private String closedByEmployeeName;
    private LocalDateTime closedAt;
}

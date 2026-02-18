package com.ordish.ai.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DidiOrder {
    private String orderId;
    private String userId;
    private String startAddr;
    private String endAddr;
    private String status;      // PENDING, DISPATCHED, CANCELLED, COMPLETED
    private String driverName;
    private String plateNumber;
    private BigDecimal price;
    private LocalDateTime createTime;
}
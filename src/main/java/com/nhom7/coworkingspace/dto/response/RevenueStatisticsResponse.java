package com.nhom7.coworkingspace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsResponse {

    private int year;

    private BigDecimal totalRevenue;

    private List<MonthlyRevenue> monthlyRevenue;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRevenue {

        private int month;

        private BigDecimal revenue;
    }
}
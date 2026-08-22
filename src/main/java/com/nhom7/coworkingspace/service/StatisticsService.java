package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;

import java.util.List;

public interface StatisticsService {

    StatisticsOverviewResponse getOverview();

    RevenueStatisticsResponse getRevenueByYear(int year);

    List<PaymentResponse> getAllPayments();
}
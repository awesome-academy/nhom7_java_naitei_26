package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.PaymentRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.entity.Payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private static final String SUCCESSFUL_BOOKING_STATUS = "COMPLETED";
    private static final String ACTIVE_VENUE_STATUS = "ACTIVE";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final VenueRepository venueRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public StatisticsOverviewResponse getOverview() {

        long totalUsers =
                userRepository.count();

        long successfulBookings =
                bookingRepository.countByStatusIgnoreCase(
                        SUCCESSFUL_BOOKING_STATUS
                );

        long activeVenues =
                venueRepository.countByStatusIgnoreCase(
                        ACTIVE_VENUE_STATUS
                );

        return StatisticsOverviewResponse.builder()
                .totalUsers(totalUsers)
                .successfulBookings(successfulBookings)
                .activeVenues(activeVenues)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueStatisticsResponse getRevenueByYear(int year) {

        BigDecimal totalRevenue =
                paymentRepository.findTotalRevenueByYear(year);

        List<Object[]> monthlyData =
                paymentRepository.findMonthlyRevenueByYear(year);

        Map<Integer, BigDecimal> revenueByMonth =
                new HashMap<>();

        for (Object[] row : monthlyData) {

            int month =
                    ((Number) row[0]).intValue();

            BigDecimal revenue =
                    new BigDecimal(
                            row[1].toString()
                    );

            revenueByMonth.put(
                    month,
                    revenue
            );
        }

        List<RevenueStatisticsResponse.MonthlyRevenue>
                monthlyRevenue = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {

            monthlyRevenue.add(
                    RevenueStatisticsResponse.MonthlyRevenue
                            .builder()
                            .month(month)
                            .revenue(
                                    revenueByMonth.getOrDefault(
                                            month,
                                            BigDecimal.ZERO
                                    )
                            )
                            .build()
            );
        }

        return RevenueStatisticsResponse.builder()
                .year(year)
                .totalRevenue(
                        totalRevenue != null
                                ? totalRevenue
                                : BigDecimal.ZERO
                )
                .monthlyRevenue(monthlyRevenue)
                .build();
    }

    @Override
@Transactional(readOnly = true)
public List<PaymentResponse> getAllPayments() {

    List<Payment> payments =
            paymentRepository.findAllByOrderByPaidAtDesc();

    return payments.stream()
            .map(payment ->
                    PaymentResponse.builder()
                            .id(payment.getId())
                            .bookingId(
                                    payment.getBooking() != null
                                            ? payment.getBooking().getId()
                                            : null
                            )
                            .amount(payment.getAmount())
                            .paymentMethod(payment.getPaymentMethod())
                            .status(payment.getStatus())
                            .paidAt(payment.getPaidAt())
                            .transactionId(payment.getTransactionId())
                            .build()
            )
            .toList();
}
}
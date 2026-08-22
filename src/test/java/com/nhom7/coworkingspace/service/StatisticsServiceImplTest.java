package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.response.PaymentResponse;
import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.entity.Booking;
import com.nhom7.coworkingspace.entity.Payment;
import com.nhom7.coworkingspace.repository.BookingRepository;
import com.nhom7.coworkingspace.repository.PaymentRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.repository.VenueRepository;
import com.nhom7.coworkingspace.service.impl.StatisticsServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private PaymentRepository paymentRepository;

    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        statisticsService = new StatisticsServiceImpl(
                userRepository,
                bookingRepository,
                venueRepository,
                paymentRepository
        );
    }

    @Test
    void getOverviewShouldReturnCorrectStatistics() {

        given(userRepository.count())
                .willReturn(22L);

        given(
                bookingRepository
                        .countByStatusIgnoreCase("COMPLETED")
        ).willReturn(10L);

        given(
                venueRepository
                        .countByStatusIgnoreCase("ACTIVE")
        ).willReturn(5L);

        StatisticsOverviewResponse result =
                statisticsService.getOverview();

        assertThat(result).isNotNull();

        assertThat(result.getTotalUsers())
                .isEqualTo(22L);

        assertThat(result.getSuccessfulBookings())
                .isEqualTo(10L);

        assertThat(result.getActiveVenues())
                .isEqualTo(5L);

        verify(userRepository)
                .count();

        verify(bookingRepository)
                .countByStatusIgnoreCase("COMPLETED");

        verify(venueRepository)
                .countByStatusIgnoreCase("ACTIVE");
    }

    @Test
    void getRevenueByYearShouldReturnMonthlyRevenue() {

        given(
                paymentRepository
                        .findTotalRevenueByYear(2026)
        ).willReturn(
                new BigDecimal("3000000")
        );

        List<Object[]> monthlyData = List.of(
                new Object[]{
                        1,
                        new BigDecimal("1000000")
                },
                new Object[]{
                        2,
                        new BigDecimal("2000000")
                }
        );

        given(
                paymentRepository
                        .findMonthlyRevenueByYear(2026)
        ).willReturn(monthlyData);

        RevenueStatisticsResponse result =
                statisticsService
                        .getRevenueByYear(2026);

        assertThat(result).isNotNull();

        assertThat(result.getYear())
                .isEqualTo(2026);

        assertThat(result.getTotalRevenue())
                .isEqualByComparingTo(
                        new BigDecimal("3000000")
                );

        assertThat(result.getMonthlyRevenue())
                .hasSize(12);

        assertThat(
                result.getMonthlyRevenue()
                        .get(0)
                        .getMonth()
        ).isEqualTo(1);

        assertThat(
                result.getMonthlyRevenue()
                        .get(0)
                        .getRevenue()
        ).isEqualByComparingTo(
                new BigDecimal("1000000")
        );

        assertThat(
                result.getMonthlyRevenue()
                        .get(1)
                        .getMonth()
        ).isEqualTo(2);

        assertThat(
                result.getMonthlyRevenue()
                        .get(1)
                        .getRevenue()
        ).isEqualByComparingTo(
                new BigDecimal("2000000")
        );

        assertThat(
                result.getMonthlyRevenue()
                        .get(2)
                        .getRevenue()
        ).isEqualByComparingTo(
                BigDecimal.ZERO
        );

        verify(paymentRepository)
                .findTotalRevenueByYear(2026);

        verify(paymentRepository)
                .findMonthlyRevenueByYear(2026);
    }

    @Test
    void getRevenueByYearShouldReturnZeroWhenNoRevenueExists() {

        given(
                paymentRepository
                        .findTotalRevenueByYear(2026)
        ).willReturn(BigDecimal.ZERO);

        given(
                paymentRepository
                        .findMonthlyRevenueByYear(2026)
        ).willReturn(List.of());

        RevenueStatisticsResponse result =
                statisticsService
                        .getRevenueByYear(2026);

        assertThat(result).isNotNull();

        assertThat(result.getTotalRevenue())
                .isEqualByComparingTo(
                        BigDecimal.ZERO
                );

        assertThat(result.getMonthlyRevenue())
                .hasSize(12);

        for (
                RevenueStatisticsResponse.MonthlyRevenue monthly :
                        result.getMonthlyRevenue()
        ) {
            assertThat(monthly.getRevenue())
                    .isEqualByComparingTo(
                            BigDecimal.ZERO
                    );
        }
    }

    @Test
    void getAllPaymentsShouldReturnPaymentHistory() {

        Booking booking = Booking.builder()
                .id(1L)
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(
                        new BigDecimal("500000")
                )
                .paymentMethod("BANK_TRANSFER")
                .status("COMPLETED")
                .paidAt(
                        LocalDateTime.of(
                                2026,
                                8,
                                20,
                                10,
                                30
                        )
                )
                .transactionId("TEST-TXN-001")
                .build();

        given(
                paymentRepository
                        .findAllByOrderByPaidAtDesc()
        ).willReturn(
                List.of(payment)
        );

        List<PaymentResponse> result =
                statisticsService.getAllPayments();

        assertThat(result)
                .isNotNull()
                .hasSize(1);

        PaymentResponse response =
                result.get(0);

        assertThat(response.getId())
                .isEqualTo(1L);

        assertThat(response.getBookingId())
                .isEqualTo(1L);

        assertThat(response.getAmount())
                .isEqualByComparingTo(
                        new BigDecimal("500000")
                );

        assertThat(response.getPaymentMethod())
                .isEqualTo("BANK_TRANSFER");

        assertThat(response.getStatus())
                .isEqualTo("COMPLETED");

        assertThat(response.getPaidAt())
                .isEqualTo(
                        LocalDateTime.of(
                                2026,
                                8,
                                20,
                                10,
                                30
                        )
                );

        assertThat(response.getTransactionId())
                .isEqualTo("TEST-TXN-001");

        verify(paymentRepository)
                .findAllByOrderByPaidAtDesc();
    }

    @Test
    void getAllPaymentsShouldReturnEmptyListWhenNoPaymentsExist() {

        given(
                paymentRepository
                        .findAllByOrderByPaidAtDesc()
        ).willReturn(List.of());

        List<PaymentResponse> result =
                statisticsService.getAllPayments();

        assertThat(result)
                .isNotNull()
                .isEmpty();

        verify(paymentRepository)
                .findAllByOrderByPaidAtDesc();
    }
}
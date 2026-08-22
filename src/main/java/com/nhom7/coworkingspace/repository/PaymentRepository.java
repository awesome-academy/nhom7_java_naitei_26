package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = """
            SELECT
                EXTRACT(MONTH FROM paid_at) AS month,
                COALESCE(SUM(amount), 0) AS revenue
            FROM payment
            WHERE EXTRACT(YEAR FROM paid_at) = :year
              AND UPPER(status) = 'COMPLETED'
            GROUP BY EXTRACT(MONTH FROM paid_at)
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> findMonthlyRevenueByYear(
            @Param("year") int year
    );

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM payment
            WHERE EXTRACT(YEAR FROM paid_at) = :year
              AND UPPER(status) = 'COMPLETED'
            """, nativeQuery = true)
    BigDecimal findTotalRevenueByYear(
            @Param("year") int year
    );

    List<Payment> findAllByOrderByPaidAtDesc();
}
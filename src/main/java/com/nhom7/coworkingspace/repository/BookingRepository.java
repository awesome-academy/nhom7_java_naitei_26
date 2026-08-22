package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository interface for {@link Booking} entity.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Override
    @EntityGraph(attributePaths = {"user", "space"})
    Optional<Booking> findById(Long id);

    long countByStatusIgnoreCase(String status);

    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            WHERE b.space.id = :spaceId
              AND UPPER(b.status) NOT IN ('CANCELLED', 'REJECTED')
              AND b.startTime < :endTime
              AND b.endTime > :startTime
            """)
    boolean existsActiveOverlap(
            @Param("spaceId") Long spaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
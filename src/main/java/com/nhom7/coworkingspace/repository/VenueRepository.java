package com.nhom7.coworkingspace.repository;

import com.nhom7.coworkingspace.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    long countByStatusIgnoreCase(String status);
}
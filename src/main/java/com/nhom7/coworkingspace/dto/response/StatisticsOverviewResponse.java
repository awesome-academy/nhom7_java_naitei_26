package com.nhom7.coworkingspace.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StatisticsOverviewResponse {

    private long totalUsers;

    private long successfulBookings;

    private long activeVenues;
}
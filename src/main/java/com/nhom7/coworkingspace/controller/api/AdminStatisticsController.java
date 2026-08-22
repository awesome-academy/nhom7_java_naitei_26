package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.RevenueStatisticsResponse;
import com.nhom7.coworkingspace.dto.response.StatisticsOverviewResponse;
import com.nhom7.coworkingspace.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StatisticsOverviewResponse>> getOverview() {

        StatisticsOverviewResponse result =
                statisticsService.getOverview();

        return ResponseEntity.ok(
                ApiResponse.success(
                        result,
                        "Fetched statistics overview successfully"
                )
        );
    }

    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RevenueStatisticsResponse>> getRevenue(
            @RequestParam int year
    ) {

        RevenueStatisticsResponse result =
                statisticsService.getRevenueByYear(year);

        return ResponseEntity.ok(
                ApiResponse.success(
                        result,
                        "Fetched revenue statistics successfully"
                )
        );
    }
}
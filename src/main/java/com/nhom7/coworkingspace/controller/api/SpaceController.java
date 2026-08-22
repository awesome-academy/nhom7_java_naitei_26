package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.SpaceSearchRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.SpaceResponse;
import com.nhom7.coworkingspace.service.SpaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces")
@RequiredArgsConstructor
@Tag(name = "Space API", description = "Endpoints for Co-working Space search and management")
@SecurityRequirement(name = "BearerAuth")
public class SpaceController {

    private final SpaceService spaceService;

    /**
     * Search and filter co-working spaces.
     *
     * <p>
     * Requires authenticated user with USER role (or higher).
     * </p>
     *
     * @param request search parameters
     * @return paginated list of matching spaces
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('USER', 'HOST', 'MODERATOR', 'ADMIN')")
    @Operation(summary = "Search & Filter Co-working spaces", description = "Allows users to search and filter co-working spaces by name, address (city, street), type (private office, working desk, meeting space), price (per month, day, hour), and availability time.")
    public ResponseEntity<ApiResponse<PageResponse<SpaceResponse>>> searchSpaces(
            @ModelAttribute SpaceSearchRequest request) {
        PageResponse<SpaceResponse> result = spaceService.searchSpaces(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Fetched co-working spaces successfully"));
    }
}

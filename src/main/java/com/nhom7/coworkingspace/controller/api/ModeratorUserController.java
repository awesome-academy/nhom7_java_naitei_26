package com.nhom7.coworkingspace.controller.api;

import com.nhom7.coworkingspace.dto.request.UpdateUserStatusRequest;
import com.nhom7.coworkingspace.dto.request.UpdateUserVerificationRequest;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.ApiResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;

@RestController
@RequestMapping("/api/moderator/users")
@RequiredArgsConstructor
@Tag(name = "Moderator User API", description = "Endpoints for Moderator and Admin to search and manage users")
@SecurityRequirement(name = "BearerAuth")
public class ModeratorUserController {

    private final UserService userService;
    private final MessageSource messageSource;

    // Search, filter, and paginate users for moderators/admins.
    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Search & Filter Users (Moderator/Admin)", description = "Allows Moderator or Admin to search users by keyword (name, email, phone), filter by status, and paginate results.")
    public ResponseEntity<ApiResponse<PageResponse<UserSearchResponse>>> searchUsers(
            @ParameterObject @ModelAttribute UserSearchRequest request) {
        PageResponse<UserSearchResponse> result = userService.searchUsers(request);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.list.fetched", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    // Update user status (ACTIVE, INACTIVE, BLOCKED).
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Update User Status (Active/Deactive/Block)", description = "Allows Moderator or Admin to update user status to ACTIVE, INACTIVE, or BLOCKED.")
    public ResponseEntity<ApiResponse<UpdateUserStatusResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        String currentEmail = authentication.getName();
        UpdateUserStatusResponse result = userService.updateUserStatus(id, request.getStatus(), currentEmail);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.status.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    // KYC: Update Identity Verification (CCCD/CMND)
    @PutMapping("/{id}/verify-identity")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Verify User Identity (CCCD KYC)", description = "Allows Moderator or Admin to verify/unverify user identity based on CCCD documentation.")
    public ResponseEntity<ApiResponse<UpdateUserVerificationResponse>> updateIdentityVerification(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserVerificationRequest request,
            Authentication authentication) {
        String currentEmail = authentication.getName();
        UpdateUserVerificationResponse result = userService.updateIdentityVerification(id, request.getVerified(),
                currentEmail);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.identity.verified.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }

    // KYC: Update Business Verification (Business License)
    @PutMapping("/{id}/verify-business")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Operation(summary = "Verify Business License (Host KYC)", description = "Allows Moderator or Admin to verify/unverify host business license.")
    public ResponseEntity<ApiResponse<UpdateUserVerificationResponse>> updateBusinessVerification(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserVerificationRequest request,
            Authentication authentication) {
        String currentEmail = authentication.getName();
        UpdateUserVerificationResponse result = userService.updateBusinessVerification(id, request.getVerified(),
                currentEmail);
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage("user.business.verified.updated", null, locale);
        return ResponseEntity.ok(ApiResponse.success(result, message));
    }
}

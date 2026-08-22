package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request parameters for searching and filtering users")
public class UserSearchRequest {

    @Schema(description = "Keyword to search by user name, email, or phone number", example = "hieu")
    private String keyword;

    @Schema(description = "Filter by account status", example = "ACTIVE", allowableValues = { "ACTIVE", "INACTIVE",
            "BLOCKED" })
    private UserStatus status;

    @Schema(description = "Filter by user role", example = "USER", allowableValues = { "USER", "MODERATOR", "HOST",
            "ADMIN" })
    private String role;

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    @Builder.Default
    private int page = 0;

    @Schema(description = "Number of items per page (1 - 100)", example = "20", defaultValue = "20")
    @Builder.Default
    private int size = 20;

    @Schema(description = "Field to sort by", example = "id", defaultValue = "id", allowableValues = { "id", "name",
            "email", "phone", "status", "createdAt" })
    @Builder.Default
    private String sortBy = "id";

    @Schema(description = "Sort direction", example = "ASC", defaultValue = "ASC", allowableValues = { "ASC", "DESC" })
    @Builder.Default
    private String sortDir = "ASC";
}

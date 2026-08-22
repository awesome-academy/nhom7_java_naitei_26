package com.nhom7.coworkingspace.dto.request;

import com.nhom7.coworkingspace.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body to update user status")
public class UpdateUserStatusRequest {

    @NotNull(message = "Status must not be null")
    @Schema(description = "New user status", example = "BLOCKED", allowableValues = {"ACTIVE", "INACTIVE", "BLOCKED"})
    private UserStatus status;
}

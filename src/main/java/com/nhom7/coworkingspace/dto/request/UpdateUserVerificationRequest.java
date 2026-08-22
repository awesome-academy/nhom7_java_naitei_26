package com.nhom7.coworkingspace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request body to update KYC verification status")
public class UpdateUserVerificationRequest {

    @NotNull(message = "verified status must not be null")
    @Schema(description = "KYC Verification status (true = verified, false = unverified)", example = "true")
    private Boolean verified;
}

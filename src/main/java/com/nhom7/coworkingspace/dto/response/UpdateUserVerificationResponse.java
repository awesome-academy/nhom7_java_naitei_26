package com.nhom7.coworkingspace.dto.response;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserVerificationResponse {

    private Long id;
    private String name;
    private String email;
    private Boolean isIdentityVerified;
    private Boolean isBusinessVerified;
    private String cccdUrl;
    private String businessLicenseUrl;
    private Set<String> roles;
}

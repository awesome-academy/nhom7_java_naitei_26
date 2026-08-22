package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.UserStatus;
import lombok.*;

import java.time.Instant;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSearchResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private Boolean isIdentityVerified;
    private Boolean isBusinessVerified;
    private String language;
    private String cccdUrl;
    private String businessLicenseUrl;
    private Instant passwordChangedAt;
    private Set<String> roles;
}

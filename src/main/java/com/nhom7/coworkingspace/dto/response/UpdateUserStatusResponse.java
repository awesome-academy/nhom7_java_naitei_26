package com.nhom7.coworkingspace.dto.response;

import com.nhom7.coworkingspace.enums.UserStatus;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserStatusResponse {

    private Long id;
    private String name;
    private String email;
    private UserStatus status;
    private Set<String> roles;
}

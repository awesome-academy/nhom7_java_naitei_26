package com.nhom7.coworkingspace.mapper;

import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToRoleNames")
    UserSearchResponse toUserSearchResponse(User user);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToRoleNames")
    UpdateUserStatusResponse toUpdateUserStatusResponse(User user);

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToRoleNames")
    UpdateUserVerificationResponse toUpdateUserVerificationResponse(User user);

    @Named("mapRolesToRoleNames")
    default Set<String> mapRolesToRoleNames(Set<Role> roles) {
        if (roles == null) {
            return Collections.emptySet();
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}

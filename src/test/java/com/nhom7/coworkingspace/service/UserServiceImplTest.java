package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.UserMapper;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private UserServiceImpl userService;

    private static final String HOST_EMAIL = "user@test.com";

    private User user;
    private Role userRole;
    private Role moderatorRole;

    @BeforeEach
    void setUp() {
        userRole = Role.builder()
                .id(1L)
                .name("USER")
                .build();

        moderatorRole = Role.builder()
                .id(3L)
                .name("MODERATOR")
                .build();

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        user = User.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .isIdentityVerified(false)
                .isBusinessVerified(false)
                .roles(roles)
                .build();
    }

    @Test
    void addRole_shouldAddModeratorAndKeepExistingUserRole() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRoleResponse response = userService.addRole(3L, "MODERATOR");

        assertNotNull(response);
        assertEquals(3L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("user@test.com", response.getEmail());
        assertEquals(2, response.getRoles().size());
        assertTrue(response.getRoles().contains("USER"));
        assertTrue(response.getRoles().contains("MODERATOR"));

        verify(userRepository, times(1))
                .findById(3L);

        verify(roleRepository, times(1))
                .findByName("MODERATOR");

        verify(userRepository, times(1))
                .save(user);
    }

    @Test
    void addRole_shouldThrowNotFound_whenUserNotFound() {
        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.addRole(99L, "MODERATOR")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("user.not.found", ex.getMessageKey());

        verify(userRepository, times(1))
                .findById(99L);

        verify(roleRepository, never())
                .findByName(anyString());

        verify(userRepository, never())
                .save(any());
    }

    @Test
    void addRole_shouldThrowNotFound_whenRoleNotFound() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("NON_EXISTENT_ROLE"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.addRole(3L, "NON_EXISTENT_ROLE")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("role.not.found", ex.getMessageKey());

        verify(userRepository, times(1))
                .findById(3L);

        verify(roleRepository, times(1))
                .findByName("NON_EXISTENT_ROLE");

        verify(userRepository, never())
                .save(any());
    }

    @Test
    void addRole_shouldNormalizeRoleNameToUpperCase() {
        when(userRepository.findById(3L))
                .thenReturn(Optional.of(user));

        when(roleRepository.findByName("MODERATOR"))
                .thenReturn(Optional.of(moderatorRole));

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateUserRoleResponse response = userService.addRole(3L, "  moderator  ");

        assertNotNull(response);
        assertTrue(response.getRoles().contains("MODERATOR"));

        verify(roleRepository, times(1))
                .findByName("MODERATOR");

        verify(userRepository, times(1))
                .save(user);
    }

    @Test
    void searchUsers_shouldReturnPagedUserSearchResponse() {
        UserSearchRequest request = UserSearchRequest.builder()
                .keyword("test")
                .status(UserStatus.ACTIVE)
                .page(0)
                .size(10)
                .sortBy("name")
                .sortDir("ASC")
                .build();

        Page<User> page = new PageImpl<>(List.of(user));

        UserSearchResponse responseDto = UserSearchResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(page);
        when(userMapper.toUserSearchResponse(user))
                .thenReturn(responseDto);

        PageResponse<UserSearchResponse> result = userService.searchUsers(request);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Test User", result.getContent().get(0).getName());
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getPageNumber());
        assertEquals(1, result.getTotalPages());
    }

    @Test
    void searchUsers_shouldHandleNullFiltersAndDefaultPagination() {
        UserSearchRequest request = UserSearchRequest.builder().build();

        Page<User> page = new PageImpl<>(List.of(user));
        UserSearchResponse responseDto = UserSearchResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), any(Pageable.class)))
                .thenReturn(page);
        when(userMapper.toUserSearchResponse(user))
                .thenReturn(responseDto);

        PageResponse<UserSearchResponse> result = userService.searchUsers(request);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void updateUserStatus_shouldUpdateStatusAndRevokeTokens_whenBlocked() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserStatusResponse expectedResponse = UpdateUserStatusResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.BLOCKED)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserStatusResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserStatusResponse response = userService.updateUserStatus(3L, UserStatus.BLOCKED, "admin@test.com");

        assertNotNull(response);
        assertEquals(UserStatus.BLOCKED, response.getStatus());
        assertEquals("user@test.com", response.getEmail());

        verify(tokenBlacklistService, times(1))
                .blacklistUserTokens(eq("user@test.com"), any());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUserStatus_shouldUpdateStatusAndRevokeTokens_whenInactive() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserStatusResponse expectedResponse = UpdateUserStatusResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.INACTIVE)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserStatusResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserStatusResponse response = userService.updateUserStatus(3L, UserStatus.INACTIVE, "admin@test.com");

        assertNotNull(response);
        assertEquals(UserStatus.INACTIVE, response.getStatus());
        verify(tokenBlacklistService, times(1))
                .blacklistUserTokens(eq("user@test.com"), any());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUserStatus_shouldUpdateStatusWithoutRevokingTokens_whenActive() {
        user.setStatus(UserStatus.BLOCKED);
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserStatusResponse expectedResponse = UpdateUserStatusResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserStatusResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserStatusResponse response = userService.updateUserStatus(3L, UserStatus.ACTIVE, "admin@test.com");

        assertNotNull(response);
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        verify(tokenBlacklistService, never()).blacklistUserTokens(anyString(), any());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateUserStatus_shouldReturnDirectlyWithoutDbSaveOrTokenRevoke_whenStatusUnchanged() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserStatusResponse expectedResponse = UpdateUserStatusResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("USER"))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userMapper.toUpdateUserStatusResponse(user)).thenReturn(expectedResponse);

        UpdateUserStatusResponse response = userService.updateUserStatus(3L, UserStatus.ACTIVE, "admin@test.com");

        assertNotNull(response);
        assertEquals(UserStatus.ACTIVE, response.getStatus());
        verify(tokenBlacklistService, never()).blacklistUserTokens(anyString(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_shouldThrowBadRequest_whenSelfBlocking() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateUserStatus(3L, UserStatus.BLOCKED, "user@test.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("user.cannot.block.self", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_shouldThrowForbidden_whenModeratorModifyingAdmin() {
        User adminTarget = User.builder()
                .id(10L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        User moderatorActor = User.builder()
                .id(2L)
                .email("moderator@test.com")
                .roles(Set.of(Role.builder().id(3L).name("MODERATOR").build()))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminTarget));
        when(userRepository.findByEmail("moderator@test.com")).thenReturn(Optional.of(moderatorActor));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateUserStatus(10L, UserStatus.BLOCKED, "moderator@test.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("user.cannot.modify.admin", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUserStatus_shouldThrowNotFound_whenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateUserStatus(999L, UserStatus.ACTIVE, "admin@test.com")
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("user.not.found", ex.getMessageKey());
    }

    @Test
    void updateIdentityVerification_shouldUpdateVerification_whenValid() {
        user.setCccdUrl("https://example.com/cccd.jpg");
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserVerificationResponse expectedResponse = UpdateUserVerificationResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .isIdentityVerified(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserVerificationResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserVerificationResponse response = userService.updateIdentityVerification(3L, true, "admin@test.com");

        assertNotNull(response);
        assertTrue(response.getIsIdentityVerified());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateIdentityVerification_shouldThrowBadRequest_whenDocumentMissing() {
        user.setCccdUrl(null);
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateIdentityVerification(3L, true, "admin@test.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("user.identity.document.missing", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateIdentityVerification_shouldReturnDirectly_whenUnchanged() {
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        user.setIsIdentityVerified(true);

        UpdateUserVerificationResponse expectedResponse = UpdateUserVerificationResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .isIdentityVerified(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userMapper.toUpdateUserVerificationResponse(user)).thenReturn(expectedResponse);

        UpdateUserVerificationResponse response = userService.updateIdentityVerification(3L, true, "admin@test.com");

        assertNotNull(response);
        assertTrue(response.getIsIdentityVerified());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateIdentityVerification_shouldAllowUnverifyingWithoutDocumentCheck() {
        user.setIsIdentityVerified(true);
        user.setCccdUrl(null);
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserVerificationResponse expectedResponse = UpdateUserVerificationResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .isIdentityVerified(false)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserVerificationResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserVerificationResponse response = userService.updateIdentityVerification(3L, false, "admin@test.com");

        assertNotNull(response);
        assertFalse(response.getIsIdentityVerified());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateBusinessVerification_shouldUpdateVerification_whenValid() {
        user.setBusinessLicenseUrl("https://example.com/license.pdf");
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        UpdateUserVerificationResponse expectedResponse = UpdateUserVerificationResponse.builder()
                .id(3L)
                .name("Test User")
                .email("user@test.com")
                .isBusinessVerified(true)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUpdateUserVerificationResponse(any(User.class))).thenReturn(expectedResponse);

        UpdateUserVerificationResponse response = userService.updateBusinessVerification(3L, true, "admin@test.com");

        assertNotNull(response);
        assertTrue(response.getIsBusinessVerified());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void updateBusinessVerification_shouldThrowBadRequest_whenDocumentMissing() {
        user.setBusinessLicenseUrl(null);
        User adminUser = User.builder()
                .id(1L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateBusinessVerification(3L, true, "admin@test.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("user.business.document.missing", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBusinessVerification_shouldThrowForbidden_whenModeratorModifyingAdmin() {
        User adminTarget = User.builder()
                .id(10L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        User moderatorActor = User.builder()
                .id(2L)
                .email("moderator@test.com")
                .roles(Set.of(Role.builder().id(3L).name("MODERATOR").build()))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminTarget));
        when(userRepository.findByEmail("moderator@test.com")).thenReturn(Optional.of(moderatorActor));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateBusinessVerification(10L, true, "moderator@test.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("user.cannot.modify.admin", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateIdentityVerification_shouldThrowForbidden_whenModeratorModifyingAdmin() {
        User adminTarget = User.builder()
                .id(10L)
                .email("admin@test.com")
                .roles(Set.of(Role.builder().id(4L).name("ADMIN").build()))
                .build();

        User moderatorActor = User.builder()
                .id(2L)
                .email("moderator@test.com")
                .roles(Set.of(Role.builder().id(3L).name("MODERATOR").build()))
                .build();

        when(userRepository.findById(10L)).thenReturn(Optional.of(adminTarget));
        when(userRepository.findByEmail("moderator@test.com")).thenReturn(Optional.of(moderatorActor));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateIdentityVerification(10L, true, "moderator@test.com")
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("user.cannot.modify.admin", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateIdentityVerification_shouldThrowBadRequest_whenSelfVerifying() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateIdentityVerification(3L, true, "user@test.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("user.cannot.verify.self", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateBusinessVerification_shouldThrowBadRequest_whenSelfVerifying() {
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        AppException ex = assertThrows(
                AppException.class,
                () -> userService.updateBusinessVerification(3L, true, "user@test.com")
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("user.cannot.verify.self", ex.getMessageKey());
        verify(userRepository, never()).save(any());
    }

    @Nested
    @DisplayName("becomeHost")
    class BecomeHostTests {

        private final MultipartFile validLicense =
                new MockMultipartFile("businessLicense", "license.jpg", "image/jpeg", "license-bytes".getBytes());

        @Test
        @DisplayName("Account status is not ACTIVE -> not upgraded to HOST, regardless of verification flags")
        void becomeHost_StatusNotActive_ThrowsForbidden() {
            user.setStatus(UserStatus.INACTIVE);
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.status.not.active")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Uploads license successfully but neither is verified yet -> not upgraded to HOST, license URL still persisted")
        void becomeHost_UploadedButNotVerified_ThrowsForbidden() {
            user.setIsBusinessVerified(false);
            user.setIsIdentityVerified(false);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(fileStorageService.storeFile(validLicense, "business-license")).willReturn("business-license/uuid.jpg");

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, validLicense))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.identity.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(user.getBusinessLicenseUrl()).isEqualTo("business-license/uuid.jpg");
            assertThat(user.getIsBusinessVerified()).isFalse();
            verify(userRepository).save(user);
            verify(roleRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("Business verified but identity not verified -> not upgraded to HOST")
        void becomeHost_BusinessVerifiedIdentityNot_ThrowsForbidden() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(false);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.identity.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Identity verified but business not verified -> not upgraded to HOST")
        void becomeHost_IdentityVerifiedBusinessNot_ThrowsForbidden() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(false);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.business.pending")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("No business license at all -> Business license is required")
        void becomeHost_NoLicense_ThrowsBadRequest() {
            user.setBusinessLicenseUrl(null);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.license.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("Both verified=true but business_license_url is NULL -> still Business license is required")
        void becomeHost_BothVerifiedButNoLicenseUrl_ThrowsBadRequest() {
            user.setBusinessLicenseUrl(null);
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, null))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.license.required")
                    .extracting("status")
                    .isEqualTo(HttpStatus.BAD_REQUEST);

            verify(roleRepository, never()).findByName(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("ACTIVE + identity verified + business verified + license URL present -> upgraded to HOST successfully")
        void becomeHost_BothVerified_UpgradesToHost() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            Role hostRole = Role.builder().id(4L).name("HOST").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(roleRepository.findByName("HOST")).willReturn(Optional.of(hostRole));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, null);

            assertThat(response.isAlreadyHost()).isFalse();
            assertThat(response.getProfile().getRoles()).contains("HOST", "USER");
            assertThat(user.getRoles()).contains(hostRole);
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("User already HOST -> not duplicated, returns already-host response without touching storage")
        void becomeHost_AlreadyHost_DoesNotDuplicateRole() {
            Role hostRole = Role.builder().id(4L).name("HOST").build();
            user.getRoles().add(hostRole);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, validLicense);

            assertThat(response.isAlreadyHost()).isTrue();
            assertThat(user.getRoles()).hasSize(2);
            verifyNoInteractions(fileStorageService);
            verify(userRepository, never()).save(any(User.class));
            verify(roleRepository, never()).findByName(anyString());
        }

        @Test
        @DisplayName("Resubmitting the exact same already-verified file does NOT reset verification -> still upgrades to HOST")
        void becomeHost_ResubmitSameFileAfterVerification_StillUpgradesToHost() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setBusinessLicenseHash(sha256Hex(validLicense));
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            Role hostRole = Role.builder().id(4L).name("HOST").build();

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(roleRepository.findByName("HOST")).willReturn(Optional.of(hostRole));
            given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

            HostUpgradeResponse response = userService.becomeHost(HOST_EMAIL, validLicense);

            assertThat(response.isAlreadyHost()).isFalse();
            assertThat(response.getProfile().getRoles()).contains("HOST");
            assertThat(user.getIsBusinessVerified()).isTrue();
            verify(fileStorageService, never()).storeFile(any(), anyString());
        }

        @Test
        @DisplayName("Uploading a genuinely different file after verification resets isBusinessVerified again")
        void becomeHost_UploadDifferentFileAfterVerification_ResetsVerification() {
            user.setBusinessLicenseUrl("business-license/existing.jpg");
            user.setBusinessLicenseHash("different-hash-value");
            user.setIsBusinessVerified(true);
            user.setIsIdentityVerified(true);

            given(userRepository.findByEmail(HOST_EMAIL)).willReturn(Optional.of(user));
            given(fileStorageService.storeFile(validLicense, "business-license"))
                    .willReturn("business-license/new-uuid.jpg");

            assertThatThrownBy(() -> userService.becomeHost(HOST_EMAIL, validLicense))
                    .isInstanceOf(AppException.class)
                    .hasMessage("host.business.pending")
                    .extracting("status")
                    .isEqualTo(HttpStatus.FORBIDDEN);

            assertThat(user.getBusinessLicenseUrl()).isEqualTo("business-license/new-uuid.jpg");
            assertThat(user.getIsBusinessVerified()).isFalse();
        }

        private String sha256Hex(MultipartFile file) {
            try {
                java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(file.getBytes());
                return java.util.HexFormat.of().formatHex(hash);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
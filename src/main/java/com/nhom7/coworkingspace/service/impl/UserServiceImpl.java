package com.nhom7.coworkingspace.service.impl;

import com.nhom7.coworkingspace.dto.request.UpdateUserRequest;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.HostUpgradeResponse;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserRoleResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserStatusResponse;
import com.nhom7.coworkingspace.dto.response.UpdateUserVerificationResponse;
import com.nhom7.coworkingspace.dto.response.UserProfileResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.entity.Role;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.exception.AppException;
import com.nhom7.coworkingspace.mapper.UserMapper;
import com.nhom7.coworkingspace.repository.RoleRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.FileStorageService;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;
import com.nhom7.coworkingspace.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS = 3600;
    private static final String HOST_ROLE_NAME = "HOST";
    private static final String BUSINESS_LICENSE_SUBDIRECTORY = "business-license";

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "email", "phone", "status", "createdAt");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserSearchResponse> searchUsers(UserSearchRequest request) {
        log.debug("[UserService] Searching users with params: keyword={}, status={}, role={}",
                request.getKeyword(), request.getStatus(), request.getRole());

        Sort.Direction direction = "ASC".equalsIgnoreCase(request.getSortDir())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        String rawSortBy = (request.getSortBy() != null) ? request.getSortBy().trim() : "id";
        String sortBy = ALLOWED_SORT_FIELDS.contains(rawSortBy) ? rawSortBy : "id";

        int page = Math.max(0, request.getPage());
        int size = Math.min(Math.max(1, request.getSize()), 100);

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Specification<User> spec = UserSpecification.buildSearchSpecification(request);
        Page<User> userPage = userRepository.findAll(spec, pageable);

        Page<UserSearchResponse> dtoPage = userPage.map(userMapper::toUserSearchResponse);
        return PageResponse.fromPage(dtoPage);
    }

    @Override
    @Transactional
    public UpdateUserRoleResponse addRole(Long userId, String roleName) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        String normalizedRoleName = roleName.trim().toUpperCase();

        Role role = roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new AppException("role.not.found", HttpStatus.NOT_FOUND));

        user.getRoles().add(role);

        User updatedUser = userRepository.save(user);

        Set<String> roleNames = updatedUser.getRoles()
                .stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UpdateUserRoleResponse.builder()
                .id(updatedUser.getId())
                .name(updatedUser.getName())
                .email(updatedUser.getEmail())
                .roles(roleNames)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateMyProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new AppException("validation.name.required", HttpStatus.BAD_REQUEST);
            }
            user.setName(trimmedName);
        }

        if (request.getPhone() != null) {
            String trimmedPhone = request.getPhone().trim();
            if (userRepository.existsByPhoneAndIdNot(trimmedPhone, user.getId())) {
                throw new AppException("user.phone.exists", HttpStatus.CONFLICT);
            }
            user.setPhone(trimmedPhone);
        }

        MultipartFile cccdImage = request.getCccdImage();
        if (cccdImage != null && !cccdImage.isEmpty()) {
            String cccdPath = fileStorageService.storeFile(cccdImage, "cccd");
            user.setCccdUrl(cccdPath);
        }

        User updatedUser = userRepository.save(user);

        return buildProfileResponse(updatedUser);
    }

    // noRollbackFor is essential here: a business license upload must be persisted even when the
    // very same call goes on to reject the upgrade (e.g. verification still pending) - otherwise
    // Spring's default rollback-on-RuntimeException would undo the save() below every time an
    // AppException is thrown afterwards, leaving business_license_url permanently NULL.
    @Override
    @Transactional(noRollbackFor = AppException.class)
    public HostUpgradeResponse becomeHost(String email, MultipartFile businessLicense) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));

        boolean alreadyHost = user.getRoles().stream()
                .anyMatch(role -> HOST_ROLE_NAME.equalsIgnoreCase(role.getName()));
        if (alreadyHost) {
            return HostUpgradeResponse.builder()
                    .profile(buildProfileResponse(user))
                    .alreadyHost(true)
                    .build();
        }

        if (businessLicense != null && !businessLicense.isEmpty()) {
            String newHash = sha256Hex(businessLicense);
            boolean isSameFileAlreadyOnFile = StringUtils.hasText(user.getBusinessLicenseUrl())
                    && newHash.equals(user.getBusinessLicenseHash());

            // A resubmission of the exact same file (e.g. a client retrying this call with the
            // same attachment still selected) must NOT wipe out a verification that was already
            // granted in the meantime - only a genuinely different file resets it.
            if (!isSameFileAlreadyOnFile) {
                String licensePath = fileStorageService.storeFile(businessLicense, BUSINESS_LICENSE_SUBDIRECTORY);
                user.setBusinessLicenseUrl(licensePath);
                user.setBusinessLicenseHash(newHash);
                user.setIsBusinessVerified(false);
                userRepository.save(user);
            }
        }

        boolean isActive = user.getStatus() == UserStatus.ACTIVE;
        boolean hasLicense = StringUtils.hasText(user.getBusinessLicenseUrl());
        boolean identityVerified = Boolean.TRUE.equals(user.getIsIdentityVerified());
        boolean businessVerified = Boolean.TRUE.equals(user.getIsBusinessVerified());

        if (!isActive) {
            throw new AppException("host.status.not.active", HttpStatus.FORBIDDEN);
        }
        if (!hasLicense) {
            throw new AppException("host.license.required", HttpStatus.BAD_REQUEST);
        }
        if (!identityVerified) {
            throw new AppException("host.identity.required", HttpStatus.FORBIDDEN);
        }
        if (!businessVerified) {
            throw new AppException("host.business.pending", HttpStatus.FORBIDDEN);
        }

        Role hostRole = roleRepository.findByName(HOST_ROLE_NAME)
                .orElseThrow(() -> new AppException("role.not.found", HttpStatus.NOT_FOUND));
        user.getRoles().add(hostRole);
        User updatedUser = userRepository.save(user);

        return HostUpgradeResponse.builder()
                .profile(buildProfileResponse(updatedUser))
                .alreadyHost(false)
                .build();
    }

    @Override
    @Transactional
    public UpdateUserStatusResponse updateUserStatus(Long targetUserId, UserStatus newStatus,
                    String currentAdminEmail) {
        log.info("[UserService] Updating user status: targetUserId={}, newStatus={}, performedBy={}",
                targetUserId, newStatus, currentAdminEmail);

        User targetUser = findUserById(targetUserId);
        User currentUser = findUserByEmail(currentAdminEmail);

        // Cannot deactivate or block your own account
        if (targetUser.getId().equals(currentUser.getId()) && newStatus != UserStatus.ACTIVE) {
            log.warn("[UserService] User {} attempted to deactivate/block their own account (id={})",
                    currentAdminEmail, targetUserId);
            throw new AppException("user.cannot.block.self", HttpStatus.BAD_REQUEST);
        }

        validateModeratorCannotModifyAdmin(currentUser, targetUser, currentAdminEmail);

        if (targetUser.getStatus() == newStatus) {
            log.info("[UserService] User status already {}, no update required: targetUserId={}", newStatus, targetUserId);
            return userMapper.toUpdateUserStatusResponse(targetUser);
        }

        targetUser.setStatus(newStatus);
        User savedUser = userRepository.save(targetUser);

        // Revoke active tokens when user is blocked or deactivated
        if (newStatus == UserStatus.BLOCKED || newStatus == UserStatus.INACTIVE) {
            log.info("[UserService] Revoking active tokens for user: {}", savedUser.getEmail());
            tokenBlacklistService.blacklistUserTokens(savedUser.getEmail(), new Date());
        }

        return userMapper.toUpdateUserStatusResponse(savedUser);
    }

    @Override
    @Transactional
    public UpdateUserVerificationResponse updateIdentityVerification(Long targetUserId, boolean verified,
            String currentAdminEmail) {
        log.info("[UserService] Updating identity verification (CCCD): targetUserId={}, verified={}, performedBy={}",
                targetUserId, verified, currentAdminEmail);

        User targetUser = findUserById(targetUserId);
        User currentUser = findUserByEmail(currentAdminEmail);

        validateNotSelfVerification(currentUser, targetUser, currentAdminEmail);
        validateModeratorCannotModifyAdmin(currentUser, targetUser, currentAdminEmail);

        if (Boolean.valueOf(verified).equals(targetUser.getIsIdentityVerified())) {
            log.info("[UserService] Identity verification already {}, no update required: targetUserId={}", verified, targetUserId);
            return userMapper.toUpdateUserVerificationResponse(targetUser);
        }

        // Cannot mark as verified if document is missing
        if (verified && (targetUser.getCccdUrl() == null || targetUser.getCccdUrl().isBlank())) {
            log.warn("[UserService] Cannot verify identity for user {} because CCCD document is missing", targetUserId);
            throw new AppException("user.identity.document.missing", HttpStatus.BAD_REQUEST);
        }

        targetUser.setIsIdentityVerified(verified);
        User savedUser = userRepository.save(targetUser);
        return userMapper.toUpdateUserVerificationResponse(savedUser);
    }

    @Override
    @Transactional
    public UpdateUserVerificationResponse updateBusinessVerification(Long targetUserId, boolean verified,
            String currentAdminEmail) {
        log.info("[UserService] Updating business verification (License): targetUserId={}, verified={}, performedBy={}",
                targetUserId, verified, currentAdminEmail);

        User targetUser = findUserById(targetUserId);
        User currentUser = findUserByEmail(currentAdminEmail);

        validateNotSelfVerification(currentUser, targetUser, currentAdminEmail);
        validateModeratorCannotModifyAdmin(currentUser, targetUser, currentAdminEmail);

        if (Boolean.valueOf(verified).equals(targetUser.getIsBusinessVerified())) {
            log.info("[UserService] Business verification already {}, no update required: targetUserId={}", verified, targetUserId);
            return userMapper.toUpdateUserVerificationResponse(targetUser);
        }

        // Cannot mark as verified if document is missing
        if (verified && (targetUser.getBusinessLicenseUrl() == null || targetUser.getBusinessLicenseUrl().isBlank())) {
            log.warn("[UserService] Cannot verify business for user {} because business license document is missing", targetUserId);
            throw new AppException("user.business.document.missing", HttpStatus.BAD_REQUEST);
        }

        targetUser.setIsBusinessVerified(verified);
        User savedUser = userRepository.save(targetUser);
        return userMapper.toUpdateUserVerificationResponse(savedUser);
    }

    private UserProfileResponse buildProfileResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .isIdentityVerified(user.getIsIdentityVerified())
                .isBusinessVerified(user.getIsBusinessVerified())
                .language(user.getLanguage())
                .cccdUrl(resolveSignedUrl(user.getCccdUrl()))
                .businessLicenseUrl(resolveSignedUrl(user.getBusinessLicenseUrl()))
                .roles(roleNames)
                .build();
    }

    private String resolveSignedUrl(String filePath) {
        if (!StringUtils.hasText(filePath)) {
            return null;
        }
        return fileStorageService.createSignedUrl(filePath, IMAGE_SIGNED_URL_EXPIRES_IN_SECONDS);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("user.not.found", HttpStatus.NOT_FOUND));
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("auth.invalid.credentials", HttpStatus.UNAUTHORIZED));
    }

    private void validateModeratorCannotModifyAdmin(User currentUser, User targetUser, String currentEmail) {
        boolean isTargetAdmin = hasRole(targetUser, "ADMIN");
        boolean isCurrentAdmin = hasRole(currentUser, "ADMIN");

        if (isTargetAdmin && !isCurrentAdmin) {
            log.warn("[UserService] Moderator {} attempted to modify Admin (id={})",
                    currentEmail, targetUser.getId());
            throw new AppException("user.cannot.modify.admin", HttpStatus.FORBIDDEN);
        }
    }

    private void validateNotSelfVerification(User currentUser, User targetUser, String currentEmail) {
        if (targetUser.getId().equals(currentUser.getId())) {
            log.warn("[UserService] User {} attempted to self-verify their own KYC documents (id={})",
                    currentEmail, targetUser.getId());
            throw new AppException("user.cannot.verify.self", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equalsIgnoreCase(role.getName()));
    }

    private String sha256Hex(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | IOException ex) {
            throw new AppException("common.error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
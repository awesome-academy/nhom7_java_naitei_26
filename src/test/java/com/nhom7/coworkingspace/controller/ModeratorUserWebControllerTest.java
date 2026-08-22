package com.nhom7.coworkingspace.controller;

import com.nhom7.coworkingspace.config.JwtProperties;
import com.nhom7.coworkingspace.controller.web.ModeratorUserWebController;
import com.nhom7.coworkingspace.dto.request.UserSearchRequest;
import com.nhom7.coworkingspace.dto.response.PageResponse;
import com.nhom7.coworkingspace.dto.response.UserSearchResponse;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.security.CustomUserDetailsService;
import com.nhom7.coworkingspace.security.JwtAuthenticationFilter;
import com.nhom7.coworkingspace.security.JwtTokenProvider;
import com.nhom7.coworkingspace.service.TokenBlacklistService;
import com.nhom7.coworkingspace.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@WebMvcTest(ModeratorUserWebController.class)
@EnableMethodSecurity
@Import({JwtAuthenticationFilter.class, JwtProperties.class})
@DisplayName("ModeratorUserWebController - Thymeleaf Web MVC & Security Tests")
class ModeratorUserWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private TokenBlacklistService tokenBlacklistService;

    /*
    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> GET /moderator/users renders template with model attributes")
    void givenModeratorRole_whenListUsers_thenReturnViewWithModel() throws Exception {
        UserSearchResponse userDto = UserSearchResponse.builder()
                .id(1L)
                .name("Nguyen Van A")
                .email("user@test.com")
                .status(UserStatus.ACTIVE)
                .roles(Set.of("USER"))
                .build();

        PageResponse<UserSearchResponse> pageResponse = PageResponse.<UserSearchResponse>builder()
                .content(List.of(userDto))
                .pageNumber(0)
                .pageSize(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        given(userService.searchUsers(any(UserSearchRequest.class))).willReturn(pageResponse);

        mockMvc.perform(get("/moderator/users"))
                .andExpect(status().isOk())
                .andExpect(view().name("moderator/users"))
                .andExpect(model().attributeExists("users"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("searchRequest"));
    }
    */

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> GET /moderator/users returns 403 Forbidden")
    void givenUserRole_whenListUsers_thenReturn403() throws Exception {
        mockMvc.perform(get("/moderator/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated -> GET /moderator/users returns 401 Unauthorized")
    void givenUnauthenticated_whenListUsers_thenReturn401() throws Exception {
        mockMvc.perform(get("/moderator/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> POST /moderator/users/{id}/status updates status and redirects")
    void givenModeratorRole_whenUpdateUserStatus_thenRedirect() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/moderator/users/5/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("status", "BLOCKED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moderator/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = {"USER"})
    @DisplayName("Authenticated USER -> POST /moderator/users/{id}/status returns 403 Forbidden")
    void givenUserRole_whenUpdateUserStatus_thenReturn403() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/moderator/users/5/status")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("status", "BLOCKED"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> POST /moderator/users/{id}/verify-identity updates identity verification and redirects")
    void givenModeratorRole_whenUpdateIdentityVerification_thenRedirect() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/moderator/users/5/verify-identity")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("verified", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moderator/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "moderator@test.com", roles = {"MODERATOR"})
    @DisplayName("Authenticated MODERATOR -> POST /moderator/users/{id}/verify-business updates business verification and redirects")
    void givenModeratorRole_whenUpdateBusinessVerification_thenRedirect() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/moderator/users/5/verify-business")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                        .param("verified", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/moderator/users"))
                .andExpect(flash().attributeExists("successMessage"));
    }
}

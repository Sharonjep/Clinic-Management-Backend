package com.clinic.management.controller;

import com.clinic.management.dto.auth.LoginRequest;
import com.clinic.management.dto.auth.RegisterRequest;
import com.clinic.management.entity.Role;
import com.clinic.management.entity.User;
import com.clinic.management.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User admin = new User("admin_test", "admin@test.com", passwordEncoder.encode("SecretPass123"), "Test Admin", Role.ROLE_ADMIN);
        userRepository.save(admin);
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Success with valid credentials")
    void testLogin_Success() throws Exception {
        LoginRequest request = new LoginRequest("admin_test", "SecretPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.username", is("admin_test")))
                .andExpect(jsonPath("$.data.role", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - 401 Unauthorized with invalid password")
    void testLogin_InvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin_test", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.error", is("Unauthorized")));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me - 401 Unauthorized when no JWT is provided")
    void testGetCurrentUser_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 403 Forbidden when caller is not ADMIN")
    @WithMockUser(username = "reception_user", roles = {"RECEPTIONIST"})
    void testRegisterUser_ForbiddenForNonAdmin() throws Exception {
        RegisterRequest request = new RegisterRequest("new_user", "new@test.com", "Password123", "New Staff", Role.ROLE_RECEPTIONIST);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - 201 Created when caller has ROLE_ADMIN")
    @WithMockUser(username = "admin_test", roles = {"ADMIN"})
    void testRegisterUser_SuccessWithAdmin() throws Exception {
        RegisterRequest request = new RegisterRequest("dr_new", "dr_new@test.com", "Password123", "Dr. New", Role.ROLE_DOCTOR);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.username", is("dr_new")))
                .andExpect(jsonPath("$.data.role", is("ROLE_DOCTOR")));
    }
}

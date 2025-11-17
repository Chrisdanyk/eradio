package cg.xis.eradio.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.domain.repository.UserRepository;
import cg.xis.eradio.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Profile Endpoint Integration Tests")
class AuthControllerProfileTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .build();

        testUser = userRepository.save(testUser);

        validToken = jwtService.generateToken(testUser);
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return user profile with valid token")
    void getProfile_WithValidToken_ShouldReturnUserProfile() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testUser.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 when token is missing")
    void getProfile_WithoutToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 with invalid token")
    void getProfile_WithInvalidToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 with malformed token")
    void getProfile_WithMalformedToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer not.a.valid.jwt.token.format")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 with token without Bearer prefix")
    void getProfile_WithTokenWithoutBearer_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 with empty token")
    void getProfile_WithEmptyToken_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return user profile for ADMIN role")
    void getProfile_WithAdminUser_ShouldReturnAdminProfile() throws Exception {
        User adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .fullName("Admin User")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .build();

        adminUser = userRepository.save(adminUser);
        String adminToken = jwtService.generateToken(adminUser);

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(adminUser.getId()))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.fullName").value("Admin User"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should handle deleted user scenario")
    void getProfile_WhenUserDeleted_ShouldReturnForbidden() throws Exception {
        userRepository.delete(testUser);

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return correct response structure")
    void getProfile_ShouldReturnCorrectResponseStructure() throws Exception {
        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").exists())
                .andExpect(jsonPath("$.email").exists())
                .andExpect(jsonPath("$.fullName").exists())
                .andExpect(jsonPath("$.role").exists());
    }

    @Test
    @DisplayName("GET /api/auth/profile - Should return 403 with invalid token signature")
    void getProfile_WithInvalidTokenSignature_ShouldReturnForbidden() throws Exception {
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsImlhdCI6MTYxNjIzOTAyMiwiZXhwIjo5OTk5OTk5OTk5fQ.invalid";

        mockMvc.perform(get("/api/auth/profile")
                        .header("Authorization", "Bearer " + invalidToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}


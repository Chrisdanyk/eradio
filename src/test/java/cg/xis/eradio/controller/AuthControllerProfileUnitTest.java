package cg.xis.eradio.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import cg.xis.eradio.domain.entity.User;
import cg.xis.eradio.dto.response.UserProfileResponse;
import cg.xis.eradio.service.UserService;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Profile Endpoint Unit Tests")
class AuthControllerProfileUnitTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .fullName("Test User")
                .password("encodedPassword")
                .role(User.Role.USER)
                .build();
    }

    @Test
    @DisplayName("getProfile - Should return user profile successfully")
    void getProfile_WithValidUser_ShouldReturnUserProfile() {
        // Given
        when(userService.getCurrentUser()).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileResponse> response = authController.getProfile();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        UserProfileResponse profile = response.getBody();
        assertEquals(1L, profile.getId());
        assertEquals("testuser", profile.getUsername());
        assertEquals("test@example.com", profile.getEmail());
        assertEquals("Test User", profile.getFullName());
        assertEquals("USER", profile.getRole());

        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile - Should return profile for ADMIN user")
    void getProfile_WithAdminUser_ShouldReturnAdminProfile() {
        // Given
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .email("admin@example.com")
                .fullName("Admin User")
                .password("encodedPassword")
                .role(User.Role.ADMIN)
                .build();

        when(userService.getCurrentUser()).thenReturn(adminUser);

        // When
        ResponseEntity<UserProfileResponse> response = authController.getProfile();

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        UserProfileResponse profile = response.getBody();
        assertEquals(2L, profile.getId());
        assertEquals("admin", profile.getUsername());
        assertEquals("admin@example.com", profile.getEmail());
        assertEquals("Admin User", profile.getFullName());
        assertEquals("ADMIN", profile.getRole());

        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile - Should throw exception when user is not authenticated")
    void getProfile_WhenUserNotAuthenticated_ShouldThrowException() {
        // Given
        when(userService.getCurrentUser())
                .thenThrow(new UsernameNotFoundException("User not authenticated"));

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> authController.getProfile());
        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile - Should throw exception when user not found")
    void getProfile_WhenUserNotFound_ShouldThrowException() {
        // Given
        when(userService.getCurrentUser())
                .thenThrow(new UsernameNotFoundException("User not found: testuser"));

        // When & Then
        assertThrows(UsernameNotFoundException.class, () -> authController.getProfile());
        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile - Should map all user fields correctly")
    void getProfile_ShouldMapAllFieldsCorrectly() {
        // Given
        User userWithAllFields = User.builder()
                .id(100L)
                .username("john_doe")
                .email("john.doe@example.com")
                .fullName("John Doe")
                .password("password123")
                .role(User.Role.USER)
                .build();

        when(userService.getCurrentUser()).thenReturn(userWithAllFields);

        // When
        ResponseEntity<UserProfileResponse> response = authController.getProfile();

        // Then
        assertNotNull(response.getBody());
        UserProfileResponse profile = response.getBody();

        assertEquals(100L, profile.getId());
        assertEquals("john_doe", profile.getUsername());
        assertEquals("john.doe@example.com", profile.getEmail());
        assertEquals("John Doe", profile.getFullName());
        assertEquals("USER", profile.getRole());

        verify(userService, times(1)).getCurrentUser();
    }

    @Test
    @DisplayName("getProfile - Should return 200 OK status")
    void getProfile_ShouldReturnOkStatus() {
        // Given
        when(userService.getCurrentUser()).thenReturn(testUser);

        // When
        ResponseEntity<UserProfileResponse> response = authController.getProfile();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}


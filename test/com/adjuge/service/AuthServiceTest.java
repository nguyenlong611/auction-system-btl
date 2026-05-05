package com.adjuge.service;

import com.adjuge.exception.AuthenticationException;
import com.adjuge.model.User;
import com.adjuge.pattern.DataStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        // Reset state before each test
        authService = new AuthService();
        // Cần đảm bảo database hoặc state in-memory được khởi tạo
        DataStore.getInstance().getAllUsers().clear();
    }

    @Test
    void testRegisterSuccess() throws AuthenticationException {
        User user = authService.register("John", "Doe", "john@example.com", "password123", "password123");
        assertNotNull(user);
        assertEquals("John", user.getFirstName());
        assertEquals("john@example.com", user.getEmail());
        assertTrue(authService.isLoggedIn());
        assertEquals(user, authService.getCurrentUser());
    }

    @Test
    void testRegisterPasswordMismatch() {
        Exception exception = assertThrows(AuthenticationException.class, () -> {
            authService.register("John", "Doe", "john@example.com", "password123", "password456");
        });
        assertEquals("Passwords do not match.", exception.getMessage());
    }

    @Test
    void testRegisterPasswordTooShort() {
        Exception exception = assertThrows(AuthenticationException.class, () -> {
            authService.register("John", "Doe", "john@example.com", "12345", "12345");
        });
        assertEquals("Password must be at least 6 characters.", exception.getMessage());
    }

    @Test
    void testLoginSuccess() throws AuthenticationException {
        // Register first
        authService.register("Alice", "Smith", "alice@example.com", "secure123", "secure123");
        authService.logout();
        assertFalse(authService.isLoggedIn());

        // Then login
        User user = authService.login("alice@example.com", "secure123");
        assertNotNull(user);
        assertTrue(authService.isLoggedIn());
    }

    @Test
    void testLoginWrongPassword() throws AuthenticationException {
        authService.register("Bob", "Brown", "bob@example.com", "password123", "password123");
        authService.logout();

        assertThrows(AuthenticationException.class, () -> {
            authService.login("bob@example.com", "wrongpass");
        });
    }

    @Test
    void testLoginNonExistentEmail() {
        assertThrows(AuthenticationException.class, () -> {
            authService.login("nobody@example.com", "password123");
        });
    }
}

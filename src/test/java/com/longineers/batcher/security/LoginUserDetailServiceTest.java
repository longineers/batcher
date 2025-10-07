package com.longineers.batcher.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.ArrayList;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginUserDetailServiceTest {

    private LoginUserDetailService loginUserDetailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loginUserDetailService = new LoginUserDetailService(passwordEncoder);
    }

    @Test
    void loadUserByUsername_ExistingUser_ReturnsUserDetails() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        UserDetails userDetails = loginUserDetailService.loadUserByUsername("user");
        assertEquals("user", userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_NonExistingUser_ThrowsException() {
        assertThrows(UsernameNotFoundException.class, () -> {
            loginUserDetailService.loadUserByUsername("nonexistent");
        });
    }
}
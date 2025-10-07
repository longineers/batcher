package com.longineers.batcher.security;

import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

public class JwtRequestFilterTest {

    @Test
    public void testDoFilterInternalWithValidToken() throws Exception {
        // Arrange
        UserDetailsService userDetailsService = mock(UserDetailsService.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtRequestFilter filter = new JwtRequestFilter(userDetailsService, jwtUtil);
        
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        String token = "valid_token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(jwtUtil.extractUsername(token)).thenReturn("username");
        when(userDetailsService.loadUserByUsername("username")).thenReturn(new User("username", "password", new ArrayList<>()));
        when(jwtUtil.isAuthenticated(anyString(), any(UserDetails.class))).thenReturn(true);

        // Act
        filter.doFilterInternal(request, response, chain);
        
        // Assert
        verify(chain).doFilter(request, response);
        assertEquals("username", SecurityContextHolder.getContext().getAuthentication().getName());
    }
}
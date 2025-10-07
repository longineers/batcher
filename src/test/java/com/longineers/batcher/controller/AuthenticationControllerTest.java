package com.longineers.batcher.controller;

import com.longineers.batcher.config.SecurityConfig;
import com.longineers.batcher.model.AuthenticationRequest;
import com.longineers.batcher.security.JwtUtil;
import com.longineers.batcher.security.LoginUserDetailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@ContextConfiguration(classes = {SecurityConfig.class, AuthenticationController.class, JwtUtil.class, LoginUserDetailService.class})
public class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    public void shouldReturnJwtWhenAuthenticated() throws Exception {
        UserDetails userDetails = new User("test", "test", new ArrayList<>());
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken(userDetails, "test"));
        when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("test-jwt");

        AuthenticationRequest request = new AuthenticationRequest();
        request.setUsername("test");
        request.setPassword("test");

        mockMvc.perform(post("/authenticate")
                .contentType("application/json")
                .content("{\"username\":\"test\",\"password\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").value("test-jwt"));
    }
}

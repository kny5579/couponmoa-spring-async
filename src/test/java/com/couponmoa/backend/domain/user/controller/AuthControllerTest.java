package com.couponmoa.backend.domain.user.controller;

import com.couponmoa.backend.common.service.RedisService;
import com.couponmoa.backend.config.JwtUtil;
import com.couponmoa.backend.config.SecurityConfig;
import com.couponmoa.backend.domain.user.dto.request.SigninRequest;
import com.couponmoa.backend.domain.user.dto.request.SignupRequest;
import com.couponmoa.backend.domain.user.dto.response.TokenResponse;
import com.couponmoa.backend.domain.user.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtUtil.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private AuthService authService;

    @Test
    void 회원가입() throws Exception {
        SignupRequest signupRequest = new SignupRequest(
                "user@example.com", "Password1234!", "name", "ROLE_USER");

        willDoNothing().given(authService).signup(any(SignupRequest.class));

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    void 로그인() throws Exception {
        SigninRequest signinRequest = new SigninRequest ("user@example.com", "Password1234!");
        TokenResponse tokenResponse = new TokenResponse("accessToken","refreshToken");
        given(authService.signin(any(SigninRequest.class))).willReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signinRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }

    @Test
    void 토큰_재발급() throws Exception {
        String refreshToken = "refreshToken";
        TokenResponse tokenResponse = new TokenResponse("accessToken","refreshToken");
        given(authService.refreshToken(anyString())).willReturn(tokenResponse);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Authorization", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.data.refreshToken").value("refreshToken"));
    }




}

package com.couponmoa.backend.domain.user.controller;

import com.couponmoa.backend.common.service.RedisService;
import com.couponmoa.backend.config.JwtAuthenticationToken;
import com.couponmoa.backend.config.JwtUtil;
import com.couponmoa.backend.config.SecurityConfig;
import com.couponmoa.backend.domain.user.dto.AuthUser;
import com.couponmoa.backend.domain.user.dto.request.UserDeleteRequest;
import com.couponmoa.backend.domain.user.dto.request.UserUpdatePasswordRequest;
import com.couponmoa.backend.domain.user.dto.request.UserUpdateRequest;
import com.couponmoa.backend.domain.user.dto.response.UserResponse;
import com.couponmoa.backend.domain.user.entity.User;
import com.couponmoa.backend.domain.user.enums.UserRole;
import com.couponmoa.backend.domain.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtUtil.class})
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RedisService redisService;

    @MockitoBean
    private UserService userService;

    private JwtAuthenticationToken userAuthenticationToken;

    @BeforeEach
    public void setUp() {
        AuthUser normalUser = new AuthUser(1L, "user@example.com", UserRole.ROLE_USER);
        userAuthenticationToken = new JwtAuthenticationToken(normalUser);
    }

    @Test
    void 사용자_조회() throws Exception {
        //given
        long userId = 1L;
        String email = "email@email.com";
        User user = new User(email, "password", "name", UserRole.ROLE_USER);
        ReflectionTestUtils.setField(user, "id", userId);
        UserResponse mockResponse = UserResponse.fromEntity(user);
        given(userService.findUser(anyLong())).willReturn(mockResponse);

        //when&then
        mockMvc.perform(get("/api/v1/users")
                        .with(authentication(userAuthenticationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void 사용자_정보_수정() throws Exception {
        //given
        long userId = 1L;
        UserUpdateRequest userUpdateRequest = new UserUpdateRequest("change@email.com", "changename");
        willDoNothing().given(userService).updateUser(anyLong(), any(UserUpdateRequest.class));

        //when&then
        mockMvc.perform(patch("/api/v1/users")
                        .with(authentication(userAuthenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userUpdateRequest)))
                .andExpect(status().isOk());
    }

    @Test
    void 비밀번호_변경() throws Exception {
        //given
        long userId = 1L;
        UserUpdatePasswordRequest request = new UserUpdatePasswordRequest(
                "Password1234!", "Password12345!");
        willDoNothing().given(userService).updateUserPassword(anyLong(), any(UserUpdatePasswordRequest.class));

        //when&then
        mockMvc.perform(put("/api/v1/users/password")
                        .with(authentication(userAuthenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void 회원_탈퇴() throws Exception {
        //given
        long userId = 1L;
        UserDeleteRequest request = new UserDeleteRequest("Password1234!");
        willDoNothing().given(userService).deleteUser(anyLong(), any(UserDeleteRequest.class));

        //when&then
        mockMvc.perform(delete("/api/v1/users")
                        .with(authentication(userAuthenticationToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }
}

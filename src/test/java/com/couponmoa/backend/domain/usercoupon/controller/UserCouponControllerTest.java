package com.couponmoa.backend.domain.usercoupon.controller;

import com.couponmoa.backend.common.exception.ApplicationException;
import com.couponmoa.backend.common.exception.ErrorCode;
import com.couponmoa.backend.config.JwtAuthenticationFilter;
import com.couponmoa.backend.config.JwtAuthenticationToken;
import com.couponmoa.backend.config.TestSecurityConfig;
import com.couponmoa.backend.domain.user.dto.AuthUser;
import com.couponmoa.backend.domain.user.enums.UserRole;
import com.couponmoa.backend.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.domain.usercoupon.dto.response.UseUserCouponResponse;
import com.couponmoa.backend.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.domain.usercoupon.service.UserCouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@WebMvcTest(
        value = UserCouponController.class,
        excludeFilters = {@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)}
)
@Import(TestSecurityConfig.class)
class UserCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCouponService userCouponService;

    private final static String URL_PREFIX = "/api/v1";

    @Nested
    @Order(1)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateUserCouponTests {

        private final static long couponId = 1L;
        private final static String REQUEST_URL = URL_PREFIX + "/coupons/{couponId}/issue";

        @Test
        @Order(1)
        void 쿠폰_발급_사용자_권한_아님_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(post(REQUEST_URL, couponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(HttpStatus.FORBIDDEN.value()));
        }

        @Test
        @Order(2)
        void 쿠폰_발급_쿠폰_소진_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            ErrorCode errorCode = ErrorCode.COUPON_SOLE_OUT;
            doThrow(new ApplicationException(errorCode))
                    .when(userCouponService).createUserCoupon(anyLong(), anyLong());

            mockMvc.perform(post(REQUEST_URL, couponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
        }

        @Test
        @Order(3)
        void 쿠폰_발급_성공() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(post(REQUEST_URL, couponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()));
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FindUserCouponsTests {

        private final static String REQUEST_URL = URL_PREFIX + "/user-coupons";

        @Test
        @Order(1)
        void 쿠폰_목록_조회_사용자_권한_아님_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(get(REQUEST_URL)
                            .with(authentication(authentication)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(HttpStatus.FORBIDDEN.value()));
        }

        @Test
        @Order(2)
        void 쿠폰_목록_조회_페이지_값이_0일_때_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(get(REQUEST_URL)
                            .with(authentication(authentication))
                            .param("page", "0"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @Order(3)
        void 쿠폰_목록_조회_성공() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            int page = 1, size = 10;
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
            List<UserCouponResponse> responseList = List.of(mock(UserCouponResponse.class));
            Page<UserCouponResponse> responsePage = new PageImpl<>(responseList, pageable, responseList.size());

            given(userCouponService.findUserCoupons(any(), any(), any(), any())).willReturn(responsePage);

            mockMvc.perform(get(REQUEST_URL)
                            .with(authentication(authentication))
                            .param("page", String.valueOf(page))
                            .param("size", String.valueOf(size)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.page.size").value(responsePage.getSize()))
                    .andExpect(jsonPath("$.data.page.number").value(responsePage.getNumber()))
                    .andExpect(jsonPath("$.data.page.totalElements").value(responsePage.getTotalElements()))
                    .andExpect(jsonPath("$.data.page.totalPages").value(responsePage.getTotalPages()));
        }
    }

    @Nested
    @Order(3)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FindUserCouponCodeTests {

        private final static long userCouponId = 1L;
        private final static String REQUEST_URL = URL_PREFIX + "/user-coupons/{userCouponId}/code";

        @Test
        @Order(1)
        void 쿠폰_코드_조회_사용자_권한_아님_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(get(REQUEST_URL, userCouponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(HttpStatus.FORBIDDEN.value()));
        }

        @Test
        @Order(2)
        void 쿠폰_코드_조회_쿠폰_없음_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            ErrorCode errorCode = ErrorCode.USER_COUPON_NOT_FOUND;
            given(userCouponService.findUserCouponCode(any(), any()))
                    .willThrow(new ApplicationException(errorCode));

            mockMvc.perform(get(REQUEST_URL, userCouponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
        }

        @Test
        @Order(3)
        void 쿠폰_코드_조회_성공() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            UserCouponCodeResponse response = new UserCouponCodeResponse("code");
            given(userCouponService.findUserCouponCode(any(), any())).willReturn(response);

            mockMvc.perform(get(REQUEST_URL, userCouponId)
                            .with(authentication(authentication)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.code").value(response.getCode()));
        }
    }

    @Nested
    @Order(4)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UseUserCouponTests {

        private final static String REQUEST_URL = URL_PREFIX + "/user-coupons/use";
        private final static UserCouponRequest request = new UserCouponRequest("code");

        @Test
        @Order(1)
        void 쿠폰_사용_처리_어드민_권한_아님_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_USER);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            mockMvc.perform(post(REQUEST_URL)
                            .with(authentication(authentication))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value(HttpStatus.FORBIDDEN.value()));
        }

        @Test
        @Order(2)
        void 쿠폰_사용_처리_요청_본문_검증_오류_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            UserCouponRequest request = new UserCouponRequest(null);

            mockMvc.perform(post(REQUEST_URL)
                            .with(authentication(authentication))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()));
        }

        @Test
        @Order(3)
        void 쿠폰_사용_처리_쿠폰_없음_실패() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            ErrorCode errorCode = ErrorCode.USER_COUPON_NOT_FOUND;
            given(userCouponService.useUserCoupon(any(), any()))
                    .willThrow(new ApplicationException(errorCode));

            mockMvc.perform(post(REQUEST_URL)
                            .with(authentication(authentication))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(HttpStatus.NOT_FOUND.value()))
                    .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
        }

        @Test
        @Order(4)
        void 쿠폰_사용_처리_성공() throws Exception {
            AuthUser authUser = new AuthUser(1L, "temp@gmail.com", UserRole.ROLE_ADMIN);
            JwtAuthenticationToken authentication = new JwtAuthenticationToken(authUser);

            UseUserCouponResponse response = mock();
            given(response.getId()).willReturn(1L);
            given(userCouponService.useUserCoupon(any(), any())).willReturn(response);

            mockMvc.perform(post(REQUEST_URL)
                            .with(authentication(authentication))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(HttpStatus.OK.value()))
                    .andExpect(jsonPath("$.data.id").value(response.getId()));
        }
    }
}
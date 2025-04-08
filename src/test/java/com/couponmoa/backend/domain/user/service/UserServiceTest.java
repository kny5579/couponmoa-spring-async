package com.couponmoa.backend.domain.user.service;

import com.couponmoa.backend.common.exception.ApplicationException;
import com.couponmoa.backend.common.exception.ErrorCode;
import com.couponmoa.backend.domain.subscribe.usercoupon.repository.UserCouponSubscribeRepository;
import com.couponmoa.backend.domain.subscribe.userstore.repository.UserStoreSubscribeRepository;
import com.couponmoa.backend.domain.user.dto.request.*;
import com.couponmoa.backend.domain.user.dto.response.UserResponse;
import com.couponmoa.backend.domain.user.entity.User;
import com.couponmoa.backend.domain.user.repository.UserRepository;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static com.couponmoa.backend.domain.user.enums.UserRole.ROLE_USER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserCouponSubscribeRepository userCouponSubRepo;

    @Mock
    private UserStoreSubscribeRepository userStoreSubRepo;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("test@example.com", "encodedPassword", "nickname", ROLE_USER);
    }

    @Nested
    class findUser {
        @Test
        void 사용자_조회_실패() {
            Long userId = 1L;

            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.USER_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class, () -> userService.findUser(userId));
            assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 사용자_조회_성공() {
            Long userId = 1L;

            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);

            UserResponse userResponse = userService.findUser(userId);

            assertEquals(user.getEmail(),userResponse.getEmail());
            assertEquals(user.getNickname(),userResponse.getNickname());
        }
    }

    @Nested
    class updateUser {
        @Test
        void 변경하려는_이메일이_이미_존재할_시_예외() {
            Long userId = 1L;
            UserUpdateRequest request = new UserUpdateRequest("change@email.com","changeName");
            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
            given(userRepository.existsByEmail(anyString())).willReturn(true);

            ApplicationException exception = assertThrows(ApplicationException.class, () -> userService.updateUser(userId, request));
            assertEquals(ErrorCode.EMAIL_ALREADY_EXIST.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 사용자_정보_수정_성공() {
            Long userId = 1L;
            ReflectionTestUtils.setField(user, "id", userId);
            UserUpdateRequest request = new UserUpdateRequest("change@email.com","changeName");
            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
            given(userRepository.existsByEmail(anyString())).willReturn(false);

            userService.updateUser(userId,request);

            assertEquals(user.getEmail(),request.getEmail());
            assertEquals(user.getNickname(),request.getNickname());
        }
    }

    @Nested
    class updateUserPassword {
        @Test
        void 변경하려는_비밀번호가_이전과_동일할_시_예외() {
            Long userId = 1L;
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("oldPassword","newPassword");

            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
            given(passwordEncoder.matches(anyString(),anyString())).willReturn(true);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userService.updateUserPassword(userId, request));
            assertEquals(ErrorCode.SAME_PASSWORD.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 기존_비밀번호_불일치_시_예외() {
            Long userId = 1L;
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("oldPassword","newPassword");

            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
            given(passwordEncoder.matches(anyString(),anyString())).willReturn(false,false);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userService.updateUserPassword(userId, request));
            assertEquals(ErrorCode.INVALID_PASSWORD.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 비밀번호_변경_성공() {
            Long userId = 1L;
            UserUpdatePasswordRequest request = new UserUpdatePasswordRequest("oldPassword","newPassword");

            given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
            given(passwordEncoder.matches(anyString(),anyString())).willReturn(false,true);
            given(passwordEncoder.encode(anyString())).willReturn("encodedPassword");

            userService.updateUserPassword(userId,request);

            assertEquals("encodedPassword",user.getPassword());
        }
    }

    @Test
    void 탈퇴_성공() {
        Long userId = 1L;
        UserDeleteRequest request = new UserDeleteRequest("password");

        given(userRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(user);
        given(passwordEncoder.matches(anyString(),anyString())).willReturn(true);

        userService.deleteUser(userId, request);

        assertNotNull(user.getDeletedAt());
    }
}

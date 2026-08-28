package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.enums.MemberRole;
import com.meeting.meetingapi.dto.request.LoginRequest;
import com.meeting.meetingapi.dto.request.RegisterRequest;
import com.meeting.meetingapi.dto.response.LoginResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * AuthService의 회원가입/로그인 정책을 검증한다.
 * 로그인 실패(INVALID_CREDENTIALS)의 HTTP 응답 포맷은 ApiErrorResponseTest에서 이미 검증하므로
 * 이 테스트는 회원가입 중복 방지, 비밀번호 인코딩 저장, 로그인 성공 경로에 집중한다.
 *
 * 사전 조건: docker-compose 로 기동된 Oracle DB(localhost:1521/XEPDB1)가 실행 중이어야 한다.
 *   docker compose up -d oracle
 *
 * 각 테스트는 @Transactional로 실행되어 종료 시 자동 롤백되며, 아이디에
 * System.nanoTime() 접미사를 사용해 테스트 간 순서에 의존하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest buildRegisterRequest(String username, String password, String nickname, String email) {
        RegisterRequest request = new RegisterRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        ReflectionTestUtils.setField(request, "nickname", nickname);
        ReflectionTestUtils.setField(request, "email", email);
        return request;
    }

    private LoginRequest buildLoginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        ReflectionTestUtils.setField(request, "username", username);
        ReflectionTestUtils.setField(request, "password", password);
        return request;
    }

    @Test
    void 이미_사용_중인_아이디로_가입하면_USERNAME_DUPLICATE_예외가_발생한다() {
        long suffix = System.nanoTime();
        String username = "auth-test-dup-" + suffix;
        memberRepository.save(Member.builder()
                .username(username).password("encoded-pw").nickname("existing")
                .email(suffix + "@example.com").role(MemberRole.ROLE_USER).build());

        RegisterRequest request = buildRegisterRequest(username, "password123", "newbie", suffix + "-new@example.com");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USERNAME_DUPLICATE));
    }

    @Test
    void 정상_회원가입_시_비밀번호가_인코딩되어_저장된다() {
        long suffix = System.nanoTime();
        String username = "auth-test-register-" + suffix;
        String rawPassword = "raw-password-" + suffix;
        RegisterRequest request = buildRegisterRequest(username, rawPassword, "newbie", suffix + "@example.com");

        authService.register(request);

        Member saved = memberRepository.findByUsername(username).orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, saved.getPassword())).isTrue();
    }

    @Test
    void 올바른_아이디와_비밀번호로_로그인하면_토큰이_발급된다() {
        long suffix = System.nanoTime();
        String username = "auth-test-login-" + suffix;
        String rawPassword = "raw-password-" + suffix;
        memberRepository.save(Member.builder()
                .username(username).password(passwordEncoder.encode(rawPassword)).nickname("tester")
                .email(suffix + "@example.com").role(MemberRole.ROLE_USER).build());

        LoginResponse response = authService.login(buildLoginRequest(username, rawPassword));

        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUsername()).isEqualTo(username);
        assertThat(response.getRole()).isEqualTo(MemberRole.ROLE_USER.name());
    }
}

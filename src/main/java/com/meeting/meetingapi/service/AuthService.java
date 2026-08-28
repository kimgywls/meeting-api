package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.dto.request.LoginRequest;
import com.meeting.meetingapi.dto.request.RegisterRequest;
import com.meeting.meetingapi.dto.response.LoginResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void register(RegisterRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new CustomException(ErrorCode.USERNAME_DUPLICATE);
        }
        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();
        Member saved = memberRepository.save(member);
        log.info("회원가입 완료. memberId={}", saved.getId());
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("로그인 실패. errorCode={}", ErrorCode.INVALID_CREDENTIALS);
                    return new CustomException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            log.warn("로그인 실패. errorCode={}", ErrorCode.INVALID_CREDENTIALS);
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(member.getUsername(), member.getRole().name());
        log.info("로그인 성공. memberId={}", member.getId());
        return new LoginResponse(token, member.getUsername(), member.getRole().name());
    }
}

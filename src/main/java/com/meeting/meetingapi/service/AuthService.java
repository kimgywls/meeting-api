package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.dto.request.LoginRequest;
import com.meeting.meetingapi.dto.request.RegisterRequest;
import com.meeting.meetingapi.dto.response.LoginResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public void register(RegisterRequest request) {
        if (memberRepository.existsByUsername(request.getUsername())) {
            throw new CustomException("이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT);
        }
        Member member = Member.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .email(request.getEmail())
                .build();
        memberRepository.save(member);
    }

    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new CustomException("아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException("아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtTokenProvider.generateToken(member.getUsername(), member.getRole().name());
        return new LoginResponse(token, member.getUsername(), member.getRole().name());
    }
}

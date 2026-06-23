package com.meeting.meetingapi.dto.response;

import com.meeting.meetingapi.domain.entity.Member;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AdminMemberResponse {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String role;
    private LocalDateTime createdAt;
    private long reservationCount;

    public AdminMemberResponse(Member member, long reservationCount) {
        this.id = member.getId();
        this.username = member.getUsername();
        this.nickname = member.getNickname();
        this.email = member.getEmail();
        this.role = member.getRole().name();
        this.createdAt = member.getCreatedAt();
        this.reservationCount = reservationCount;
    }
}

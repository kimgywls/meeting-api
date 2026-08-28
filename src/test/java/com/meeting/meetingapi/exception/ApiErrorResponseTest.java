package com.meeting.meetingapi.exception;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.MemberRole;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import com.meeting.meetingapi.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GlobalExceptionHandler가 실제 HTTP 계층에서 표준 ErrorResponse({code, message})와
 * 올바른 HTTP Status로 응답하는지 실제 컨트롤러 호출을 통해 검증한다.
 *
 * 사전 조건: docker-compose 로 기동된 Oracle DB(localhost:1521/XEPDB1)가 실행 중이어야 한다.
 *   docker compose up -d oracle
 *
 * 각 테스트는 @Transactional로 실행되어 종료 시 자동 롤백되므로 별도 teardown이 필요 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiErrorResponseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void 존재하지_않는_회의실_조회시_404와_ROOM_NOT_FOUND를_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 회의실입니다."));
    }

    @Test
    void 아이디_또는_비밀번호가_틀리면_401과_INVALID_CREDENTIALS를_반환한다() throws Exception {
        String loginJson = """
                {"username":"no-such-user-%d","password":"wrong-password"}
                """.formatted(System.nanoTime());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("아이디 또는 비밀번호가 올바르지 않습니다."));
    }

    @Test
    void 예약_시간이_겹치면_409와_RESERVATION_TIME_CONFLICT를_반환한다() throws Exception {
        long suffix = System.nanoTime();
        Room room = roomRepository.save(Room.builder()
                .name("ErrTestRoom-" + suffix).location("Test Floor").capacity(10).build());
        Member existingOwner = memberRepository.save(Member.builder()
                .username("err-owner-" + suffix).password("pw").nickname("existing-owner")
                .email(suffix + "-owner@example.com").role(MemberRole.ROLE_USER).build());
        Member requester = memberRepository.save(Member.builder()
                .username("err-requester-" + suffix).password("pw").nickname("requester")
                .email(suffix + "-requester@example.com").role(MemberRole.ROLE_USER).build());

        LocalDate date = LocalDate.now().plusDays(1);
        reservationRepository.save(Reservation.builder()
                .member(existingOwner).room(room).title("existing reservation")
                .date(date).startTime(LocalTime.of(10, 0)).endTime(LocalTime.of(11, 0))
                .build());

        String token = jwtTokenProvider.generateToken(requester.getUsername(), requester.getRole().name());
        String reservationJson = """
                {"roomId":%d,"title":"conflict reservation","date":"%s","startTime":"10:00:00","endTime":"11:00:00"}
                """.formatted(room.getId(), date);

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_TIME_CONFLICT"))
                .andExpect(jsonPath("$.message").value("해당 시간에 이미 예약이 있습니다."));
    }

    @Test
    void 본인_예약이_아니면_취소시_403과_RESERVATION_ACCESS_DENIED를_반환한다() throws Exception {
        long suffix = System.nanoTime();
        Room room = roomRepository.save(Room.builder()
                .name("ErrTestRoom2-" + suffix).location("Test Floor").capacity(10).build());
        Member owner = memberRepository.save(Member.builder()
                .username("err-owner2-" + suffix).password("pw").nickname("owner")
                .email(suffix + "-owner2@example.com").role(MemberRole.ROLE_USER).build());
        Member other = memberRepository.save(Member.builder()
                .username("err-other-" + suffix).password("pw").nickname("other")
                .email(suffix + "-other@example.com").role(MemberRole.ROLE_USER).build());

        Reservation reservation = reservationRepository.save(Reservation.builder()
                .member(owner).room(room).title("other's reservation")
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .build());

        String otherToken = jwtTokenProvider.generateToken(other.getUsername(), other.getRole().name());

        mockMvc.perform(delete("/api/reservations/{id}", reservation.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("RESERVATION_ACCESS_DENIED"))
                .andExpect(jsonPath("$.message").value("본인의 예약만 취소할 수 있습니다."));
    }
}

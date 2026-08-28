package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.MemberRole;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.ReservationRequest;
import com.meeting.meetingapi.dto.request.ReservationUpdateRequest;
import com.meeting.meetingapi.dto.response.ReservationResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ReservationService의 핵심 비즈니스 규칙(입력 검증, 취소 권한/상태 전이)을
 * 단일 요청 기준으로 검증한다.
 *
 * 동시성/락 동작은 ReservationConcurrencyTest, 표준 HTTP 오류 응답 포맷은
 * ApiErrorResponseTest에서 이미 검증하므로, 이 테스트는 CustomException의
 * ErrorCode와 서비스 반환값/DB 상태만 확인하고 HTTP 계층은 다루지 않는다.
 *
 * 사전 조건: docker-compose 로 기동된 Oracle DB(localhost:1521/XEPDB1)가 실행 중이어야 한다.
 *   docker compose up -d oracle
 *
 * 각 테스트는 @Transactional로 실행되어 종료 시 자동 롤백되므로 별도 teardown이 필요 없고,
 * 회의실/회원 이름에 System.nanoTime() 접미사를 사용해 테스트 간 순서에 의존하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private Room room;
    private Member member;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;

    @BeforeEach
    void setUp() {
        long suffix = System.nanoTime();
        room = roomRepository.save(Room.builder()
                .name("RSTestRoom-" + suffix).location("Test Floor").capacity(10).build());
        member = memberRepository.save(Member.builder()
                .username("rs-test-user-" + suffix).password("pw").nickname("tester")
                .email(suffix + "@example.com").role(MemberRole.ROLE_USER).build());
        date = LocalDate.now().plusDays(1);
        startTime = LocalTime.of(10, 0);
        endTime = LocalTime.of(11, 0);
    }

    private ReservationRequest buildRequest(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        ReservationRequest request = new ReservationRequest();
        ReflectionTestUtils.setField(request, "roomId", roomId);
        ReflectionTestUtils.setField(request, "title", "테스트 예약");
        ReflectionTestUtils.setField(request, "date", date);
        ReflectionTestUtils.setField(request, "startTime", startTime);
        ReflectionTestUtils.setField(request, "endTime", endTime);
        return request;
    }

    private ReservationUpdateRequest buildUpdateRequest(String title, LocalDate date, LocalTime startTime, LocalTime endTime) {
        ReservationUpdateRequest request = new ReservationUpdateRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "date", date);
        ReflectionTestUtils.setField(request, "startTime", startTime);
        ReflectionTestUtils.setField(request, "endTime", endTime);
        return request;
    }

    @Test
    void 정상_요청이면_예약이_생성되고_응답과_DB값이_요청과_일치한다() {
        ReservationRequest request = buildRequest(room.getId(), date, startTime, endTime);

        ReservationResponse response = reservationService.createReservation(request, member.getUsername());

        assertThat(response.getRoomId()).isEqualTo(room.getId());
        assertThat(response.getDate()).isEqualTo(date);
        assertThat(response.getStartTime()).isEqualTo(startTime);
        assertThat(response.getEndTime()).isEqualTo(endTime);
        assertThat(response.getUsername()).isEqualTo(member.getUsername());
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.CONFIRMED.name());

        Optional<Reservation> saved = reservationRepository.findById(response.getId());
        assertThat(saved).isPresent();
        assertThat(saved.get().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }

    @Test
    void 시작_시간이_종료_시간보다_늦거나_같으면_INVALID_TIME_RANGE_예외가_발생한다() {
        ReservationRequest request = buildRequest(room.getId(), date, LocalTime.of(11, 0), LocalTime.of(10, 0));

        assertThatThrownBy(() -> reservationService.createReservation(request, member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TIME_RANGE));
    }

    @Test
    void 과거_날짜로_예약하면_RESERVATION_PAST_DATE_예외가_발생한다() {
        ReservationRequest request = buildRequest(room.getId(), LocalDate.now().minusDays(1), startTime, endTime);

        assertThatThrownBy(() -> reservationService.createReservation(request, member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_PAST_DATE));
    }

    @Test
    void 본인_예약을_취소하면_상태가_CANCELLED로_바뀐다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());

        reservationService.cancelReservation(created.getId(), member.getUsername());

        Reservation cancelled = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    void 존재하지_않는_예약을_취소하면_RESERVATION_NOT_FOUND_예외가_발생한다() {
        long nonExistentId = Long.MAX_VALUE;

        assertThatThrownBy(() -> reservationService.cancelReservation(nonExistentId, member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    void 이미_취소된_예약을_다시_취소하면_RESERVATION_ALREADY_CANCELLED_예외가_발생한다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());
        reservationService.cancelReservation(created.getId(), member.getUsername());

        assertThatThrownBy(() -> reservationService.cancelReservation(created.getId(), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_CANCELLED));
    }

    @Test
    void 정상_수정이면_응답과_DB값에_변경사항이_반영된다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());
        LocalDate newDate = date.plusDays(1);
        LocalTime newStartTime = LocalTime.of(14, 0);
        LocalTime newEndTime = LocalTime.of(15, 0);

        ReservationResponse updated = reservationService.updateReservation(created.getId(),
                buildUpdateRequest("수정된 제목", newDate, newStartTime, newEndTime), member.getUsername());

        assertThat(updated.getTitle()).isEqualTo("수정된 제목");
        assertThat(updated.getDate()).isEqualTo(newDate);
        assertThat(updated.getStartTime()).isEqualTo(newStartTime);
        assertThat(updated.getEndTime()).isEqualTo(newEndTime);
        assertThat(updated.getRoomId()).isEqualTo(room.getId());

        Reservation saved = reservationRepository.findById(created.getId()).orElseThrow();
        assertThat(saved.getTitle()).isEqualTo("수정된 제목");
        assertThat(saved.getDate()).isEqualTo(newDate);
        assertThat(saved.getStartTime()).isEqualTo(newStartTime);
        assertThat(saved.getEndTime()).isEqualTo(newEndTime);
    }

    @Test
    void 취소된_예약을_수정하면_RESERVATION_ALREADY_CANCELLED_예외가_발생한다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());
        reservationService.cancelReservation(created.getId(), member.getUsername());

        assertThatThrownBy(() -> reservationService.updateReservation(created.getId(),
                buildUpdateRequest("수정 시도", date, startTime, endTime), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_CANCELLED));
    }

    @Test
    void 존재하지_않는_예약을_수정하면_RESERVATION_NOT_FOUND_예외가_발생한다() {
        long nonExistentId = Long.MAX_VALUE;

        assertThatThrownBy(() -> reservationService.updateReservation(nonExistentId,
                buildUpdateRequest("수정 시도", date, startTime, endTime), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    void 타인의_예약을_수정하면_RESERVATION_UPDATE_ACCESS_DENIED_예외가_발생한다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());
        long otherSuffix = System.nanoTime();
        Member other = memberRepository.save(Member.builder()
                .username("rs-test-other-" + otherSuffix).password("pw").nickname("other")
                .email(otherSuffix + "@example.com").role(MemberRole.ROLE_USER).build());

        assertThatThrownBy(() -> reservationService.updateReservation(created.getId(),
                buildUpdateRequest("수정 시도", date, startTime, endTime), other.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_UPDATE_ACCESS_DENIED));
    }

    @Test
    void 시작_시간이_종료_시간보다_늦거나_같게_수정하면_INVALID_TIME_RANGE_예외가_발생한다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());

        assertThatThrownBy(() -> reservationService.updateReservation(created.getId(),
                buildUpdateRequest("수정 시도", date, LocalTime.of(11, 0), LocalTime.of(10, 0)), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_TIME_RANGE));
    }

    @Test
    void 과거_날짜로_수정하면_RESERVATION_PAST_DATE_예외가_발생한다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());

        assertThatThrownBy(() -> reservationService.updateReservation(created.getId(),
                buildUpdateRequest("수정 시도", LocalDate.now().minusDays(1), startTime, endTime), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_PAST_DATE));
    }

    @Test
    void 다른_예약과_겹치는_시간으로_수정하면_RESERVATION_TIME_CONFLICT_예외가_발생한다() {
        reservationService.createReservation(
                buildRequest(room.getId(), date, LocalTime.of(13, 0), LocalTime.of(14, 0)), member.getUsername());
        ReservationResponse target = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());

        assertThatThrownBy(() -> reservationService.updateReservation(target.getId(),
                buildUpdateRequest("수정 시도", date, LocalTime.of(13, 30), LocalTime.of(14, 30)), member.getUsername()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_TIME_CONFLICT));
    }

    @Test
    void 자기_자신의_기존_시간대로_다시_수정하면_중복_검사에서_제외되어_정상_수정된다() {
        ReservationResponse created = reservationService.createReservation(
                buildRequest(room.getId(), date, startTime, endTime), member.getUsername());

        ReservationResponse updated = reservationService.updateReservation(created.getId(),
                buildUpdateRequest("동일 시간대 재수정", date, startTime, endTime), member.getUsername());

        assertThat(updated.getTitle()).isEqualTo("동일 시간대 재수정");
        assertThat(updated.getStartTime()).isEqualTo(startTime);
        assertThat(updated.getEndTime()).isEqualTo(endTime);
    }
}

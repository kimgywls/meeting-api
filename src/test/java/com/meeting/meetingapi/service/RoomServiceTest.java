package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.MemberRole;
import com.meeting.meetingapi.dto.request.RoomRequest;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
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
 * RoomService의 회의실 생성/삭제 정책을 검증한다.
 * 조회(GET /api/rooms/{id})의 404 응답 포맷은 ApiErrorResponseTest에서 이미 검증하므로
 * 이 테스트는 생성/삭제 시의 비즈니스 규칙(중복 이름 금지, 사용 중인 회의실 삭제 금지)에 집중한다.
 *
 * 사전 조건: docker-compose 로 기동된 Oracle DB(localhost:1521/XEPDB1)가 실행 중이어야 한다.
 *   docker compose up -d oracle
 *
 * 각 테스트는 @Transactional로 실행되어 종료 시 자동 롤백되며, 회의실/회원 이름에
 * System.nanoTime() 접미사를 사용해 테스트 간 순서에 의존하지 않는다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RoomServiceTest {

    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private ReservationRepository reservationRepository;

    private RoomRequest buildRequest(String name, String location, Integer capacity, String description) {
        RoomRequest request = new RoomRequest();
        ReflectionTestUtils.setField(request, "name", name);
        ReflectionTestUtils.setField(request, "location", location);
        ReflectionTestUtils.setField(request, "capacity", capacity);
        ReflectionTestUtils.setField(request, "description", description);
        return request;
    }

    @Test
    void 이미_존재하는_이름으로_회의실을_생성하면_ROOM_NAME_DUPLICATE_예외가_발생한다() {
        long suffix = System.nanoTime();
        String duplicateName = "RoomServiceTest-Duplicate-" + suffix;
        roomRepository.save(Room.builder().name(duplicateName).location("Test Floor").capacity(10).build());

        RoomRequest request = buildRequest(duplicateName, "Other Floor", 5, null);

        assertThatThrownBy(() -> roomService.createRoom(request))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ROOM_NAME_DUPLICATE));
    }

    @Test
    void 확정된_예약이_있는_회의실은_ROOM_HAS_ACTIVE_RESERVATION_예외로_삭제되지_않는다() {
        long suffix = System.nanoTime();
        Room room = roomRepository.save(Room.builder()
                .name("RoomServiceTest-HasReservation-" + suffix).location("Test Floor").capacity(10).build());
        Member member = memberRepository.save(Member.builder()
                .username("room-test-user-" + suffix).password("pw").nickname("tester")
                .email(suffix + "@example.com").role(MemberRole.ROLE_USER).build());
        reservationRepository.save(Reservation.builder()
                .member(member).room(room).title("existing reservation")
                .date(LocalDate.now().plusDays(1)).startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(10, 0))
                .build());

        assertThatThrownBy(() -> roomService.deleteRoom(room.getId()))
                .isInstanceOfSatisfying(CustomException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ROOM_HAS_ACTIVE_RESERVATION));
    }

    @Test
    void 예약이_없는_회의실은_정상_삭제되고_DB에서도_제거된다() {
        long suffix = System.nanoTime();
        Room room = roomRepository.save(Room.builder()
                .name("RoomServiceTest-NoReservation-" + suffix).location("Test Floor").capacity(10).build());

        roomService.deleteRoom(room.getId());

        Optional<Room> found = roomRepository.findById(room.getId());
        assertThat(found).isEmpty();
    }
}

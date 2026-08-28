package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.MemberRole;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.ReservationRequest;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 회의실 / 동일 시간대에 대한 동시 예약 요청이
 * 비관적 락(PESSIMISTIC_WRITE) + @Transactional 조합으로
 * 실제 Oracle DB 상에서 안전하게 직렬화되는지 검증한다.
 *
 * 사전 조건: docker-compose 로 기동된 Oracle DB(localhost:1521/XEPDB1)가 실행 중이어야 한다.
 *   docker compose up -d oracle
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        // 기본 Hikari 풀(10)과 스레드 수(10)가 같으면 'DB 락 경합'이 아니라
        // '커넥션 풀 대기'로 인해 직렬화될 수 있어, 테스트에서만 여유 있게 늘린다.
        "spring.datasource.hikari.maximum-pool-size=20",
        "spring.jpa.show-sql=false"
})
class ReservationConcurrencyTest {

    private static final int THREAD_COUNT = 10;

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
    private LocalDate targetDate;
    private LocalTime startTime;
    private LocalTime endTime;

    @BeforeEach
    void setUp() {
        long uniqueSuffix = System.nanoTime();

        room = roomRepository.save(Room.builder()
                .name("ConcurrencyTestRoom-" + uniqueSuffix)
                .location("Test Floor")
                .capacity(10)
                .build());

        member = memberRepository.save(Member.builder()
                .username("concurrency-user-" + uniqueSuffix)
                .password("test-password")
                .nickname("동시성테스터")
                .email("concurrency-" + uniqueSuffix + "@example.com")
                .role(MemberRole.ROLE_USER)
                .build());

        targetDate = LocalDate.now().plusDays(1);
        startTime = LocalTime.of(10, 0);
        endTime = LocalTime.of(11, 0);
    }

    @AfterEach
    void tearDown() {
        reservationRepository.findByMember(member)
                .forEach(r -> reservationRepository.deleteById(r.getId()));
        memberRepository.deleteById(member.getId());
        roomRepository.deleteById(room.getId());
    }

    @Test
    void 동일_회의실_동일_시간대_동시_예약_요청_시_단_하나만_성공한다() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger conflictFailCount = new AtomicInteger();
        AtomicInteger unexpectedFailCount = new AtomicInteger();

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    ReservationRequest request = buildRequest();
                    // 모든 스레드가 준비될 때까지 대기 → 동시에 요청을 쏘기 위함
                    readyLatch.countDown();
                    startLatch.await();

                    reservationService.createReservation(request, member.getUsername());
                    successCount.incrementAndGet();
                } catch (CustomException e) {
                    // "해당 시간에 이미 예약이 있습니다." 등 정상적인 중복 예약 거부
                    conflictFailCount.incrementAndGet();
                } catch (Exception e) {
                    unexpectedFailCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).as("모든 스레드가 제한 시간 내에 종료되어야 한다").isTrue();
        assertThat(unexpectedFailCount.get()).as("예상치 못한 예외는 없어야 한다").isZero();
        assertThat(successCount.get()).as("성공한 예약 건수는 1건이어야 한다").isEqualTo(1);
        assertThat(conflictFailCount.get()).as("나머지는 중복 예약으로 실패해야 한다").isEqualTo(THREAD_COUNT - 1);

        List<Reservation> confirmedReservations =
                reservationRepository.findByRoomAndDateAndStatus(room, targetDate, ReservationStatus.CONFIRMED);
        assertThat(confirmedReservations)
                .as("DB에는 최종적으로 예약이 1건만 존재해야 한다")
                .hasSize(1);
    }

    private ReservationRequest buildRequest() {
        ReservationRequest request = new ReservationRequest();
        ReflectionTestUtils.setField(request, "roomId", room.getId());
        ReflectionTestUtils.setField(request, "title", "동시성 테스트 예약");
        ReflectionTestUtils.setField(request, "date", targetDate);
        ReflectionTestUtils.setField(request, "startTime", startTime);
        ReflectionTestUtils.setField(request, "endTime", endTime);
        return request;
    }
}

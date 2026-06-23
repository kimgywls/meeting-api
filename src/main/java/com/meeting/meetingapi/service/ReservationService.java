package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.ReservationRequest;
import com.meeting.meetingapi.dto.response.ReservationResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<ReservationResponse> getMyReservations(String username) {
        Member member = findMemberByUsername(username);
        return reservationRepository.findByMember(member).stream()
                .map(ReservationResponse::new)
                .toList();
    }

    @Transactional
    public ReservationResponse createReservation(ReservationRequest request, String username) {
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new CustomException("시작 시간은 종료 시간보다 빨라야 합니다.", HttpStatus.BAD_REQUEST);
        }
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new CustomException("과거 날짜에는 예약할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        // 비관적 락으로 동시 예약 방지
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new CustomException("존재하지 않는 회의실입니다.", HttpStatus.NOT_FOUND));

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                request.getRoomId(), request.getDate(), request.getStartTime(), request.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new CustomException("해당 시간에 이미 예약이 있습니다.", HttpStatus.CONFLICT);
        }

        Member member = findMemberByUsername(username);
        Reservation reservation = Reservation.builder()
                .member(member)
                .room(room)
                .title(request.getTitle())
                .date(request.getDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();

        return new ReservationResponse(reservationRepository.save(reservation));
    }

    @Transactional
    public void cancelReservation(Long id, String username) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new CustomException("존재하지 않는 예약입니다.", HttpStatus.NOT_FOUND));

        if (!reservation.getMember().getUsername().equals(username)) {
            throw new CustomException("본인의 예약만 취소할 수 있습니다.", HttpStatus.FORBIDDEN);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new CustomException("이미 취소된 예약입니다.", HttpStatus.BAD_REQUEST);
        }

        reservation.cancel();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getRoomReservations(Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException("존재하지 않는 회의실입니다.", HttpStatus.NOT_FOUND));
        return reservationRepository.findByRoomAndDateAndStatus(room, date, ReservationStatus.CONFIRMED).stream()
                .map(ReservationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(ReservationResponse::new)
                .toList();
    }

    private Member findMemberByUsername(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException("존재하지 않는 회원입니다.", HttpStatus.NOT_FOUND));
    }
}

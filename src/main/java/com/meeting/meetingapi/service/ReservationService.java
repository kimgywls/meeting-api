package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.ReservationRequest;
import com.meeting.meetingapi.dto.response.ReservationResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
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
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
        if (request.getDate().isBefore(LocalDate.now())) {
            throw new CustomException(ErrorCode.RESERVATION_PAST_DATE);
        }

        // 비관적 락으로 동시 예약 방지
        Room room = roomRepository.findByIdWithLock(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<Reservation> overlapping = reservationRepository.findOverlapping(
                request.getRoomId(), request.getDate(), request.getStartTime(), request.getEndTime());
        if (!overlapping.isEmpty()) {
            throw new CustomException(ErrorCode.RESERVATION_TIME_CONFLICT);
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
                .orElseThrow(() -> new CustomException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.getMember().getUsername().equals(username)) {
            throw new CustomException(ErrorCode.RESERVATION_ACCESS_DENIED);
        }
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new CustomException(ErrorCode.RESERVATION_ALREADY_CANCELLED);
        }

        reservation.cancel();
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getRoomReservations(Long roomId, LocalDate date) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
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
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}

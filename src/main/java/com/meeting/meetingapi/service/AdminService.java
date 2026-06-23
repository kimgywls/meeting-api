package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.response.AdminMemberResponse;
import com.meeting.meetingapi.dto.response.AdminReservationResponse;
import com.meeting.meetingapi.dto.response.RoomStatsResponse;
import com.meeting.meetingapi.repository.MemberRepository;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public Page<AdminReservationResponse> getAllReservations(LocalDate date, Long roomId, Pageable pageable) {
        return reservationRepository.findWithFilter(date, roomId, pageable)
                .map(AdminReservationResponse::new);
    }

    @Transactional(readOnly = true)
    public List<AdminReservationResponse> getTodayReservations() {
        return reservationRepository.findByDateAndStatus(LocalDate.now(), ReservationStatus.CONFIRMED)
                .stream()
                .map(AdminReservationResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<AdminMemberResponse> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(member -> {
                    long count = reservationRepository.countByMember(member);
                    return new AdminMemberResponse(member, count);
                });
    }

    @Transactional(readOnly = true)
    public List<RoomStatsResponse> getRoomStats() {
        LocalDate startOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate endOfMonth = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());

        return roomRepository.findAll().stream()
                .map(room -> {
                    long total = reservationRepository.countByRoomAndStatus(room, ReservationStatus.CONFIRMED);
                    long thisMonth = reservationRepository.countByRoomAndStatusAndDateBetween(
                            room, ReservationStatus.CONFIRMED, startOfMonth, endOfMonth);
                    return new RoomStatsResponse(room, total, thisMonth);
                })
                .toList();
    }
}

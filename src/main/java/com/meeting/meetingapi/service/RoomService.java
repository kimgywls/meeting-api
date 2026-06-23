package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.RoomRequest;
import com.meeting.meetingapi.dto.response.RoomResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<RoomResponse> getRooms() {
        return roomRepository.findAll().stream()
                .map(RoomResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoom(Long id) {
        return new RoomResponse(findRoomById(id));
    }

    @Transactional
    public RoomResponse createRoom(RoomRequest request) {
        if (roomRepository.existsByName(request.getName())) {
            throw new CustomException("이미 존재하는 회의실 이름입니다.", HttpStatus.CONFLICT);
        }
        Room room = Room.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .build();
        return new RoomResponse(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoomById(id);
        room.update(request.getName(), request.getLocation(), request.getCapacity(), request.getDescription());
        return new RoomResponse(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = findRoomById(id);
        if (reservationRepository.existsByRoomAndStatus(room, ReservationStatus.CONFIRMED)) {
            throw new CustomException("확정된 예약이 있는 회의실은 삭제할 수 없습니다.", HttpStatus.CONFLICT);
        }
        roomRepository.delete(room);
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRooms(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (startTime == null && endTime == null) {
            return roomRepository.findAvailableRoomsByDate(date).stream()
                    .map(RoomResponse::new)
                    .toList();
        }
        if (startTime != null && endTime == null) {
            return roomRepository.findAvailableRoomsFromTime(date, startTime).stream()
                    .map(RoomResponse::new)
                    .toList();
        }
        if (startTime == null) {
            throw new CustomException("종료 시간만 단독으로 입력할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (!startTime.isBefore(endTime)) {
            throw new CustomException("시작 시간은 종료 시간보다 빨라야 합니다.", HttpStatus.BAD_REQUEST);
        }
        return roomRepository.findAvailableRooms(date, startTime, endTime).stream()
                .map(RoomResponse::new)
                .toList();
    }

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new CustomException("존재하지 않는 회의실입니다.", HttpStatus.NOT_FOUND));
    }
}

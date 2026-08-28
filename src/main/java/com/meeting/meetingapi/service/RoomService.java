package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import com.meeting.meetingapi.dto.request.RoomRequest;
import com.meeting.meetingapi.dto.response.RoomResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.exception.ErrorCode;
import com.meeting.meetingapi.repository.ReservationRepository;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
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
            throw new CustomException(ErrorCode.ROOM_NAME_DUPLICATE);
        }
        Room room = Room.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .build();
        Room saved = roomRepository.save(room);
        log.info("회의실 생성 완료. roomId={}, roomName={}", saved.getId(), saved.getName());
        return new RoomResponse(saved);
    }

    @Transactional
    public RoomResponse updateRoom(Long id, RoomRequest request) {
        Room room = findRoomById(id);
        room.update(request.getName(), request.getLocation(), request.getCapacity(), request.getDescription());
        log.info("회의실 수정 완료. roomId={}, roomName={}", room.getId(), room.getName());
        return new RoomResponse(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Room room = findRoomById(id);
        if (reservationRepository.existsByRoomAndStatus(room, ReservationStatus.CONFIRMED)) {
            throw new CustomException(ErrorCode.ROOM_HAS_ACTIVE_RESERVATION);
        }
        roomRepository.delete(room);
        log.info("회의실 삭제 완료. roomId={}, roomName={}", room.getId(), room.getName());
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
            throw new CustomException(ErrorCode.INVALID_TIME_PARAMETER);
        }
        if (!startTime.isBefore(endTime)) {
            throw new CustomException(ErrorCode.INVALID_TIME_RANGE);
        }
        return roomRepository.findAvailableRooms(date, startTime, endTime).stream()
                .map(RoomResponse::new)
                .toList();
    }

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }
}

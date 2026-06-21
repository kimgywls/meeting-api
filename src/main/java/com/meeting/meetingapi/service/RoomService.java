package com.meeting.meetingapi.service;

import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.dto.request.RoomRequest;
import com.meeting.meetingapi.dto.response.RoomResponse;
import com.meeting.meetingapi.exception.CustomException;
import com.meeting.meetingapi.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

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
        roomRepository.delete(findRoomById(id));
    }

    private Room findRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new CustomException("존재하지 않는 회의실입니다.", HttpStatus.NOT_FOUND));
    }
}

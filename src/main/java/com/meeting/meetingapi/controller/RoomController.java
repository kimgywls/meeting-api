package com.meeting.meetingapi.controller;

import com.meeting.meetingapi.dto.request.RoomRequest;
import com.meeting.meetingapi.dto.response.RoomResponse;
import com.meeting.meetingapi.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room", description = "회의실 관리 API")
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    @Operation(summary = "회의실 목록 조회")
    public ResponseEntity<List<RoomResponse>> getRooms() {
        return ResponseEntity.ok(roomService.getRooms());
    }

    @GetMapping("/available")
    @Operation(summary = "예약 가능한 회의실 조회")
    public ResponseEntity<List<RoomResponse>> getAvailableRooms(
            @RequestParam LocalDate date,
            @RequestParam(required = false) @Schema(type = "string", example = "10:00:00") LocalTime startTime,
            @RequestParam(required = false) @Schema(type = "string", example = "11:00:00") LocalTime endTime) {
        return ResponseEntity.ok(roomService.getAvailableRooms(date, startTime, endTime));
    }

    @GetMapping("/{id}")
    @Operation(summary = "회의실 상세 조회")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(roomService.getRoom(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "회의실 등록 (ADMIN)")
    public ResponseEntity<RoomResponse> createRoom(@RequestBody RoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.createRoom(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "회의실 수정 (ADMIN)")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable Long id, @RequestBody RoomRequest request) {
        return ResponseEntity.ok(roomService.updateRoom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "회의실 삭제 (ADMIN)")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.noContent().build();
    }
}

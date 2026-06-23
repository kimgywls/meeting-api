package com.meeting.meetingapi.controller;

import com.meeting.meetingapi.dto.response.AdminMemberResponse;
import com.meeting.meetingapi.dto.response.AdminReservationResponse;
import com.meeting.meetingapi.dto.response.RoomStatsResponse;
import com.meeting.meetingapi.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "관리자 API")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/reservations")
    @Operation(summary = "전체 예약 현황 (ADMIN)", description = "날짜·회의실 필터 및 페이징 지원")
    public ResponseEntity<Page<AdminReservationResponse>> getAllReservations(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) Long roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        return ResponseEntity.ok(adminService.getAllReservations(date, roomId, pageable));
    }

    @GetMapping("/reservations/today")
    @Operation(summary = "오늘 예약 현황 (ADMIN)")
    public ResponseEntity<List<AdminReservationResponse>> getTodayReservations() {
        return ResponseEntity.ok(adminService.getTodayReservations());
    }

    @GetMapping("/members")
    @Operation(summary = "전체 회원 목록 (ADMIN)")
    public ResponseEntity<Page<AdminMemberResponse>> getAllMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(adminService.getAllMembers(pageable));
    }

    @GetMapping("/rooms/stats")
    @Operation(summary = "회의실별 예약 통계 (ADMIN)")
    public ResponseEntity<List<RoomStatsResponse>> getRoomStats() {
        return ResponseEntity.ok(adminService.getRoomStats());
    }
}

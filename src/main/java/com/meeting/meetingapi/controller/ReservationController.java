package com.meeting.meetingapi.controller;

import com.meeting.meetingapi.dto.request.ReservationRequest;
import com.meeting.meetingapi.dto.response.ReservationResponse;
import com.meeting.meetingapi.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reservation", description = "예약 관리 API")
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping("/api/reservations")
    @Operation(summary = "내 예약 목록")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        return ResponseEntity.ok(reservationService.getMyReservations(authentication.getName()));
    }

    @PostMapping("/api/reservations")
    @Operation(summary = "예약 신청")
    public ResponseEntity<ReservationResponse> createReservation(
            @RequestBody ReservationRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.createReservation(request, authentication.getName()));
    }

    @DeleteMapping("/api/reservations/{id}")
    @Operation(summary = "예약 취소")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable Long id, Authentication authentication) {
        reservationService.cancelReservation(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/rooms/{roomId}/reservations")
    @Operation(summary = "특정 회의실 날짜별 예약 현황")
    public ResponseEntity<List<ReservationResponse>> getRoomReservations(
            @PathVariable Long roomId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(reservationService.getRoomReservations(roomId, date));
    }

}

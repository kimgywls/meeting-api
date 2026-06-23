package com.meeting.meetingapi.dto.response;

import com.meeting.meetingapi.domain.entity.Reservation;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class AdminReservationResponse {
    private Long id;
    private String username;
    private String roomName;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;

    public AdminReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.username = reservation.getMember().getUsername();
        this.roomName = reservation.getRoom().getName();
        this.title = reservation.getTitle();
        this.date = reservation.getDate();
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.status = reservation.getStatus().name();
    }
}

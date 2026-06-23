package com.meeting.meetingapi.dto.response;

import com.meeting.meetingapi.domain.entity.Reservation;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
public class ReservationResponse {
    private Long id;
    private Long roomId;
    private String roomName;
    private String roomLocation;
    private String title;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private String username;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.roomId = reservation.getRoom().getId();
        this.roomName = reservation.getRoom().getName();
        this.roomLocation = reservation.getRoom().getLocation();
        this.title = reservation.getTitle();
        this.date = reservation.getDate();
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.status = reservation.getStatus().name();
        this.username = reservation.getMember().getUsername();
    }
}

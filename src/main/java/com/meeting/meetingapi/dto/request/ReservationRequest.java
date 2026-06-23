package com.meeting.meetingapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class ReservationRequest {
    private Long roomId;
    private String title;
    private LocalDate date;
    @Schema(type = "string", example = "10:00:00")
    private LocalTime startTime;
    @Schema(type = "string", example = "11:00:00")
    private LocalTime endTime;
}

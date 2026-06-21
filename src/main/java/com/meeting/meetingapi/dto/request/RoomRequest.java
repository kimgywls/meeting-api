package com.meeting.meetingapi.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RoomRequest {
    private String name;
    private String location;
    @Schema(example = "50")
    private Integer capacity;
    private String description;
}

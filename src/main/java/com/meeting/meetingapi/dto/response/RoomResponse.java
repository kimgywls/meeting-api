package com.meeting.meetingapi.dto.response;

import com.meeting.meetingapi.domain.entity.Room;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RoomResponse {
    private Long id;
    private String name;
    private String location;
    private Integer capacity;
    private String description;
    private LocalDateTime createdAt;

    public RoomResponse(Room room) {
        this.id = room.getId();
        this.name = room.getName();
        this.location = room.getLocation();
        this.capacity = room.getCapacity();
        this.description = room.getDescription();
        this.createdAt = room.getCreatedAt();
    }
}

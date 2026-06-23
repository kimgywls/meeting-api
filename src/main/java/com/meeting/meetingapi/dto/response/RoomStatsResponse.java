package com.meeting.meetingapi.dto.response;

import com.meeting.meetingapi.domain.entity.Room;
import lombok.Getter;

@Getter
public class RoomStatsResponse {
    private Long roomId;
    private String roomName;
    private long totalCount;
    private long thisMonthCount;

    public RoomStatsResponse(Room room, long totalCount, long thisMonthCount) {
        this.roomId = room.getId();
        this.roomName = room.getName();
        this.totalCount = totalCount;
        this.thisMonthCount = thisMonthCount;
    }
}

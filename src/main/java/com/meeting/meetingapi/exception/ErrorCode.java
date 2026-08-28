package com.meeting.meetingapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Room
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회의실입니다."),
    ROOM_NAME_DUPLICATE(HttpStatus.CONFLICT, "이미 존재하는 회의실 이름입니다."),
    ROOM_HAS_ACTIVE_RESERVATION(HttpStatus.CONFLICT, "확정된 예약이 있는 회의실은 삭제할 수 없습니다."),

    // Reservation
    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 예약입니다."),
    RESERVATION_TIME_CONFLICT(HttpStatus.CONFLICT, "해당 시간에 이미 예약이 있습니다."),
    RESERVATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 예약만 취소할 수 있습니다."),
    RESERVATION_UPDATE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "본인의 예약만 수정할 수 있습니다."),
    RESERVATION_ALREADY_CANCELLED(HttpStatus.CONFLICT, "이미 취소된 예약입니다."),
    RESERVATION_PAST_DATE(HttpStatus.BAD_REQUEST, "과거 날짜에는 예약할 수 없습니다."),
    INVALID_TIME_RANGE(HttpStatus.BAD_REQUEST, "시작 시간은 종료 시간보다 빨라야 합니다."),
    INVALID_TIME_PARAMETER(HttpStatus.BAD_REQUEST, "종료 시간만 단독으로 입력할 수 없습니다."),

    // Member / Auth
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),
    USERNAME_DUPLICATE(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),

    // Common
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    DATA_INTEGRITY_VIOLATION(HttpStatus.CONFLICT, "요청을 처리할 수 없습니다. 입력값을 확인해주세요."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}

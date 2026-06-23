package com.meeting.meetingapi.repository;

import com.meeting.meetingapi.domain.entity.Member;
import com.meeting.meetingapi.domain.entity.Reservation;
import com.meeting.meetingapi.domain.entity.Room;
import com.meeting.meetingapi.domain.enums.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByMember(Member member);

    List<Reservation> findByRoomAndDateAndStatus(Room room, LocalDate date, ReservationStatus status);

    boolean existsByRoomAndStatus(Room room, ReservationStatus status);

    @Query("SELECT r FROM Reservation r WHERE r.room.id = :roomId " +
           "AND r.date = :date " +
           "AND r.status = 'CONFIRMED' " +
           "AND r.startTime < :endTime " +
           "AND r.endTime > :startTime")
    List<Reservation> findOverlapping(@Param("roomId") Long roomId,
                                      @Param("date") LocalDate date,
                                      @Param("startTime") LocalTime startTime,
                                      @Param("endTime") LocalTime endTime);

    List<Reservation> findByDateAndStatus(LocalDate date, ReservationStatus status);

    long countByMember(Member member);

    long countByRoomAndStatus(Room room, ReservationStatus status);

    long countByRoomAndStatusAndDateBetween(Room room, ReservationStatus status,
                                            LocalDate startDate, LocalDate endDate);

    @Query("SELECT r FROM Reservation r " +
           "WHERE (:date IS NULL OR r.date = :date) " +
           "AND (:roomId IS NULL OR r.room.id = :roomId)")
    Page<Reservation> findWithFilter(@Param("date") LocalDate date,
                                     @Param("roomId") Long roomId,
                                     Pageable pageable);
}

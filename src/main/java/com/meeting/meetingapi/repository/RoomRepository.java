package com.meeting.meetingapi.repository;

import com.meeting.meetingapi.domain.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByNameContainingIgnoreCase(String name);
    boolean existsByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Room r WHERE r.id = :id")
    Optional<Room> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (" +
           "SELECT res.room.id FROM Reservation res " +
           "WHERE res.date = :date " +
           "AND res.status = 'CONFIRMED' " +
           "AND res.startTime < :endTime " +
           "AND res.endTime > :startTime)")
    List<Room> findAvailableRooms(@Param("date") LocalDate date,
                                  @Param("startTime") LocalTime startTime,
                                  @Param("endTime") LocalTime endTime);

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (" +
           "SELECT res.room.id FROM Reservation res " +
           "WHERE res.date = :date " +
           "AND res.status = 'CONFIRMED')")
    List<Room> findAvailableRoomsByDate(@Param("date") LocalDate date);

    @Query("SELECT r FROM Room r WHERE r.id NOT IN (" +
           "SELECT res.room.id FROM Reservation res " +
           "WHERE res.date = :date " +
           "AND res.status = 'CONFIRMED' " +
           "AND res.endTime > :startTime)")
    List<Room> findAvailableRoomsFromTime(@Param("date") LocalDate date,
                                          @Param("startTime") LocalTime startTime);
}

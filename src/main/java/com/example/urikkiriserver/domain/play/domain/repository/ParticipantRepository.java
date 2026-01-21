package com.example.urikkiriserver.domain.play.domain.repository;

import com.example.urikkiriserver.domain.play.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByRoomIdIdAndUserIdId(Long roomId, Long userId);

    @Query("SELECT p FROM Participant p JOIN FETCH p.userId WHERE p.roomId.id = :roomId AND p.userId.id = :userId")
    Optional<Participant> findByRoomIdIdAndUserIdId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    List<Participant> findAllByRoomIdId(Long roomId);

    int countByRoomIdId(Long roomId);


    @Query("SELECT p FROM Participant p JOIN FETCH p.userId WHERE p.roomId.id = :roomId")
    List<Participant> findAllByRoomIdIdWithUser(@Param("roomId") Long roomId);
}

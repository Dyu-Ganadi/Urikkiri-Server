package com.example.urikkiriserver.domain.play.domain.repository;

import com.example.urikkiriserver.domain.play.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByRoomIdIdAndUserIdId(Long roomId, Long userId);

    Optional<Participant> findByRoomIdIdAndUserIdId(Long roomId, Long userId);

    List<Participant> findAllByRoomIdId(Long roomId);

    int countByRoomIdId(Long roomId);

    @Query("SELECT p FROM Participant p JOIN FETCH p.userId WHERE p.roomId.id = :roomId")
    List<Participant> findAllByRoomIdIdWithUser(@Param("roomId") Long roomId);
}

package com.example.urikkiriserver.domain.play.domain.repository;

import com.example.urikkiriserver.domain.play.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    boolean existsByRoomIdIdAndUserIdId(Long roomId, Long userId);

    List<Participant> findAllByRoomIdId(Long roomId);
}
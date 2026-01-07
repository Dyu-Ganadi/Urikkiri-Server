package com.example.urikkiriserver.domain.play.domain.repository;

import com.example.urikkiriserver.domain.play.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
}
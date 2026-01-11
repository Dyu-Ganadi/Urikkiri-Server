package com.example.urikkiriserver.domain.play.domain.repository;

import com.example.urikkiriserver.domain.play.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);
}
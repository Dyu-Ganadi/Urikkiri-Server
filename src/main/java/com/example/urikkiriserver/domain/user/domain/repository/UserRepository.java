package com.example.urikkiriserver.domain.user.domain.repository;

import com.example.urikkiriserver.domain.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);
    
    List<User> findAllByOrderByBananaxpDesc();
}

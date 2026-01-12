package com.example.urikkiriserver.domain.user.domain.repository;

import com.example.urikkiriserver.domain.user.domain.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);

    Page<User> findAllByOrderByBananaxpDescIdAsc(Pageable pageable);

    @Query("""
        SELECT u FROM tbl_user u
        ORDER BY u.bananaxp DESC, u.id ASC
    """)
    List<User> findFirstRankings(Pageable pageable);

    @Query("""
        SELECT u FROM tbl_user u
        WHERE
            (u.bananaxp < :lastBananaxp)
            OR (u.bananaxp = :lastBananaxp AND u.id > :lastUserId)
        ORDER BY u.bananaxp DESC, u.id ASC
    """)
    List<User> findNextRankings(
            @Param("lastBananaxp") Integer lastBananaxp,
            @Param("lastUserId") Long lastUserId,
            Pageable pageable
    );
}

package com.example.urikkiriserver.domain.card.domain.repository;

import com.example.urikkiriserver.domain.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    @Query(value = "SELECT * FROM tbl_card ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Card> findRandomCards(int count);
}

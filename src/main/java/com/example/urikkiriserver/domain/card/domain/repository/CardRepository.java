package com.example.urikkiriserver.domain.card.domain.repository;

import com.example.urikkiriserver.domain.card.domain.Card;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {
}

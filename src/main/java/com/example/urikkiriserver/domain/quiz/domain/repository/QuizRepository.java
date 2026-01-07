package com.example.urikkiriserver.domain.quiz.domain.repository;

import com.example.urikkiriserver.domain.quiz.domain.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
}

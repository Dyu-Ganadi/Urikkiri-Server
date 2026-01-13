package com.example.urikkiriserver.domain.play.domain;

import com.example.urikkiriserver.domain.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "tbl_participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room roomId;

    @Column(nullable = false)
    private int bananaScore;

    @Column(nullable = false)
    private boolean isExaminer;

    public void winGame() {
        this.bananaScore += 1; // TODO: SCORE_PER_WIN 상수로 변경
    }
}

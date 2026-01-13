package com.example.urikkiriserver.domain.user.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity(name = "tbl_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(255)")
    private String email;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(15)")
    private String nickname;

    @Column(nullable = false, columnDefinition = "CHAR(60)")
    private String password;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer level;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer bananaxp;

    public void addXp(int reward) {
        this.bananaxp += reward;
        updateLevel();
    }

    private void updateLevel() {
        if (this.bananaxp <= 10) {
            this.level = 1;
        } else if (this.bananaxp <= 20) {
            this.level = 2;
        } else if (this.bananaxp <= 30) {
            this.level = 3;
        } else {
            this.level = 4;
        }
    }
}

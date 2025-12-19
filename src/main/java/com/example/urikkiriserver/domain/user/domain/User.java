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

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(30)")
    private String email;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(8)")
    private String nickname;

    @Column(nullable = false, columnDefinition = "CHAR(60)")
    private String password;
}

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

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR2(254)")
    private String email;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR2(15)")
    private String nickname;

    @Column(nullable = false, columnDefinition = "CHAR(64)")
    private String password;
}

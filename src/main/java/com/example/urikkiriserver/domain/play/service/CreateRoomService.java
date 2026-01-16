package com.example.urikkiriserver.domain.play.service;

import com.example.urikkiriserver.domain.play.domain.Participant;
import com.example.urikkiriserver.domain.play.domain.Room;
import com.example.urikkiriserver.domain.play.domain.repository.ParticipantRepository;
import com.example.urikkiriserver.domain.play.domain.repository.RoomRepository;
import com.example.urikkiriserver.domain.play.presentation.dto.response.RoomResponse;
import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.global.websocket.dto.ParticipantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CreateRoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final Random random = new Random();

    @Transactional
    public RoomResponse execute(User user) {
        return createRoom(user);
    }

    private RoomResponse createRoom(User user) {
        String roomCode = generateRoomCode();

        Room savedRoom = roomRepository.save(
            Room.builder()
                .code(roomCode)
                .build()
        );

        participantRepository.save(Participant.builder()
            .userId(user)
            .roomId(savedRoom)
            .bananaScore(0)
            .isExaminer(true)
            .build());

        // 방장 정보를 포함한 참가자 목록 생성
        List<ParticipantInfo> participants = List.of(
            ParticipantInfo.of(
                user.getId(),
                user.getNickname(),
                user.getLevel(),
                true  // 방장이므로 examiner = true
            )
        );

        return RoomResponse.of(roomCode, participants);
    }

    private String generateRoomCode() {
        String roomCode;
        do {
            // 100000 ~ 999999 범위의 6자리 숫자 생성
            roomCode = String.format("%06d", random.nextInt(900000) + 100000);
        } while (roomRepository.existsByCode(roomCode));

        return roomCode;
    }
}

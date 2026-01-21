package com.example.urikkiriserver.domain.play.service;

import com.example.urikkiriserver.domain.play.domain.Participant;
import com.example.urikkiriserver.domain.play.domain.Room;
import com.example.urikkiriserver.domain.play.domain.repository.ParticipantRepository;
import com.example.urikkiriserver.domain.play.domain.repository.RoomRepository;
import com.example.urikkiriserver.domain.play.exception.RoomAlreadyFullException;
import com.example.urikkiriserver.domain.play.exception.RoomNotFoundException;
import com.example.urikkiriserver.domain.play.presentation.dto.response.JoinRoomResponse;
import com.example.urikkiriserver.domain.user.domain.User;
import com.example.urikkiriserver.global.websocket.dto.ParticipantInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JoinRoomService {

    private final RoomRepository roomRepository;
    private final ParticipantRepository participantRepository;

    private static final int MAX_PARTICIPANTS = 4;

    @Transactional
    public JoinRoomResponse execute(String roomCode, User user) {
        // 1. 방이 존재하는지 확인
        Room room = roomRepository.findByCode(roomCode)
            .orElseThrow(() -> RoomNotFoundException.EXCEPTION);

        // 2. 사용자가 이미 다른 방에 참가 중인 경우 자동으로 제거
        List<Participant> existingParticipants = participantRepository.findAll().stream()
                .filter(p -> p.getUserId().getId().equals(user.getId()))
                .toList();

        if (!existingParticipants.isEmpty()) {
            participantRepository.deleteAll(existingParticipants);
        }

        // 3. 방이 꽉 찼는지 확인
        int currentParticipants = participantRepository.countByRoomIdId(room.getId());
        if (currentParticipants >= MAX_PARTICIPANTS) {
            throw RoomAlreadyFullException.EXCEPTION;
        }

        // 4. 참가자 추가 (시험관이 아닌 일반 참가자로 추가)
        participantRepository.save(Participant.builder()
            .userId(user)
            .roomId(room)
            .bananaScore(0)
            .isExaminer(false)
            .build());


        // 5. 전체 참가자 목록 조회 (User를 Eager Fetch)
        List<ParticipantInfo> participants = participantRepository.findAllByRoomIdIdWithUser(room.getId())
            .stream()
            .map(ParticipantInfo::from)
            .toList();

        return JoinRoomResponse.of(room, participants);
    }
}

package com.example.urikkiriserver.domain.play.presentation;

import com.example.urikkiriserver.domain.play.service.CreateRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/play-together")
@RequiredArgsConstructor
public class RoomController {

    private final CreateRoomService createRoomService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom() {
        return createRoomService.execute();
    }
}


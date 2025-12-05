package com.example.urikkiriserver.component;

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    @Override
    public void afterConnectionEstablished(@Nullable WebSocketSession session) {
        // 밑에 주석 처리된 코드는 다음 코드와 동일: @AuthenticationPrincipal User user
        // User user = (User) session.getAttributes().get("userPrincipal");
        // log.info("{} has been connected", user);
        // TODO: 추후 세션을 방별로 보관한 뒤 단체 메시지 발송 가능
    }

    @Override
    public void afterConnectionClosed(@Nullable WebSocketSession session, @NonNull CloseStatus status) {
        // 밑에 주석 처리된 코드는 다음 코드와 동일: @AuthenticationPrincipal User user
        // User user = (User) session.getAttributes().get("userPrincipal");
        // log.info("{} has been disconnected", user);
    }
}

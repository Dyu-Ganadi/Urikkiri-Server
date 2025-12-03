package com.example.urikkiriserver.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Arrays;

public class WebSocketUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 오브젝트를 JSON으로 변환해서 세션에 보내주는 함수
     * @param o 전송할 오브젝트
     * @param sessions 오브젝트를 전송할 각각의 세션들
     * @see WebSocketSession
     * @see WebSocketUtils#sendToEachSocket(TextMessage, WebSocketSession...)
     * */
    public static void sendObject(Object o, WebSocketSession... sessions) {
        try {
            sendToEachSocket(new TextMessage(objectMapper.writeValueAsString(o)), sessions);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * string값을 세션에 전송하는 함수
     * @param message 얘 타입이 {@link TextMessage}이긴 한데 그냥 {@code new TextMessage(String)} 해버리면 변환되니까 그냥 쓸 수 있음
     * @param sessions 오브젝트를 전송할 각각의 세션들
     * @see WebSocketSession
     * @see TextMessage
     * */
    public static void sendToEachSocket(TextMessage message, WebSocketSession... sessions) {
        Arrays.stream(sessions).forEach(roomSession -> {
            try {
                roomSession.sendMessage(message);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}

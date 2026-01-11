package com.example.urikkiriserver.global.websocket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketMessage(
        @JsonProperty("type") WebSocketMessageType type,
        @JsonProperty("roomCode") String roomCode,
        @JsonProperty("data") Object data,
        @JsonProperty("message") String message
) {
    public static WebSocketMessage of(WebSocketMessageType type, String message) {
        return new WebSocketMessage(type, null, null, message);
    }

    public static WebSocketMessage of(WebSocketMessageType type, String roomCode, String message) {
        return new WebSocketMessage(type, roomCode, null, message);
    }

    public static WebSocketMessage withData(WebSocketMessageType type, String roomCode, Object data, String message) {
        return new WebSocketMessage(type, roomCode, data, message);
    }
}


package com.example.urikkiriserver.global.websocket;

/**
 * WebSocket 클라이언트의 타입을 구분
 * LOBBY: 방 생성/참여 등 로비 기능을 사용하는 프론트엔드 클라이언트
 * GAME: 실제 게임 플레이를 위한 게임 클라이언트
 */
public enum ClientType {
    LOBBY,      // 프론트엔드 (방 생성, 참여)
    GAME        // 게임 서버 또는 게임 플레이 클라이언트
}


# 로비-게임 분리 아키텍처 변경 사항

## 📋 변경 요약

기존의 단일 WebSocket 연결 방식에서 **로비 연결**과 **게임 연결**을 분리하는 아키텍처로 변경되었습니다.

---

## 🎯 변경 이유

1. **관심사의 분리**: 방 생성/참여 로직과 실제 게임 플레이 로직을 명확히 구분
2. **확장성**: 향후 로비 서버와 게임 서버를 물리적으로 분리 가능
3. **클라이언트 구분**: 프론트엔드(로비)와 게임 클라이언트를 독립적으로 관리

---

## 🔄 플로우 비교

### 이전 (v1.x)
```
클라이언트 → WebSocket 연결 → 방 생성/참여 → 4명 모임 → 즉시 GAME_START
```

### 현재 (v2.0)
```
클라이언트 → 로비 WebSocket 연결 (유지) → 방 생성/참여 → 4명 모임 
    → GAME_READY 수신 
    → Unity가 별도의 WebSocket 연결 생성 (CONNECT_GAME)
    → 모든 플레이어 연결 확인
    → GAME_START
```
**중요**: 로비 연결은 유지되고, Unity는 별도의 새로운 연결을 생성합니다.

---

## 📦 신규 파일

### 1. `ClientType.java`
```java
public enum ClientType {
    LOBBY,      // 로비 클라이언트
    GAME        // 게임 클라이언트
}
```

### 2. `GameReadyData.java`
4명이 모였을 때 클라이언트에게 전달되는 데이터
- 참가자 목록
- Unity 게임 실행 안내 메시지

### 3. `GAME_CONNECTION_FLOW.md`
완전한 연결 플로우와 클라이언트 구현 가이드

---

## 🔧 수정된 파일

### 1. `WebSocketMessageType.java`
새로운 메시지 타입 추가:
- `GAME_READY`: 4명 모임 알림
- `CONNECT_GAME`: 게임 서버 연결 요청
- `GAME_START`: 게임 시작 (4명 모두 연결 완료 시, 질문 포함)

**주의**: `GAME_CONNECTED` 메시지는 제거되었습니다. 4명이 모두 연결되면 바로 `GAME_START`가 전송됩니다.

### 2. `WebSocketSessionManager.java`
세션 관리 방식 변경:
- `lobbyRoomSessions`: 로비 클라이언트 세션 관리
- `gameRoomSessions`: 게임 클라이언트 세션 관리
- `addLobbySession()`: 로비 세션 추가
- `addGameSession()`: 게임 세션 추가
- `getLobbySessionsByRoom()`: 로비 세션 조회
- `getGameSessionsByRoom()`: 게임 세션 조회

### 3. `WebSocketHandler.java`

#### 새로운 핸들러 메서드:
- `handleConnectGame()`: 게임 서버 연결 요청 처리
- `startGameForConnectedPlayers()`: 모든 플레이어 연결 후 게임 시작

#### 수정된 로직:
- `handleCreateRoom()`: 방장 정보를 포함한 참가자 목록 반환
- `handleJoinRoom()`: 기존 유저 + 새 유저 정보를 모두 포함한 전체 참가자 목록 브로드캐스트
- `handleJoinRoom()`: 4명 모이면 `GAME_READY` 전송 (기존 `GAME_START` 대신)
- `handleLeaveRoom()`: roomCode와 userId로 특정 유저만 삭제, 모든 유저가 나가면 participantRepository에서 해당 방의 유저 정보 모두 삭제
- `handleSubmitCard()`: 게임 세션으로만 메시지 전송
- `handleExaminerSelect()`: 게임 세션으로만 메시지 전송
- `endGame()`: 게임 세션으로만 메시지 전송

---

## 📡 메시지 플로우

### 로비 단계 (Lobby Connection)

```
1. 클라이언트 → 서버: WebSocket 연결 (토큰)
2. 서버 → 클라이언트: CONNECTED

3. 클라이언트 → 서버: CREATE_ROOM
4. 서버 → 클라이언트: ROOM_CREATED (roomCode, participants[방장])

5. 다른 클라이언트 → 서버: JOIN_ROOM (roomCode)
6. 서버 → 모든 클라이언트: USER_JOINED (전체 participants: 기존 유저 + 새 유저)

[4명 모임]

8. 서버 → 모든 프론트엔드: GAME_READY
   {
     "participants": [...]
   }
```

### 게임 단계 (Game Connection)

```
9. Unity → 서버: 새 WebSocket 연결 (토큰)
10. 서버 → Unity: CONNECTED

11. Unity → 서버: CONNECT_GAME (roomCode)
12. 서버: 토큰으로 유저 확인 → 게임 세션에 추가

[4명의 Unity가 모두 CONNECT_GAME 완료]

13. 서버 → 모든 Unity: GAME_START
    {
      "participants": [...],
      "question": {...}
    }

14. 게임 진행 (SUBMIT_CARD, EXAMINER_SELECT, NEXT_ROUND, ROUND_END)
```

---

## 🔑 핵심 개념

### 세션 분리
- **로비 세션**: 방 생성/참여, 대기 중인 플레이어 관리
- **게임 세션**: 실제 게임 플레이, 카드 제출, 점수 관리

### 별도 연결 프로세스
1. 로비에서 4명이 모이면 `GAME_READY` 수신
2. 프론트엔드는 로비 연결 유지 (닫지 않음!)
3. Unity 게임을 실행하고 토큰+방코드 전달
4. Unity가 게임 서버 URL로 **새로운** WebSocket 연결 생성
5. Unity가 `CONNECT_GAME` 메시지로 방 코드 전달
6. 서버가 게임 세션에 추가
7. 모든 플레이어 연결 확인 후 `GAME_START`

### 브로드캐스트 타겟 변경
- **로비 메시지**: `sessionManager.getLobbySessionsByRoom(roomCode)`
- **게임 메시지**: `sessionManager.getGameSessionsByRoom(roomCode)`

---

## 🌐 환경 변수

게임 서버 URL을 환경 변수로 설정 가능:

```bash
GAME_SERVER_WS_URL=ws://your-game-server.com/ws
```

설정하지 않으면 기본값: `ws://localhost:8080/ws`

---

## 💻 클라이언트 구현 예시

### React/TypeScript 예시

```typescript
class GameClient {
    private lobbyWs: WebSocket | null = null;
    private gameWs: WebSocket | null = null;
    
    // 로비 연결
    connectToLobby(token: string) {
        this.lobbyWs = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
        
        this.lobbyWs.onmessage = (event) => {
            const message = JSON.parse(event.data);
            
            if (message.type === 'GAME_READY') {
                // 로비 연결 유지 (닫지 않음!)
                // Unity 게임에 토큰과 방코드 전달
                this.launchUnityGame(token, message.roomCode);
            }
        };
    }

    // Unity 게임 실행
    launchUnityGame(token: string, roomCode: string) {
        // Unity 게임에 토큰과 방코드 전달
        if (window.unityInstance) {
            unityInstance.SendMessage('GameManager', 'ConnectToServer',
                JSON.stringify({
                    token: token,
                    roomCode: message.roomCode,
                    participants: message.data.participants
                    roomCode: roomCode,
            );
        }
    }
}
```

---

## 📚 문서

### 새로운 문서
- **[GAME_CONNECTION_FLOW.md](../docs/api/GAME_CONNECTION_FLOW.md)**: 완전한 연결 플로우 가이드

### 업데이트된 문서
- **[README.md](../docs/api/README.md)**: 메시지 타입 목록 및 플로우 업데이트

---

## ✅ 호환성

### 레거시 지원
기존 코드와의 호환성을 위해 다음 메서드는 유지됩니다:
- `addSession()` → `addLobbySession()` 호출
- `getSessionsByRoom()` → `getLobbySessionsByRoom()` 호출

### 마이그레이션 가이드

**기존 클라이언트가 해야 할 변경사항:**

1. `GAME_START` 대신 `GAME_READY` 핸들링 추가
2. `GAME_READY` 수신 시 로비 연결 유지하고 Unity 게임 실행
3. Unity에서 별도의 WebSocket 연결 생성
4. Unity가 `CONNECT_GAME` 메시지 전송

---

## 🧪 테스트

### 빌드 확인
```bash
./gradlew clean build -x test
# ✅ BUILD SUCCESSFUL
```

### 테스트 시나리오

1. **로비 플로우**
   - WebSocket 연결
   - 방 생성
   - 3명 추가 참여
   - GAME_READY 수신 확인

2. **게임 플로우**
   - Unity 게임 서버 연결
   - CONNECT_GAME 전송
   - 4명 모두 연결 후 GAME_START 수신 (GAME_CONNECTED 없음)

3. **게임 진행**
   - 카드 제출
   - 출제자 선택
   - 다음 라운드
   - 게임 종료

---

## 🚀 향후 확장 가능성

1. **물리적 서버 분리**
   - 로비 서버: 방 관리, 매칭
   - 게임 서버: 게임 로직 처리

2. **로드 밸런싱**
   - 여러 게임 서버로 부하 분산
   - GAME_READY에서 동적으로 게임 서버 URL 할당

3. **게임 서버 클러스터링**
   - Redis Pub/Sub으로 게임 상태 동기화
   - 게임 서버 장애 시 페일오버

---

## 📞 문의

구현에 대한 질문이나 피드백은 이슈로 남겨주세요.


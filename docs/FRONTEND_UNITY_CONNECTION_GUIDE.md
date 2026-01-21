# ✅ 프론트엔드-Unity 게임 연결 구조 최종 정리

## 🎯 핵심 개념

**프론트엔드와 Unity 게임은 같은 서버에 각각 별도의 WebSocket 연결을 맺습니다.**

```
                      같은 서버
                   (ws://localhost:8080/ws)
                          |
        +----------------+----------------+
        |                                 |
   [프론트엔드]                        [Unity 게임]
   로비 연결 유지                     게임 플레이 전용
```

---

## 📋 전체 플로우

### 1단계: 프론트엔드 로비 (방 생성/참여)
```
프론트엔드 → 서버: WebSocket 연결 (토큰)
프론트엔드 → 서버: CREATE_ROOM or JOIN_ROOM
프론트엔드 ← 서버: ROOM_CREATED or USER_JOINED
```

### 2단계: 4명 모임 → GAME_READY
```
[4명째 입장]
모든 프론트엔드 ← 서버: GAME_READY
```

**GAME_READY 메시지:**
```json
{
  "type": "GAME_READY",
  "roomCode": "123456",
  "data": {
    "participants": [...]
  },
  }
```

### 3단계: Unity 게임 실행 (로비 연결 유지)
```
프론트엔드: 로비 WebSocket 연결 유지
프론트엔드: Unity 게임 실행
프론트엔드 → Unity: 토큰 + 방코드 전달
```

**프론트엔드 코드:**
```javascript
if (message.type === 'GAME_READY') {
    console.log('게임 준비 완료! Unity 게임을 실행합니다...');
    
    // ⚠️ lobbyWs는 닫지 않고 유지!
    launchUnityGame({
        token: localStorage.getItem('accessToken'),
        roomCode: message.roomCode,
        serverUrl: 'ws://localhost:8080/ws'  // 같은 서버
    });
}

### 4단계: Unity가 서버에 새 WebSocket 연결 (로비와 별개)
```
Unity → 서버: 새로운 WebSocket 연결 (토큰 사용)
Unity → 서버: CONNECT_GAME (방코드)
Unity ← 서버: 연결 확인
```

**Unity가 보내는 CONNECT_GAME 메시지:**
```json
{
  "type": "CONNECT_GAME",
  "roomCode": "123456"
}
```

**중요**: Unity는 **프론트엔드와 별개의 WebSocket 연결**을 만듭니다. 같은 서버 URL을 사용하지만 완전히 독립적인 세션입니다.

### 5단계: 4명 모두 Unity 연결 완료 → 게임 시작
```
[마지막 플레이어 Unity 연결]
모든 Unity ← 서버: GAME_START (질문 포함)
```

**GAME_START 메시지:**
```json
{
  "type": "GAME_START",
  "roomCode": "123456",
  "data": {
    "participants": [...],
    "question": {
      "quizId": 42,
      "content": "가장 좋아하는 음식은?"
    }
  }
}
```

### 6단계: 게임 진행
```
Unity → 서버: SUBMIT_CARD
Unity ← 서버: ALL_CARDS_SUBMITTED (출제자에게만)
Unity → 서버: EXAMINER_SELECT
Unity ← 서버: EXAMINER_SELECTED
Unity ← 서버: NEXT_ROUND or ROUND_END
```

### 7단계: 게임 종료
```
Unity ← 서버: ROUND_END (최종 순위)
Unity: 연결 종료
프론트엔드: 로비로 복귀 (로비 연결은 계속 유지되어 있음)
```

---

## 🔑 중요 포인트

### 1. 두 개의 독립적인 연결
- **프론트엔드 연결**: 로비 기능 (방 생성/참여/목록)
- **Unity 연결**: 게임 플레이 (카드 제출/점수)

### 2. 같은 서버, 다른 세션
- 서버 URL은 동일: `ws://localhost:8080/ws`
- 하지만 WebSocket 세션은 별개
- 서버는 토큰으로 같은 유저임을 인식

### 3. 프론트엔드 연결 유지
- 프론트엔드는 게임 중에도 로비 연결 유지
- 게임 종료 후 로비로 바로 복귀 가능
- Unity 연결만 종료됨

### 4. 세션 관리
서버는 내부적으로 두 가지 세션 맵을 관리:
- `lobbyRoomSessions`: 프론트엔드 연결
- `gameRoomSessions`: Unity 게임 연결

---

## 📊 상태 다이어그램

```
[프론트엔드]                    [서버]                    [Unity]
    |                           |                           |
    |--WebSocket 연결 (토큰)-->  |                           |
    |<--CONNECTED---------------|                           |
    |                           |                           |
    |--CREATE_ROOM------------>  |                           |
    |<--ROOM_CREATED-----------|                           |
    |                           |                           |
    |--JOIN_ROOM (x3)--------->  |                           |
    |<--USER_JOINED (x3)-------|                           |
    |                           |                           |
    [4명 모임]                   |                           |
    |<--GAME_READY-------------|                           |
    |                           |                           |
    |--Unity 실행 (토큰+방코드)-> |                          |
    |                           |<--WebSocket 연결 (토큰)--|
    |                           |                          |
    |                           |<--CONNECT_GAME----------|
    |                           |   (토큰에서 유저 확인)    |
    |                           |   (게임 세션에 추가)     |
    |                           |                          |
    |                           [4명 모두 Unity 연결]      |
    |                           |                          |
    |                           |--GAME_START------------->|
    |                           |   (질문 포함)            |
    |                           |                          |
    |  [로비 연결 유지]           |<--SUBMIT_CARD-----------|
    |                           |--ALL_CARDS_SUBMITTED---->|
    |                           |<--EXAMINER_SELECT-------|
    |                           |--EXAMINER_SELECTED------>|
    |                           |                          |
    |                           |--ROUND_END-------------->|
    |<-게임 종료 알림------------|   [Unity 연결 종료]
    |                           |
    [로비 화면으로 복귀]          |
```

---

## 💻 코드 예시

### 프론트엔드 (React)
```jsx
function GameLobby() {
    const [lobbyWs, setLobbyWs] = useState(null);
    const [unityInstance, setUnityInstance] = useState(null);

    useEffect(() => {
        // 로비 WebSocket 연결
        const token = localStorage.getItem('accessToken');
        const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
        
        ws.onmessage = (event) => {
            const message = JSON.parse(event.data);
            
            if (message.type === 'GAME_READY') {
                // Unity 게임 실행
                launchUnityGame({
                    token: token,
                    roomCode: message.roomCode,
                    serverUrl: 'ws://localhost:8080/ws'
                });
            }
        };
        
        setLobbyWs(ws);
        
        // ⚠️ cleanup에서 로비 연결은 유지
        return () => {
            // 컴포넌트 언마운트 시에만 닫기
        };
    }, []);

    function launchUnityGame(config) {
        // Unity WebGL 로드
        createUnityInstance(canvas, unityConfig).then((instance) => {
            setUnityInstance(instance);
            
            // Unity에 설정 전달
            instance.SendMessage(
                'GameManager', 
                'ConnectToServer', 
                JSON.stringify(config)
            );
        });
    }

    return (
        <div>
            <button onClick={() => lobbyWs.send(JSON.stringify({type: 'CREATE_ROOM'}))}>
                방 만들기
            </button>
            <canvas id="unity-canvas"></canvas>
        </div>
    );
}
```

### Unity (C#)
```csharp
public class GameServerConnection : MonoBehaviour
{
    private WebSocket websocket;
    
    // 프론트엔드에서 호출됨
    public void ConnectToServer(string configJson)
    {
        var config = JsonUtility.FromJson<ServerConfig>(configJson);
        StartCoroutine(ConnectToGameServer(config));
    }
    
    IEnumerator ConnectToGameServer(ServerConfig config)
    {
        // 새 WebSocket 연결 (프론트엔드와는 별개)
        websocket = new WebSocket($"{config.serverUrl}?token={config.token}");
        
        websocket.OnOpen += () =>
        {
            Debug.Log("✅ 게임 서버 연결");
            
            // CONNECT_GAME 전송
            var msg = new { type = "CONNECT_GAME", roomCode = config.roomCode };
            websocket.SendText(JsonUtility.ToJson(msg));
        };
        
        websocket.OnMessage += (bytes) =>
        {
            var message = System.Text.Encoding.UTF8.GetString(bytes);
            var msg = JsonUtility.FromJson<GameMessage>(message);
            
            switch (msg.type)
            {
                case "GAME_START":
                    OnGameStart(msg.data);
                    break;
                case "CARD_SUBMITTED":
                    OnCardSubmitted();
                    break;
                case "EXAMINER_SELECTED":
                    OnExaminerSelected(msg.data);
                    break;
                case "ROUND_END":
                    OnGameEnd(msg.data);
                    break;
            }
        };
        
        yield return websocket.Connect();
    }
    
    public void SubmitCard(int cardId)
    {
        var msg = new {
            type = "SUBMIT_CARD",
            roomCode = currentRoomCode,
            data = new { cardId = cardId }
        };
        websocket.SendText(JsonUtility.ToJson(msg));
    }
}
```

---

## 🔧 서버 구현 포인트

### WebSocketSessionManager
```java
// 두 가지 세션 맵 관리
private Map<String, Set<WebSocketSession>> lobbyRoomSessions;  // 프론트엔드
private Map<String, Set<WebSocketSession>> gameRoomSessions;   // Unity
```

### WebSocketHandler
```java
// 1. JOIN_ROOM: 프론트엔드 로비 세션에 추가
sessionManager.addLobbySession(roomCode, session);

// 2. 4명 모이면: GAME_READY 전송
if (participants.size() == 4) {
    sessionManager.getLobbySessionsByRoom(roomCode)
        .forEach(s -> sendMessage(s, gameReadyMessage));
}

// 3. CONNECT_GAME: Unity 게임 세션에 추가
sessionManager.addGameSession(roomCode, session);

// 4. 4명 Unity 연결: GAME_START 전송
if (gameSessions.size() == participants.size()) {
    sessionManager.getGameSessionsByRoom(roomCode)
        .forEach(s -> sendMessage(s, gameStartMessage));
}

// 5. 게임 중: 게임 세션으로만 메시지 전송
sessionManager.getGameSessionsByRoom(roomCode)
    .forEach(s -> sendMessage(s, gameMessage));
```

---

## ✅ 체크리스트

### 프론트엔드 개발자
- [ ] 로비 WebSocket 연결 구현
- [ ] GAME_READY 수신 처리
- [ ] Unity 게임 실행 및 설정 전달
- [ ] 로비 연결 유지 (게임 중에도)
- [ ] 게임 종료 후 로비 복귀

### Unity 개발자
- [ ] 프론트엔드에서 설정 수신
- [ ] 새 WebSocket 연결 구현
- [ ] CONNECT_GAME 전송
- [ ] GAME_START 수신 및 게임 시작
- [ ] 게임 메시지 송수신
- [ ] 게임 종료 시 연결 종료

### 백엔드 개발자
- [x] 로비/게임 세션 분리 관리
- [x] GAME_READY 메시지 구현
- [x] CONNECT_GAME 핸들러 구현
- [x] 게임 메시지 라우팅 (게임 세션으로만)
- [x] 빌드 성공 확인

---

## 📚 관련 문서

- [게임 연결 플로우 상세 가이드](./GAME_CONNECTION_FLOW.md)
- [WebSocket API 명세서](./README.md)
- [아키텍처 변경 사항](../ARCHITECTURE_CHANGES.md)

---

**작성일**: 2026년 1월 14일  
**빌드 상태**: ✅ 성공  
**테스트 상태**: 수동 테스트 필요


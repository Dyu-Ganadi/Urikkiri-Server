# 🚀 빠른 시작 가이드

5분 안에 우리끼리 게임 WebSocket API를 시작하세요!

---

## 1단계: 로그인 & 토큰 받기 (30초)

### REST API로 로그인

```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "1234"
  }'
```

**응답**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

토큰을 복사하세요! 이제 WebSocket 연결에 사용합니다.

---

## 2단계: WebSocket 연결 (30초)

### 프론트엔드 (JavaScript)

```javascript
const token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

ws.onopen = () => {
    console.log('✅ 연결 성공!');
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('📨 받음:', message);
};

ws.onerror = (error) => {
    console.error('❌ 에러:', error);
};
```

### Unity (C#)

```csharp
using NativeWebSocket;

string token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
WebSocket ws = new WebSocket($"ws://localhost:8080/ws?token={token}");

ws.OnOpen += () => {
    Debug.Log("✅ 연결 성공!");
};

ws.OnMessage += (bytes) => {
    string message = System.Text.Encoding.UTF8.GetString(bytes);
    Debug.Log($"📨 받음: {message}");
};

await ws.Connect();
```

---

## 3단계: 방 만들기 (10초)

### 방 생성

```javascript
ws.send(JSON.stringify({
    type: 'CREATE_ROOM'
}));
```

**응답**:
```json
{
  "type": "ROOM_CREATED",
  "roomCode": "764185",
  "message": "Room created successfully"
}
```

**방 코드를 친구들과 공유하세요!** 📲

---

## 4단계: 방 참여 (10초)

### 다른 클라이언트에서 참여

```javascript
ws.send(JSON.stringify({
    type: 'JOIN_ROOM',
    roomCode: '764185'
}));
```

**응답**:
```json
{
  "type": "USER_JOINED",
  "roomCode": "764185",
  "data": [
    { "userId": 1, "nickname": "방장", "level": 5 },
    { "userId": 2, "nickname": "나", "level": 3 }
  ],
  "message": "나 joined the room"
}
```

---

## 5단계: 4명 모이면 게임 시작! (자동)

### 4명째가 입장하면...

**모든 프론트엔드가 받음**:
```json
{
  "type": "GAME_READY",
  "roomCode": "764185",
  "data": {
    "participants": [
      { "userId": 1, "nickname": "플레이어1", "isExaminer": true },
      { "userId": 2, "nickname": "플레이어2", "isExaminer": false },
      { "userId": 3, "nickname": "플레이어3", "isExaminer": false },
      { "userId": 4, "nickname": "플레이어4", "isExaminer": false }
    ]
  }
}
```

### 프론트엔드: Unity 실행

```javascript
ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.type === 'GAME_READY') {
        console.log('게임 준비 완료! Unity 게임을 실행합니다...');
        
        // ⚠️ 로비 연결은 유지 (닫지 않음!)
        // Unity 게임 실행!
        launchUnityGame({
            token: token,
            roomCode: message.roomCode,
            serverUrl: 'ws://localhost:8080/ws'
        });
    }
};

function launchUnityGame(config) {
    // Unity에 설정 전달
    if (window.unityInstance) {
        unityInstance.SendMessage('GameManager', 'ConnectToServer', JSON.stringify(config));
    }
}
```

### Unity: 게임 서버에 새로운 WebSocket 연결

Unity가 별도의 WebSocket 연결을 생성합니다:

```csharp
// Unity C# 코드
public void ConnectToServer(string configJson) {
    var config = JsonUtility.FromJson<ServerConfig>(configJson);
    StartCoroutine(ConnectToGameServer(config));
}

IEnumerator ConnectToGameServer(ServerConfig config) {
    websocket = new WebSocket($"{config.serverUrl}?token={config.token}");
    
    websocket.OnOpen += () => {
        Debug.Log("✅ 게임 서버 연결 성공");
        
        // CONNECT_GAME 전송
        var msg = new { type = "CONNECT_GAME", roomCode = config.roomCode };
        websocket.SendText(JsonUtility.ToJson(msg));
    };
    
    yield return websocket.Connect();
}
```

### 서버: 모든 Unity 연결 완료 → GAME_START

```csharp
// 프론트엔드에서 받은 설정
string token = config.token;
string roomCode = config.roomCode;

// 새 연결 생성
WebSocket gameWs = new WebSocket($"ws://localhost:8080/ws?token={token}");

gameWs.OnOpen += () => {
    // CONNECT_GAME 전송
    var connectMsg = new {
        type = "CONNECT_GAME",
## 6단계: Unity 게임 연결 (30초)
    };
    gameWs.SendText(JsonUtility.ToJson(connectMsg));
};
```

### 4명 Unity 연결 → GAME_START!

**모든 Unity가 받음**:
```json
{
  "type": "GAME_START",
  "roomCode": "764185",
  "data": {
    "participants": [...],
    "question": {
      "quizId": 42,
      "content": "가장 좋아하는 음식은?"
    }
  }
### Unity: 새 WebSocket 연결
```

---

## 7단계: 게임 플레이 (2분)

### 카드 제출 (출제자 제외 3명)

```csharp
gameWs.SendText(JsonUtility.ToJson(new {
    type = "SUBMIT_CARD",
    roomCode = roomCode,
    data = new { cardId = 123 }
}));
```

### 출제자가 선택

**출제자만 받음**:
```json
{
  "type": "ALL_CARDS_SUBMITTED",
  "data": [
    { "participantId": 2, "cardWord": "치킨" },
    { "participantId": 3, "cardWord": "피자" },
    { "participantId": 4, "cardWord": "떡볶이" }
  ]
}
```

**출제자가 선택**:
```csharp
gameWs.SendText(JsonUtility.ToJson(new {
    type = "EXAMINER_SELECT",
    roomCode = roomCode,
    data = new { participantId = 2 }
}));
```

### 결과 발표

**모든 Unity가 받음**:
```json
{
  "type": "EXAMINER_SELECTED",
  "data": {
    "selectedParticipantId": 2,
    "selectedCardWord": "치킨",
    "winnerNickname": "플레이어2",
    "newScore": 1
  }
}
```

---

## 8단계: 다음 라운드 또는 게임 종료

### 다음 라운드 (5점 미만)

```json
{
  "type": "NEXT_ROUND",
  "data": {
    "examinerId": 3,
    "examinerNickname": "플레이어3",
    "question": {
      "content": "가장 가고 싶은 여행지는?"
    }
  }
}
```

→ 7단계로 돌아가서 반복!

### 게임 종료 (5점 달성)

```json
{
  "type": "ROUND_END",
  "data": {
    "rankings": [
      { "userId": 2, "nickname": "플레이어2", "finalScore": 5, "rank": 1, "xpReward": 20 },
      { "userId": 3, "nickname": "플레이어3", "finalScore": 3, "rank": 2, "xpReward": 10 },
      { "userId": 1, "nickname": "플레이어1", "finalScore": 2, "rank": 3, "xpReward": 5 },
      { "userId": 4, "nickname": "플레이어4", "finalScore": 1, "rank": 4, "xpReward": 2 }
    ]
  }
}
```

**게임 끝!** 🎉

---

## 완전한 프론트엔드 예제 (복붙 가능)

```javascript
class GameClient {
    constructor(token) {
        this.token = token;
        this.ws = null;
        this.roomCode = null;
    }

    connect() {
        this.ws = new WebSocket(`ws://localhost:8080/ws?token=${this.token}`);
        
        this.ws.onopen = () => console.log('✅ 연결됨');
        
        this.ws.onmessage = (event) => {
            const msg = JSON.parse(event.data);
            this.handleMessage(msg);
        };
    }

    handleMessage(msg) {
        switch (msg.type) {
            case 'CONNECTED':
                console.log('서버 연결 확인');
                break;
            case 'ROOM_CREATED':
                this.roomCode = msg.roomCode;
                console.log(`방 생성: ${this.roomCode}`);
                break;
            case 'USER_JOINED':
                this.roomCode = msg.roomCode;
                console.log(`방 참여: ${this.roomCode}`);
                console.log('참가자:', msg.data);
                break;
            case 'GAME_READY':
                console.log('🎮 게임 준비 완료!');
                this.launchUnityGame(msg.roomCode);
                break;
            case 'ERROR':
                console.error('❌ 에러:', msg.message);
                break;
        }
    }

    createRoom() {
        this.ws.send(JSON.stringify({ type: 'CREATE_ROOM' }));
    }

    joinRoom(roomCode) {
        this.ws.send(JSON.stringify({ 
            type: 'JOIN_ROOM', 
            roomCode: roomCode 
        }));
    }

    launchUnityGame(roomCode) {
        console.log('Unity 게임 실행...');
        // Unity 게임에 토큰과 방코드 전달
        if (window.unityInstance) {
            unityInstance.SendMessage('GameManager', 'ConnectToServer', 
                JSON.stringify({
                    token: this.token,
                    roomCode: roomCode,
                    serverUrl: 'ws://localhost:8080/ws'
                })
            );
        }
    }
}

// 사용법
const token = "YOUR_ACCESS_TOKEN";
const client = new GameClient(token);
client.connect();

// 방 만들기
client.createRoom();

// 또는 방 참여
// client.joinRoom('764185');
```

---

## 완전한 Unity 예제 (복붙 가능)

```csharp
using UnityEngine;
using NativeWebSocket;
using System;

public class GameClient : MonoBehaviour
{
    private WebSocket ws;
    private string roomCode;

    [Serializable]
    public class ServerConfig
    {
        public string token;
        public string roomCode;
        public string serverUrl;
    }

    // 프론트엔드에서 호출
    public void ConnectToServer(string configJson)
    {
        var config = JsonUtility.FromJson<ServerConfig>(configJson);
        StartCoroutine(Connect(config));
    }

    private async System.Threading.Tasks.Task Connect(ServerConfig config)
    {
        roomCode = config.roomCode;
        ws = new WebSocket($"{config.serverUrl}?token={config.token}");

        ws.OnOpen += () =>
        {
            Debug.Log("✅ 게임 서버 연결");
            SendMessage("CONNECT_GAME", new { roomCode = roomCode });
        };

        ws.OnMessage += (bytes) =>
        {
            string json = System.Text.Encoding.UTF8.GetString(bytes);
            HandleMessage(json);
        };

        await ws.Connect();
    }

    private void HandleMessage(string json)
    {
        var msg = JsonUtility.FromJson<GameMessage>(json);

        switch (msg.type)
        {
            case "GAME_START":
                Debug.Log("🎮 게임 시작!");
                OnGameStart(msg.data);
                break;
            case "CARD_SUBMITTED":
                Debug.Log("카드 제출 완료");
                break;
            case "ALL_CARDS_SUBMITTED":
                Debug.Log("모든 카드 제출 완료 (출제자만)");
                OnAllCardsSubmitted(msg.data);
                break;
            case "EXAMINER_SELECTED":
                Debug.Log("출제자가 선택함");
                OnExaminerSelected(msg.data);
                break;
            case "NEXT_ROUND":
                Debug.Log("다음 라운드");
                OnNextRound(msg.data);
                break;
            case "ROUND_END":
                Debug.Log("🏆 게임 종료");
                OnGameEnd(msg.data);
                break;
        }
    }

    public void SubmitCard(int cardId)
    {
        SendMessage("SUBMIT_CARD", new {
            roomCode = roomCode,
            data = new { cardId = cardId }
        });
    }

    public void SelectWinner(long participantId)
    {
        SendMessage("EXAMINER_SELECT", new {
            roomCode = roomCode,
            data = new { participantId = participantId }
        });
    }

    private void SendMessage(string type, object data)
    {
        string json = JsonUtility.ToJson(new { type = type, data = data });
        ws.SendText(json);
    }

    void Update()
    {
        #if !UNITY_WEBGL || UNITY_EDITOR
        ws?.DispatchMessageQueue();
        #endif
    }

    [Serializable]
    private class GameMessage
    {
        public string type;
        public string roomCode;
        public object data;
    }
}
```

---

## 다음 단계

### 📚 더 알아보기

1. **[완전한 API 명세서](./WEBSOCKET_API_COMPLETE.md)** - 모든 메시지 상세 설명
2. **[프론트엔드-Unity 연결 가이드](../FRONTEND_UNITY_CONNECTION_GUIDE.md)** - 아키텍처 이해
3. **[게임 연결 플로우](./GAME_CONNECTION_FLOW.md)** - Unity 개발자용 상세 가이드

### 🧪 테스트

```bash
# 로컬 서버 실행
./gradlew bootRun

# 브라우저 콘솔에서 테스트
# http://localhost:8080 접속 후 F12
```

### 🐛 문제 해결

**연결 안 됨**:
- 토큰이 유효한지 확인 (로그인 다시 시도)
- 서버가 실행 중인지 확인
- 포트 번호 확인 (기본 8080)

**메시지 안 받음**:
- JSON 형식 확인
- `type` 필드 대소문자 확인 (모두 대문자)
- roomCode 필드 확인

**출제자가 카드 제출**:
- 출제자(`isExaminer: true`)는 카드 제출 불가
- GAME_START에서 isExaminer 확인

---

**축하합니다! 🎉**  
이제 우리끼리 게임 API를 사용할 준비가 되었습니다!

**질문이 있으면**: 전체 API 명세서를 참고하세요!


# 게임 연결 플로우 가이드

## 개요

우리끼리 서버는 **로비 연결**과 **게임 연결**을 분리하여 처리합니다.

- **로비 연결**: 방 생성/참여 등 게임 시작 전 대기 단계
- **게임 연결**: 실제 게임 플레이 (카드 제출, 출제자 선택 등)

## 전체 플로우

```
1. 프론트엔드 → 서버: WebSocket 연결 (토큰)
2. 프론트엔드 → 서버: CREATE_ROOM or JOIN_ROOM
3. [4명 모임]
4. 서버 → 모든 프론트엔드: GAME_READY
5. 프론트엔드 → Unity 게임 실행: 토큰 + 방코드 전달
6. Unity 게임 → 서버: 새로운 WebSocket 연결 (토큰)
7. Unity 게임 → 서버: CONNECT_GAME 메시지 (방코드)
8. [4명의 Unity 게임이 모두 연결 완료]
9. 서버 → 모든 Unity 게임: GAME_START (질문 포함)
10. 게임 진행...
```

**중요 사항:**
- **프론트엔드와 Unity는 같은 서버에 각각 별도의 WebSocket 연결을 맺습니다**
- 프론트엔드는 로비 연결을 **유지** (게임 종료 후 로비로 복귀 가능)
- Unity 게임은 **새로운 연결**로 게임 플레이만 담당
- 서버는 토큰으로 같은 유저임을 인식하고 게임 세션에 추가
- **GAME_CONNECTED 메시지 없음** - 4명 모두 연결되면 바로 `GAME_START`

---

## 1단계: 프론트엔드 로비 서버 연결

### WebSocket 연결
```javascript
const token = localStorage.getItem('accessToken');
const lobbyWs = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

lobbyWs.onopen = () => {
    console.log('✅ 서버 연결 성공');
};
```

**주의**: 이 연결은 게임이 시작되어도 **유지**됩니다. 게임 종료 후 로비로 복귀할 때 사용합니다.

### 방 생성 또는 참여
```javascript
// 방 생성
lobbyWs.send(JSON.stringify({
    type: 'CREATE_ROOM'
}));

// 또는 방 참여
lobbyWs.send(JSON.stringify({
    type: 'JOIN_ROOM',
    roomCode: '123456'
}));
```

---

## 2단계: GAME_READY 수신 및 Unity 게임 실행

4명이 모두 방에 참여하면 서버가 `GAME_READY` 메시지를 전송합니다.

### 서버 → 프론트엔드
```json
{
  "type": "GAME_READY",
  "roomCode": "123456",
  "data": {
    "participants": [
      {
        "userId": 1,
        "nickname": "플레이어1",
        "level": 5,
        "isExaminer": true
      },
      {
        "userId": 2,
        "nickname": "플레이어2",
        "level": 3,
        "isExaminer": false
      },
      {
        "userId": 3,
        "nickname": "플레이어3",
        "level": 2,
        "isExaminer": false
      },
      {
        "userId": 4,
        "nickname": "플레이어4",
        "level": 7,
        "isExaminer": false
      }
    ],
    "message": "All players ready. Launch Unity game with your token and room code."
  },
  "message": "All players ready! Launch Unity game with your token and room code."
}
```

### 프론트엔드 처리
```javascript
lobbyWs.onmessage = (event) => {
    const message = JSON.parse(event.data);
    
    if (message.type === 'GAME_READY') {
        console.log('🎮 게임 준비 완료!');
        
        const roomCode = message.roomCode;
        const token = localStorage.getItem('accessToken');
        
        // 게임 화면으로 전환하고 Unity 게임 실행
        // Unity 게임에 토큰과 방코드 전달
        launchUnityGame({
            token: token,
            roomCode: roomCode,
            serverUrl: 'ws://localhost:8080/ws'  // 같은 서버
        });
        
        // ⚠️ 로비 연결은 유지! (닫지 않음)
        // lobbyWs는 계속 열려있어야 나중에 게임 종료 후 로비로 돌아올 수 있음
    }
};

function launchUnityGame(config) {
    // Unity WebGL 실행 또는 네이티브 앱으로 데이터 전달
    // 예: Unity WebGL
    if (window.unityInstance) {
        unityInstance.SendMessage('GameManager', 'ConnectToServer', JSON.stringify(config));
    }
    
    // 또는 Unity 앱 실행 (딥링크 등)
    // window.location.href = `unitygame://connect?token=${config.token}&roomCode=${config.roomCode}`;
}
```

---

## 3단계: Unity 게임 서버 연결

Unity 게임 클라이언트가 받은 토큰과 방코드로 **새로운 WebSocket 연결**을 생성합니다.
**프론트엔드와는 별개의 연결**이지만 **같은 서버**에 연결합니다.

### Unity C# 예시
```csharp
using System;
using UnityEngine;
using NativeWebSocket; // 또는 다른 WebSocket 라이브러리

public class GameServerConnection : MonoBehaviour
{
    private WebSocket websocket;
    private string token;
    private string roomCode;
    private string serverUrl;

    // 프론트엔드에서 호출
    public void ConnectToServer(string configJson)
    {
        var config = JsonUtility.FromJson<ServerConfig>(configJson);
        this.token = config.token;
        this.roomCode = config.roomCode;
        this.serverUrl = config.serverUrl;
        
        StartConnection();
    }

    async void StartConnection()
    {
        // 새로운 WebSocket 연결 생성 (프론트엔드와는 별개)
        websocket = new WebSocket($"{serverUrl}?token={token}");

        websocket.OnOpen += () =>
        {
            Debug.Log("✅ 게임용 WebSocket 연결 성공");
            
            // CONNECT_GAME 메시지 전송
            var connectMsg = new
            {
                type = "CONNECT_GAME",
                roomCode = roomCode
            };
            
            string json = JsonUtility.ToJson(connectMsg);
            websocket.SendText(json);
            
            Debug.Log($"CONNECT_GAME 전송: roomCode={roomCode}");
        };

        websocket.OnMessage += (bytes) =>
        {
            string message = System.Text.Encoding.UTF8.GetString(bytes);
            HandleMessage(message);
        };

        websocket.OnError += (e) =>
        {
            Debug.LogError($"❌ WebSocket 에러: {e}");
        };

        websocket.OnClose += (e) =>
        {
            Debug.Log("🔌 게임 연결 종료");
        };

        await websocket.Connect();
    }

    void HandleMessage(string messageJson)
    {
        var message = JsonUtility.FromJson<GameMessage>(messageJson);
        
        switch (message.type)
        {
            case "GAME_START":
                Debug.Log("🎮 게임 시작!");
                // 게임 시작 로직
                StartGame(message.data);
                break;
                
            case "CARD_SUBMITTED":
                Debug.Log("카드 제출 완료");
                break;
                
            case "ALL_CARDS_SUBMITTED":
                Debug.Log("모든 카드 제출 완료");
                OnAllCardsSubmitted(message.data);
                break;
                
            // 다른 메시지 처리...
        }
    }

    void Update()
    {
        #if !UNITY_WEBGL || UNITY_EDITOR
        websocket?.DispatchMessageQueue();
        #endif
    }

    [Serializable]
    public class ServerConfig
    {
        public string token;
        public string roomCode;
        public string gameServerUrl;
    }

    [Serializable]
    public class GameMessage
    {
        public string type;
        public string roomCode;
        public object data;
        public string message;
    }
}
```

### JavaScript (WebGL) 예시
```javascript
function connectToGameServer(gameServerUrl, token, roomCode) {
    const gameWs = new WebSocket(`${gameServerUrl}?token=${token}`);
    
    gameWs.onopen = () => {
        console.log('✅ 게임 서버 연결 성공');
        
        // CONNECT_GAME 메시지 전송
        gameWs.send(JSON.stringify({
            type: 'CONNECT_GAME',
            roomCode: roomCode
        }));
    };
    
    gameWs.onmessage = (event) => {
        const message = JSON.parse(event.data);
        handleGameMessage(message);
    };
    
    return gameWs;
}
```

---

## 4단계: GAME_START 수신 (모든 플레이어 연결 완료)

모든 플레이어(4명)가 게임 서버에 `CONNECT_GAME`으로 연결하면 즉시 게임이 시작됩니다.

### 서버 → 모든 Unity 게임
```json
{
  "type": "GAME_START",
  "roomCode": "123456",
  "data": {
    "participants": [
      {
        "userId": 1,
        "nickname": "플레이어1",
        "level": 5,
        "isExaminer": true
      },
      {
        "userId": 2,
        "nickname": "플레이어2",
        "level": 3,
        "isExaminer": false
      },
      {
        "userId": 3,
        "nickname": "플레이어3",
        "level": 2,
        "isExaminer": false
      },
      {
        "userId": 4,
        "nickname": "플레이어4",
        "level": 7,
        "isExaminer": false
      }
    ],
    "question": {
      "quizId": 42,
      "content": "가장 좋아하는 음식은?"
    }
  },
  "message": "Game is starting! All players connected."
}
```

---

## 완전한 클라이언트 예시

```javascript
class GameClient {
    constructor(token) {
        this.token = token;
        this.lobbyWs = null;
        this.gameWs = null;
        this.currentRoomCode = null;
    }

    // 1. 로비 연결
    connectToLobby() {
        this.lobbyWs = new WebSocket(`ws://localhost:8080/ws?token=${this.token}`);
        
        this.lobbyWs.onopen = () => {
            console.log('✅ 로비 서버 연결');
        };
        
        this.lobbyWs.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleLobbyMessage(message);
        };
    }

    // 2. 로비 메시지 처리
    handleLobbyMessage(message) {
        switch (message.type) {
            case 'CONNECTED':
                console.log('연결 확인');
                break;
                
            case 'ROOM_CREATED':
                this.currentRoomCode = message.roomCode;
                console.log('방 생성:', this.currentRoomCode);
                break;
                
            case 'ROOM_JOINED':
                this.currentRoomCode = message.roomCode;
                console.log('방 참여:', this.currentRoomCode);
                console.log('참가자 목록:', message.data);
                break;
                
            case 'USER_JOINED':
                console.log('새 참가자:', message.data);
                break;
                
            case 'GAME_READY':
                console.log('🎮 게임 준비 완료!');
                this.connectToGame(message.data.gameServerUrl, this.currentRoomCode);
                break;
                
            case 'ERROR':
                console.error('에러:', message.message);
                break;
        }
    }

    // 3. 게임 서버 연결
    connectToGame(gameServerUrl, roomCode) {
        // 로비 연결 닫기
        if (this.lobbyWs) {
            this.lobbyWs.close();
        }
        
        // 게임 서버 연결
        this.gameWs = new WebSocket(`${gameServerUrl}?token=${this.token}`);
        
        this.gameWs.onopen = () => {
            console.log('✅ 게임 서버 연결');
            
            // CONNECT_GAME 전송
            this.gameWs.send(JSON.stringify({
                type: 'CONNECT_GAME',
                roomCode: roomCode
            }));
        };
        
        this.gameWs.onmessage = (event) => {
            const message = JSON.parse(event.data);
            this.handleGameMessage(message);
        };
    }

    // 4. 게임 메시지 처리
    handleGameMessage(message) {
        switch (message.type) {
            case 'GAME_START':
                console.log('🎮 게임 시작!');
                console.log('질문:', message.data.question.content);
                console.log('참가자:', message.data.participants);
                // UI 업데이트: 게임 화면으로 전환
                break;
                
            case 'CARD_SUBMITTED':
                console.log('카드 제출 완료');
                break;
                
            case 'ALL_CARDS_SUBMITTED':
                console.log('모든 카드 제출 완료');
                console.log('제출된 카드:', message.data);
                break;
                
            case 'EXAMINER_SELECTED':
                console.log('출제자가 선택함:', message.data);
                break;
                
            case 'NEXT_ROUND':
                console.log('다음 라운드 시작');
                console.log('새 출제자:', message.data.examinerNickname);
                console.log('새 질문:', message.data.question.content);
                break;
                
            case 'ROUND_END':
                console.log('게임 종료!');
                console.log('최종 순위:', message.data.rankings);
                break;
                
            case 'ERROR':
                console.error('에러:', message.message);
                break;
        }
    }

    // 방 생성
    createRoom() {
        if (this.lobbyWs && this.lobbyWs.readyState === WebSocket.OPEN) {
            this.lobbyWs.send(JSON.stringify({ type: 'CREATE_ROOM' }));
        }
    }

    // 방 참여
    joinRoom(roomCode) {
        if (this.lobbyWs && this.lobbyWs.readyState === WebSocket.OPEN) {
            this.lobbyWs.send(JSON.stringify({
                type: 'JOIN_ROOM',
                roomCode: roomCode
            }));
        }
    }

    // 카드 제출
    submitCard(cardId) {
        if (this.gameWs && this.gameWs.readyState === WebSocket.OPEN) {
            this.gameWs.send(JSON.stringify({
                type: 'SUBMIT_CARD',
                roomCode: this.currentRoomCode,
                data: { cardId: cardId }
            }));
        }
    }

    // 출제자 카드 선택
    selectCard(participantId) {
        if (this.gameWs && this.gameWs.readyState === WebSocket.OPEN) {
            this.gameWs.send(JSON.stringify({
                type: 'EXAMINER_SELECT',
                roomCode: this.currentRoomCode,
                data: { participantId: participantId }
            }));
        }
    }
}

// 사용 예시
const token = localStorage.getItem('accessToken');
const client = new GameClient(token);

// 로비 연결
client.connectToLobby();

// 방 생성 버튼 클릭 시
document.getElementById('createRoomBtn').onclick = () => {
    client.createRoom();
};

// 방 참여 버튼 클릭 시
document.getElementById('joinRoomBtn').onclick = () => {
    const roomCode = document.getElementById('roomCodeInput').value;
    client.joinRoom(roomCode);
};
```

---

## 주요 차이점 정리

### 이전 (단일 연결)
- WebSocket 1개로 로비 + 게임 처리
- 4명 모이면 바로 GAME_START

### 현재 (분리된 연결)
- WebSocket 1: 로비 연결 (방 생성/참여)
- WebSocket 2: 게임 연결 (실제 플레이)
- 4명 모이면 GAME_READY → 클라이언트 재연결 → 모두 연결되면 GAME_START

---

## 메시지 타입 전체 목록

### 로비 단계
- `CONNECTED` - 연결 성공
- `CREATE_ROOM` - 방 생성 요청
- `ROOM_CREATED` - 방 생성 완료
- `JOIN_ROOM` - 방 참여 요청
- `ROOM_JOINED` - 방 참여 완료
- `USER_JOINED` - 새 참가자 알림
- `GAME_READY` - 4명 모임, 게임 서버 연결 안내

### 게임 단계
- `CONNECT_GAME` - 게임 서버 연결 요청
- `GAME_START` - 게임 시작 (4명 모두 연결 시, 질문 포함)
- `SUBMIT_CARD` - 카드 제출
- `CARD_SUBMITTED` - 카드 제출 확인
- `ALL_CARDS_SUBMITTED` - 모든 카드 제출 완료
- `EXAMINER_SELECT` - 출제자 선택
- `EXAMINER_SELECTED` - 선택 완료
- `NEXT_ROUND` - 다음 라운드
- `ROUND_END` - 게임 종료

### 공통
- `ERROR` - 에러 발생
- `ROOM_EXIT` - 방 나가기

---

## 환경 변수 설정

게임 서버 URL은 환경 변수로 설정할 수 있습니다:

```bash
# .env 또는 환경 변수
GAME_SERVER_WS_URL=ws://your-game-server.com/ws
```

설정하지 않으면 기본값 `ws://localhost:8080/ws`가 사용됩니다.


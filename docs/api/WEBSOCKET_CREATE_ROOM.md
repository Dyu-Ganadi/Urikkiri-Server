# 방 생성 API

## 개요
새로운 게임 방을 생성하고 방장으로 입장합니다.

## 요청

### WebSocket 메시지
```json
{
  "type": "CREATE_ROOM"
}
```

### 필드 설명
- `type` (string, 필수): 메시지 타입, 반드시 `"CREATE_ROOM"` 이어야 함

## 응답

### 성공 응답
```json
{
  "type": "ROOM_CREATED",
  "roomCode": "764185",
  "data": [
    {
      "userId": 1,
      "nickname": "방장",
      "level": 5
    }
  ],
  "message": "Room created successfully"
}
```

### 필드 설명
- `type` (string): 응답 타입 `"ROOM_CREATED"`
- `roomCode` (string): 생성된 방의 6자리 코드
- `data` (array): 현재 방의 참가자 목록 (방장 정보 포함)
  - `userId` (number): 사용자 ID
  - `nickname` (string): 사용자 닉네임
  - `level` (number): 사용자 레벨
- `message` (string): 성공 메시지

### 에러 응답
```json
{
  "type": "ERROR",
  "message": "Failed to Create Room"
}
```

## 동작 방식

1. 클라이언트가 `CREATE_ROOM` 메시지 전송
2. 서버에서 6자리 숫자 방 코드 생성 (100000~999999)
3. 방 생성자를 시험관(examiner)으로 등록
4. WebSocket 세션을 해당 방에 추가
5. 성공 응답 전송

## 클라이언트 구현 예시

### JavaScript
```javascript
class GameRoom {
  constructor(webSocket) {
    this.ws = webSocket;
  }

  createRoom() {
    const message = {
      type: 'CREATE_ROOM'
    };
    this.ws.send(JSON.stringify(message));
  }

  handleMessage(event) {
    const message = JSON.parse(event.data);
    
    switch(message.type) {
      case 'ROOM_CREATED':
        console.log('방 생성 성공!');
        console.log('방 코드:', message.roomCode);
        console.log('참가자 목록:', message.data);
        // UI 업데이트: 방 코드 표시 및 참가자 목록 표시
        this.displayRoomCode(message.roomCode);
        this.displayParticipants(message.data);
        break;
      
      case 'ERROR':
        console.error('방 생성 실패:', message.message);
        alert('방 생성에 실패했습니다: ' + message.message);
        break;
    }
  }

  displayRoomCode(roomCode) {
    // 방 코드를 사용자에게 표시
    document.getElementById('roomCode').textContent = roomCode;
    // 다른 사용자들이 입장할 수 있도록 공유
  }

  displayParticipants(participants) {
    const container = document.getElementById('participants');
    container.innerHTML = '';
    participants.forEach(p => {
      const div = document.createElement('div');
      div.textContent = `${p.nickname} (Lv.${p.level})`;
      container.appendChild(div);
    });
    
    // 참가자 수 표시
    const status = document.getElementById('status');
    status.textContent = `대기 중... (${participants.length}/4)`;
  }
}

// 사용 예시
const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`${WS_URL}?token=${token}`);
const gameRoom = new GameRoom(ws);

ws.onopen = () => {
  // 방 생성 버튼 클릭 시
  document.getElementById('createRoomBtn').addEventListener('click', () => {
    gameRoom.createRoom();
  });
};

ws.onmessage = (event) => {
  gameRoom.handleMessage(event);
};
```

### React 예시
```jsx
import { useEffect, useRef, useState } from 'react';

function CreateRoomPage() {
  const [roomCode, setRoomCode] = useState(null);
  const [participants, setParticipants] = useState([]);
  const [error, setError] = useState(null);
  const wsRef = useRef(null);

  useEffect(() => {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    const token = localStorage.getItem('accessToken');
    wsRef.current = new WebSocket(`${WS_URL}?token=${token}`);

    wsRef.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'ROOM_CREATED') {
        setRoomCode(message.roomCode);
        setParticipants(message.data);
        setError(null);
      } else if (message.type === 'ERROR') {
        setError(message.message);
      }
    };

    return () => {
      wsRef.current?.close();
    };
  }, []);

  const handleCreateRoom = () => {
    wsRef.current?.send(JSON.stringify({
      type: 'CREATE_ROOM'
    }));
  };

  return (
    <div>
      <button onClick={handleCreateRoom}>방 만들기</button>
      {roomCode && (
        <div>
          <h2>방 코드: {roomCode}</h2>
          <button onClick={() => navigator.clipboard.writeText(roomCode)}>
            코드 복사
          </button>
          <h3>참가자 ({participants.length}/4)</h3>
          <ul>
            {participants.map(p => (
              <li key={p.userId}>
                {p.nickname} (Lv.{p.level})
              </li>
            ))}
          </ul>
        </div>
      )}
      {error && <div className="error">{error}</div>}
    </div>
  );
}
```

## 데이터베이스 변경사항

### Room 테이블
새로운 레코드가 생성됩니다:
```sql
INSERT INTO tbl_room (code) VALUES ('764185');
```

### Participant 테이블
방 생성자가 시험관으로 등록됩니다:
```sql
INSERT INTO tbl_participant (user_id, room_id, banana_score, is_examiner) 
VALUES (1, 2, 0, true);
```

## 방 코드 생성 규칙

- **형식**: 6자리 숫자
- **범위**: 100000 ~ 999999
- **중복 방지**: 이미 존재하는 코드는 재생성
- **예시**: `764185`, `123456`, `999999`

## 에러 케이스

### 1. 방 생성 실패
```json
{
  "type": "ERROR",
  "message": "Failed to Create Room"
}
```

**발생 원인:**
- 데이터베이스 연결 실패
- 트랜잭션 오류
- 서버 내부 오류

### 2. 인증 실패
WebSocket 연결이 끊어지고 에러 메시지가 전송되지 않습니다.

## 권한

- ✅ 방 생성자는 자동으로 **시험관(Examiner)** 역할을 부여받습니다
- ✅ 시험관은 게임 진행을 관리할 수 있습니다
- ✅ 방당 시험관은 1명만 존재합니다

## 제한사항

- 한 사용자가 여러 방을 동시에 생성할 수 있습니다
- 방 코드는 방이 삭제되기 전까지 유지됩니다
- 최대 생성 가능한 방의 수는 900,000개입니다 (코드 범위 제한)

## 환경 변수 설정

### 개발 환경 (.env.development)
```env
REACT_APP_WS_URL=ws://localhost:8080/ws
```

### 프로덕션 환경 (.env.production)
```env
REACT_APP_WS_URL=wss://api.urikkiri.com/ws
```

## UI/UX 권장사항

1. **방 코드 표시**
   - 큰 글씨로 명확하게 표시
   - 복사 버튼 제공
   - 공유 기능 제공 (카카오톡, URL 복사 등)

2. **대기 화면**
   - 다른 참가자를 기다리는 화면 표시
   - 현재 참가자 목록 실시간 업데이트
   - "게임 시작" 버튼 (시험관만 표시)

3. **에러 처리**
   - 방 생성 실패 시 재시도 옵션 제공
   - 명확한 에러 메시지 표시

## 다음 단계

방 생성 후:
1. 방 코드를 다른 사용자에게 공유
2. 다른 사용자들이 [방 참가](./WEBSOCKET_JOIN_ROOM.md)
3. 모든 참가자가 입장하면 게임 시작

## 관련 API

- [WebSocket 연결](./WEBSOCKET_CONNECTION.md)
- [방 참가하기](./WEBSOCKET_JOIN_ROOM.md)



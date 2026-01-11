# 방 참가 API

## 개요
6자리 방 코드를 사용하여 기존 게임 방에 참가합니다.

## 요청

### WebSocket 메시지
```json
{
  "type": "JOIN_ROOM",
  "roomCode": "764185"
}
```

### 필드 설명
- `type` (string, 필수): 메시지 타입, 반드시 `"JOIN_ROOM"` 이어야 함
- `roomCode` (string, 필수): 참가할 방의 6자리 코드

## 응답

### 성공 응답

#### 1. 참가자 본인에게 전송 (ROOM_JOINED)
전체 참가자 목록을 포함한 응답:
```json
{
  "type": "ROOM_JOINED",
  "roomCode": "764185",
  "data": [
    {
      "userId": 1,
      "nickname": "방장",
      "level": 5
    },
    {
      "userId": 2,
      "nickname": "참가자1",
      "level": 3
    },
    {
      "userId": 3,
      "nickname": "나",
      "level": 2
    }
  ],
  "message": "Successfully joined room"
}
```

#### 2. 기존 참가자들에게 브로드캐스트 (USER_JOINED)
새로 참가한 유저 정보만 전송:
```json
{
  "type": "USER_JOINED",
  "roomCode": "764185",
  "data": {
    "userId": 3,
    "nickname": "나",
    "level": 2
  },
  "message": "나 joined the room"
}
```

#### 3. 4명이 모이면 자동으로 게임 시작 (GAME_START)
4번째 참가자가 입장하여 총 4명이 되면, 모든 참가자에게 자동으로 게임 시작 메시지가 전송됩니다:
```json
{
  "type": "GAME_START",
  "roomCode": "764185",
  "data": {
    "participants": [
      {
        "userId": 1,
        "nickname": "방장",
        "level": 5
      },
      {
        "userId": 2,
        "nickname": "참가자1",
        "level": 3
      },
      {
        "userId": 3,
        "nickname": "참가자2",
        "level": 2
      },
      {
        "userId": 4,
        "nickname": "참가자3",
        "level": 7
      }
    ],
    "question": {
      "quizId": 42,
      "content": "가장 좋아하는 음식은?"
    }
  },
  "message": "Game is starting! All 4 players are ready."
}
```

**중요:** 
- 이 메시지는 서버에서 자동으로 전송되며, **모든 참가자가 동일한 질문을 받습니다**.
- 클라이언트는 이 메시지를 받으면 즉시 게임 화면으로 전환하고, **별도로 질문 조회 API를 호출할 필요 없이** `data.question`을 사용하면 됩니다.

### 필드 설명
- `type` (string): 응답 타입
- `roomCode` (string): 방 코드
- `data` (object/array): 참가자 정보
  - `userId` (number): 사용자 ID
  - `nickname` (string): 사용자 닉네임
  - `level` (number): 사용자 레벨
- `message` (string): 상태 메시지

### 에러 응답

#### 1. 방을 찾을 수 없음
```json
{
  "type": "ERROR",
  "message": "Room Not Found"
}
```

#### 2. 이미 해당 방에 참가 중
```json
{
  "type": "ERROR",
  "message": "User is Already in This Room"
}
```

#### 3. 방이 꽉 참
```json
{
  "type": "ERROR",
  "message": "Room is Already Full"
}
```

#### 4. 방 코드 누락
```json
{
  "type": "ERROR",
  "message": "Room Code is Required"
}
```

## 동작 방식

1. 클라이언트가 `JOIN_ROOM` 메시지와 방 코드 전송
2. 서버에서 방 존재 여부 확인
3. 중복 참가 확인 (같은 방에 이미 참가 중인지)
4. 방 인원 제한 확인 (최대 4명)
5. 참가자로 등록 (일반 참가자, examiner=false)
6. 참가자 본인에게 전체 참가자 목록 전송 (`ROOM_JOINED`)
7. 기존 참가자들에게 새 참가자 정보 브로드캐스트 (`USER_JOINED`)
8. **참가자가 4명이 되면** 모든 참가자에게 게임 시작 메시지 자동 전송 (`GAME_START`)

## 클라이언트 구현 예시

### JavaScript
```javascript
class GameRoom {
  constructor(webSocket) {
    this.ws = webSocket;
  }

  joinRoom(roomCode) {
    const message = {
      type: 'JOIN_ROOM',
      roomCode: roomCode
    };
    this.ws.send(JSON.stringify(message));
  }

  handleMessage(event) {
    const message = JSON.parse(event.data);
    
    switch(message.type) {
      case 'ROOM_JOINED':
        console.log('방 참가 성공!');
        console.log('방 코드:', message.roomCode);
        console.log('참가자 목록:', message.data);
        this.displayParticipants(message.data);
        break;
      
      case 'USER_JOINED':
        console.log('새로운 참가자:', message.data);
        this.addParticipant(message.data);
        break;
      
      case 'GAME_START':
        console.log('게임 시작!');
        console.log('전체 참가자:', message.data.participants);
        console.log('질문:', message.data.question.content);
        this.startGame(message.data);
        break;
      
      case 'ERROR':
        console.error('방 참가 실패:', message.message);
        alert(`방 참가 실패: ${message.message}`);
        break;
    }
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

  addParticipant(participant) {
    const container = document.getElementById('participants');
    const div = document.createElement('div');
    div.textContent = `${participant.nickname} (Lv.${participant.level})`;
    container.appendChild(div);
    
    // 참가자 수 업데이트
    const currentCount = container.children.length;
    const status = document.getElementById('status');
    status.textContent = `대기 중... (${currentCount}/4)`;
  }

  startGame(data) {
    // 대기 화면 숨기기
    document.getElementById('waitingScreen').style.display = 'none';
    
    // 게임 화면 표시
    document.getElementById('gameScreen').style.display = 'block';
    
    // 질문 표시
    document.getElementById('question').textContent = data.question.content;
    
    // 게임 초기화
    console.log('게임 시작! 참가자:', data.participants);
    console.log('질문:', data.question.content);
  }
}

// 사용 예시
const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`${WS_URL}?token=${token}`);
const gameRoom = new GameRoom(ws);

ws.onopen = () => {
  // 방 참가 버튼 클릭 시
  document.getElementById('joinRoomBtn').addEventListener('click', () => {
    const roomCode = document.getElementById('roomCodeInput').value;
    gameRoom.joinRoom(roomCode);
  });
};

ws.onmessage = (event) => {
  gameRoom.handleMessage(event);
};
```

### React 예시
```jsx
import { useEffect, useRef, useState } from 'react';

function JoinRoomPage() {
  const [roomCode, setRoomCode] = useState('');
  const [participants, setParticipants] = useState([]);
  const [error, setError] = useState(null);
  const [isJoined, setIsJoined] = useState(false);
  const [gameStarted, setGameStarted] = useState(false);
  const [question, setQuestion] = useState(null);
  const wsRef = useRef(null);

  useEffect(() => {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    const token = localStorage.getItem('accessToken');
    wsRef.current = new WebSocket(`${WS_URL}?token=${token}`);

    wsRef.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      switch(message.type) {
        case 'ROOM_JOINED':
          setParticipants(message.data);
          setIsJoined(true);
          setError(null);
          break;
        
        case 'USER_JOINED':
          setParticipants(prev => [...prev, message.data]);
          break;
        
        case 'GAME_START':
          setParticipants(message.data.participants);
          setQuestion(message.data.question);
          setGameStarted(true);
          break;
        
        case 'ERROR':
          setError(message.message);
          break;
      }
    };

    return () => {
      wsRef.current?.close();
    };
  }, []);

  const handleJoinRoom = () => {
    if (!roomCode || roomCode.length !== 6) {
      setError('6자리 방 코드를 입력해주세요');
      return;
    }

    wsRef.current?.send(JSON.stringify({
      type: 'JOIN_ROOM',
      roomCode: roomCode
    }));
  };

  if (gameStarted) {
    return (
      <div>
        <h2>게임 시작!</h2>
        <h3>질문: {question?.content}</h3>
        <h3>참가자 목록</h3>
        <ul>
          {participants.map(p => (
            <li key={p.userId}>
              {p.nickname} (Lv.{p.level})
            </li>
          ))}
        </ul>
        {/* 게임 화면 컴포넌트 */}
      </div>
    );
  }

  if (isJoined) {
    return (
      <div>
        <h2>방 코드: {roomCode}</h2>
        <h3>참가자 목록 ({participants.length}/4)</h3>
        <ul>
          {participants.map(p => (
            <li key={p.userId}>
              {p.nickname} (Lv.{p.level})
            </li>
          ))}
        </ul>
        <p>다른 참가자를 기다리는 중...</p>
      </div>
    );
  }

  return (
    <div>
      <h2>방 참가하기</h2>
      <input
        type="text"
        placeholder="6자리 방 코드 입력"
        maxLength={6}
        value={roomCode}
        onChange={(e) => setRoomCode(e.target.value)}
      />
      <button onClick={handleJoinRoom}>참가하기</button>
      {error && <div className="error">{error}</div>}
    </div>
  );
}
```

### URL 파라미터로 방 코드 전달
```jsx
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';

function JoinRoomPage() {
  const { roomCode } = useParams(); // URL: /join/:roomCode
  const wsRef = useRef(null);

  useEffect(() => {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    const token = localStorage.getItem('accessToken');
    wsRef.current = new WebSocket(`${WS_URL}?token=${token}`);

    wsRef.current.onopen = () => {
      // 연결되자마자 자동으로 방 참가
      wsRef.current?.send(JSON.stringify({
        type: 'JOIN_ROOM',
        roomCode: roomCode
      }));
    };

    // ... 메시지 핸들러
  }, [roomCode]);

  // ...
}
```

## 데이터베이스 변경사항

### Participant 테이블
새로운 참가자가 일반 참가자로 등록됩니다:
```sql
INSERT INTO tbl_participant (user_id, room_id, banana_score, is_examiner) 
VALUES (3, 2, 0, false);
```

## 제한사항

- **최대 인원**: 방당 최대 4명까지 참가 가능
- **중복 참가**: 같은 사용자가 동일한 방에 중복 참가 불가
- **역할**: 일반 참가자로 등록 (시험관 아님)
- **방 코드**: 정확히 6자리 숫자여야 함

## 에러 케이스

### 1. 방을 찾을 수 없음 (ROOM_NOT_FOUND)
```json
{
  "type": "ERROR",
  "message": "Room Not Found"
}
```
**발생 원인:**
- 존재하지 않는 방 코드
- 방이 이미 삭제됨
- 방 코드 입력 오류

### 2. 이미 참가 중 (ALREADY_IN_ROOM)
```json
{
  "type": "ERROR",
  "message": "User is Already in This Room"
}
```
**발생 원인:**
- 동일한 방에 중복 참가 시도
- 이미 해당 방에 참가되어 있음

### 3. 방이 꽉 참 (ROOM_ALREADY_FULL)
```json
{
  "type": "ERROR",
  "message": "Room is Already Full"
}
```
**발생 원인:**
- 방에 이미 4명이 참가 중
- 동시에 여러 명이 참가 시도

### 4. 방 코드 누락 (WEBSOCKET_ROOM_CODE_REQUIRED)
```json
{
  "type": "ERROR",
  "message": "Room Code is Required"
}
```
**발생 원인:**
- roomCode 필드가 null
- roomCode 필드가 빈 문자열

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

1. **방 코드 입력**
   - 6자리 숫자만 입력 가능하도록 제한
   - 입력 즉시 자동으로 하이픈 또는 공백 추가 (예: 764-185)
   - 클립보드에서 붙여넣기 지원

2. **참가자 목록**
   - 실시간으로 업데이트되는 참가자 목록
   - 방장(시험관) 표시
   - 현재 인원 / 최대 인원 표시 (예: 3/4)

3. **에러 처리**
   - 명확한 에러 메시지 표시
   - 잘못된 방 코드 입력 시 재입력 유도
   - 방이 꽉 찼을 때 다른 방 생성/참가 옵션 제공

4. **로딩 상태**
   - 참가 요청 중 로딩 표시
   - 참가 성공 후 자동으로 대기실로 이동

## 공유 기능 구현

### URL로 방 공유
```javascript
// 방 코드를 URL에 포함하여 공유
const shareUrl = `${window.location.origin}/join/${roomCode}`;

// 클립보드에 복사
navigator.clipboard.writeText(shareUrl);

// 카카오톡 공유 (Kakao SDK 사용)
Kakao.Link.sendDefault({
  objectType: 'feed',
  content: {
    title: '우리끼리 게임 참가',
    description: `방 코드: ${roomCode}`,
    link: {
      mobileWebUrl: shareUrl,
      webUrl: shareUrl
    }
  }
});
```

## 다음 단계

방 참가 후:
1. 대기실에서 다른 참가자 대기 (최대 4명)
2. **4명이 모이면 자동으로 게임 시작** (서버에서 `GAME_START` 메시지 자동 전송)
3. 게임 진행

## 관련 API

- [WebSocket 연결](./WEBSOCKET_CONNECTION.md)
- [방 생성하기](./WEBSOCKET_CREATE_ROOM.md)


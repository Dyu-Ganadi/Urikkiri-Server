# WebSocket API 명세서 목차

## 📖 빠른 시작

### 역할별 추천 문서

| 역할 | 추천 문서 | 설명 |
|------|----------|------|
| **처음 시작** | [빠른 시작 가이드](./QUICK_START.md) ⚡ | 5분 안에 시작하기 |
| **프론트엔드** | [프론트엔드-Unity 연결](../FRONTEND_UNITY_CONNECTION_GUIDE.md) 📱 | 전체 아키텍처 이해 |
| **Unity 개발자** | [게임 연결 플로우](./GAME_CONNECTION_FLOW.md) 🎮 | Unity 구현 상세 |
| **전체 API** | [완전한 WebSocket API](./WEBSOCKET_API_COMPLETE.md) 📚 | 모든 메시지 명세 |
| **REST API** | [REST API 명세서](./REST_API.md) 🔐 | 로그인, 카드 조회 등 |

---

## 개요

우리끼리 게임의 모든 실시간 통신은 WebSocket을 통해 이루어집니다.

**핵심 개념** (v2.0):

```
프론트엔드 (로비) ─┐
                  ├─→ ws://server/ws ─→ [서버]
Unity 게임 (플레이) ─┘
```

- **프론트엔드**: 방 생성/참여, 로비 대기
- **Unity 게임**: 실제 게임 플레이 (카드 제출, 점수)
- **같은 서버**, 다른 WebSocket 연결

---

## 📋 API 카테고리

### 🔐 인증 & 기본

- **[REST API](./REST_API.md)** - 로그인, 회원가입, 내 정보 조회
- **[WebSocket 연결](./WEBSOCKET_CONNECTION.md)** - 기본 연결 방법, 인증

### 🏠 로비 (프론트엔드)

| API | 설명 | 문서 |
|-----|------|------|
| `CREATE_ROOM` | 방 생성 | [상세](./WEBSOCKET_CREATE_ROOM.md) |
| `JOIN_ROOM` | 방 참여 | [상세](./WEBSOCKET_JOIN_ROOM.md) |
| `ROOM_EXIT` | 게임 시작 전 방 나가기 | [상세](./WEBSOCKET_API_COMPLETE.md#room_exit) |
| `GAME_READY` | 4명 모임 알림 | [상세](./WEBSOCKET_API_COMPLETE.md#7-game_ready---게임-준비-완료-) |

### 🎮 게임 (Unity)

| API | 설명 | 문서 |
|-----|------|------|
| `CONNECT_GAME` | 게임 연결 | [상세](./WEBSOCKET_API_COMPLETE.md#1-connect_game---게임-연결-) |
| `GAME_START` | 게임 시작 | [상세](./WEBSOCKET_API_COMPLETE.md#2-game_start---게임-시작-) |
| `SUBMIT_CARD` | 카드 제출 | [상세](./WEBSOCKET_SUBMIT_CARD.md) |
| `EXAMINER_SELECT` | 출제자 선택 | [상세](./WEBSOCKET_EXAMINER_SELECT.md) |
| `NEXT_ROUND` | 다음 라운드 | [상세](./WEBSOCKET_NEXT_ROUND.md) |
| `ROUND_END` | 게임 종료 | [상세](./WEBSOCKET_ROUND_END.md) |
| `LEAVE_ROOM` | 게임 종료 후 방 나가기 | [상세](./WEBSOCKET_LEAVE_ROOM.md) |

### 🎴 카드

- **[GET /api/cards/random](./GET_RANDOM_CARDS.md)** - 랜덤 카드 10장 조회

---

## 메시지 타입 전체 목록

| 메시지 타입 | 방향 | 설명 | 연결 타입 |
|------------|------|------|-----------|
| `CONNECTED` | 서버 → 클라이언트 | 연결 성공 | 로비/게임 |
| `CREATE_ROOM` | 클라이언트 → 서버 | 방 생성 요청 | 로비 |
| `ROOM_CREATED` | 서버 → 클라이언트 | 방 생성 완료 | 로비 |
| `JOIN_ROOM` | 클라이언트 → 서버 | 방 참가 요청 | 로비 |
| `USER_JOINED` | 서버 → 모든 참가자 | 방 참가 완료 (전체 참가자 목록) | 로비 |
| `ROOM_EXIT` | 클라이언트 ↔ 서버 | 게임 시작 전 방 나가기 | 로비 |
| `LEAVE_ROOM` | 클라이언트 ↔ 서버 | 게임 종료 후 방 나가기 | 게임 |
| `GAME_READY` | 서버 → 모든 참가자 | 4명 모임, 게임 서버 연결 안내 | 로비 |
| `CONNECT_GAME` | 클라이언트 → 서버 | 게임 서버 연결 요청 | 게임 |
| `GAME_START` | 서버 → 모든 참가자 | 게임 시작 (4명 모두 연결 완료) | 게임 |
| `SUBMIT_CARD` | 클라이언트 → 서버 | 카드 제출 | 게임 |
| `CARD_SUBMITTED` | 서버 → 클라이언트 | 카드 제출 확인 | 게임 |
| `ALL_CARDS_SUBMITTED` | 서버 → 출제자 | 모든 카드 제출 완료 | 게임 |
| `EXAMINER_SELECT` | 출제자 → 서버 | 승자 선택 | 게임 |
| `EXAMINER_SELECTED` | 서버 → 모든 참가자 | 승자 발표 | 게임 |
| `NEXT_ROUND` | 서버 → 모든 참가자 | 다음 턴 시작 | 게임 |
| `ROUND_END` | 서버 → 모든 참가자 | 게임 종료 | 게임 |
| `ERROR` | 서버 → 클라이언트 | 에러 발생 | 로비/게임 |

---

## 게임 플로우

전체 게임 진행 과정은 [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)를 참고하세요.

### 간단 플로우
```
1. WebSocket 연결 (로비)
   ↓
2. 방 생성 또는 참가
   ↓
3. 4명 모이면 GAME_READY 수신
   ↓
4. Unity 게임으로 토큰+방코드 전달
   ↓
5. Unity가 게임 서버로 새 WebSocket 연결
   ↓
6. Unity → 서버: CONNECT_GAME (방코드)
   ↓
7. 4명 모두 연결 완료 → 서버 → 모두: GAME_START
   ↓
8. 반복:
   - 참가자들이 카드 제출 (SUBMIT_CARD)
   - 출제자가 승자 선택 (EXAMINER_SELECT)
   - 승자 발표 (EXAMINER_SELECTED)
   - 5점 미만: 다음 턴 (NEXT_ROUND)
   - 5점 달성: 게임 종료 (ROUND_END)
   ↓
9. 결과 확인 후 방 나가기 (LEAVE_ROOM)
   - 모든 참가자가 나가면 방 자동 종료
   - 참가자 정보 DB에서 삭제
```

**상세 플로우는 [게임 연결 플로우 가이드](./GAME_CONNECTION_FLOW.md)를 참고하세요.**
```

---

## REST API

게임 중 일부 데이터는 REST API를 통해 조회합니다:

### [카드 조회](./GET_RANDOM_CARDS.md)
- `GET /play-together/cards`: 랜덤 카드 5개 조회
- 참가자만 호출 (출제자 제외)
- 매 턴마다 호출

---

## 에러 처리

모든 에러는 다음 형식으로 전송됩니다:

```json
{
  "type": "ERROR",
  "message": "에러 메시지"
}
```

### 주요 에러 코드

| 에러 메시지 | 발생 상황 |
|-----------|----------|
| `Room Code is Required` | 방 코드 누락 |
| `Room Not Found` | 존재하지 않는 방 |
| `Room is Already Full` | 방이 가득 참 (4명) |
| `User is Already in This Room` | 이미 참가 중 |
| `Examiner Cannot Submit Card` | 출제자가 카드 제출 시도 |
| `Examiner Not Found` | 출제자를 찾을 수 없음 |
| `Participant Not Found` | 참가자를 찾을 수 없음 |
| `Card Not Found` | 카드를 찾을 수 없음 |
| `Invalid WebSocket Message Format` | 잘못된 메시지 형식 |

---

## 클라이언트 구현 가이드

### 1. 연결 초기화
```javascript
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onopen = () => {
  console.log('WebSocket 연결 성공');
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  handleMessage(message);
};
```

### 2. 메시지 전송
```javascript
const sendMessage = (type, room_code, data = null) => {
  ws.send(JSON.stringify({
    type,
    room_code,
    data
  }));
};
```

### 3. 메시지 처리
```javascript
const handleMessage = (message) => {
  switch (message.type) {
    case 'GAME_START':
      startGame(message.data);
      break;
    case 'ALL_CARDS_SUBMITTED':
      showSubmittedCards(message.data);
      break;
    case 'EXAMINER_SELECTED':
      showWinner(message.data);
      break;
    case 'NEXT_ROUND':
      startNextRound(message.data);
      break;
    case 'ROUND_END':
      showGameResult(message.data);
      break;
    case 'ERROR':
      showError(message.message);
      break;
  }
};
```

---

## 테스트

### Postman WebSocket
1. Postman에서 WebSocket 연결 생성
2. URL: `ws://localhost:8080/ws?token={JWT_TOKEN}`
3. 메시지 전송 테스트

### 브라우저 콘솔
```javascript
const token = 'your_jwt_token';
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

ws.onmessage = (e) => console.log(JSON.parse(e.data));

// 방 생성
ws.send(JSON.stringify({ type: 'CREATE_ROOM' }));

// 방 참가
ws.send(JSON.stringify({ type: 'JOIN_ROOM', room_code: '123456' }));
```

---

## 참고사항

1. **인증 필수**: 모든 WebSocket 연결에 JWT 토큰 필요
2. **방 코드**: 6자리 숫자 (100000 ~ 999999)
3. **참가 인원**: 정확히 4명 (방장 포함)
4. **출제자 로테이션**: 자동으로 돌아가면서 출제자 역할 수행
5. **게임 종료 조건**: 누군가 바나나 점수 5점 달성
6. **경험치 보상**: 순위별 차등 지급 (1위: 20, 2위: 10, 3위: 5, 4위: 2)
7. **레벨 시스템**: 
   - 1-10 XP: 레벨 1
   - 11-20 XP: 레벨 2
   - 21-30 XP: 레벨 3
   - 31+ XP: 레벨 4

---

## 문의

API 관련 문의사항이 있으시면 백엔드 팀에게 연락 주세요.


# WebSocket API 명세서 목차

## 개요
우리끼리 게임의 모든 실시간 통신은 WebSocket을 통해 이루어집니다.

---

## 연결 및 기본

### [WebSocket 연결](./WEBSOCKET_CONNECTION.md)
- WebSocket 연결 방법
- 인증 (JWT)
- 기본 메시지 구조

---

## 방 관리

### [방 생성](./WEBSOCKET_CREATE_ROOM.md)
- `CREATE_ROOM`: 새 게임 방 생성
- `ROOM_CREATED`: 방 생성 완료 응답

### [방 참가](./WEBSOCKET_JOIN_ROOM.md)
- `JOIN_ROOM`: 기존 방 참가
- `ROOM_JOINED`: 참가 완료 응답
- `USER_JOINED`: 다른 참가자에게 새 유저 알림
- 자동 게임 시작 (4명 모이면)

---

## 게임 진행

### [게임 시작](./WEBSOCKET_JOIN_ROOM.md#game_start)
- `GAME_START`: 4명이 모이면 자동 시작
- 첫 출제자 지정
- 첫 질문 발급

### [카드 제출](./WEBSOCKET_SUBMIT_CARD.md)
- `SUBMIT_CARD`: 참가자가 카드 선택 및 제출
- `CARD_SUBMITTED`: 제출 확인 응답
- `ALL_CARDS_SUBMITTED`: 3명 모두 제출 완료 (출제자에게만)

### [출제자 카드 선택](./WEBSOCKET_EXAMINER_SELECT.md)
- `EXAMINER_SELECT`: 출제자가 승자 선택
- `EXAMINER_SELECTED`: 선택 완료 알림 (모든 참가자)
- 점수 업데이트

### [다음 라운드 시작](./WEBSOCKET_NEXT_ROUND.md)
- `NEXT_ROUND`: 5점 미만일 경우 다음 턴 시작
- 새로운 출제자 지정 (로테이션)
- 새로운 질문 발급

### [게임 종료](./WEBSOCKET_ROUND_END.md)
- `ROUND_END`: 5점 달성 시 게임 종료
- 최종 순위 발표
- 경험치 보상

---

## 메시지 타입 전체 목록

| 메시지 타입 | 방향 | 설명 |
|------------|------|------|
| `CONNECTED` | 서버 → 클라이언트 | 연결 성공 |
| `CREATE_ROOM` | 클라이언트 → 서버 | 방 생성 요청 |
| `ROOM_CREATED` | 서버 → 클라이언트 | 방 생성 완료 |
| `JOIN_ROOM` | 클라이언트 → 서버 | 방 참가 요청 |
| `ROOM_JOINED` | 서버 → 클라이언트 | 방 참가 완료 |
| `USER_JOINED` | 서버 → 다른 참가자들 | 새 유저 참가 알림 |
| `GAME_START` | 서버 → 모든 참가자 | 게임 시작 (4명 모임) |
| `SUBMIT_CARD` | 클라이언트 → 서버 | 카드 제출 |
| `CARD_SUBMITTED` | 서버 → 클라이언트 | 카드 제출 확인 |
| `ALL_CARDS_SUBMITTED` | 서버 → 출제자 | 모든 카드 제출 완료 |
| `EXAMINER_SELECT` | 출제자 → 서버 | 승자 선택 |
| `EXAMINER_SELECTED` | 서버 → 모든 참가자 | 승자 발표 |
| `NEXT_ROUND` | 서버 → 모든 참가자 | 다음 턴 시작 |
| `ROUND_END` | 서버 → 모든 참가자 | 게임 종료 |
| `ERROR` | 서버 → 클라이언트 | 에러 발생 |

---

## 게임 플로우

전체 게임 진행 과정은 [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)를 참고하세요.

### 간단 플로우
```
1. WebSocket 연결
   ↓
2. 방 생성 또는 참가
   ↓
3. 4명 모이면 자동 게임 시작 (GAME_START)
   ↓
4. 반복:
   - 참가자들이 카드 제출 (SUBMIT_CARD)
   - 출제자가 승자 선택 (EXAMINER_SELECT)
   - 승자 발표 (EXAMINER_SELECTED)
   - 5점 미만: 다음 턴 (NEXT_ROUND)
   - 5점 달성: 게임 종료 (ROUND_END)
   ↓
5. 결과 확인 후 로비로 복귀
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
const sendMessage = (type, roomCode, data = null) => {
  ws.send(JSON.stringify({
    type,
    roomCode,
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
ws.send(JSON.stringify({ type: 'JOIN_ROOM', roomCode: '123456' }));
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


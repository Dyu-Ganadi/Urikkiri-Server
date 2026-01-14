# WebSocket API 완전 가이드

## 📚 목차

1. [개요](#개요)
2. [연결 방법](#연결-방법)
3. [로비 API (프론트엔드)](#로비-api-프론트엔드)
4. [게임 API (Unity)](#게임-api-unity)
5. [전체 플로우](#전체-플로우)
6. [에러 처리](#에러-처리)

---

## 개요

우리끼리 게임은 **프론트엔드(로비)**와 **Unity(게임)**가 같은 서버에 각각 별도의 WebSocket 연결을 맺습니다.

```
프론트엔드 ─┐
           ├─→ ws://server/ws ─→ [서버]
Unity 게임 ─┘
```

### 연결 타입

| 클라이언트 | 용도 | 연결 시점 | 종료 시점 |
|-----------|------|----------|----------|
| **프론트엔드** | 방 생성/참여/대기 | 앱 시작 시 | 앱 종료 시 |
| **Unity 게임** | 게임 플레이 | 4명 모임 후 | 게임 종료 시 |

---

## 연결 방법

### 1. WebSocket 연결

**URL**: `ws://localhost:8080/ws?token={accessToken}`

**헤더 (선택)**: `Authorization: Bearer {accessToken}`

**파라미터**:
- `token` (필수): JWT Access Token

### 2. 인증

서버는 토큰을 검증하여 유저를 식별합니다:
```
토큰 → userId → 세션에 User 객체 저장
```

### 3. 메시지 형식

**공통 구조**:
```json
{
  "type": "MESSAGE_TYPE",
  "roomCode": "123456",
  "data": {},
  "message": "설명 메시지"
}
```

**필드 설명**:
- `type` (필수): 메시지 타입 (WebSocketMessageType)
- `roomCode` (선택): 방 코드
- `data` (선택): 메시지 데이터
- `message` (선택): 사람이 읽을 수 있는 설명

---

## 로비 API (프론트엔드)

프론트엔드가 사용하는 WebSocket API입니다. 방 생성, 참여, 대기 기능을 제공합니다.

### 1. CONNECTED - 연결 확인

**방향**: 서버 → 프론트엔드

**설명**: WebSocket 연결이 성공적으로 수립되었음을 알립니다.

**메시지**:
```json
{
  "type": "CONNECTED",
  "message": "WebSocket connection established. Send CREATE_ROOM or JOIN_ROOM message."
}
```

---

### 2. CREATE_ROOM - 방 생성

**방향**: 프론트엔드 → 서버

**설명**: 새로운 게임 방을 생성합니다.

**요청**:
```json
{
  "type": "CREATE_ROOM"
}
```

**응답**: `ROOM_CREATED`

---

### 3. ROOM_CREATED - 방 생성 완료

**방향**: 서버 → 프론트엔드

**설명**: 방 생성이 완료되고 방장으로 입장했습니다.

**메시지**:
```json
{
  "type": "ROOM_CREATED",
  "roomCode": "764185",
  "message": "Room created successfully"
}
```

**필드**:
- `roomCode`: 6자리 방 코드 (친구들과 공유)

---

### 4. JOIN_ROOM - 방 참여

**방향**: 프론트엔드 → 서버

**설명**: 기존 방에 참여합니다.

**요청**:
```json
{
  "type": "JOIN_ROOM",
  "roomCode": "764185"
}
```

**필드**:
- `roomCode` (필수): 참여할 방 코드

**응답**: `ROOM_JOINED`

---

### 5. ROOM_JOINED - 방 참여 완료

**방향**: 서버 → 프론트엔드 (입장한 본인)

**설명**: 방 참여가 완료되었습니다. 현재 방의 모든 참가자 목록을 받습니다.

**메시지**:
```json
{
  "type": "ROOM_JOINED",
  "roomCode": "764185",
  "data": [
    {
      "userId": 1,
      "nickname": "방장",
      "level": 5,
      "isExaminer": true
    },
    {
      "userId": 2,
      "nickname": "나",
      "level": 3,
      "isExaminer": false
    }
  ],
  "message": "Successfully joined room"
}
```

**data 필드**: `ParticipantInfo[]`
- `userId`: 유저 ID
- `nickname`: 닉네임
- `level`: 레벨
- `isExaminer`: 출제자 여부 (첫 라운드 출제자가 미리 지정됨)

---

### 6. USER_JOINED - 새 참가자 입장

**방향**: 서버 → 프론트엔드 (기존 참가자들)

**설명**: 새로운 참가자가 방에 입장했습니다.

**메시지**:
```json
{
  "type": "USER_JOINED",
  "roomCode": "764185",
  "data": {
    "userId": 3,
    "nickname": "친구1",
    "level": 2,
    "isExaminer": false
  },
  "message": "친구1 joined the room"
}
```

**data 필드**: `ParticipantInfo`

---

### 7. GAME_READY - 게임 준비 완료 ⭐

**방향**: 서버 → 프론트엔드 (모든 참가자)

**설명**: 4명이 모두 모였습니다! Unity 게임을 실행하세요.

**메시지**:
```json
{
  "type": "GAME_READY",
  "roomCode": "764185",
  "data": {
    "participants": [
      {
        "userId": 1,
        "nickname": "방장",
        "level": 5,
        "isExaminer": true
      },
      {
        "userId": 2,
        "nickname": "나",
        "level": 3,
        "isExaminer": false
      },
      {
        "userId": 3,
        "nickname": "친구1",
        "level": 2,
        "isExaminer": false
      },
      {
        "userId": 4,
        "nickname": "친구2",
        "level": 7,
        "isExaminer": false
      }
    ],
    "message": "All players ready. Launch Unity game with your token and room code."
  },
  "message": "All players ready! Launch Unity game with your token and room code."
}
```

**data 필드**: `GameReadyData`
- `participants`: 전체 참가자 목록 (4명)

**프론트엔드 처리**:
```javascript
if (message.type === 'GAME_READY') {
    // Unity 게임 실행
    launchUnityGame({
        token: localStorage.getItem('accessToken'),
        roomCode: message.roomCode,
        serverUrl: 'ws://localhost:8080/ws'
    });
    
    // ⚠️ 로비 WebSocket 연결은 유지!
}
```

---

### 8. ROOM_EXIT - 방 나가기

**방향**: 프론트엔드 → 서버

**설명**: 현재 방에서 나갑니다.

**요청**:
```json
{
  "type": "ROOM_EXIT",
  "roomCode": "764185"
}
```

---

## 게임 API (Unity)

Unity 게임이 사용하는 WebSocket API입니다. 게임 플레이 전용입니다.

### 연결 프로세스

```
1. Unity: 프론트엔드에서 token + roomCode 받음
2. Unity: 새 WebSocket 연결 (ws://server/ws?token={token})
3. Unity → 서버: CONNECT_GAME
4. 서버: 토큰으로 유저 확인 → 게임 세션에 추가
5. [4명 모두 연결] → 서버 → Unity: GAME_START
```

---

### 1. CONNECT_GAME - 게임 연결 ⭐

**방향**: Unity → 서버

**설명**: Unity가 게임 서버에 연결을 요청합니다.

**요청**:
```json
{
  "type": "CONNECT_GAME",
  "roomCode": "764185"
}
```

**필드**:
- `roomCode` (필수): 프론트엔드에서 받은 방 코드

**서버 처리**:
1. 토큰에서 userId 추출
2. roomCode + userId로 Participant 조회
3. 게임 세션에 추가
4. 4명 모두 연결되면 → `GAME_START` 전송

---

### 2. GAME_START - 게임 시작 ⭐

**방향**: 서버 → Unity (모든 플레이어)

**설명**: 4명의 Unity가 모두 연결되었습니다. 게임을 시작합니다!

**메시지**:
```json
{
  "type": "GAME_START",
  "roomCode": "764185",
  "data": {
    "participants": [
      {
        "userId": 1,
        "nickname": "방장",
        "level": 5,
        "isExaminer": true
      },
      {
        "userId": 2,
        "nickname": "나",
        "level": 3,
        "isExaminer": false
      },
      {
        "userId": 3,
        "nickname": "친구1",
        "level": 2,
        "isExaminer": false
      },
      {
        "userId": 4,
        "nickname": "친구2",
        "level": 7,
        "isExaminer": false
      }
    ],
    "question": {
      "quizId": 42,
      "content": "가장 좋아하는 음식은?"
    }
  },
  "message": "Game is starting! All 4 players connected."
}
```

**data 필드**: `GameStartData`
- `participants`: 참가자 목록 (isExaminer로 출제자 구분)
- `question`: 첫 번째 질문
  - `quizId`: 질문 ID
  - `content`: 질문 내용

**Unity 처리**:
- 출제자(`isExaminer: true`)는 대기
- 나머지 3명은 카드 선택 화면 표시

---

### 3. SUBMIT_CARD - 카드 제출

**방향**: Unity (출제자 제외) → 서버

**설명**: 선택한 카드를 제출합니다.

**요청**:
```json
{
  "type": "SUBMIT_CARD",
  "roomCode": "764185",
  "data": {
    "cardId": 123
  }
}
```

**필드**:
- `data.cardId` (필수): 선택한 카드 ID (GET /api/cards/random에서 받은 카드)

**응답**: `CARD_SUBMITTED`

**제약**:
- 출제자는 제출 불가
- 한 번 제출하면 수정 불가

---

### 4. CARD_SUBMITTED - 카드 제출 확인

**방향**: 서버 → Unity (제출한 본인)

**설명**: 카드 제출이 완료되었습니다.

**메시지**:
```json
{
  "type": "CARD_SUBMITTED",
  "roomCode": "764185",
  "message": "Card submitted successfully"
}
```

---

### 5. ALL_CARDS_SUBMITTED - 모든 카드 제출 완료

**방향**: 서버 → Unity (출제자만)

**설명**: 3명이 모두 카드를 제출했습니다. 출제자가 선택할 차례입니다.

**메시지**:
```json
{
  "type": "ALL_CARDS_SUBMITTED",
  "roomCode": "764185",
  "data": [
    {
      "participantId": 2,
      "participantNickname": "나",
      "cardId": 123,
      "cardWord": "치킨"
    },
    {
      "participantId": 3,
      "participantNickname": "친구1",
      "cardId": 456,
      "cardWord": "피자"
    },
    {
      "participantId": 4,
      "participantNickname": "친구2",
      "cardId": 789,
      "cardWord": "떡볶이"
    }
  ],
  "message": "All cards have been submitted"
}
```

**data 필드**: `SubmittedCardInfo[]`
- `participantId`: 제출자 ID
- `participantNickname`: 제출자 닉네임
- `cardId`: 카드 ID
- `cardWord`: 카드 단어

**Unity 처리**:
- 출제자 화면에 3개 카드 표시
- 출제자가 가장 적절한 카드 선택

---

### 6. EXAMINER_SELECT - 출제자 선택

**방향**: Unity (출제자만) → 서버

**설명**: 출제자가 승자를 선택합니다.

**요청**:
```json
{
  "type": "EXAMINER_SELECT",
  "roomCode": "764185",
  "data": {
    "participantId": 2
  }
}
```

**필드**:
- `data.participantId` (필수): 선택한 참가자 ID

**응답**: `EXAMINER_SELECTED`

**제약**:
- 출제자만 가능
- ALL_CARDS_SUBMITTED 이후에만 가능

---

### 7. EXAMINER_SELECTED - 선택 결과

**방향**: 서버 → Unity (모든 플레이어)

**설명**: 출제자가 선택을 완료했습니다. 선택된 플레이어의 점수가 올랐습니다.

**메시지**:
```json
{
  "type": "EXAMINER_SELECTED",
  "roomCode": "764185",
  "data": {
    "selectedParticipantId": 2,
    "selectedCardWord": "치킨",
    "winnerNickname": "나",
    "newScore": 1
  },
  "message": "Examiner has selected a card"
}
```

**data 필드**: `ExaminerSelectionDto`
- `selectedParticipantId`: 선택된 참가자 ID
- `selectedCardWord`: 선택된 카드 단어
- `winnerNickname`: 승자 닉네임
- `newScore`: 승자의 새 점수

**Unity 처리**:
- 선택된 카드 강조 표시
- 점수 UI 업데이트
- 다음 메시지 대기 (NEXT_ROUND or ROUND_END)

---

### 8. NEXT_ROUND - 다음 라운드

**방향**: 서버 → Unity (모든 플레이어)

**설명**: 아직 5점에 도달하지 못했습니다. 다음 라운드를 시작합니다.

**메시지**:
```json
{
  "type": "NEXT_ROUND",
  "roomCode": "764185",
  "data": {
    "examinerId": 3,
    "examinerNickname": "친구1",
    "question": {
      "quizId": 88,
      "content": "가장 가고 싶은 여행지는?"
    }
  },
  "message": "Next turn is starting!"
}
```

**data 필드**: `NextRoundData`
- `examinerId`: 새 출제자 ID
- `examinerNickname`: 새 출제자 닉네임
- `question`: 새 질문

**Unity 처리**:
- 출제자 UI 업데이트
- 새 질문 표시
- 카드 선택 화면으로 전환 (출제자 제외)
- 제출된 카드 초기화

**출제자 선택 규칙**:
- 라운드마다 로테이션
- 이전 출제자는 다시 출제자가 되지 않음 (같은 게임 내에서)

---

### 9. ROUND_END - 게임 종료

**방향**: 서버 → Unity (모든 플레이어)

**설명**: 누군가 5점에 도달했습니다! 게임이 종료됩니다.

**메시지**:
```json
{
  "type": "ROUND_END",
  "roomCode": "764185",
  "data": {
    "rankings": [
      {
        "userId": 3,
        "nickname": "친구1",
        "finalScore": 5,
        "rank": 1,
        "xpReward": 20
      },
      {
        "userId": 2,
        "nickname": "나",
        "finalScore": 3,
        "rank": 2,
        "xpReward": 10
      },
      {
        "userId": 1,
        "nickname": "방장",
        "finalScore": 2,
        "rank": 3,
        "xpReward": 5
      },
      {
        "userId": 4,
        "nickname": "친구2",
        "finalScore": 1,
        "rank": 4,
        "xpReward": 2
      }
    ]
  },
  "message": "Game has ended!"
}
```

**data 필드**: `GameResultDto`
- `rankings`: 최종 순위 (`PlayerRankInfo[]`)
  - `userId`: 유저 ID
  - `nickname`: 닉네임
  - `finalScore`: 최종 점수
  - `rank`: 순위 (1~4)
  - `xpReward`: 경험치 보상

**경험치 보상**:
- 1위: 20 EXP
- 2위: 10 EXP
- 3위: 5 EXP
- 4위: 2 EXP

**Unity 처리**:
- 최종 순위 화면 표시
- 경험치 획득 애니메이션
- "로비로 돌아가기" 버튼
- WebSocket 연결 종료
- 프론트엔드로 제어 반환

---

## 전체 플로우

### 단계별 플로우

```
[로비 단계 - 프론트엔드]
1. 프론트엔드 → 서버: WebSocket 연결 (token)
2. 서버 → 프론트엔드: CONNECTED

3. 프론트엔드 → 서버: CREATE_ROOM
4. 서버 → 프론트엔드: ROOM_CREATED (roomCode)

5. 프론트엔드2 → 서버: JOIN_ROOM (roomCode)
6. 서버 → 프론트엔드2: ROOM_JOINED (participants)
7. 서버 → 프론트엔드1: USER_JOINED (new participant)

... (3, 4번째 플레이어도 같은 과정)

[4명 모임]
8. 서버 → 모든 프론트엔드: GAME_READY

[게임 단계 - Unity]
9. 프론트엔드 → Unity: 토큰 + 방코드 전달
10. Unity → 서버: 새 WebSocket 연결 (token)
11. Unity → 서버: CONNECT_GAME (roomCode)

... (4명의 Unity 모두 연결)

[게임 시작]
12. 서버 → 모든 Unity: GAME_START (participants + question)

[라운드 진행]
13. Unity (3명) → 서버: SUBMIT_CARD
14. 서버 → Unity (본인): CARD_SUBMITTED
15. 서버 → Unity (출제자): ALL_CARDS_SUBMITTED
16. Unity (출제자) → 서버: EXAMINER_SELECT
17. 서버 → 모든 Unity: EXAMINER_SELECTED

[분기]
- 5점 미만: 서버 → 모든 Unity: NEXT_ROUND (13번으로)
- 5점 달성: 서버 → 모든 Unity: ROUND_END (종료)

[게임 종료]
18. Unity: WebSocket 연결 종료
19. 프론트엔드: 로비 화면으로 복귀
```

### 다이어그램

```
프론트엔드         서버          Unity
    |              |              |
    |--연결(token)->|              |
    |<-CONNECTED----|              |
    |              |              |
    |--CREATE----->|              |
    |<-CREATED-----|              |
    |              |              |
    [친구 입장...]  |              |
    |<-READY-------|              |
    |              |              |
    |-Unity실행---->|              |
    |              |<--연결(token)|
    |              |              |
    |              |<-CONNECT_GAME|
    |              | (userId확인) |
    |              |              |
    |              [4명 Unity연결] |
    |              |              |
    |              |-GAME_START-->|
    |              |   (질문포함)  |
    |              |              |
    |  [로비 유지]  |<-SUBMIT_CARD-|
    |              |-CARD_SUBMIT->|
    |              |              |
    |              |-ALL_CARDS--->|
    |              |<-EXAMINER_SEL|
    |              |-SELECTED---->|
    |              |              |
    |              |-NEXT/END---->|
    |              |              |
    |<-게임종료알림-|   [연결종료]  |
    |              |              |
```

---

## 에러 처리

### ERROR 메시지

**방향**: 서버 → 클라이언트

**설명**: 요청 처리 중 에러가 발생했습니다.

**메시지**:
```json
{
  "type": "ERROR",
  "message": "에러 메시지"
}
```

### 주요 에러

| 에러 메시지 | 원인 | 해결 방법 |
|-----------|------|----------|
| "Authentication required" | 토큰 없음/만료 | 재로그인 후 새 토큰으로 연결 |
| "Room not found" | 존재하지 않는 방 코드 | 방 코드 확인 |
| "Room is full" | 방이 이미 4명 | 다른 방 참여 |
| "You are not the examiner" | 출제자가 아닌데 선택 시도 | 출제자만 선택 가능 |
| "Examiner cannot submit card" | 출제자가 카드 제출 시도 | 출제자는 대기 |
| "Invalid message format" | 잘못된 JSON 형식 | 메시지 형식 확인 |

---

## 부록

### DTO 클래스

**ParticipantInfo**
```java
{
  "userId": Long,
  "nickname": String,
  "level": Integer,
  "isExaminer": Boolean
}
```

**GameReadyData**
```java
{
  "participants": ParticipantInfo[],
  "message": String
}
```

**GameStartData**
```java
{
  "participants": ParticipantInfo[],
  "question": {
    "quizId": Long,
    "content": String
  }
}
```

**SubmittedCardInfo**
```java
{
  "participantId": Long,
  "participantNickname": String,
  "cardId": Long,
  "cardWord": String
}
```

**ExaminerSelectionDto**
```java
{
  "selectedParticipantId": Long,
  "selectedCardWord": String,
  "winnerNickname": String,
  "newScore": Integer
}
```

**NextRoundData**
```java
{
  "examinerId": Long,
  "examinerNickname": String,
  "question": {
    "quizId": Long,
    "content": String
  }
}
```

**GameResultDto**
```java
{
  "rankings": PlayerRankInfo[]
}
```

**PlayerRankInfo**
```java
{
  "userId": Long,
  "nickname": String,
  "finalScore": Integer,
  "rank": Integer,
  "xpReward": Integer
}
```

---

## 테스트 도구

### curl 예시 (REST API)

```bash
# 로그인
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"1234"}'

# 랜덤 카드 조회
curl -X GET http://localhost:8080/api/cards/random \
  -H "Authorization: Bearer {token}"
```

### JavaScript 테스트 클라이언트

```javascript
const ws = new WebSocket('ws://localhost:8080/ws?token=YOUR_TOKEN');

ws.onopen = () => {
    console.log('Connected');
    ws.send(JSON.stringify({ type: 'CREATE_ROOM' }));
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('Received:', message);
};
```

---

**문서 버전**: 2.0  
**최종 업데이트**: 2026-01-14  
**작성자**: Backend Team


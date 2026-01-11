# WebSocket 게임 플로우

## 개요
게임의 모든 진행(라운드 시작, 카드 제출, 우승자 선택 등)은 WebSocket을 통해 이루어집니다. 
모든 참가자가 동일한 질문을 받아야 하므로, 서버가 질문을 선택하고 브로드캐스트합니다.

## 전체 게임 플로우

```
1. 게임 시작 (GAME_START) - 서버 → 모든 참가자
   - 첫 번째 질문 포함
   ↓
2. 카드 조회 (REST API: GET /play-together/cards) - 출제자 제외
   ↓
3. 카드 제출 (SUBMIT_CARD) - 참가자 → 서버
   ↓
4. 모든 카드 제출 완료 (ALL_CARDS_SUBMITTED) - 서버 → 출제자
   ↓
5. 우승 카드 선택 (SELECT_WINNER) - 출제자 → 서버
   ↓
6. 라운드 결과 (ROUND_RESULT) - 서버 → 모든 참가자
   ↓
7. 다음 라운드 시작 (NEXT_ROUND) - 서버 → 모든 참가자
   - 새로운 질문 포함
   ↓
8. 2번으로 돌아가기 (게임 종료까지 반복)
```

---

## 1. GAME_START (서버 → 모든 참가자)

### 개요
4명이 모이면 서버가 자동으로 게임을 시작하고 첫 번째 질문을 브로드캐스트합니다.

### 메시지 형식
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

### 클라이언트 처리
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'GAME_START') {
    const { participants, question } = message.data;
    
    // 질문 표시
    displayQuestion(question.content);
    
    // 출제자 확인
    const me = participants.find(p => p.userId === myUserId);
    
    if (me.isExaminer) {
      // 출제자: 대기 화면
      showExaminerWaitingScreen();
    } else {
      // 일반 참가자: 카드 조회
      fetchCards();
    }
  }
};
```

---

## 2. SUBMIT_CARD (참가자 → 서버)

### 개요
출제자를 제외한 참가자가 선택한 카드를 제출합니다.

### 요청 메시지
```json
{
  "type": "SUBMIT_CARD",
  "roomCode": "764185",
  "cardId": 23
}
```

### 필드 설명
- `type`: `"SUBMIT_CARD"` (필수)
- `roomCode`: 방 코드 (필수)
- `cardId`: 선택한 카드 ID (필수)

### 클라이언트 예시
```javascript
const submitCard = (cardId) => {
  ws.send(JSON.stringify({
    type: 'SUBMIT_CARD',
    roomCode: roomCode,
    cardId: cardId
  }));
  
  console.log('카드 제출 완료');
};
```

---

## 3. CARD_SUBMITTED (서버 → 제출한 참가자)

### 개요
카드 제출이 성공적으로 처리되었음을 알립니다.

### 메시지 형식
```json
{
  "type": "CARD_SUBMITTED",
  "roomCode": "764185",
  "message": "Card submitted successfully"
}
```

### 클라이언트 처리
```javascript
if (message.type === 'CARD_SUBMITTED') {
  showWaitingMessage('다른 참가자들이 제출하기를 기다리는 중...');
}
```

---

## 4. ALL_CARDS_SUBMITTED (서버 → 출제자)

### 개요
모든 참가자(3명)가 카드를 제출했을 때 출제자에게만 전송됩니다.

### 메시지 형식
```json
{
  "type": "ALL_CARDS_SUBMITTED",
  "roomCode": "764185",
  "data": [
    {
      "participantId": 2,
      "nickname": "참가자1",
      "cardId": 5,
      "word": "다솜",
      "meaning": "사랑"
    },
    {
      "participantId": 3,
      "nickname": "참가자2",
      "cardId": 12,
      "word": "미리내",
      "meaning": "은하수"
    },
    {
      "participantId": 4,
      "nickname": "참가자3",
      "cardId": 8,
      "word": "바람꽃",
      "meaning": "바람에 흔들리는 꽃"
    }
  ],
  "message": "All cards have been submitted"
}
```

### 클라이언트 처리 (출제자만)
```javascript
if (message.type === 'ALL_CARDS_SUBMITTED') {
  const cards = message.data;
  displaySubmittedCards(cards);
  showSelectWinnerButton();
}
```

---

## 5. SELECT_WINNER (출제자 → 서버)

### 개요
출제자가 가장 마음에 드는 카드를 선택합니다.

### 요청 메시지
```json
{
  "type": "SELECT_WINNER",
  "roomCode": "764185",
  "participantId": 3
}
```

### 필드 설명
- `type`: `"SELECT_WINNER"` (필수)
- `roomCode`: 방 코드 (필수)
- `participantId`: 우승자의 참가자 ID (필수)

### 클라이언트 예시 (출제자)
```javascript
const selectWinner = (participantId) => {
  ws.send(JSON.stringify({
    type: 'SELECT_WINNER',
    roomCode: roomCode,
    participantId: participantId
  }));
};
```

---

## 6. ROUND_RESULT (서버 → 모든 참가자)

### 개요
라운드 우승자와 선택된 카드 정보를 모든 참가자에게 브로드캐스트합니다.

### 메시지 형식
```json
{
  "type": "ROUND_RESULT",
  "roomCode": "764185",
  "data": {
    "winnerId": 3,
    "winnerNickname": "참가자2",
    "cardId": 12,
    "word": "미리내",
    "meaning": "은하수",
    "newScore": 1
  },
  "message": "참가자2 wins this round!"
}
```

### 필드 설명
- `winnerId`: 우승자 사용자 ID
- `winnerNickname`: 우승자 닉네임
- `cardId`: 선택된 카드 ID
- `word`: 선택된 카드 단어
- `meaning`: 선택된 카드 뜻
- `newScore`: 우승자의 새로운 점수 (bananaScore)

### 클라이언트 처리
```javascript
if (message.type === 'ROUND_RESULT') {
  const { winnerNickname, word, meaning, newScore } = message.data;
  
  showRoundResult({
    winner: winnerNickname,
    card: `${word} (${meaning})`,
    score: newScore
  });
  
  // 3초 후 다음 라운드 대기
  setTimeout(() => {
    showWaitingForNextRound();
  }, 3000);
}
```

---

## 7. NEXT_ROUND (서버 → 모든 참가자)

### 개요
다음 라운드를 시작하고 새로운 질문을 브로드캐스트합니다.

### 메시지 형식
```json
{
  "type": "NEXT_ROUND",
  "roomCode": "764185",
  "data": {
    "round": 2,
    "question": {
      "quizId": 58,
      "content": "가장 가고 싶은 여행지는?"
    }
  },
  "message": "Round 2 is starting!"
}
```

### 필드 설명
- `round`: 현재 라운드 번호
- `question`: 새로운 질문
  - `quizId`: 질문 ID
  - `content`: 질문 내용

### 클라이언트 처리
```javascript
if (message.type === 'NEXT_ROUND') {
  const { round, question } = message.data;
  
  // 새 질문 표시
  displayQuestion(question.content);
  displayRoundNumber(round);
  
  // 출제자 확인
  const me = getCurrentParticipant();
  
  if (me.isExaminer) {
    showExaminerWaitingScreen();
  } else {
    // 새 카드 조회
    fetchCards();
  }
}
```

---

## React 전체 예시

```typescript
import { useEffect, useState } from 'react';

interface Participant {
  userId: number;
  nickname: string;
  level: number;
  isExaminer?: boolean;
}

interface Question {
  quizId: number;
  content: string;
}

interface Card {
  cardId: number;
  word: string;
  meaning: string;
}

interface SubmittedCard extends Card {
  participantId: number;
  nickname: string;
}

interface RoundResult {
  winnerId: number;
  winnerNickname: string;
  cardId: number;
  word: string;
  meaning: string;
  newScore: number;
}

const GameScreen = ({ roomCode, ws }: { roomCode: string; ws: WebSocket }) => {
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [question, setQuestion] = useState<Question | null>(null);
  const [round, setRound] = useState(1);
  const [myCards, setMyCards] = useState<Card[]>([]);
  const [submittedCards, setSubmittedCards] = useState<SubmittedCard[]>([]);
  const [isExaminer, setIsExaminer] = useState(false);
  const [roundResult, setRoundResult] = useState<RoundResult | null>(null);

  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      switch (message.type) {
        case 'GAME_START':
          handleGameStart(message.data);
          break;
        
        case 'CARD_SUBMITTED':
          showWaitingMessage();
          break;
        
        case 'ALL_CARDS_SUBMITTED':
          setSubmittedCards(message.data);
          break;
        
        case 'ROUND_RESULT':
          setRoundResult(message.data);
          setTimeout(() => setRoundResult(null), 3000);
          break;
        
        case 'NEXT_ROUND':
          handleNextRound(message.data);
          break;
      }
    };
  }, [ws]);

  const handleGameStart = async (data: any) => {
    setParticipants(data.participants);
    setQuestion(data.question);
    
    const me = data.participants.find((p: Participant) => p.userId === myUserId);
    setIsExaminer(me.isExaminer);
    
    if (!me.isExaminer) {
      await fetchCards();
    }
  };

  const handleNextRound = async (data: any) => {
    setRound(data.round);
    setQuestion(data.question);
    setSubmittedCards([]);
    
    if (!isExaminer) {
      await fetchCards();
    }
  };

  const fetchCards = async () => {
    const token = localStorage.getItem('accessToken');
    const response = await fetch('/play-together/cards', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();
    setMyCards(data.cards);
  };

  const submitCard = (cardId: number) => {
    ws.send(JSON.stringify({
      type: 'SUBMIT_CARD',
      roomCode: roomCode,
      cardId: cardId
    }));
  };

  const selectWinner = (participantId: number) => {
    ws.send(JSON.stringify({
      type: 'SELECT_WINNER',
      roomCode: roomCode,
      participantId: participantId
    }));
  };

  return (
    <div>
      <h2>라운드 {round}</h2>
      <h3>질문: {question?.content}</h3>
      
      {roundResult && (
        <div className="round-result">
          <h2>{roundResult.winnerNickname} 승리!</h2>
          <p>{roundResult.word} ({roundResult.meaning})</p>
          <p>점수: {roundResult.newScore}</p>
        </div>
      )}
      
      {isExaminer ? (
        <ExaminerView 
          submittedCards={submittedCards}
          onSelectWinner={selectWinner}
        />
      ) : (
        <PlayerView 
          cards={myCards}
          onSubmitCard={submitCard}
        />
      )}
    </div>
  );
};
```

---

## 주의사항

1. **질문은 항상 WebSocket으로**: REST API로 질문을 조회하지 않습니다.
2. **동일한 질문 보장**: 서버가 한 번만 질문을 조회하고 브로드캐스트하므로 모든 참가자가 같은 질문을 받습니다.
3. **라운드 동기화**: `NEXT_ROUND` 메시지로 모든 참가자가 동시에 다음 라운드를 시작합니다.
4. **출제자 제외**: 출제자는 카드를 받지 않고 대기합니다.

## 관련 API
- [카드 조회 API (REST)](../api/GET_RANDOM_CARDS.md)
- [방 참가 (WebSocket)](../api/WEBSOCKET_JOIN_ROOM.md)


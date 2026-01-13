# WebSocket 게임 플로우

## 개요
게임의 모든 진행(라운드 시작, 카드 제출, 우승자 선택 등)은 WebSocket을 통해 이루어집니다. 
모든 참가자가 동일한 질문을 받아야 하므로, 서버가 질문을 선택하고 브로드캐스트합니다.

## 전체 게임 플로우

```
1. 게임 시작 (GAME_START) - 서버 → 모든 참가자
   - 첫 번째 질문 포함
   - 첫 번째 출제자 지정
   ↓
2. 카드 조회 (REST API: GET /play-together/cards) - 출제자 제외 3명
   ↓
3. 카드 제출 (SUBMIT_CARD) - 참가자 → 서버
   ↓
4. 모든 카드 제출 완료 (ALL_CARDS_SUBMITTED) - 서버 → 출제자
   ↓
5. 우승 카드 선택 (EXAMINER_SELECT) - 출제자 → 서버
   ↓
6. 선택 완료 알림 (EXAMINER_SELECTED) - 서버 → 모든 참가자
   ↓
7-A. 5점 미만: 다음 턴 시작 (NEXT_ROUND) - 서버 → 모든 참가자
     - 새로운 출제자 지정
     - 새로운 질문 포함
     → 2번으로 돌아가기
   
7-B. 5점 달성: 게임 종료 (ROUND_END) - 서버 → 모든 참가자
     - 최종 순위 발표
     - 경험치 보상
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

## 5. EXAMINER_SELECT (출제자 → 서버)

### 개요
출제자가 가장 마음에 드는 카드를 선택합니다.

### 요청 메시지
```json
{
  "type": "EXAMINER_SELECT",
  "roomCode": "764185",
  "data": {
    "participantId": 3
  }
}
```

### 필드 설명
- `type`: `"EXAMINER_SELECT"` (필수)
- `roomCode`: 방 코드 (필수)
- `data.participantId`: 승자의 User ID (필수)

### 클라이언트 예시 (출제자)
```javascript
const selectWinner = (participantId) => {
  ws.send(JSON.stringify({
    type: 'EXAMINER_SELECT',
    roomCode: roomCode,
    data: { participantId }
  }));
};
```

---

## 6. EXAMINER_SELECTED (서버 → 모든 참가자)

### 개요
출제자가 승자를 선택한 후 모든 참가자에게 결과를 알립니다.

### 메시지 형식
```json
{
  "type": "EXAMINER_SELECTED",
  "roomCode": "764185",
  "data": {
    "participantId": 3,
    "cardWord": "미리내",
    "winnerNickname": "참가자2",
    "newBananaScore": 2
  },
  "message": "Examiner has selected a card"
}
```

### 필드 설명
- `participantId`: 승자의 User ID
- `cardWord`: 선택된 카드의 단어
- `winnerNickname`: 승자 닉네임
- `newBananaScore`: 승자의 새로운 점수

### 클라이언트 처리
```javascript
if (message.type === 'EXAMINER_SELECTED') {
  const { winnerNickname, cardWord, newBananaScore } = message.data;
  
  // 승자 발표
  showWinnerAnnouncement(winnerNickname, cardWord, newBananaScore);
  
  // 점수판 업데이트
  updateScore(message.data.participantId, newBananaScore);
}
```

---

## 7. NEXT_ROUND (서버 → 모든 참가자)

### 개요
5점 미만일 경우 다음 턴이 시작됩니다. 새로운 출제자와 질문이 발급됩니다.

### 메시지 형식
```json
{
  "type": "NEXT_ROUND",
  "roomCode": "764185",
  "data": {
    "newExaminerId": 4,
    "newExaminerNickname": "참가자3",
    "quiz": {
      "id": 15,
      "content": "가장 행복했던 순간은?"
    }
  },
  "message": "Next turn is starting!"
}
```

### 필드 설명
- `newExaminerId`: 새 출제자의 User ID
- `newExaminerNickname`: 새 출제자 닉네임
- `quiz`: 새로운 질문

### 클라이언트 처리
```javascript
if (message.type === 'NEXT_ROUND') {
  const { newExaminerId, newExaminerNickname, quiz } = message.data;
  
  // 내가 새 출제자인지 확인
  const isExaminer = (myUserId === newExaminerId);
  
  // 질문 표시
  displayQuestion(quiz.content);
  
  if (isExaminer) {
    showExaminerWaitingScreen();
  } else {
    fetchCards(); // 카드 조회
  }
}
```

---

## 8. ROUND_END (서버 → 모든 참가자)

### 개요
누군가 5점을 달성하면 게임이 종료되고 최종 순위가 발표됩니다.

### 메시지 형식
```json
{
  "type": "ROUND_END",
  "roomCode": "764185",
  "data": {
    "winnerNickname": "참가자2",
    "rankings": [
      {
        "rank": 1,
        "userId": 3,
        "nickname": "참가자2",
        "bananaScore": 5,
        "xpReward": 20
      },
      {
        "rank": 2,
        "userId": 2,
        "nickname": "참가자1",
        "bananaScore": 3,
        "xpReward": 10
      },
      {
        "rank": 3,
        "userId": 1,
        "nickname": "방장",
        "bananaScore": 2,
        "xpReward": 5
      },
      {
        "rank": 4,
        "userId": 4,
        "nickname": "참가자3",
        "bananaScore": 1,
        "xpReward": 2
      }
    ]
  },
  "message": "Game has ended"
}
```

### 클라이언트 처리
```javascript
if (message.type === 'ROUND_END') {
  const { winnerNickname, rankings } = message.data;
  
  // 게임 종료 화면 표시
  showGameResult(winnerNickname, rankings);
  
  // WebSocket 연결 종료
  setTimeout(() => {
    ws.close();
    goToLobby();
  }, 5000);
}
```
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
  participantId: number;
  cardWord: string;
  winnerNickname: string;
  newBananaScore: number;
}

interface GameResult {
  winnerNickname: string;
  rankings: Array<{
    rank: number;
    userId: number;
    nickname: string;
    bananaScore: number;
    xpReward: number;
  }>;
}

const GameScreen = ({ roomCode, ws }: { roomCode: string; ws: WebSocket }) => {
  const [participants, setParticipants] = useState<Participant[]>([]);
  const [question, setQuestion] = useState<Question | null>(null);
  const [myCards, setMyCards] = useState<Card[]>([]);
  const [submittedCards, setSubmittedCards] = useState<SubmittedCard[]>([]);
  const [isExaminer, setIsExaminer] = useState(false);
  const [roundResult, setRoundResult] = useState<RoundResult | null>(null);
  const [gameResult, setGameResult] = useState<GameResult | null>(null);

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
        
        case 'EXAMINER_SELECTED':
          setRoundResult(message.data);
          setTimeout(() => setRoundResult(null), 3000);
          break;
        
        case 'NEXT_ROUND':
          handleNextRound(message.data);
          break;
        
        case 'ROUND_END':
          setGameResult(message.data);
          break;
      }
    };
  }, [ws]);

  const handleGameStart = async (data: any) => {
    setParticipants(data.participants);
    setQuestion(data.quiz);
    
    const me = data.participants.find((p: Participant) => p.userId === myUserId);
    setIsExaminer(me.isExaminer);
    
    if (!me.isExaminer) {
      await fetchCards();
    }
  };

  const handleNextRound = async (data: any) => {
    setQuestion(data.quiz);
    setSubmittedCards([]);
    
    // 내가 새 출제자인지 확인
    const amINewExaminer = (myUserId === data.newExaminerId);
    setIsExaminer(amINewExaminer);
    
    if (!amINewExaminer) {
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
      data: { cardId }
    }));
  };

  const selectWinner = (participantId: number) => {
    ws.send(JSON.stringify({
      type: 'EXAMINER_SELECT',
      roomCode: roomCode,
      data: { participantId }
    }));
  };

  if (gameResult) {
    return <GameResultScreen result={gameResult} />;
  }

  return (
    <div>
      <h3>질문: {question?.content}</h3>
      
      {roundResult && (
        <div className="round-result">
          <h2>{roundResult.winnerNickname} 승리!</h2>
          <p>{roundResult.cardWord}</p>
          <p>점수: {roundResult.newBananaScore}</p>
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
3. **출제자 로테이션**: 출제자는 매 턴마다 변경되며, 아직 출제자가 아니었던 사람 중 랜덤 선택됩니다.
4. **출제자 제외**: 출제자는 카드를 받지 않고 대기합니다.
5. **5점 달성**: 누군가 5점을 달성하면 `ROUND_END`로 게임이 종료됩니다.
6. **점수 기록**: 바나나 점수는 DB에 저장되며, 게임 종료 시 경험치로 변환됩니다.

## 관련 API
- [카드 조회 API (REST)](../api/GET_RANDOM_CARDS.md)
- [방 참가 (WebSocket)](../api/WEBSOCKET_JOIN_ROOM.md)
- [카드 제출 (WebSocket)](../api/WEBSOCKET_SUBMIT_CARD.md)
- [출제자 카드 선택 (WebSocket)](../api/WEBSOCKET_EXAMINER_SELECT.md)
- [다음 라운드 시작 (WebSocket)](../api/WEBSOCKET_NEXT_ROUND.md)
- [게임 종료 (WebSocket)](../api/WEBSOCKET_ROUND_END.md)


# 카드 제출 API (WebSocket)

## 개요
참가자(출제자 제외)가 선택한 카드를 WebSocket을 통해 제출합니다. 3명의 참가자가 모두 카드를 제출하면 출제자에게 자동으로 알림이 전송됩니다.

## 전제 조건
- WebSocket 연결이 되어 있어야 함
- 게임이 시작된 상태 (`GAME_START` 수신)
- 출제자가 아닌 일반 참가자만 제출 가능
- REST API로 카드 5개를 조회한 상태

---

## 1. SUBMIT_CARD (참가자 → 서버)

### 개요
참가자가 선택한 카드를 서버에 제출합니다.

### 요청 메시지
```json
{
  "type": "SUBMIT_CARD",
  "roomCode": "764185",
  "data": {
    "cardId": 23
  }
}
```

### 필드 설명
- `type` (string, 필수): `"SUBMIT_CARD"`
- `roomCode` (string, 필수): 방 코드 (6자리)
- `data` (object, 필수): 제출 데이터
  - `cardId` (number, 필수): 선택한 카드 ID

### 클라이언트 예시

#### JavaScript
```javascript
const submitCard = (cardId) => {
  const message = {
    type: 'SUBMIT_CARD',
    roomCode: roomCode,
    data: {
      cardId: cardId
    }
  };
  
  ws.send(JSON.stringify(message));
  console.log('카드 제출:', cardId);
};

// 카드 선택 시
document.querySelectorAll('.card').forEach(card => {
  card.addEventListener('click', () => {
    const cardId = parseInt(card.dataset.cardId);
    submitCard(cardId);
    
    // UI 업데이트
    card.classList.add('selected');
    disableAllCards();
  });
});
```

#### React
```typescript
const submitCard = (cardId: number) => {
  if (!ws) return;
  
  const message = {
    type: 'SUBMIT_CARD',
    roomCode: roomCode,
    data: { cardId }
  };
  
  ws.send(JSON.stringify(message));
  setSelectedCard(cardId);
  setIsSubmitted(true);
};

// 컴포넌트에서
<div className="cards">
  {cards.map(card => (
    <button
      key={card.cardId}
      onClick={() => submitCard(card.cardId)}
      disabled={isSubmitted}
      className={selectedCard === card.cardId ? 'selected' : ''}
    >
      <h3>{card.word}</h3>
      <p>{card.meaning}</p>
    </button>
  ))}
</div>
```

---

## 2. CARD_SUBMITTED (서버 → 제출한 참가자)

### 개요
카드 제출이 성공적으로 처리되었음을 확인하는 응답입니다.

### 응답 메시지
```json
{
  "type": "CARD_SUBMITTED",
  "roomCode": "764185",
  "message": "Card submitted successfully"
}
```

### 필드 설명
- `type` (string): `"CARD_SUBMITTED"`
- `roomCode` (string): 방 코드
- `message` (string): 성공 메시지

### 클라이언트 처리

#### JavaScript
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'CARD_SUBMITTED') {
    console.log('카드 제출 완료!');
    
    // UI 업데이트
    showWaitingMessage('다른 참가자들이 제출하기를 기다리는 중...');
    disableCardSelection();
  }
};

function showWaitingMessage(text) {
  const waitingDiv = document.getElementById('waiting-message');
  waitingDiv.textContent = text;
  waitingDiv.style.display = 'block';
}

function disableCardSelection() {
  document.querySelectorAll('.card').forEach(card => {
    card.style.pointerEvents = 'none';
    card.classList.add('disabled');
  });
}
```

#### React
```typescript
interface Message {
  type: string;
  roomCode: string;
  message?: string;
  data?: any;
}

const handleWebSocketMessage = (event: MessageEvent) => {
  const message: Message = JSON.parse(event.data);
  
  switch (message.type) {
    case 'CARD_SUBMITTED':
      setIsSubmitted(true);
      setWaitingMessage('다른 참가자들이 제출하기를 기다리는 중...');
      break;
  }
};

// UI
{isSubmitted && (
  <div className="waiting-overlay">
    <div className="spinner"></div>
    <p>{waitingMessage}</p>
  </div>
)}
```

---

## 3. ALL_CARDS_SUBMITTED (서버 → 출제자)

### 개요
3명의 참가자가 모두 카드를 제출하면 **출제자에게만** 전송되는 메시지입니다. 제출된 모든 카드 정보가 포함됩니다.

### 응답 메시지
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

### 필드 설명
- `type` (string): `"ALL_CARDS_SUBMITTED"`
- `roomCode` (string): 방 코드
- `data` (array): 제출된 카드 목록 (3개)
  - `participantId` (number): 참가자 ID (우승자 선택 시 사용)
  - `nickname` (string): 사용자 닉네임
  - `cardId` (number): 제출된 카드 ID
  - `word` (string): 카드 단어
  - `meaning` (string): 카드 뜻
- `message` (string): 완료 메시지

### 클라이언트 처리 (출제자만)

#### JavaScript
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'ALL_CARDS_SUBMITTED') {
    console.log('모든 카드 제출 완료!');
    
    const submittedCards = message.data;
    displaySubmittedCards(submittedCards);
    showSelectWinnerButton();
  }
};

function displaySubmittedCards(cards) {
  const container = document.getElementById('submitted-cards');
  container.innerHTML = '<h2>제출된 카드들</h2>';
  
  cards.forEach(card => {
    const cardDiv = document.createElement('div');
    cardDiv.className = 'submitted-card';
    cardDiv.innerHTML = `
      <h3>${card.word}</h3>
      <p>${card.meaning}</p>
      <button onclick="selectWinner(${card.participantId})">
        이 카드 선택
      </button>
    `;
    container.appendChild(cardDiv);
  });
}

function selectWinner(participantId) {
  // EXAMINER_SELECT 메시지 전송 (다음 API 참고)
  ws.send(JSON.stringify({
    type: 'EXAMINER_SELECT',
    roomCode: roomCode,
    data: { participantId }
  }));
}
```

#### React
```typescript
interface SubmittedCard {
  participantId: number;
  nickname: string;
  cardId: number;
  word: string;
  meaning: string;
}

const ExaminerView = () => {
  const [submittedCards, setSubmittedCards] = useState<SubmittedCard[]>([]);
  const [selectedWinner, setSelectedWinner] = useState<number | null>(null);

  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'ALL_CARDS_SUBMITTED') {
        setSubmittedCards(message.data);
      }
    };
  }, []);

  const handleSelectWinner = (participantId: number) => {
    setSelectedWinner(participantId);
    
    ws.send(JSON.stringify({
      type: 'EXAMINER_SELECT',
      roomCode: roomCode,
      data: { participantId }
    }));
  };

  if (submittedCards.length === 0) {
    return (
      <div className="waiting-screen">
        <h2>출제자 대기 중</h2>
        <p>참가자들이 카드를 제출하기를 기다리는 중...</p>
        <div className="spinner"></div>
      </div>
    );
  }

  return (
    <div className="examiner-selection">
      <h2>가장 마음에 드는 카드를 선택하세요</h2>
      <div className="cards-grid">
        {submittedCards.map(card => (
          <div
            key={card.participantId}
            className={`card ${selectedWinner === card.participantId ? 'selected' : ''}`}
            onClick={() => handleSelectWinner(card.participantId)}
          >
            <h3>{card.word}</h3>
            <p>{card.meaning}</p>
            <small>제출자: {card.nickname}</small>
          </div>
        ))}
      </div>
    </div>
  );
};
```

---

## 전체 플로우

```
1. 게임 시작 (GAME_START)
   ↓
2. 참가자들이 카드 조회 (REST API: GET /play-together/cards)
   ↓
3. 참가자 A가 카드 선택 및 제출 (SUBMIT_CARD)
   → 서버: 메모리에 저장
   → 참가자 A: CARD_SUBMITTED 수신
   ↓
4. 참가자 B가 카드 제출 (SUBMIT_CARD)
   → 서버: 메모리에 저장
   → 참가자 B: CARD_SUBMITTED 수신
   ↓
5. 참가자 C가 카드 제출 (SUBMIT_CARD) - 마지막!
   → 서버: 메모리에 저장
   → 참가자 C: CARD_SUBMITTED 수신
   → 서버: 3명 완료 감지!
   → 출제자: ALL_CARDS_SUBMITTED 수신 (제출된 카드 3개 포함)
   ↓
6. 출제자가 우승 카드 선택 (EXAMINER_SELECT)
   ↓
7. 결과 발표 (EXAMINER_SELECTED)
   ↓
8. 다음 턴 또는 게임 종료 (NEXT_ROUND / ROUND_END)
```

---

## 에러 응답

### 1. 방 코드 누락
```json
{
  "type": "ERROR",
  "message": "Room Code is Required"
}
```

### 2. 출제자는 카드 제출 불가
```json
{
  "type": "ERROR",
  "message": "Examiner Cannot Submit Card"
}
```
**발생 원인:** 출제자가 카드를 제출하려고 시도한 경우

### 3. 카드를 찾을 수 없음
```json
{
  "type": "ERROR",
  "message": "Card Not Found"
}
```

### 4. 참가자를 찾을 수 없음
```json
{
  "type": "ERROR",
  "message": "Participant Not Found"
}
```

### 5. 잘못된 메시지 형식
```json
{
  "type": "ERROR",
  "message": "Invalid WebSocket Message Format"
}
```

---

## 주의사항

1. **출제자는 제출 불가**: 출제자가 카드 제출을 시도하면 `EXAMINER_CANNOT_SUBMIT_CARD` 에러가 발생합니다.
2. **중복 제출 방지**: 클라이언트에서 제출 후 카드 선택을 비활성화해야 합니다.
3. **3명 완료 후 자동 처리**: 서버가 자동으로 감지하여 출제자에게 알림을 보냅니다.
4. **메모리 저장**: 제출된 카드는 DB가 아닌 메모리에 저장되며, 라운드 종료 시 초기화됩니다.
5. **순서 무관**: 어떤 순서로 제출해도 상관없습니다.

---

## 상태 관리 (메모리)

서버는 다음 정보를 메모리에서 관리합니다:

```java
// GameRoundManager
Map<String, List<SubmittedCardInfo>> submittedCards;

// 방별로 제출된 카드 추적
roomCode: "764185" → [
  { participantId: 2, cardId: 5, word: "다솜", ... },
  { participantId: 3, cardId: 12, word: "미리내", ... },
  { participantId: 4, cardId: 8, word: "바람꽃", ... }
]
```

---

## UI/UX 권장사항

### 일반 참가자 화면
1. **카드 선택 화면**
   - 5개 카드를 그리드로 표시
   - 클릭 시 선택 강조 표시
   - 제출 버튼 활성화

2. **제출 후 대기 화면**
   - 선택한 카드 표시
   - "다른 참가자 대기 중..." 메시지
   - 로딩 스피너

3. **진행 상황 표시**
   - "제출 완료: 1/3", "제출 완료: 2/3" 등

### 출제자 화면
1. **대기 화면**
   - "참가자들이 카드를 제출하는 중..." 메시지
   - 진행 바 또는 스피너

2. **선택 화면**
   - 제출된 3개 카드를 크게 표시
   - 각 카드에 제출자 닉네임 표시
   - 클릭 시 선택 효과
   - 확인 버튼

---

## 관련 API
- [카드 조회 (REST)](./GET_RANDOM_CARDS.md)
- [출제자 카드 선택](./WEBSOCKET_EXAMINER_SELECT.md)
- [다음 라운드 시작](./WEBSOCKET_NEXT_ROUND.md)
- [게임 종료](./WEBSOCKET_ROUND_END.md)
- [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)
- [방 참가](./WEBSOCKET_JOIN_ROOM.md)


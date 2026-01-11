# 카드 조회 API

## 개요
게임에서 사용할 랜덤 순우리말 카드 5개를 조회합니다. 출제자를 제외한 일반 참가자들이 질문을 받은 후 카드를 조회합니다.

## 요청

### HTTP 요청
```
GET /play-together/cards
```

### 헤더
```
Authorization: Bearer {accessToken}
```

### 요청 예시
```bash
curl -X GET "http://localhost:8080/play-together/cards" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## 응답

### 성공 응답 (200 OK)
```json
{
  "cards": [
    {
      "cardId": 1,
      "word": "가람",
      "meaning": "강"
    },
    {
      "cardId": 5,
      "word": "다솜",
      "meaning": "사랑"
    },
    {
      "cardId": 12,
      "word": "미리내",
      "meaning": "은하수"
    },
    {
      "cardId": 23,
      "word": "노을",
      "meaning": "저녁 하늘의 빛"
    },
    {
      "cardId": 8,
      "word": "바람꽃",
      "meaning": "바람에 흔들리는 꽃"
    }
  ]
}
```

### 필드 설명
- `cards` (array): 카드 목록 (항상 5개)
  - `cardId` (number): 카드 ID
  - `word` (string): 순우리말 단어
  - `meaning` (string): 단어의 뜻

## 사용 시나리오

### 1. 게임 시작 후 카드 조회
```javascript
// WebSocket에서 GAME_START 받은 후
ws.onmessage = async (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'GAME_START') {
    // 1. 질문은 이미 메시지에 포함되어 있음
    const question = message.data.question;
    console.log('질문:', question.content);
    
    // 2. 출제자가 아니면 카드 조회
    const myParticipant = message.data.participants.find(p => p.userId === myUserId);
    if (!myParticipant.isExaminer) {
      const cards = await fetch('/play-together/cards', {
        headers: { 'Authorization': `Bearer ${token}` }
      }).then(res => res.json());
      
      console.log('내 카드:', cards.cards);
      displayCards(cards.cards);
    }
  }
};
```

### 2. 카드 선택 후 제출
```javascript
const selectCard = (cardId) => {
  // WebSocket으로 카드 제출
  ws.send(JSON.stringify({
    type: 'SUBMIT_CARD',
    roomCode: roomCode,
    cardId: cardId
  }));
};
```

## React 예시

```typescript
import { useEffect, useState } from 'react';

interface Card {
  cardId: number;
  word: string;
  meaning: string;
}

interface CardListResponse {
  cards: Card[];
}

const CardSelection = ({ isExaminer }: { isExaminer: boolean }) => {
  const [cards, setCards] = useState<Card[]>([]);
  const [selectedCard, setSelectedCard] = useState<number | null>(null);

  useEffect(() => {
    if (!isExaminer) {
      fetchCards();
    }
  }, [isExaminer]);

  const fetchCards = async () => {
    try {
      const token = localStorage.getItem('accessToken');
      const response = await fetch('/play-together/cards', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });
      
      const data: CardListResponse = await response.json();
      setCards(data.cards);
    } catch (error) {
      console.error('카드 조회 실패:', error);
    }
  };

  const handleCardSelect = (cardId: number) => {
    setSelectedCard(cardId);
    // WebSocket으로 제출
    // ws.send(JSON.stringify({ type: 'SUBMIT_CARD', cardId }));
  };

  if (isExaminer) {
    return <div>출제자는 카드를 받을 수 없습니다. 다른 참가자들이 제출하기를 기다려주세요.</div>;
  }

  return (
    <div>
      <h3>카드를 선택하세요</h3>
      <div className="card-grid">
        {cards.map(card => (
          <div 
            key={card.cardId} 
            className={`card ${selectedCard === card.cardId ? 'selected' : ''}`}
            onClick={() => handleCardSelect(card.cardId)}
          >
            <h4>{card.word}</h4>
            <p>{card.meaning}</p>
          </div>
        ))}
      </div>
    </div>
  );
};

export default CardSelection;
```

## 게임 플로우

```
1. 게임 시작 (WebSocket: GAME_START) - 질문 포함
   ↓
2. 카드 조회 (REST: GET /play-together/cards) - 출제자 제외
   ↓
3. 카드 선택 및 제출 (WebSocket: SUBMIT_CARD) - 출제자 제외
   ↓
4. 모든 카드 제출 완료 (WebSocket: ALL_CARDS_SUBMITTED) - 출제자에게
   ↓
5. 출제자 우승 카드 선택 (WebSocket: SELECT_WINNER)
   ↓
6. 라운드 결과 발표 (WebSocket: ROUND_RESULT) - 모든 참가자
   ↓
7. 다음 라운드 시작 (WebSocket: NEXT_ROUND) - 새로운 질문 포함
   ↓
8. 2번으로 돌아가기
```

**중요:** 질문은 모두 WebSocket 메시지(`GAME_START`, `NEXT_ROUND`)에 포함되어 있습니다. 
카드만 REST API로 각자 조회합니다.

## 에러 응답

### 401 Unauthorized
```json
{
  "status": "UNAUTHORIZED",
  "message": "Expired JWT",
  "timestamp": "2026-01-11T10:30:00"
}
```

### 404 Not Found
```json
{
  "status": "NOT_FOUND",
  "message": "Card Not Found",
  "timestamp": "2026-01-11T10:30:00"
}
```

## 주의사항

1. **랜덤 조회**: 매번 호출 시 다른 5개의 카드가 반환됩니다.
2. **출제자 제외**: 출제자는 이 API를 호출하지 않습니다.
3. **각 유저마다 다름**: 참가자마다 다른 5개의 카드를 받습니다.
4. **한 번만 호출**: 라운드당 한 번만 호출하면 됩니다.

## 관련 API
- [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)
- [방 참가 API](./WEBSOCKET_JOIN_ROOM.md)


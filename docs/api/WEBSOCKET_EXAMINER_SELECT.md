# 출제자 카드 선택 API (WebSocket)

## 개요
출제자가 제출된 카드 중 가장 마음에 드는 카드를 선택하여 승자를 결정합니다. 선택된 참가자는 바나나 점수 1점을 획득하고, 5점 미만이면 다음 턴으로 진행됩니다.

## 전제 조건
- WebSocket 연결이 되어 있어야 함
- 게임이 시작된 상태
- 3명의 참가자가 모두 카드를 제출한 상태 (`ALL_CARDS_SUBMITTED` 수신)
- 현재 사용자가 출제자여야 함

---

## 1. EXAMINER_SELECT (출제자 → 서버)

### 개요
출제자가 승자를 선택합니다.

### 요청 메시지
```json
{
  "type": "EXAMINER_SELECT",
  "roomCode": "764185",
  "data": {
    "participantId": 2
  }
}
```

### 필드 설명
- `type` (string, 필수): `"EXAMINER_SELECT"`
- `roomCode` (string, 필수): 방 코드 (6자리)
- `data` (object, 필수): 선택 데이터
  - `participantId` (number, 필수): 승자로 선택한 참가자의 User ID

### 클라이언트 예시

#### JavaScript
```javascript
const selectWinner = (participantId) => {
  const message = {
    type: 'EXAMINER_SELECT',
    roomCode: roomCode,
    data: {
      participantId: participantId
    }
  };
  
  ws.send(JSON.stringify(message));
  console.log('승자 선택:', participantId);
};

// 카드 선택 UI
function displaySubmittedCards(cards) {
  const container = document.getElementById('submitted-cards');
  container.innerHTML = '<h2>가장 마음에 드는 카드를 선택하세요</h2>';
  
  cards.forEach(card => {
    const cardDiv = document.createElement('div');
    cardDiv.className = 'submitted-card';
    cardDiv.innerHTML = `
      <h3>${card.word}</h3>
      <p>${card.meaning}</p>
      <small>제출자: ${card.nickname}</small>
    `;
    
    cardDiv.addEventListener('click', () => {
      // 선택 효과
      document.querySelectorAll('.submitted-card').forEach(c => 
        c.classList.remove('selected')
      );
      cardDiv.classList.add('selected');
      
      // 승자 선택
      selectWinner(card.participantId);
    });
    
    container.appendChild(cardDiv);
  });
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

const ExaminerSelection: React.FC = () => {
  const [submittedCards, setSubmittedCards] = useState<SubmittedCard[]>([]);
  const [selectedWinner, setSelectedWinner] = useState<number | null>(null);
  const [isSelecting, setIsSelecting] = useState(false);

  const handleSelectWinner = (participantId: number) => {
    if (isSelecting) return; // 중복 클릭 방지
    
    setSelectedWinner(participantId);
    setIsSelecting(true);
    
    const message = {
      type: 'EXAMINER_SELECT',
      roomCode: roomCode,
      data: { participantId }
    };
    
    ws.send(JSON.stringify(message));
  };

  return (
    <div className="examiner-selection">
      <h2>가장 마음에 드는 카드를 선택하세요</h2>
      <div className="cards-grid">
        {submittedCards.map(card => (
          <div
            key={card.participantId}
            className={`card ${selectedWinner === card.participantId ? 'selected' : ''}`}
            onClick={() => handleSelectWinner(card.participantId)}
            style={{ cursor: isSelecting ? 'not-allowed' : 'pointer' }}
          >
            <h3>{card.word}</h3>
            <p>{card.meaning}</p>
            <small>제출자: {card.nickname}</small>
            {selectedWinner === card.participantId && (
              <div className="selection-badge">선택됨 ✓</div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};
```

---

## 2. EXAMINER_SELECTED (서버 → 모든 참가자)

### 개요
출제자가 승자를 선택한 후 모든 참가자에게 결과를 알립니다.

### 응답 메시지
```json
{
  "type": "EXAMINER_SELECTED",
  "roomCode": "764185",
  "data": {
    "participantId": 2,
    "cardWord": "다솜",
    "winnerNickname": "철수",
    "newBananaScore": 1
  },
  "message": "Examiner has selected a card"
}
```

### 필드 설명
- `type` (string): `"EXAMINER_SELECTED"`
- `roomCode` (string): 방 코드
- `data` (object): 선택 결과
  - `participantId` (number): 승자의 User ID
  - `cardWord` (string): 선택된 카드의 단어
  - `winnerNickname` (string): 승자의 닉네임
  - `newBananaScore` (number): 승자의 새로운 바나나 점수
- `message` (string): 완료 메시지

### 클라이언트 처리

#### JavaScript
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'EXAMINER_SELECTED') {
    const { participantId, cardWord, winnerNickname, newBananaScore } = message.data;
    
    console.log(`${winnerNickname}님이 승리!`);
    console.log(`선택된 카드: ${cardWord}`);
    console.log(`새 점수: ${newBananaScore}점`);
    
    // 결과 화면 표시
    displayRoundResult(winnerNickname, cardWord, newBananaScore);
    
    // 점수판 업데이트
    updateScoreboard(participantId, newBananaScore);
  }
};

function displayRoundResult(nickname, word, score) {
  const resultDiv = document.getElementById('round-result');
  resultDiv.innerHTML = `
    <div class="winner-announcement">
      <h2>🎉 ${nickname}님이 승리했습니다!</h2>
      <div class="winning-card">
        <h3>${word}</h3>
      </div>
      <p>현재 점수: ${score}점</p>
    </div>
  `;
  resultDiv.style.display = 'block';
  
  // 3초 후 자동으로 숨김 (다음 라운드 준비)
  setTimeout(() => {
    resultDiv.style.display = 'none';
  }, 3000);
}
```

#### React
```typescript
interface RoundResult {
  participantId: number;
  cardWord: string;
  winnerNickname: string;
  newBananaScore: number;
}

const GameRoom: React.FC = () => {
  const [roundResult, setRoundResult] = useState<RoundResult | null>(null);
  const [scores, setScores] = useState<Map<number, number>>(new Map());

  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'EXAMINER_SELECTED') {
        const result = message.data as RoundResult;
        
        // 결과 표시
        setRoundResult(result);
        
        // 점수 업데이트
        setScores(prev => {
          const updated = new Map(prev);
          updated.set(result.participantId, result.newBananaScore);
          return updated;
        });
        
        // 3초 후 결과 숨김
        setTimeout(() => setRoundResult(null), 3000);
      }
    };
  }, []);

  return (
    <div className="game-room">
      {/* 점수판 */}
      <Scoreboard scores={scores} />
      
      {/* 승자 발표 모달 */}
      {roundResult && (
        <div className="winner-modal">
          <div className="winner-content">
            <h2>🎉 {roundResult.winnerNickname}님 승리!</h2>
            <div className="winning-card">
              <h3>{roundResult.cardWord}</h3>
            </div>
            <p className="score">
              현재 점수: <strong>{roundResult.newBananaScore}</strong>점
            </p>
          </div>
        </div>
      )}
      
      {/* 게임 화면 */}
      <GameScreen />
    </div>
  );
};
```

---

## 3. 다음 단계 분기

### A. 5점 미만 → NEXT_ROUND (다음 턴 시작)

`EXAMINER_SELECTED` 직후, 승자가 5점 미만이면 자동으로 다음 턴이 시작됩니다.

```json
{
  "type": "NEXT_ROUND",
  "roomCode": "764185",
  "data": {
    "newExaminerId": 3,
    "newExaminerNickname": "민수",
    "quiz": {
      "id": 42,
      "content": "다음 중 맞춤법이 올바른 것은?"
    }
  },
  "message": "Next turn is starting!"
}
```

**처리:**
- 새로운 출제자 지정
- 새로운 질문 발급
- 모든 참가자가 다시 카드 선택 화면으로 이동
- 출제자 히스토리 업데이트 (모두 출제했으면 히스토리 초기화)

자세한 내용: [NEXT_ROUND 명세서](./WEBSOCKET_NEXT_ROUND.md)

### B. 5점 달성 → ROUND_END (게임 종료)

승자가 5점을 달성하면 게임이 종료됩니다.

```json
{
  "type": "ROUND_END",
  "roomCode": "764185",
  "data": {
    "winnerNickname": "철수",
    "rankings": [
      {
        "rank": 1,
        "userId": 2,
        "nickname": "철수",
        "bananaScore": 5,
        "xpReward": 100
      },
      {
        "rank": 2,
        "userId": 3,
        "nickname": "민수",
        "bananaScore": 3,
        "xpReward": 75
      },
      {
        "rank": 3,
        "userId": 4,
        "nickname": "영희",
        "bananaScore": 2,
        "xpReward": 50
      },
      {
        "rank": 4,
        "userId": 5,
        "nickname": "지훈",
        "bananaScore": 1,
        "xpReward": 25
      }
    ]
  },
  "message": "Game has ended"
}
```

자세한 내용: [ROUND_END 명세서](./WEBSOCKET_ROUND_END.md)

---

## 전체 플로우

```
1. 3명이 카드 제출 완료
   ↓
2. 출제자: ALL_CARDS_SUBMITTED 수신
   ↓
3. 출제자: 카드 선택 (EXAMINER_SELECT)
   ↓
4. 서버: 승자 점수 +1, DB 업데이트
   ↓
5. 모든 참가자: EXAMINER_SELECTED 수신
   ↓
6. 점수 확인
   ├─ 5점 미만 → NEXT_ROUND (새 출제자, 새 질문)
   └─ 5점 달성 → ROUND_END (게임 종료, 순위 발표)
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

### 2. 출제자가 아님
```json
{
  "type": "ERROR",
  "message": "Invalid WebSocket Message Format"
}
```
**발생 원인:** 출제자가 아닌 사용자가 EXAMINER_SELECT를 시도한 경우

### 3. 참가자를 찾을 수 없음
```json
{
  "type": "ERROR",
  "message": "Participant Not Found"
}
```

### 4. 카드를 찾을 수 없음
```json
{
  "type": "ERROR",
  "message": "Card Not Found"
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

1. **출제자만 선택 가능**: 일반 참가자가 선택을 시도하면 에러가 발생합니다.
2. **중복 선택 방지**: 클라이언트에서 선택 후 버튼을 비활성화해야 합니다.
3. **자동 진행**: 선택 후 서버가 자동으로 다음 턴 또는 게임 종료를 처리합니다.
4. **트랜잭션 처리**: 점수 업데이트, 출제자 변경, 게임 종료가 하나의 트랜잭션으로 처리됩니다.
5. **출제자 로테이션**: 아직 출제자가 아니었던 사람 중에서 랜덤으로 선택됩니다.
6. **히스토리 초기화**: 4명 모두 출제자를 했으면 히스토리가 초기화되고 다시 처음부터 시작합니다.

---

## UI/UX 권장사항

### 출제자 화면
1. **카드 선택**
   - 3개 카드를 균등하게 배치
   - 각 카드에 제출자 닉네임 표시
   - 호버 효과로 선택 가능함을 표시
   - 클릭 시 확인 모달 또는 즉시 선택

2. **선택 확인**
   ```
   "철수님의 카드 '다솜'을 선택하시겠습니까?"
   [취소] [확인]
   ```

3. **로딩 상태**
   - 선택 후 서버 응답 대기 중 표시
   - 다른 카드 선택 비활성화

### 모든 참가자 화면
1. **승자 발표 애니메이션**
   - 승자 카드를 크게 표시
   - 축하 효과 (confetti, 애니메이션)
   - 점수 증가 애니메이션

2. **점수판 업데이트**
   - 실시간으로 점수 변화 반영
   - 1위 강조 표시
   - 5점 달성 시 특별 효과

3. **다음 턴 전환**
   - "다음 출제자: 민수님" 안내
   - 3초 카운트다운
   - 새 질문 표시

---

## 데이터베이스 업데이트

서버는 다음 정보를 DB에 저장합니다:

1. **Participant 테이블**
   - `bananaScore` +1 (승자)
   - `isExaminer` 변경 (현재 출제자 → false, 새 출제자 → true)

2. **User 테이블** (게임 종료 시)
   - `xp` 증가 (순위별 차등 지급)
   - `level` 자동 계산

---

## 관련 API
- [카드 제출](./WEBSOCKET_SUBMIT_CARD.md)
- [다음 라운드 시작](./WEBSOCKET_NEXT_ROUND.md)
- [게임 종료](./WEBSOCKET_ROUND_END.md)
- [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)


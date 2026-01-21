# 다음 라운드 시작 API (WebSocket)

## 개요
출제자가 승자를 선택한 후, 승자의 점수가 5점 미만이면 자동으로 다음 턴이 시작됩니다. 새로운 출제자가 지정되고 새로운 질문이 발급됩니다.

## 전제 조건
- 출제자가 승자를 선택한 상태 (`EXAMINER_SELECTED` 수신)
- 승자의 바나나 점수가 5점 미만
- 게임이 계속 진행 중

---

## NEXT_ROUND (서버 → 모든 참가자)

### 개요
다음 턴을 시작하며 새로운 출제자와 질문을 알립니다.

### 응답 메시지
```json
{
  "type": "NEXT_ROUND",
  "room_code": "764185",
  "data": {
    "new_examiner_id": 3,
    "new_examiner_nickname": "민수",
    "quiz": {
      "id": 42,
      "content": "다음 중 맞춤법이 올바른 것은?"
    }
  },
  "message": "Next turn is starting!"
}
```

### 필드 설명
- `type` (string): `"NEXT_ROUND"`
- `roomCode` (string): 방 코드
- `data` (object): 다음 턴 정보
  - `newExaminerId` (number): 새 출제자의 User ID
  - `newExaminerNickname` (string): 새 출제자의 닉네임
  - `quiz` (object): 새로운 질문
    - `id` (number): 질문 ID
    - `content` (string): 질문 내용
- `message` (string): 턴 시작 메시지

---

## 클라이언트 처리

### JavaScript
```javascript
let isExaminer = false;
let currentQuiz = null;

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'NEXT_ROUND') {
    const { newExaminerId, newExaminerNickname, quiz } = message.data;
    
    // 내가 새 출제자인지 확인
    const myUserId = getCurrentUserId(); // 현재 로그인한 유저 ID
    isExaminer = (myUserId === newExaminerId);
    
    // 질문 저장
    currentQuiz = quiz;
    
    console.log(`다음 출제자: ${newExaminerNickname}`);
    console.log(`새 질문: ${quiz.content}`);
    
    // 화면 전환
    if (isExaminer) {
      showExaminerScreen(quiz);
    } else {
      showParticipantScreen(quiz);
    }
  }
};

function showExaminerScreen(quiz) {
  const container = document.getElementById('game-screen');
  container.innerHTML = `
    <div class="examiner-view">
      <div class="role-badge examiner">출제자</div>
      <h2>질문</h2>
      <p class="quiz-content">${quiz.content}</p>
      <p class="instruction">참가자들이 카드를 제출하기를 기다리는 중...</p>
      <div class="waiting-spinner"></div>
    </div>
  `;
}

function showParticipantScreen(quiz) {
  const container = document.getElementById('game-screen');
  container.innerHTML = `
    <div class="participant-view">
      <div class="role-badge participant">참가자</div>
      <h2>질문</h2>
      <p class="quiz-content">${quiz.content}</p>
      <button onclick="fetchCards()">카드 받기</button>
    </div>
  `;
}

async function fetchCards() {
  try {
    const response = await fetch('/play-together/cards', {
      headers: {
        'Authorization': `Bearer ${accessToken}`
      }
    });
    const cards = await response.json();
    displayCards(cards);
  } catch (error) {
    console.error('카드 조회 실패:', error);
  }
}
```

### React
```typescript
interface NextRoundData {
  newExaminerId: number;
  newExaminerNickname: string;
  quiz: {
    id: number;
    content: string;
  };
}

const GameRoom: React.FC = () => {
  const [isExaminer, setIsExaminer] = useState(false);
  const [currentQuiz, setCurrentQuiz] = useState<Quiz | null>(null);
  const [newExaminerName, setNewExaminerName] = useState<string>('');
  const { userId } = useAuth(); // 현재 로그인한 사용자 ID

  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'NEXT_ROUND') {
        const data: NextRoundData = message.data;
        
        // 출제자 확인
        setIsExaminer(userId === data.newExaminerId);
        setNewExaminerName(data.newExaminerNickname);
        setCurrentQuiz(data.quiz);
        
        // 알림 표시
        toast.info(`다음 출제자: ${data.newExaminerNickname}`);
      }
    };
  }, [userId]);

  return (
    <div className="game-room">
      {/* 출제자 안내 배너 */}
      <div className="examiner-banner">
        {isExaminer ? (
          <span className="badge examiner">당신은 출제자입니다</span>
        ) : (
          <span className="badge participant">
            출제자: {newExaminerName}
          </span>
        )}
      </div>

      {/* 질문 표시 */}
      {currentQuiz && (
        <div className="quiz-section">
          <h2>질문</h2>
          <p className="quiz-content">{currentQuiz.content}</p>
        </div>
      )}

      {/* 역할별 화면 */}
      {isExaminer ? (
        <ExaminerWaitingScreen />
      ) : (
        <ParticipantCardSelection quiz={currentQuiz} />
      )}
    </div>
  );
};

// 출제자 대기 화면
const ExaminerWaitingScreen: React.FC = () => {
  return (
    <div className="examiner-waiting">
      <h3>참가자들이 카드를 제출하는 중...</h3>
      <div className="spinner-container">
        <div className="spinner"></div>
      </div>
      <p className="hint">
        3명이 모두 제출하면 자동으로 카드가 표시됩니다
      </p>
    </div>
  );
};

// 참가자 카드 선택 화면
const ParticipantCardSelection: React.FC<{ quiz: Quiz | null }> = ({ quiz }) => {
  const [cards, setCards] = useState<Card[]>([]);
  const [loading, setLoading] = useState(false);

  const fetchCards = async () => {
    setLoading(true);
    try {
      const response = await fetch('/play-together/cards', {
        headers: {
          'Authorization': `Bearer ${accessToken}`
        }
      });
      const data = await response.json();
      setCards(data.cards);
    } catch (error) {
      toast.error('카드를 불러올 수 없습니다');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (quiz) {
      fetchCards();
    }
  }, [quiz]);

  if (loading) {
    return <div>카드를 불러오는 중...</div>;
  }

  return (
    <div className="card-selection">
      <h3>질문에 맞는 카드를 선택하세요</h3>
      <div className="cards-grid">
        {cards.map(card => (
          <CardItem key={card.cardId} card={card} />
        ))}
      </div>
    </div>
  );
};
```

---

## 출제자 선택 로직

### 서버 동작
1. **출제자 히스토리 확인**
   - 현재 방에서 이미 출제자를 했던 Participant ID 목록 조회

2. **다음 출제자 선택**
   - 아직 출제자가 아니었던 참가자 중 랜덤 선택
   - 모두 출제자를 했으면 히스토리 초기화 후 다시 랜덤 선택

3. **DB 업데이트**
   - 이전 출제자의 `isExaminer` → `false`
   - 새 출제자의 `isExaminer` → `true`

4. **히스토리 추가**
   - 새 출제자의 Participant ID를 히스토리에 추가

### 예시 시나리오

**참가자:** A(방장), B, C, D

**턴 1:**
- 출제자: A (방 생성자)
- 히스토리: [A]

**턴 2:**
- 선택 가능: B, C, D → 랜덤 선택 → B
- 히스토리: [A, B]

**턴 3:**
- 선택 가능: C, D → 랜덤 선택 → C
- 히스토리: [A, B, C]

**턴 4:**
- 선택 가능: D → D 선택
- 히스토리: [A, B, C, D]

**턴 5:**
- 선택 가능: 없음 → 히스토리 초기화
- 히스토리: [] → 다시 A, B, C, D 중 랜덤 선택

---

## 게임 흐름

```
출제자 선택 완료 (EXAMINER_SELECTED)
  ↓
점수 확인
  ├─ 5점 달성 → ROUND_END (게임 종료)
  └─ 5점 미만 ↓
              
다음 출제자 선택
  ↓
히스토리 확인
  ├─ 안 한 사람 있음 → 그 중 랜덤 선택
  └─ 모두 했음 → 히스토리 초기화 → 랜덤 선택
  ↓
DB 업데이트 (isExaminer)
  ↓
새 질문 조회
  ↓
NEXT_ROUND 브로드캐스트
  ↓
모든 참가자가 새 턴 시작
  ├─ 출제자 → 대기 화면
  └─ 참가자 → 카드 조회 화면
```

---

## 상태 변화

### 서버 메모리 (GameRoundManager)

```java
// 제출된 카드 초기화
submittedCards.put(room_code, new ArrayList<>());

// 출제자 히스토리 업데이트
examinerHistory.get(roomCode).add(newExaminerParticipantId);

// 모두 출제했으면 히스토리 초기화
if (examinerHistory.get(roomCode).size() == 4) {
    examinerHistory.put(room_code, new ArrayList<>());
}
```

### 데이터베이스 (Participant)

```sql
-- 이전 출제자
UPDATE tbl_participant 
SET is_examiner = false 
WHERE id = {previous_examiner_id};

-- 새 출제자
UPDATE tbl_participant 
SET is_examiner = true 
WHERE id = {new_examiner_id};
```

---

## 타이밍 다이어그램

```
출제자 선택 (t=0s)
  ↓
EXAMINER_SELECTED 전송 (t=0.1s)
  └─→ 모든 클라이언트: 승자 발표 화면 표시
      
승자 점수 < 5 확인 (t=0.2s)
  ↓
다음 출제자 선택 (t=0.3s)
  ↓
DB 업데이트 (t=0.5s)
  ↓
새 질문 조회 (t=0.6s)
  ↓
NEXT_ROUND 전송 (t=0.7s)
  └─→ 모든 클라이언트: 새 턴 화면으로 전환
```

**권장 UX:**
- `EXAMINER_SELECTED` 수신 후 2-3초간 승자 발표 화면 유지
- `NEXT_ROUND` 수신 시 자동으로 다음 턴 화면으로 전환
- 새 출제자 안내 메시지 표시 (toast, banner 등)

---

## UI/UX 권장사항

### 1. 전환 애니메이션
```javascript
// 승자 발표 → 다음 턴 전환
function transitionToNextRound(nextRoundData) {
  // 1. 승자 발표 화면 페이드아웃 (1초)
  fadeOut('.winner-announcement', 1000);
  
  // 2. 새 출제자 안내 표시 (2초)
  showNotification(`다음 출제자: ${nextRoundData.newExaminerNickname}`, 2000);
  
  // 3. 새 화면 페이드인 (1초)
  setTimeout(() => {
    fadeIn('.game-screen', 1000);
  }, 2000);
}
```

### 2. 출제자 표시
```html
<!-- 출제자인 경우 -->
<div class="player-card examiner me">
  <span class="crown">👑</span>
  <span class="name">나 (출제자)</span>
</div>

<!-- 다른 출제자 -->
<div class="player-card examiner">
  <span class="crown">👑</span>
  <span class="name">민수 (출제자)</span>
</div>

<!-- 일반 참가자 -->
<div class="player-card">
  <span class="name">영희</span>
</div>
```

### 3. 진행 상황 표시
```typescript
const ProgressIndicator: React.FC = () => {
  const [turnCount, setTurnCount] = useState(1);
  
  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      if (message.type === 'NEXT_ROUND') {
        setTurnCount(prev => prev + 1);
      }
    };
  }, []);
  
  return (
    <div className="progress-indicator">
      <p>턴 {turnCount}</p>
      <p className="goal">목표: 5점 달성</p>
    </div>
  );
};
```

### 4. 질문 변경 효과
```css
/* 질문이 바뀔 때 애니메이션 */
@keyframes question-change {
  0% { opacity: 0; transform: translateY(-20px); }
  100% { opacity: 1; transform: translateY(0); }
}

.quiz-content.new {
  animation: question-change 0.5s ease-out;
}
```

---

## 주의사항

1. **자동 전송**: `EXAMINER_SELECTED` 직후 서버가 자동으로 전송하므로 클라이언트에서 요청할 필요 없음
2. **게임 종료 우선**: 5점 달성 시 `NEXT_ROUND` 대신 `ROUND_END`가 전송됨
3. **출제자 확인**: 클라이언트는 `newExaminerId`를 현재 사용자 ID와 비교하여 역할 판단
4. **카드 자동 조회**: 참가자는 `NEXT_ROUND` 수신 시 즉시 카드를 조회해야 함
5. **제출 상태 초기화**: 새 턴 시작 시 이전 제출 상태를 모두 초기화해야 함

---

## 에러 처리

이 메시지는 서버에서 자동으로 전송되므로 클라이언트 에러는 발생하지 않습니다.

다만, 다음 상황에서 `NEXT_ROUND`가 전송되지 않을 수 있습니다:
- 승자가 5점을 달성한 경우 → `ROUND_END` 전송
- 서버 오류 발생 시 → `ERROR` 전송

---

## 관련 API
- [출제자 카드 선택](./WEBSOCKET_EXAMINER_SELECT.md)
- [게임 종료](./WEBSOCKET_ROUND_END.md)
- [카드 제출](./WEBSOCKET_SUBMIT_CARD.md)
- [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)


# 게임 종료 API (WebSocket)

## 개요
참가자 중 한 명이 바나나 점수 5점을 달성하면 게임이 종료됩니다. 모든 참가자의 최종 순위와 경험치 보상이 계산되어 전송됩니다.

## 전제 조건
- 게임이 진행 중인 상태
- 출제자가 승자를 선택함 (`EXAMINER_SELECTED` 수신)
- 승자의 바나나 점수가 5점 달성

---

## ROUND_END (서버 → 모든 참가자)

### 개요
게임 종료 시 최종 순위와 보상을 알립니다.

### 응답 메시지
```json
{
  "type": "ROUND_END",
  "room_code": "764185",
  "data": {
    "winner_nickname": "철수",
    "rankings": [
      {
        "rank": 1,
        "user_id": 2,
        "nickname": "철수",
        "banana_score": 5,
        "xp_reward": 20
      },
      {
        "rank": 2,
        "user_id": 3,
        "nickname": "민수",
        "banana_score": 3,
        "xp_reward": 10
      },
      {
        "rank": 3,
        "user_id": 4,
        "nickname": "영희",
        "banana_score": 2,
        "xp_reward": 5
      },
      {
        "rank": 4,
        "user_id": 5,
        "nickname": "지훈",
        "banana_score": 1,
        "xp_reward": 2
      }
    ]
  },
  "message": "Game has ended"
}
```

### 필드 설명
- `type` (string): `"ROUND_END"`
- `roomCode` (string): 방 코드
- `data` (object): 게임 결과
  - `winnerNickname` (string): 우승자 닉네임
  - `rankings` (array): 순위 정보 (4명)
    - `rank` (number): 순위 (1~4)
    - `userId` (number): 사용자 ID
    - `nickname` (string): 사용자 닉네임
    - `bananaScore` (number): 최종 바나나 점수
    - `xpReward` (number): 획득한 경험치
- `message` (string): 종료 메시지

---

## 경험치 보상 규칙

순위별로 차등 지급됩니다:

| 순위 | 경험치 |
|------|--------|
| 1위  | 20 XP |
| 2위  | 10 XP  |
| 3위  | 5 XP  |
| 4위  | 2 XP  |

**동점자 처리:**
- 동일 점수인 경우 동일한 순위로 처리
- 예: 2명이 2점으로 공동 2위 → 둘 다 2위, 10 XP

---

## 레벨 시스템

경험치 누적에 따라 레벨이 자동으로 계산됩니다:

| 누적 경험치 | 레벨 |
|------------|------|
| 1-10 XP   | 레벨 1 |
| 11-20 XP  | 레벨 2 |
| 21-30 XP  | 레벨 3 |
| 31+ XP    | 레벨 4 |

**레벨 업:**
- 게임 종료 시 경험치가 추가되면서 자동으로 레벨이 재계산됩니다
- 레벨 4가 최대 레벨입니다

---

## 클라이언트 처리

### JavaScript
```javascript
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'ROUND_END') {
    const { winnerNickname, rankings } = message.data;
    
    console.log(`게임 종료! 우승자: ${winnerNickname}`);
    
    // 나의 결과 찾기
    const myUserId = getCurrentUserId();
    const myResult = rankings.find(r => r.userId === myUserId);
    
    if (myResult) {
      console.log(`내 순위: ${myResult.rank}위`);
      console.log(`내 점수: ${myResult.bananaScore}점`);
      console.log(`획득 경험치: ${myResult.xpReward} XP`);
    }
    
    // 결과 화면 표시
    displayGameResult(winnerNickname, rankings, myResult);
  }
};

function displayGameResult(winner, rankings, myResult) {
  const container = document.getElementById('game-result');
  
  // 우승자 발표
  let html = `
    <div class="game-result">
      <h1>🎉 게임 종료!</h1>
      <h2>우승자: ${winner}</h2>
      
      <div class="rankings">
        <h3>최종 순위</h3>
  `;
  
  // 순위표
  rankings.forEach(player => {
    const isMe = player.userId === myResult.userId;
    const medal = getMedal(player.rank);
    
    html += `
      <div class="rank-item ${isMe ? 'me' : ''}">
        <span class="medal">${medal}</span>
        <span class="rank">${player.rank}위</span>
        <span class="nickname">${player.nickname}</span>
        <span class="score">${player.bananaScore}점</span>
        <span class="xp">+${player.xpReward} XP</span>
      </div>
    `;
  });
  
  html += `
      </div>
      
      <div class="my-reward">
        <p>당신의 결과</p>
        <p class="big-number">${myResult.rank}위</p>
        <p class="xp-earned">+${myResult.xpReward} 경험치</p>
      </div>
      
      <button onclick="goToLobby()">로비로 돌아가기</button>
    </div>
  `;
  
  container.innerHTML = html;
}

function getMedal(rank) {
  const medals = {
    1: '🥇',
    2: '🥈',
    3: '🥉',
    4: '4️⃣'
  };
  return medals[rank] || '';
}

function goToLobby() {
  // WebSocket 연결 종료
  ws.close();
  
  // 로비 페이지로 이동
  window.location.href = '/lobby';
}
```

### React
```typescript
interface PlayerRankInfo {
  rank: number;
  user_id: number;
  nickname: string;
  bananaScore: number;
  xpReward: number;
}

interface GameResultData {
  winnerNickname: string;
  rankings: PlayerRankInfo[];
}

const GameResult: React.FC = () => {
  const [gameResult, setGameResult] = useState<GameResultData | null>(null);
  const { userId } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      
      if (message.type === 'ROUND_END') {
        setGameResult(message.data);
        
        // 사운드 효과
        playSound('game-end');
        
        // 우승자면 축하 효과
        const myRank = message.data.rankings.find(
          (r: PlayerRankInfo) => r.userId === userId
        );
        if (myRank?.rank === 1) {
          playSound('victory');
          showConfetti();
        }
      }
    };
  }, [userId]);

  if (!gameResult) return null;

  const myResult = gameResult.rankings.find(r => r.userId === userId);

  return (
    <div className="game-result-screen">
      {/* 우승자 발표 */}
      <div className="winner-announcement">
        <h1>🎉 게임 종료!</h1>
        <h2 className="winner-name">{gameResult.winnerNickname} 우승!</h2>
      </div>

      {/* 순위표 */}
      <div className="rankings-container">
        <h3>최종 순위</h3>
        <div className="rankings-list">
          {gameResult.rankings.map((player) => (
            <RankItem
              key={player.userId}
              player={player}
              isMe={player.userId === userId}
            />
          ))}
        </div>
      </div>

      {/* 내 결과 강조 */}
      {myResult && (
        <div className="my-result-card">
          <h3>당신의 결과</h3>
          <div className="result-details">
            <div className="rank-badge">
              <span className="medal">{getMedalEmoji(myResult.rank)}</span>
              <span className="rank-text">{myResult.rank}위</span>
            </div>
            <div className="stats">
              <div className="stat">
                <span className="label">최종 점수</span>
                <span className="value">{myResult.bananaScore}점</span>
              </div>
              <div className="stat highlight">
                <span className="label">획득 경험치</span>
                <span className="value">+{myResult.xpReward} XP</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 버튼 */}
      <div className="action-buttons">
        <button 
          className="btn-primary"
          onClick={() => {
            ws.close();
            navigate('/lobby');
          }}
        >
          로비로 돌아가기
        </button>
      </div>
    </div>
  );
};

// 순위 항목 컴포넌트
const RankItem: React.FC<{
  player: PlayerRankInfo;
  isMe: boolean;
}> = ({ player, isMe }) => {
  return (
    <div className={`rank-item ${isMe ? 'me' : ''} rank-${player.rank}`}>
      <span className="medal">{getMedalEmoji(player.rank)}</span>
      <span className="rank">{player.rank}위</span>
      <span className="nickname">
        {player.nickname}
        {isMe && <span className="me-badge"> (나)</span>}
      </span>
      <span className="score">{player.bananaScore}점</span>
      <span className="xp-reward">+{player.xpReward} XP</span>
    </div>
  );
};

// 메달 이모지 반환
const getMedalEmoji = (rank: number): string => {
  const medals: Record<number, string> = {
    1: '🥇',
    2: '🥈',
    3: '🥉',
  };
  return medals[rank] || '🎖️';
};

// 축하 효과
const showConfetti = () => {
  // react-confetti 또는 canvas-confetti 라이브러리 사용
  confetti({
    particleCount: 100,
    spread: 70,
    origin: { y: 0.6 }
  });
};
```

---

## 게임 종료 플로우

```
출제자가 승자 선택 (EXAMINER_SELECT)
  ↓
승자 점수 +1
  ↓
점수 확인
  ├─ 5점 미만 → NEXT_ROUND (다음 턴)
  └─ 5점 달성 ↓
              
게임 종료 처리 시작
  ↓
1. 모든 참가자 조회 (bananaScore 포함)
  ↓
2. 점수순 정렬
  ↓
3. 순위 계산 (동점자 처리)
  ↓
4. 경험치 계산 (순위별 차등)
  ↓
5. DB 업데이트 (User.xp)
  ↓
6. ROUND_END 브로드캐스트
  ↓
7. 게임 상태 정리 (메모리 초기화)
  ↓
모든 참가자: 결과 화면 표시
```

---

## 서버 동작 상세

### 1. 점수 확인
```java
if (winner.getBananaScore() >= 5) {
    endGame(room_code, room.getId());
}
```

### 2. 순위 계산
```java
// 모든 참가자 조회
var participants = participantRepository.findAllByRoomIdIdWithUser(roomId);

// 점수순 정렬
participants.sort((a, b) -> 
    Integer.compare(b.getBananaScore(), a.getBananaScore())
);

// 순위 계산
int currentRank = 1;
for (int i = 0; i < participants.size(); i++) {
    if (i > 0 && participants.get(i).getBananaScore() 
        < participants.get(i-1).getBananaScore()) {
        currentRank = i + 1;
    }
    // 순위 설정
}
```

### 3. 경험치 지급 및 레벨 자동 계산
```java
int[] xpRewards = {20, 10, 5, 2};

for (int i = 0; i < participants.size(); i++) {
    var participant = participants.get(i);
    var user = participant.getUserId();
    
    int xpReward = xpRewards[Math.min(i, 3)];
    user.addXp(xpReward);  // 경험치 추가 및 레벨 자동 계산
    
    userRepository.save(user);
}
```

### 4. 게임 상태 초기화
```java
// 메모리에서 게임 상태 제거
gameRoundManager.endGame(roomCode);

// WebSocket 세션 정리는 클라이언트가 연결 종료
```

---

## 데이터베이스 업데이트

### User 테이블
```sql
-- 경험치 증가 및 레벨 자동 계산
UPDATE tbl_user
SET bananaxp = bananaxp + {xpReward},
    level = CASE 
        WHEN bananaxp + {xpReward} <= 10 THEN 1
        WHEN bananaxp + {xpReward} <= 20 THEN 2
        WHEN bananaxp + {xpReward} <= 30 THEN 3
        ELSE 4
    END
WHERE id = {userId};
```

**레벨 계산 로직:**
- 애플리케이션 레벨에서 `User.addXp()` 메서드가 자동으로 레벨을 계산합니다
- 경험치가 추가될 때마다 `updateLevel()` 메서드가 호출됩니다

### Room & Participant 테이블
- 게임 종료 후에도 DB에 남아 있음 (기록 보관)
- 새 게임 시작 시 새 Room 생성

---

## UI/UX 권장사항

### 1. 애니메이션 시퀀스
```javascript
async function showGameResult(data) {
  // 1. 우승자 발표 (2초)
  await showWinnerAnnouncement(data.winnerNickname);
  await sleep(2000);
  
  // 2. 순위 공개 (1명씩 0.5초 간격)
  for (let player of data.rankings) {
    await revealRank(player);
    await sleep(500);
  }
  
  // 3. 내 결과 강조 (1초)
  await highlightMyResult();
  await sleep(1000);
  
  // 4. 버튼 표시
  showButtons();
}
```

### 2. 순위 스타일링
```css
/* 1위 - 금색 */
.rank-item.rank-1 {
  background: linear-gradient(135deg, #FFD700, #FFA500);
  animation: pulse 2s infinite;
}

/* 2위 - 은색 */
.rank-item.rank-2 {
  background: linear-gradient(135deg, #C0C0C0, #999999);
}

/* 3위 - 동색 */
.rank-item.rank-3 {
  background: linear-gradient(135deg, #CD7F32, #8B4513);
}

/* 4위 - 기본 */
.rank-item.rank-4 {
  background: #f5f5f5;
}

/* 내 결과 강조 */
.rank-item.me {
  border: 3px solid #4CAF50;
  transform: scale(1.05);
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.02); }
}
```

### 3. 경험치 증가 애니메이션
```typescript
const XpCounter: React.FC<{ value: number }> = ({ value }) => {
  const [count, setCount] = useState(0);

  useEffect(() => {
    let start = 0;
    const duration = 1500; // 1.5초
    const increment = value / (duration / 16); // 60fps

    const timer = setInterval(() => {
      start += increment;
      if (start >= value) {
        setCount(value);
        clearInterval(timer);
      } else {
        setCount(Math.floor(start));
      }
    }, 16);

    return () => clearInterval(timer);
  }, [value]);

  return (
    <span className="xp-counter">+{count} XP</span>
  );
};
```

### 4. 사운드 효과
```javascript
const sounds = {
  'game-end': '/sounds/game-end.mp3',
  'victory': '/sounds/victory.mp3',
  'rank-reveal': '/sounds/rank-reveal.mp3',
  'xp-gain': '/sounds/xp-gain.mp3'
};

function playSound(soundName) {
  const audio = new Audio(sounds[soundName]);
  audio.play();
}
```

---

## 통계 표시 (추가 기능)

게임 결과와 함께 다음 정보를 표시할 수 있습니다:

```typescript
interface GameStats {
  totalTurns: number;      // 총 턴 수
  playTime: string;        // 플레이 타임 (분:초)
  myBestCard: string;      // 내가 낸 최고의 카드
  examinerTurns: number;   // 내가 출제자였던 횟수
}

const GameStats: React.FC<{ stats: GameStats }> = ({ stats }) => {
  return (
    <div className="game-stats">
      <h4>게임 통계</h4>
      <div className="stats-grid">
        <div className="stat">
          <span className="label">총 턴 수</span>
          <span className="value">{stats.totalTurns}</span>
        </div>
        <div className="stat">
          <span className="label">플레이 타임</span>
          <span className="value">{stats.playTime}</span>
        </div>
        <div className="stat">
          <span className="label">출제자 횟수</span>
          <span className="value">{stats.examinerTurns}</span>
        </div>
      </div>
    </div>
  );
};
```

---

## 다음 액션

### 1. 로비로 돌아가기
```javascript
function goToLobby() {
  // WebSocket 연결 종료
  ws.close();
  
  // 상태 초기화
  resetGameState();
  
  // 페이지 이동
  window.location.href = '/lobby';
}
```

### 2. 재경기 (확장 기능)
```javascript
function playAgain() {
  // 같은 방 코드로 재입장 시도
  // 또는 새 방 생성 페이지로 이동
  window.location.href = '/create-room';
}
```

---

## 주의사항

1. **자동 전송**: 5점 달성 시 서버가 자동으로 전송
2. **경험치 적용**: 서버에서 DB에 자동 저장, 클라이언트는 표시만
3. **WebSocket 종료**: 결과 확인 후 클라이언트가 연결을 종료해야 함
4. **동점자 처리**: 같은 점수면 동일 순위, 동일 경험치
5. **메모리 정리**: 게임 종료 시 서버 메모리에서 게임 상태 제거

---

## 에러 처리

이 메시지는 서버에서 자동으로 전송되므로 클라이언트 에러는 발생하지 않습니다.

하지만 다음 상황을 고려해야 합니다:
- WebSocket 연결이 끊긴 경우
- 게임 도중 참가자가 나간 경우 (향후 구현)

---

## 관련 API
- [출제자 카드 선택](./WEBSOCKET_EXAMINER_SELECT.md)
- [다음 라운드 시작](./WEBSOCKET_NEXT_ROUND.md)
- [WebSocket 게임 플로우](../websocket/WEBSOCKET_GAME_FLOW.md)


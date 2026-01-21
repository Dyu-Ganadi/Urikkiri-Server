# 방 나가기 API (WebSocket)

## 개요
게임 종료 후 참가자가 방을 나가는 기능입니다. 개별 유저만 삭제되고 방은 유지되므로, 남은 참가자들은 계속해서 같은 방에서 게임을 이어갈 수 있습니다.

## 전제 조건
- 게임이 종료된 상태 (`ROUND_END` 수신 완료)
- 유효한 방 코드 필요
- 게임 WebSocket 연결 상태

---

## LEAVE_ROOM (클라이언트 → 서버)

### 개요
게임 종료 후 방을 나가는 요청을 보냅니다.

### 요청 메시지
```json
{
  "type": "LEAVE_ROOM",
  "room_code": "764185"
}
```

### 필드 설명
- `type` (string): `"LEAVE_ROOM"`
- `roomCode` (string): 나가려는 방의 코드 (6자리 숫자)

### 요청 예시
```javascript
// Unity 예시
const message = {
  type: "LEAVE_ROOM",
  room_code: "764185"
};

ws.send(JSON.stringify(message));
```

---

## LEAVE_ROOM (서버 → 클라이언트)

### 응답 1: 방 나가기 확인 (본인)

```json
{
  "type": "LEAVE_ROOM",
  "room_code": "764185",
  "message": "Successfully left the room"
}
```

### 응답 2: 다른 참가자 퇴장 알림 (남은 참가자들)

```json
{
  "type": "LEAVE_ROOM",
  "room_code": "764185",
  "data": {
    "user_id": 2,
    "nickname": "철수",
    "remainingCount": 3
  },
  "message": "철수 has left the room"
}
```

### 응답 3: 모든 참가자 퇴장 (마지막 참가자)

모든 참가자가 나가도 방은 유지됩니다. 새로운 참가자들이 다시 참가할 수 있습니다.

```json
{
  "type": "LEAVE_ROOM",
  "room_code": "764185",
  "message": "Successfully left the room"
}
```

**참고:** 방은 삭제되지 않고, 게임 상태(메모리)만 정리됩니다. 새로운 참가자들이 이 방에 다시 참가 가능합니다.

---

## 처리 로직

### 1. 참가자 퇴장
```
1. 클라이언트 → 서버: LEAVE_ROOM 전송 (roomCode 포함)
2. 서버: 
   - roomCode와 userId로 특정 유저의 Participant만 삭제
   - WebSocket 세션 제거
3. 서버 → 본인: 방 나가기 확인 메시지
4. 서버 → 남은 참가자: 퇴장 알림 브로드캐스트
```

**중요:** roomCode와 participantId(userId)를 사용하여 특정 유저만 삭제하므로, 같은 방의 다른 참가자들은 영향을 받지 않습니다.

### 2. 모든 참가자 퇴장 시
```
1. 마지막 참가자가 LEAVE_ROOM 전송
2. 서버: 남은 참가자 수 확인 → 0명
3. 서버:
   - 해당 유저의 Participant 삭제 (roomCode + userId로 특정)
   - ParticipantRepository에서 해당 방의 모든 유저 정보 삭제됨
   - GameRoundManager에서 게임 상태 정리 (메모리)
   - Room은 삭제하지 않음 (재사용 가능)
4. 서버 → 마지막 참가자: 방 나가기 확인 메시지
```

**중요:** 
- 모든 참가자가 나가면 ParticipantRepository에서 해당 방의 유저 정보가 모두 삭제됩니다
- 방(Room)은 삭제되지 않으므로, 새로운 참가자들이 같은 방 코드로 다시 참가하여 게임을 이어갈 수 있습니다

---

## 에러 처리

### 방 코드 누락
```json
{
  "type": "ERROR",
  "message": "Room Code is Required"
}
```

### 존재하지 않는 방
```json
{
  "type": "ERROR",
  "message": "Room Not Found"
}
```

### 참가자 정보 없음
```json
{
  "type": "ERROR",
  "message": "Participant Not Found"
}
```

---

## 사용 예시

### Unity (C#)
```csharp
public class GameWebSocketManager : MonoBehaviour
{
    private WebSocket ws;
    private string roomCode;

    // 게임 종료 후 방 나가기
    public void LeaveRoom()
    {
        var message = new {
            type = "LEAVE_ROOM",
            room_code = this.roomCode
        };

        string json = JsonUtility.ToJson(message);
        ws.Send(json);
    }

    // 메시지 수신 처리
    private void OnWebSocketMessage(string data)
    {
        var message = JsonUtility.FromJson<WebSocketMessage>(data);

        switch (message.type)
        {
            case "LEAVE_ROOM":
                HandleLeaveRoom(message);
                break;
        }
    }

    private void HandleLeaveRoom(WebSocketMessage message)
    {
        if (message.message == "Successfully left the room")
        {
            // 방 나가기 성공 - 로비 씬으로 이동
            Debug.Log("방을 성공적으로 나갔습니다.");
            ws.Close();
            SceneManager.LoadScene("LobbyScene");
        }
        else if (message.data != null)
        {
            // 다른 참가자 퇴장 알림
            var exitData = JsonUtility.FromJson<UserExitDto>(message.data.ToString());
            Debug.Log($"{exitData.nickname}님이 방을 나갔습니다. 남은 인원: {exitData.remainingCount}");
            UpdateParticipantList(exitData.remainingCount);
            
            // 남은 인원이 4명이면 다시 게임 시작 가능
            if (exitData.remainingCount >= 4)
            {
                ShowReadyButton();
            }
        }
    }
}
```

### JavaScript (프론트엔드)
```javascript
// 방 나가기 버튼 클릭
function leaveRoom(roomCode) {
  const message = {
    type: 'LEAVE_ROOM',
    room_code: roomCode
  };
  
  ws.send(JSON.stringify(message));
}

// 메시지 수신 처리
ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  
  if (message.type === 'LEAVE_ROOM') {
    if (message.message === 'Successfully left the room') {
      // 방 나가기 성공
      console.log('방을 나갔습니다.');
      ws.close();
      window.location.href = '/lobby';
    } else if (message.data) {
      // 다른 사람이 나갔을 때
      const { nickname, remainingCount } = message.data;
      console.log(`${nickname}님이 나갔습니다. 남은 인원: ${remainingCount}`);
      updateParticipantUI(remainingCount);
      
      // 남은 인원이 충분하면 계속 게임 가능
      if (remainingCount >= 4) {
        showReadyButton();
      }
    }
  }
};
```

---

## ROOM_EXIT vs LEAVE_ROOM

| 구분 | ROOM_EXIT | LEAVE_ROOM |
|------|-----------|-----------|
| **사용 시점** | 게임 시작 전 | 게임 종료 후 |
| **연결 타입** | 로비 WebSocket | 게임 WebSocket |
| **제약 조건** | 게임 시작 전에만 가능 | 게임 종료 후 가능 |
| **방 유지** | 방은 유지 | 방은 유지 |
| **삭제 대상** | 해당 유저의 Participant | 해당 유저의 Participant |
| **재참가** | 가능 (다시 JOIN_ROOM) | 가능 (다시 JOIN_ROOM) |

---

## 플로우 다이어그램

```
[게임 종료 (ROUND_END)]
         ↓
[사용자: 결과 화면 확인]
         ↓
[사용자: "나가기" 버튼 클릭]
         ↓
[클라이언트 → 서버: LEAVE_ROOM]
         ↓
[서버: 해당 유저의 Participant만 삭제]
         ↓
[서버: 남은 참가자 확인]
         ↓
    ┌────┴────┐
    ↓         ↓
[참가자 있음] [참가자 없음]
    ↓         ↓
[알림 전송]  [게임 상태만 정리]
    ↓         ↓
         ↓
[방은 유지됨 - 재참가 가능]
         ↓
[클라이언트: 로비로 이동]
```

---

## 주의사항

1. **게임 시작 전 방 나가기는 `ROOM_EXIT` 사용**
   - `LEAVE_ROOM`은 게임 종료 후에만 사용

2. **방은 유지됨**
   - 모든 참가자가 나가도 방(Room)은 삭제되지 않음
   - 게임 상태(GameRoundManager)만 메모리에서 정리됨
   - 새로운 참가자들이 같은 방 코드로 다시 참가 가능

3. **재참가 가능**
   - 나간 후 다시 같은 방에 JOIN_ROOM으로 재참가 가능
   - 남은 사람들과 함께 게임 진행 가능

4. **WebSocket 연결 종료**
   - 방을 나간 후에는 게임 WebSocket 연결을 닫아야 함
   - 로비로 돌아가려면 새로운 로비 WebSocket 연결 필요

5. **순차적 퇴장**
   - 여러 참가자가 동시에 나가도 순차적으로 처리됨
   - 각 퇴장마다 남은 참가자들에게 알림 전송

6. **계속 게임 가능**
   - 일부만 나가고 남은 사람들은 새로운 참가자를 기다려 게임 재개 가능
   - 참가자가 4명이 되면 다시 게임 시작 가능

---

## 테스트 시나리오

### 시나리오 1: 정상적인 방 나가기
```
1. 4명이 게임 진행
2. 게임 종료 (ROUND_END)
3. Player1: LEAVE_ROOM 전송
   → Player1: "Successfully left" 수신
   → Player2,3,4: "Player1 has left" 수신 (3명 남음)
4. Player2: LEAVE_ROOM 전송
   → Player2: "Successfully left" 수신
   → Player3,4: "Player2 has left" 수신 (2명 남음)
5. Player3: LEAVE_ROOM 전송
   → Player3: "Successfully left" 수신
   → Player4: "Player3 has left" 수신 (1명 남음)
6. Player4: LEAVE_ROOM 전송 (마지막)
   → Player4: "Successfully left" 수신
   → 방은 유지됨 (게임 상태만 정리)
   → 새로운 참가자 대기 상태
```

### 시나리오 2: 일부만 나가고 계속 게임
```
1. 4명이 게임 진행 (Player1, 2, 3, 4)
2. 게임 종료 (ROUND_END)
3. Player1, Player2: LEAVE_ROOM (2명 나감)
   → Player3, Player4는 방에 남음
4. 새로운 Player5, Player6이 JOIN_ROOM
   → 다시 4명이 됨
5. 같은 방에서 게임 재시작 가능
```

### 시나리오 3: 재참가
```
1. Player1이 LEAVE_ROOM으로 방 나감
2. Player1이 다시 JOIN_ROOM으로 같은 방에 재참가
   → 성공적으로 재참가됨
3. 다시 게임 진행 가능
```

### 시나리오 4: 에러 케이스
```
1. 잘못된 방 코드로 LEAVE_ROOM 전송
   → ERROR: "Room Not Found"

2. 이미 나간 후 다시 LEAVE_ROOM 전송
   → ERROR: "Participant Not Found"

3. 방 코드 없이 LEAVE_ROOM 전송
   → ERROR: "Room Code is Required"
```

---

## 관련 API

- [ROUND_END](./WEBSOCKET_ROUND_END.md) - 게임 종료
- [ROOM_EXIT](./WEBSOCKET_API_COMPLETE.md#room_exit) - 게임 시작 전 방 나가기
- [WebSocket 연결](./WEBSOCKET_CONNECTION.md) - 기본 연결 방법

---

## 문의

API 관련 문의사항이 있으시면 백엔드 팀에게 연락 주세요.


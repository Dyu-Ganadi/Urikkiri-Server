# WebSocket 연결 가이드

## 📡 연결 흐름

### 1. WebSocket 연결 수립
```
Client → ws://localhost:8080/ws (JWT 인증)
  ↓
CustomHandshakeInterceptor (JWT 검증)
  ↓
WebSocketHandler.afterConnectionEstablished()
  ↓
{ "type": "CONNECTED", "message": "..." }
```

## 🔐 인증 프로세스

### CustomHandshakeInterceptor
```java
@Component
public class CustomHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, 
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // Security Context에서 인증된 사용자 정보 추출
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            attributes.put("userPrincipal", 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        }
        return true;
    }
}
```

**역할:**
- HTTP 요청이 Spring Security 필터를 통과한 후 실행
- 인증된 사용자 정보를 WebSocket 세션에 복사
- JWT 토큰이 유효하지 않으면 연결 거부

## 🎯 연결 엔드포인트

### WebSocketConfig
```java
@EnableWebSocket
@Configuration
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(customHandshakeInterceptor);
    }
}
```

**설정:**
- 엔드포인트: `ws://localhost:8080/ws`
- CORS: 모든 origin 허용 (`*`)
- Interceptor: JWT 인증 처리

## 📨 메시지 타입

### WebSocketMessageType
```java
public enum WebSocketMessageType {
    // 연결 관련
    CONNECTED,           // 연결 성공
    
    // 방 관련
    CREATE_ROOM,         // 방 생성 요청
    ROOM_CREATED,        // 방 생성 완료
    JOIN_ROOM,           // 방 참가 요청
    ROOM_JOINED,         // 방 참가 완료
    LEAVE_ROOM,          // 방 나가기
    
    // 에러
    ERROR                // 에러 발생
}
```

### WebSocketMessage (record)
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketMessage(
    WebSocketMessageType type,
    String roomCode,
    Object data,
    String message
) {
    // 정적 팩토리 메서드
    public static WebSocketMessage of(WebSocketMessageType type, String message) {
        return new WebSocketMessage(type, null, null, message);
    }
    
    public static WebSocketMessage of(WebSocketMessageType type, String roomCode, String message) {
        return new WebSocketMessage(type, roomCode, null, message);
    }
}
```

## 🚀 클라이언트 구현

### JavaScript 예제

#### 1. 기본 연결
```javascript
// JWT 토큰 준비 (로그인 후 받은 토큰)
const token = localStorage.getItem('jwtToken');

// WebSocket 연결
const ws = new WebSocket('ws://localhost:8080/ws');

ws.onopen = () => {
    console.log('✅ WebSocket 연결 성공');
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('📨 서버 메시지:', message);
    handleMessage(message);
};

ws.onerror = (error) => {
    console.error('❌ WebSocket 에러:', error);
};

ws.onclose = (event) => {
    console.log('🔌 연결 종료:', event.code, event.reason);
};
```

#### 2. 메시지 핸들링
```javascript
function handleMessage(message) {
    switch (message.type) {
        case 'CONNECTED':
            console.log('✅ 서버 연결 완료:', message.message);
            // 방 생성 또는 참가 가능
            break;
            
        case 'ROOM_CREATED':
            console.log('🎉 방 생성 완료:', message.roomCode);
            // 방 코드를 UI에 표시
            displayRoomCode(message.roomCode);
            break;
            
        case 'ROOM_JOINED':
            console.log('✅ 방 참가 완료:', message.roomCode);
            // 게임 화면으로 이동
            startGame(message.roomCode);
            break;
            
        case 'ERROR':
            console.error('❌ 에러:', message.message);
            showError(message.message);
            break;
    }
}
```

#### 3. 방 생성
```javascript
function createRoom() {
    const message = {
        type: 'CREATE_ROOM'
    };
    ws.send(JSON.stringify(message));
}

// 사용
ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    
    if (msg.type === 'CONNECTED') {
        createRoom(); // 연결 성공 후 방 생성
    }
};
```

#### 4. 방 참가
```javascript
function joinRoom(roomCode) {
    const message = {
        type: 'JOIN_ROOM',
        roomCode: roomCode
    };
    ws.send(JSON.stringify(message));
}

// 사용
const roomCode = prompt('방 코드를 입력하세요:');
joinRoom(roomCode);
```

### TypeScript 예제

```typescript
enum WebSocketMessageType {
    CONNECTED = 'CONNECTED',
    CREATE_ROOM = 'CREATE_ROOM',
    ROOM_CREATED = 'ROOM_CREATED',
    JOIN_ROOM = 'JOIN_ROOM',
    ROOM_JOINED = 'ROOM_JOINED',
    ERROR = 'ERROR'
}

interface WebSocketMessage {
    type: WebSocketMessageType;
    roomCode?: string;
    data?: any;
    message?: string;
}

class WebSocketClient {
    private ws: WebSocket;
    private roomCode: string | null = null;

    constructor(private url: string) {
        this.connect();
    }

    private connect(): void {
        this.ws = new WebSocket(this.url);
        
        this.ws.onopen = () => {
            console.log('✅ WebSocket 연결 성공');
        };
        
        this.ws.onmessage = (event) => {
            const message: WebSocketMessage = JSON.parse(event.data);
            this.handleMessage(message);
        };
        
        this.ws.onerror = (error) => {
            console.error('❌ WebSocket 에러:', error);
        };
        
        this.ws.onclose = () => {
            console.log('🔌 연결 종료');
        };
    }

    private handleMessage(message: WebSocketMessage): void {
        switch (message.type) {
            case WebSocketMessageType.CONNECTED:
                console.log('서버 연결 완료');
                break;
                
            case WebSocketMessageType.ROOM_CREATED:
                this.roomCode = message.roomCode!;
                console.log('방 생성:', this.roomCode);
                break;
                
            case WebSocketMessageType.ROOM_JOINED:
                this.roomCode = message.roomCode!;
                console.log('방 참가:', this.roomCode);
                break;
                
            case WebSocketMessageType.ERROR:
                console.error('에러:', message.message);
                break;
        }
    }

    public createRoom(): void {
        this.send({ type: WebSocketMessageType.CREATE_ROOM });
    }

    public joinRoom(roomCode: string): void {
        this.send({
            type: WebSocketMessageType.JOIN_ROOM,
            roomCode: roomCode
        });
    }

    private send(message: Partial<WebSocketMessage>): void {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        }
    }

    public close(): void {
        this.ws.close();
    }
}

// 사용
const client = new WebSocketClient('ws://localhost:8080/ws');
client.createRoom();
```

## 🔄 연결 상태 관리

### 세션 관리자
```java
@Component
public class WebSocketSessionManager {
    // roomCode -> Set<WebSocketSession>
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    // 방에 세션 추가
    public void addSession(String roomCode, WebSocketSession session) {
        roomSessions.computeIfAbsent(roomCode, k -> new CopyOnWriteArraySet<>())
                    .add(session);
    }
    
    // 방에서 세션 제거
    public void removeSession(String roomCode, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        }
    }
    
    // 특정 방의 모든 세션 조회
    public Set<WebSocketSession> getSessionsByRoom(String roomCode) {
        return roomSessions.getOrDefault(roomCode, Set.of());
    }
    
    // 세션이 속한 방 코드 찾기
    public String getRoomCodeBySession(WebSocketSession session) {
        return roomSessions.entrySet().stream()
                .filter(entry -> entry.getValue().contains(session))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}
```

**특징:**
- `ConcurrentHashMap`: 스레드 안전성 보장
- `CopyOnWriteArraySet`: 동시 수정 안전
- 방 참가자 0명 시 자동 정리

## ⚠️ 에러 처리

### 서버 측 에러

**1. 인증 실패**
```json
{
    "type": "ERROR",
    "message": "Authentication required"
}
```
→ JWT 토큰이 없거나 만료됨

**2. 방 코드 없음**
```json
{
    "type": "ERROR",
    "message": "Room code is required"
}
```
→ JOIN_ROOM 메시지에 roomCode가 없음

**3. 방이 존재하지 않음**
```json
{
    "type": "ERROR",
    "message": "Room does not exist"
}
```
→ 유효하지 않은 방 코드

**4. 참가 권한 없음**
```json
{
    "type": "ERROR",
    "message": "You are not a participant of this room"
}
```
→ Participant로 등록되지 않은 사용자

**5. 잘못된 메시지 형식**
```json
{
    "type": "ERROR",
    "message": "Invalid message format"
}
```
→ JSON 파싱 실패

### 클라이언트 에러 처리

```javascript
ws.onerror = (error) => {
    console.error('WebSocket 에러:', error);
    showNotification('연결에 문제가 발생했습니다.', 'error');
};

ws.onclose = (event) => {
    if (event.code === 1006) {
        // 비정상 종료
        console.error('연결이 비정상적으로 종료되었습니다.');
        reconnect();
    }
};

function reconnect() {
    setTimeout(() => {
        console.log('재연결 시도...');
        ws = new WebSocket('ws://localhost:8080/ws');
    }, 3000);
}
```

## 🎮 실제 사용 시나리오

### 시나리오 1: 방 생성 후 퀴즈 진행

```javascript
let ws;
let currentRoomCode;

// 1. WebSocket 연결
function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8080/ws');
    
    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        
        switch (msg.type) {
            case 'CONNECTED':
                // 2. 방 생성 요청
                ws.send(JSON.stringify({ type: 'CREATE_ROOM' }));
                break;
                
            case 'ROOM_CREATED':
                // 3. 방 코드 저장
                currentRoomCode = msg.roomCode;
                console.log('방 코드:', currentRoomCode);
                
                // 4. 퀴즈 출제 가능
                // submitQuiz();
                break;
        }
    };
}

connectWebSocket();
```

### 시나리오 2: 방 참가 후 대기

```javascript
function joinExistingRoom(roomCode) {
    ws = new WebSocket('ws://localhost:8080/ws');
    
    ws.onmessage = (event) => {
        const msg = JSON.parse(event.data);
        
        switch (msg.type) {
            case 'CONNECTED':
                // 방 참가 요청
                ws.send(JSON.stringify({
                    type: 'JOIN_ROOM',
                    roomCode: roomCode
                }));
                break;
                
            case 'ROOM_JOINED':
                console.log('방 참가 성공!');
                showGameScreen();
                break;
                
            case 'ERROR':
                alert('방 참가 실패: ' + msg.message);
                break;
        }
    };
}

// 사용
const code = document.getElementById('roomCodeInput').value;
joinExistingRoom(code);
```

## 🔍 디버깅 팁

### 1. 브라우저 개발자 도구
```javascript
// WebSocket 상태 확인
console.log('WebSocket 상태:', ws.readyState);
// 0: CONNECTING
// 1: OPEN
// 2: CLOSING
// 3: CLOSED
```

### 2. 메시지 로깅
```javascript
ws.onmessage = (event) => {
    console.log('📩 받은 메시지:', event.data);
    const msg = JSON.parse(event.data);
    handleMessage(msg);
};

ws.send = (function(original) {
    return function(data) {
        console.log('📤 보낸 메시지:', data);
        return original.call(this, data);
    };
})(ws.send);
```

### 3. 서버 로그 확인
```
User testuser connected to WebSocket (waiting for room action)
User testuser created and joined room 123456
```

## 📋 체크리스트

### 연결 전
- [ ] JWT 토큰 준비됨
- [ ] WebSocket URL 확인 (`ws://localhost:8080/ws`)
- [ ] CORS 설정 확인

### 연결 후
- [ ] `CONNECTED` 메시지 수신 확인
- [ ] 방 생성/참가 메시지 전송 가능
- [ ] 에러 핸들링 구현

### 테스트
- [ ] 정상 연결 테스트
- [ ] 인증 실패 테스트 (토큰 없이)
- [ ] 방 생성 테스트
- [ ] 방 참가 테스트 (유효한 코드)
- [ ] 방 참가 실패 테스트 (잘못된 코드)
- [ ] 재연결 테스트

## 🚀 성능 최적화

### 1. 연결 재사용
```javascript
// ❌ 나쁜 예: 매번 새로운 연결
function sendMessage(msg) {
    const ws = new WebSocket('ws://localhost:8080/ws');
    ws.onopen = () => ws.send(JSON.stringify(msg));
}

// ✅ 좋은 예: 연결 재사용
const ws = new WebSocket('ws://localhost:8080/ws');
function sendMessage(msg) {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(msg));
    }
}
```

### 2. 메시지 큐
```javascript
class WebSocketQueue {
    constructor(url) {
        this.ws = new WebSocket(url);
        this.queue = [];
        
        this.ws.onopen = () => {
            this.queue.forEach(msg => this.ws.send(msg));
            this.queue = [];
        };
    }
    
    send(message) {
        const json = JSON.stringify(message);
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(json);
        } else {
            this.queue.push(json);
        }
    }
}
```

## 📚 참고 자료

- Spring WebSocket 공식 문서
- WebSocket API (MDN)
- RFC 6455 - The WebSocket Protocol


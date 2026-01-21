# WebSocket 연결 가이드

## 📡 연결 흐름

### WebSocket 연결 수립
```
Client → ws://localhost:8080/ws?token={JWT}
  ↓
CustomHandshakeInterceptor (JWT 검증)
  ↓
WebSocketHandler.afterConnectionEstablished()
  ↓
Client: { "type": "CONNECTED", "message": "WebSocket connection established..." }
```

**토큰 전달 방법:**
- **권장 (브라우저 환경)**: 쿼리 파라미터 - `ws://localhost:8080/ws?token={JWT}`
- **대안 (네이티브 앱)**: Authorization 헤더 - `Authorization: Bearer {JWT}`

## 🔐 인증 프로세스

### CustomHandshakeInterceptor
```java
@Component
@RequiredArgsConstructor
public class CustomHandshakeInterceptor implements HandshakeInterceptor {
    
    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;

        // 1. 쿼리 파라미터에서 토큰 추출 (브라우저 환경용)
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            String[] params = query.split("&");
            for (String param : params) {
                if (param.startsWith("token=")) {
                    token = param.substring(6); // "token=" 이후의 값
                    break;
                }
            }
        }

        // 2. Authorization 헤더에서 토큰 추출 (네이티브 앱 등에서 사용 가능)
        if (token == null) {
            List<String> authHeaders = request.getHeaders().get("Authorization");
            if (authHeaders != null && !authHeaders.isEmpty()) {
                token = authHeaders.get(0);
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
            }
        }

        // 토큰이 없으면 연결 거부
        if (token == null || token.isEmpty()) {
            return false;
        }

        try {
            // JWT 토큰 검증 및 인증 정보 추출
            Authentication authentication = jwtProvider.authentication(token);

            // AuthDetails에서 실제 User 객체 추출
            if (authentication.getPrincipal() instanceof AuthDetails authDetails) {
                attributes.put("userPrincipal", authDetails.getUser());
                return true;
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
```

**역할:**
- **우선 순위 1**: 쿼리 파라미터에서 JWT 토큰 추출 (`?token={JWT}` 형식)
  - 브라우저 WebSocket API는 커스텀 헤더를 설정할 수 없으므로 쿼리 파라미터 사용
- **우선 순위 2**: Authorization 헤더에서 JWT 토큰 추출 (`Bearer {token}` 형식)
  - 네이티브 앱이나 커스텀 헤더를 지원하는 WebSocket 클라이언트용
- `JwtProvider`를 통해 토큰 검증 및 `Authentication` 객체 생성
- `AuthDetails`에서 실제 `User` 엔티티 추출하여 세션 attributes에 저장
- JWT 토큰이 없거나 유효하지 않으면 연결 거부 (`return false`)

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

### WebSocketMessageType (global/websocket/dto)
```java
public enum WebSocketMessageType {
    CONNECTED,           // 연결 성공
    CREATE_ROOM,         // 방 생성 요청
    ROOM_CREATED,        // 방 생성 완료
    JOIN_ROOM,           // 방 참가 요청
    USER_JOINED,         // 방 참가 완료 (전체 참가자 목록)
    LEAVE_ROOM,          // 방 나가기
    ERROR                // 에러 발생
}
```

### WebSocketMessage (global/websocket/dto)
```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WebSocketMessage(
    WebSocketMessageType type,
    String room_code,
    Object data,
    String message
) {
    // 정적 팩토리 메서드
    public static WebSocketMessage of(WebSocketMessageType type, String message);
    public static WebSocketMessage of(WebSocketMessageType type, String room_code, String message);
    public static WebSocketMessage withData(WebSocketMessageType type, String room_code, 
                                           Object data, String message);
}
```

### ParticipantInfo (global/websocket/dto)
```java
public record ParticipantInfo(
    Long userId,
    String nickname,
    int bananaScore
) {
    public static ParticipantInfo from(Participant participant);
}
```

## 🚀 클라이언트 구현

### JavaScript 기본 연결

```javascript
// JWT 토큰 준비 (로그인 후 받은 토큰)
const token = localStorage.getItem('jwtToken');

// WebSocket 연결 (쿼리 파라미터로 토큰 전달)
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);

ws.onopen = () => {
    console.log('✅ WebSocket 연결 성공');
};

ws.onmessage = (event) => {
    const message = JSON.parse(event.data);
    console.log('📨 서버 메시지:', message);
    
    if (message.type === 'CONNECTED') {
        console.log('✅ 서버로부터 연결 확인:', message.message);
        // 이제 다른 메시지를 보낼 수 있습니다
    }
};

ws.onerror = (error) => {
    console.error('❌ WebSocket 에러:', error);
};

ws.onclose = (event) => {
    console.log('🔌 연결 종료:', event.code, event.reason);
};
```

**중요:** 브라우저의 표준 WebSocket API는 커스텀 헤더(Authorization 등)를 설정할 수 없습니다. 따라서 토큰을 **쿼리 파라미터**로 전달해야 합니다.

### 메시지 송신 예제

```javascript
// 메시지 전송 함수
function sendMessage(type, data = {}) {
    if (ws.readyState === WebSocket.OPEN) {
        const message = { type, ...data };
        ws.send(JSON.stringify(message));
        console.log('📤 메시지 전송:', message);
    } else {
        console.error('WebSocket이 연결되지 않았습니다.');
    }
}

// 사용 예시
ws.onmessage = (event) => {
    const msg = JSON.parse(event.data);
    
    if (msg.type === 'CONNECTED') {
        // 연결 후 원하는 메시지 전송
        sendMessage('CREATE_ROOM');
        // 또는
        sendMessage('JOIN_ROOM', { room_code: '123456' });
    }
};
```

### TypeScript 예제

```typescript
enum WebSocketMessageType {
    CONNECTED = 'CONNECTED',
    CREATE_ROOM = 'CREATE_ROOM',
    ROOM_CREATED = 'ROOM_CREATED',
    JOIN_ROOM = 'JOIN_ROOM',
    USER_JOINED = 'USER_JOINED',
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

    constructor(private url: string, private token: string) {
        this.connect();
    }

    private connect(): void {
        // 토큰을 쿼리 파라미터로 추가
        const wsUrl = `${this.url}?token=${this.token}`;
        this.ws = new WebSocket(wsUrl);
        
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
                console.log('서버 연결 완료:', message.message);
                break;
                
            case WebSocketMessageType.ERROR:
                console.error('에러:', message.message);
                break;
                
            default:
                console.log('메시지 수신:', message);
        }
    }

    public send(message: Partial<WebSocketMessage>): void {
        if (this.ws.readyState === WebSocket.OPEN) {
            this.ws.send(JSON.stringify(message));
        }
    }

    public close(): void {
        this.ws.close();
    }
}

// 사용
const token = localStorage.getItem('jwtToken');
const client = new WebSocketClient('ws://localhost:8080/ws', token);
```

## 🔄 세션 관리

### WebSocketSessionManager
```java
@Component
public class WebSocketSessionManager {
    // roomCode -> Set<WebSocketSession>
    private final Map<String, Set<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    public void addSession(String room_code, WebSocketSession session) {
        roomSessions.computeIfAbsent(room_code, k -> new CopyOnWriteArraySet<>()).add(session);
    }
    
    public void removeSession(String room_code, WebSocketSession session) {
        Set<WebSocketSession> sessions = roomSessions.get(roomCode);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        }
    }
    
    public Set<WebSocketSession> getSessionsByRoom(String roomCode) {
        return roomSessions.getOrDefault(room_code, Set.of());
    }
    
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
→ JWT 토큰이 없거나 만료됨 (CustomHandshakeInterceptor에서 연결 거부)  
→ 쿼리 파라미터 `?token=...` 또는 Authorization 헤더에 유효한 JWT 토큰을 포함해야 함

**2. 잘못된 메시지 형식**
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
        const token = localStorage.getItem('jwtToken');
        ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
    }, 3000);
}
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

// 전송 메시지 로깅
const originalSend = ws.send.bind(ws);
ws.send = function(data) {
    console.log('📤 보낸 메시지:', data);
    return originalSend(data);
};
```

### 3. 서버 로그 확인
```
User {nickname} connected to WebSocket (waiting for room action)
```

## 📋 체크리스트

### 연결 전
- [ ] JWT 토큰 준비됨
- [ ] WebSocket URL 확인 (`ws://localhost:8080/ws?token={JWT}`)
- [ ] 토큰을 쿼리 파라미터로 전달 (브라우저 환경)
- [ ] CORS 설정 확인

### 연결 후
- [ ] `CONNECTED` 메시지 수신 확인
- [ ] 메시지 송수신 가능
- [ ] 에러 핸들링 구현

### 테스트
- [ ] 정상 연결 테스트
- [ ] 인증 실패 테스트 (토큰 없이)
- [ ] 재연결 테스트

## 🚀 성능 최적화

### 1. 연결 재사용
```javascript
// ❌ 나쁜 예: 매번 새로운 연결
function sendMessage(msg) {
    const token = localStorage.getItem('jwtToken');
    const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
    ws.onopen = () => ws.send(JSON.stringify(msg));
}

// ✅ 좋은 예: 연결 재사용
const token = localStorage.getItem('jwtToken');
const ws = new WebSocket(`ws://localhost:8080/ws?token=${token}`);
function sendMessage(msg) {
    if (ws.readyState === WebSocket.OPEN) {
        ws.send(JSON.stringify(msg));
    }
}
```

### 2. 메시지 큐
```javascript
class WebSocketQueue {
    constructor(url, token) {
        this.ws = new WebSocket(`${url}?token=${token}`);
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

- [Spring WebSocket 공식 문서](https://docs.spring.io/spring-framework/reference/web/websocket.html)
- [WebSocket API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [RFC 6455 - The WebSocket Protocol](https://datatracker.ietf.org/doc/html/rfc6455)

## 🔗 관련 파일

### 서버 코드
- `CustomHandshakeInterceptor.java` - JWT 인증 처리
- `WebSocketHandler.java` - 메시지 핸들링
- `WebSocketSessionManager.java` - 세션 관리
- `SecurityConfig.java` - Security 설정 (`/ws/**` 허용)

### DTO
- `WebSocketMessage.java` - 메시지 포맷 (global/websocket/dto)
- `WebSocketMessageType.java` - 메시지 타입 (global/websocket/dto)
- `ParticipantInfo.java` - 참가자 정보 (global/websocket/dto)


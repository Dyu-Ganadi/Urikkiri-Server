# WebSocket 연결 API

## 개요
게임 방 생성 및 참가를 위한 WebSocket 연결을 설정합니다.

## Endpoint
```
ws://{HOST}/ws?token={JWT_TOKEN}
```

## 환경 변수 설정

### 개발 환경 (.env.development)
```env
REACT_APP_WS_URL=ws://localhost:8080/ws
```

### 프로덕션 환경 (.env.production)
```env
REACT_APP_WS_URL=wss://api.urikkiri.com/ws
```

> **참고**: 프로덕션 환경에서는 보안을 위해 `wss://` (WebSocket Secure) 프로토콜을 사용해야 합니다.

## 연결 방법

### Query Parameter로 토큰 전달
```javascript
const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
const token = localStorage.getItem('accessToken');
const ws = new WebSocket(`${WS_URL}?token=${token}`);
```

## 연결 이벤트

### 연결 성공
WebSocket 연결이 성공하면 서버에서 다음 메시지를 전송합니다:

**응답 메시지:**
```json
{
  "type": "CONNECTED",
  "message": "WebSocket connection established. Send CREATE_ROOM or JOIN_ROOM message."
}
```

### 연결 실패
다음 경우에 연결이 거부됩니다:
- JWT 토큰이 없는 경우
- JWT 토큰이 만료된 경우
- JWT 토큰이 유효하지 않은 경우

## 클라이언트 구현 예시

### Vanilla JavaScript
```javascript
class GameWebSocket {
  constructor(token) {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    this.ws = new WebSocket(`${WS_URL}?token=${token}`);
    this.setupEventHandlers();
  }

  setupEventHandlers() {
    this.ws.onopen = () => {
      console.log('WebSocket 연결 성공');
    };

    this.ws.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log('서버 메시지:', message);
      
      switch(message.type) {
        case 'CONNECTED':
          console.log('연결 확인:', message.message);
          break;
        case 'ERROR':
          console.error('에러:', message.message);
          break;
        // 다른 메시지 타입 처리...
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket 에러:', error);
    };

    this.ws.onclose = (event) => {
      console.log('WebSocket 연결 종료:', event.code, event.reason);
    };
  }

  send(message) {
    if (this.ws.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(message));
    } else {
      console.error('WebSocket이 열려있지 않습니다');
    }
  }

  close() {
    this.ws.close();
  }
}

// 사용 예시
const token = localStorage.getItem('accessToken');
const gameWs = new GameWebSocket(token);
```

### React Hook 예시
```jsx
import { useEffect, useRef, useState } from 'react';

function useWebSocket() {
  const [isConnected, setIsConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const wsRef = useRef(null);

  useEffect(() => {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    const token = localStorage.getItem('accessToken');
    
    if (!token) {
      console.error('토큰이 없습니다');
      return;
    }

    wsRef.current = new WebSocket(`${WS_URL}?token=${token}`);

    wsRef.current.onopen = () => {
      console.log('WebSocket 연결 성공');
      setIsConnected(true);
    };

    wsRef.current.onmessage = (event) => {
      const message = JSON.parse(event.data);
      setLastMessage(message);
    };

    wsRef.current.onerror = (error) => {
      console.error('WebSocket 에러:', error);
    };

    wsRef.current.onclose = () => {
      console.log('WebSocket 연결 종료');
      setIsConnected(false);
    };

    return () => {
      wsRef.current?.close();
    };
  }, []);

  const sendMessage = (message) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    }
  };

  return { isConnected, lastMessage, sendMessage };
}

// 사용 예시
function GamePage() {
  const { isConnected, lastMessage, sendMessage } = useWebSocket();

  useEffect(() => {
    if (lastMessage) {
      console.log('받은 메시지:', lastMessage);
    }
  }, [lastMessage]);

  return (
    <div>
      <p>연결 상태: {isConnected ? '연결됨' : '연결 안됨'}</p>
      {/* ... */}
    </div>
  );
}
```

### Context API를 사용한 전역 관리
```jsx
import { createContext, useContext, useEffect, useRef, useState } from 'react';

const WebSocketContext = createContext(null);

export function WebSocketProvider({ children }) {
  const [isConnected, setIsConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const wsRef = useRef(null);

  useEffect(() => {
    const WS_URL = process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws';
    const token = localStorage.getItem('accessToken');
    
    if (!token) return;

    wsRef.current = new WebSocket(`${WS_URL}?token=${token}`);

    wsRef.current.onopen = () => {
      setIsConnected(true);
    };

    wsRef.current.onmessage = (event) => {
      setLastMessage(JSON.parse(event.data));
    };

    wsRef.current.onclose = () => {
      setIsConnected(false);
    };

    return () => wsRef.current?.close();
  }, []);

  const sendMessage = (message) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(message));
    }
  };

  return (
    <WebSocketContext.Provider value={{ isConnected, lastMessage, sendMessage }}>
      {children}
    </WebSocketContext.Provider>
  );
}

export const useWebSocket = () => useContext(WebSocketContext);
```

## 에러 응답

### 인증 실패
연결이 거부되고 WebSocket이 즉시 닫힙니다.

**에러 타입:**
- 토큰 누락
- 토큰 만료
- 토큰 검증 실패

## 주의사항

1. **토큰 관리**
   - JWT 토큰은 반드시 URL 인코딩해야 합니다
   - 토큰 만료 시 재연결이 필요합니다

2. **연결 유지**
   - 네트워크 변경 시 재연결이 필요할 수 있습니다
   - 서버 재시작 시 모든 연결이 끊어집니다

3. **보안**
   - 프로덕션 환경에서는 `wss://` 프로토콜을 사용해야 합니다
   - 토큰이 URL에 노출되므로 보안에 주의해야 합니다

## 연결 상태 확인

```javascript
// WebSocket 연결 상태
console.log(ws.readyState);

// 0: CONNECTING - 연결 시도 중
// 1: OPEN - 연결됨, 통신 가능
// 2: CLOSING - 연결 종료 중
// 3: CLOSED - 연결 종료됨
```

## 다음 단계

연결 성공 후 다음 작업을 수행할 수 있습니다:
- [방 생성하기](./WEBSOCKET_CREATE_ROOM.md)
- [방 참가하기](./WEBSOCKET_JOIN_ROOM.md)



# REST API 명세서

WebSocket 연결 전에 필요한 REST API들입니다.

---

## Base URL

```
http://localhost:8080/api
```

---

## 인증 (Authentication)

### POST /user/login - 로그인

**설명**: 사용자 로그인 후 Access Token을 받습니다.

**요청**:
```http
POST /api/user/login
Content-Type: application/json

{
  "username": "testuser",
  "password": "1234"
}
```

**응답 (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImlhdCI6MTY3...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsImlhdCI6MTY3..."
}
```

**에러 (401 Unauthorized)**:
```json
{
  "status": 401,
  "message": "Invalid username or password"
}
```

**curl 예시**:
```bash
curl -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "1234"
  }'
```

---

### POST /user/signup - 회원가입

**설명**: 새 사용자 계정을 생성합니다.

**요청**:
```http
POST /api/user/signup
Content-Type: application/json

{
  "username": "newuser",
  "password": "1234",
  "nickname": "닉네임"
}
```

**응답 (201 Created)**:
```json
{
  "user_id": 123,
  "username": "newuser",
  "nickname": "닉네임",
  "level": 1
}
```

**에러 (409 Conflict)**:
```json
{
  "status": 409,
  "message": "Username already exists"
}
```

**curl 예시**:
```bash
curl -X POST http://localhost:8080/api/user/signup \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "password": "1234",
    "nickname": "닉네임"
  }'
```

---

## 사용자 정보 (User)

### GET /user/my - 내 정보 조회

**설명**: 현재 로그인한 사용자의 정보를 조회합니다.

**인증**: 필수 (Bearer Token)

**요청**:
```http
GET /api/user/my
Authorization: Bearer {accessToken}
```

**응답 (200 OK)**:
```json
{
  "user_id": 1,
  "username": "testuser",
  "nickname": "테스터",
  "level": 5,
  "exp": 120,
  "totalGames": 25,
  "wins": 8
}
```

**에러 (401 Unauthorized)**:
```json
{
  "status": 401,
  "message": "Invalid or expired token"
}
```

**curl 예시**:
```bash
curl -X GET http://localhost:8080/api/user/my \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 카드 (Cards)

### GET /cards/random - 랜덤 카드 조회

**설명**: 게임에 사용할 랜덤 카드 10장을 조회합니다.

**인증**: 필수 (Bearer Token)

**요청**:
```http
GET /api/cards/random
Authorization: Bearer {accessToken}
```

**응답 (200 OK)**:
```json
{
  "cards": [
    {
      "card_id": 1,
      "word": "치킨"
    },
    {
      "card_id": 2,
      "word": "피자"
    },
    {
      "card_id": 3,
      "word": "떡볶이"
    },
    {
      "card_id": 4,
      "word": "햄버거"
    },
    {
      "card_id": 5,
      "word": "초밥"
    },
    {
      "card_id": 6,
      "word": "파스타"
    },
    {
      "card_id": 7,
      "word": "라면"
    },
    {
      "card_id": 8,
      "word": "족발"
    },
    {
      "card_id": 9,
      "word": "삼겹살"
    },
    {
      "card_id": 10,
      "word": "김밥"
    }
  ]
}
```

**사용 시점**:
- Unity 게임 시작 시 (GAME_START 받은 후)
- 출제자가 아닌 플레이어만 호출
- 받은 카드 중 하나를 선택하여 SUBMIT_CARD로 제출

**curl 예시**:
```bash
curl -X GET http://localhost:8080/api/cards/random \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 에러 응답 형식

모든 에러는 다음 형식을 따릅니다:

```json
{
  "status": 400,
  "message": "에러 메시지",
  "timestamp": "2026-01-14T10:30:00"
}
```

### 공통 에러 코드

| 상태 코드 | 설명 | 원인 |
|----------|------|------|
| 400 | Bad Request | 잘못된 요청 형식 |
| 401 | Unauthorized | 인증 실패 (토큰 없음/만료) |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 중복된 데이터 |
| 500 | Internal Server Error | 서버 에러 |

---

## 인증 토큰 사용

### 헤더에 포함

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### JavaScript 예시

```javascript
const token = localStorage.getItem('accessToken');

fetch('http://localhost:8080/api/user/my', {
    headers: {
        'Authorization': `Bearer ${token}`
    }
})
.then(response => response.json())
.then(data => console.log(data));
```

### Unity 예시

```csharp
using UnityEngine.Networking;

string token = PlayerPrefs.GetString("accessToken");

UnityWebRequest request = UnityWebRequest.Get("http://localhost:8080/api/user/my");
request.SetRequestHeader("Authorization", $"Bearer {token}");

await request.SendWebRequest();

if (request.result == UnityWebRequest.Result.Success)
{
    string json = request.downloadHandler.text;
    Debug.Log(json);
}
```

---

## 전체 플로우

### 1. 회원가입 및 로그인

```
1. POST /api/user/signup (신규 사용자만)
2. POST /api/user/login
3. accessToken 저장
```

### 2. 로비 연결

```
4. WebSocket 연결 (ws://server/ws?token={accessToken})
5. CREATE_ROOM or JOIN_ROOM
6. [4명 모임] → GAME_READY 수신
```

### 3. 게임 플레이

```
7. Unity: 새 WebSocket 연결
8. Unity: CONNECT_GAME
9. Unity: GAME_START 수신
10. GET /api/cards/random (출제자 제외)
11. Unity: SUBMIT_CARD
12. 게임 진행...
```

---

## Postman Collection

Postman Collection을 사용하여 쉽게 테스트할 수 있습니다.

### Import JSON

```json
{
  "info": {
    "name": "Urikkiri API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Login",
      "request": {
        "method": "POST",
        "header": [
          {
            "key": "Content-Type",
            "value": "application/json"
          }
        ],
        "body": {
          "mode": "raw",
          "raw": "{\n  \"username\": \"testuser\",\n  \"password\": \"1234\"\n}"
        },
        "url": {
          "raw": "http://localhost:8080/api/user/login",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "user", "login"]
        }
      }
    },
    {
      "name": "Get My Info",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{accessToken}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/user/my",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "user", "my"]
        }
      }
    },
    {
      "name": "Get Random Cards",
      "request": {
        "method": "GET",
        "header": [
          {
            "key": "Authorization",
            "value": "Bearer {{accessToken}}"
          }
        ],
        "url": {
          "raw": "http://localhost:8080/api/cards/random",
          "protocol": "http",
          "host": ["localhost"],
          "port": "8080",
          "path": ["api", "cards", "random"]
        }
      }
    }
  ]
}
```

---

## 환경 변수

### 개발 환경

```bash
# .env 또는 application.yml
SERVER_PORT=8080
JWT_SECRET=your-secret-key-here
JWT_EXPIRATION=3600000  # 1 hour in ms
```

### 프로덕션 환경

```bash
SERVER_PORT=8080
JWT_SECRET=your-production-secret-key
JWT_EXPIRATION=3600000
DATABASE_URL=your-database-url
```

---

**문서 버전**: 1.0  
**최종 업데이트**: 2026-01-14  
**관련 문서**: [WebSocket API](./WEBSOCKET_API_COMPLETE.md)


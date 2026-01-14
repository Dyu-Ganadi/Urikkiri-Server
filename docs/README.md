# 📚 우리끼리 게임 - 완전한 문서 가이드

우리끼리 게임 백엔드 API의 모든 문서를 한눈에 볼 수 있습니다.

---

## 🚀 시작하기

### 1분 가이드 - 어떤 문서를 봐야 할까요?

```
처음 시작하시나요?
    ├─ YES → [빠른 시작 가이드](api/QUICK_START.md) ⚡
    └─ NO → 아래에서 역할 선택
    
당신의 역할은?
    ├─ 프론트엔드 개발자 → [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) 📱
    ├─ Unity 개발자 → [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) 🎮
    ├─ 백엔드 개발자 → [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) 🏗️
    └─ PM/기획자 → [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) 👥

API 레퍼런스가 필요한가요?
    ├─ WebSocket API → [완전한 WebSocket API](api/WEBSOCKET_API_COMPLETE.md) 📡
    └─ REST API → [REST API 명세서](api/REST_API.md) 🔐
```

---

## 📖 문서 카탈로그

### ⚡ 빠른 시작

| 문서 | 설명 | 소요 시간 | 난이도 |
|------|------|----------|--------|
| [빠른 시작 가이드](api/QUICK_START.md) | 5분 안에 API 테스트하기 | 5분 | ⭐ |
| [REST API](api/REST_API.md) | 로그인, 카드 조회 등 | 10분 | ⭐ |

### 📱 프론트엔드 개발자

| 문서 | 설명 | 읽는 순서 |
|------|------|----------|
| [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) | 전체 아키텍처 이해 | 1️⃣ |
| [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) | 유저 관점에서 본 플로우 | 2️⃣ |
| [WebSocket API 완전판](api/WEBSOCKET_API_COMPLETE.md) | 로비 API 상세 | 3️⃣ |

**핵심 포인트**:
- 로비 WebSocket 연결 유지
- `GAME_READY` 수신 시 Unity 실행
- 토큰 + 방코드를 Unity에 전달

### 🎮 Unity 개발자

| 문서 | 설명 | 읽는 순서 |
|------|------|----------|
| [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) | Unity 구현 상세 가이드 | 1️⃣ |
| [WebSocket API 완전판](api/WEBSOCKET_API_COMPLETE.md) | 게임 API 상세 | 2️⃣ |
| [빠른 시작 가이드](api/QUICK_START.md) | Unity 예제 코드 | 3️⃣ |

**핵심 포인트**:
- 프론트엔드에서 토큰 + 방코드 받기
- 새 WebSocket 연결 생성
- `CONNECT_GAME` → `GAME_START` → 게임 진행

### 🏗️ 백엔드 개발자

| 문서 | 설명 | 중요도 |
|------|------|--------|
| [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) | v2.0 변경 내역 | ⭐⭐⭐ |
| [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) | 세션 관리 로직 | ⭐⭐⭐ |
| [WebSocket API 완전판](api/WEBSOCKET_API_COMPLETE.md) | 전체 API 명세 | ⭐⭐ |

**핵심 포인트**:
- 로비 세션 vs 게임 세션 분리
- 토큰으로 같은 유저 인식
- 메시지 라우팅 (로비/게임)

### 👥 PM / 기획자

| 문서 | 설명 | 추천 |
|------|------|------|
| [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) | 유저가 보는 전체 플로우 | ⭐⭐⭐ |
| [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) | 전체 시스템 다이어그램 | ⭐⭐ |

---

## 📚 주제별 문서

### 🔌 WebSocket 연결

- **[WebSocket 연결 기본](api/WEBSOCKET_CONNECTION.md)** - 연결 방법, 인증
- **[게임 연결 플로우](api/GAME_CONNECTION_FLOW.md)** - Unity 연결 상세
- **[프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md)** - 전체 아키텍처

### 🏠 로비 API

- **[방 생성](api/WEBSOCKET_CREATE_ROOM.md)** - CREATE_ROOM, ROOM_CREATED
- **[방 참여](api/WEBSOCKET_JOIN_ROOM.md)** - JOIN_ROOM, ROOM_JOINED, USER_JOINED

### 🎮 게임 API

- **[카드 조회](api/GET_RANDOM_CARDS.md)** - REST API로 랜덤 카드 받기
- **[카드 제출](api/WEBSOCKET_SUBMIT_CARD.md)** - SUBMIT_CARD, CARD_SUBMITTED
- **[출제자 선택](api/WEBSOCKET_EXAMINER_SELECT.md)** - EXAMINER_SELECT, EXAMINER_SELECTED
- **[다음 라운드](api/WEBSOCKET_NEXT_ROUND.md)** - NEXT_ROUND
- **[게임 종료](api/WEBSOCKET_ROUND_END.md)** - ROUND_END

### 📋 참조 자료

- **[완전한 WebSocket API](api/WEBSOCKET_API_COMPLETE.md)** - 모든 메시지 한곳에
- **[REST API 명세서](api/REST_API.md)** - 로그인, 카드 등
- **[빠른 시작](api/QUICK_START.md)** - 복붙 가능한 예제 코드

---

## 🎯 시나리오별 가이드

### 시나리오 1: 첫 API 호출

```
1. [REST API](api/REST_API.md) - 로그인으로 토큰 받기
2. [빠른 시작](api/QUICK_START.md) - 5분 안에 테스트
3. [WebSocket 연결](api/WEBSOCKET_CONNECTION.md) - 기본 연결
```

### 시나리오 2: 프론트엔드 구현

```
1. [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) - 아키텍처 이해
2. [빠른 시작](api/QUICK_START.md) - 프론트엔드 예제 복사
3. [로비 API들](api/README.md#-로비-프론트엔드) - 방 생성/참여 구현
4. [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) - 테스트 시나리오
```

### 시나리오 3: Unity 게임 구현

```
1. [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) - Unity 구현 가이드
2. [빠른 시작](api/QUICK_START.md) - Unity 예제 복사
3. [게임 API들](api/README.md#-게임-unity) - 카드 제출, 선택 등
4. [완전한 API](api/WEBSOCKET_API_COMPLETE.md) - 메시지 형식 확인
```

### 시나리오 4: 전체 시스템 이해

```
1. [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) - 유저 관점
2. [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) - 기술 구조
3. [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) - 설계 배경
```

---

## 📁 문서 구조

```
docs/
├── README.md (이 파일)
├── ARCHITECTURE_CHANGES.md          # 아키텍처 변경 내역
├── FRONTEND_UNITY_CONNECTION_GUIDE.md  # 프론트-Unity 연결
├── USER_EXPERIENCE_FLOW.md          # 유저 경험 플로우
│
└── api/
    ├── README.md                    # API 문서 목차
    ├── QUICK_START.md               # ⚡ 빠른 시작
    ├── WEBSOCKET_API_COMPLETE.md    # 📚 완전한 API
    ├── REST_API.md                  # 🔐 REST API
    │
    ├── GAME_CONNECTION_FLOW.md      # 🎮 Unity 연결 가이드
    ├── WEBSOCKET_CONNECTION.md      # 기본 연결
    │
    ├── WEBSOCKET_CREATE_ROOM.md     # 방 생성
    ├── WEBSOCKET_JOIN_ROOM.md       # 방 참여
    │
    ├── GET_RANDOM_CARDS.md          # 카드 조회
    ├── WEBSOCKET_SUBMIT_CARD.md     # 카드 제출
    ├── WEBSOCKET_EXAMINER_SELECT.md # 출제자 선택
    ├── WEBSOCKET_NEXT_ROUND.md      # 다음 라운드
    └── WEBSOCKET_ROUND_END.md       # 게임 종료
```

---

## 🔍 검색 가이드

### 키워드로 문서 찾기

| 찾고 있는 것 | 문서 |
|------------|------|
| "로그인하고 싶어요" | [REST API](api/REST_API.md) |
| "WebSocket 연결이 안 돼요" | [WebSocket 연결](api/WEBSOCKET_CONNECTION.md) |
| "방을 만들고 싶어요" | [방 생성](api/WEBSOCKET_CREATE_ROOM.md) |
| "4명 모였는데 어떻게 해요?" | [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) |
| "Unity에서 서버 연결" | [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) |
| "카드 제출하는 방법" | [카드 제출](api/WEBSOCKET_SUBMIT_CARD.md) |
| "출제자가 선택하는 법" | [출제자 선택](api/WEBSOCKET_EXAMINER_SELECT.md) |
| "전체 메시지 목록" | [완전한 WebSocket API](api/WEBSOCKET_API_COMPLETE.md) |
| "유저는 뭘 보나요?" | [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) |
| "왜 이렇게 만들었나요?" | [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) |

### 에러별 문서

| 에러 메시지 | 문서 |
|-----------|------|
| "Authentication required" | [WebSocket 연결](api/WEBSOCKET_CONNECTION.md) |
| "Room not found" | [방 참여](api/WEBSOCKET_JOIN_ROOM.md) |
| "You are not the examiner" | [출제자 선택](api/WEBSOCKET_EXAMINER_SELECT.md) |
| "Examiner cannot submit card" | [카드 제출](api/WEBSOCKET_SUBMIT_CARD.md) |

---

## ✅ 체크리스트

### 프론트엔드 개발자

- [ ] [REST API](api/REST_API.md) 읽음 - 로그인 이해
- [ ] [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) 읽음 - 아키텍처 이해
- [ ] [빠른 시작](api/QUICK_START.md) 예제 실행 - 방 생성/참여 테스트
- [ ] `GAME_READY` 핸들러 구현 - Unity 실행 로직
- [ ] 로비 WebSocket 연결 유지 확인 - 게임 후 로비 복귀

### Unity 개발자

- [ ] [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) 읽음 - Unity 구현 이해
- [ ] [빠른 시작](api/QUICK_START.md) Unity 예제 복사 - 연결 테스트
- [ ] 프론트엔드에서 토큰+방코드 받기 구현
- [ ] `CONNECT_GAME` 전송 - 게임 서버 연결
- [ ] `GAME_START` 핸들러 - 게임 시작 로직
- [ ] [GET 랜덤 카드](api/GET_RANDOM_CARDS.md) 호출 - 카드 받기
- [ ] `SUBMIT_CARD` 구현 - 카드 제출
- [ ] 출제자 로직 - `ALL_CARDS_SUBMITTED`, `EXAMINER_SELECT`
- [ ] `NEXT_ROUND`, `ROUND_END` 핸들러 - 게임 진행

### 백엔드 개발자

- [ ] [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) 읽음
- [ ] [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) 읽음
- [ ] 로비/게임 세션 관리 이해
- [ ] 메시지 라우팅 로직 이해
- [ ] 에러 처리 구현

---

## 🆘 도움이 필요한가요?

### 문제 해결 순서

1. **에러 메시지** 확인 → [위 에러별 문서](#에러별-문서) 참조
2. **키워드 검색** → [검색 가이드](#검색-가이드) 활용
3. **시나리오** 찾기 → [시나리오별 가이드](#-시나리오별-가이드) 참조
4. **전체 API** 확인 → [완전한 WebSocket API](api/WEBSOCKET_API_COMPLETE.md)

### 자주 묻는 질문

**Q: 로비 연결을 닫아야 하나요?**  
A: 아니요! [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md#3-프론트엔드-연결-유지) 참조

**Q: Unity가 재연결해야 하나요?**  
A: 네! [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md#3단계-unity-게임-서버-연결) 참조

**Q: 같은 유저인 걸 어떻게 인식하나요?**  
A: 토큰으로! [유저 경험 플로우](USER_EXPERIENCE_FLOW.md#-서버가-같은-유저임을-인식하는-방법) 참조

---

## 📊 문서 통계

- **전체 문서**: 17개
- **총 줄 수**: 약 4,000줄
- **총 단어**: 약 50,000단어
- **코드 예제**: 100+ 개
- **다이어그램**: 15+ 개

---

## 🎓 학습 경로

### 초급 (1-2시간)

```
1. [빠른 시작](api/QUICK_START.md) - 30분
2. [WebSocket 연결](api/WEBSOCKET_CONNECTION.md) - 20분
3. [REST API](api/REST_API.md) - 30분
4. 직접 테스트 - 30분
```

### 중급 (3-4시간)

```
1. 초급 과정 완료
2. [프론트엔드-Unity 연결](FRONTEND_UNITY_CONNECTION_GUIDE.md) - 1시간
3. [게임 연결 플로우](api/GAME_CONNECTION_FLOW.md) - 1시간
4. [완전한 WebSocket API](api/WEBSOCKET_API_COMPLETE.md) - 1시간
5. 구현 및 테스트 - 1시간
```

### 고급 (전체 이해)

```
1. 중급 과정 완료
2. [아키텍처 변경사항](ARCHITECTURE_CHANGES.md) - 1시간
3. [유저 경험 플로우](USER_EXPERIENCE_FLOW.md) - 30분
4. 모든 개별 API 문서 숙지 - 2시간
5. 전체 시스템 구현 - 4시간+
```

---

**문서 버전**: 2.0  
**최종 업데이트**: 2026-01-14  
**작성자**: Backend Team  
**피드백**: Issues로 남겨주세요!

**Happy Coding! 🚀**


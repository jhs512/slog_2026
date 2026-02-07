# CLAUDE.md — slog_2026 프로젝트 가이드

이 프로젝트는 다른 프로젝트들의 **디자인적 롤모델**이 되는 것을 목표로 한다.
코드는 곧 설계 문서이며, 모든 결정에는 이유가 있어야 한다.

## 프로젝트 개요

블로그/게시판 플랫폼 (front + back 모노레포)

- **Backend**: Kotlin 2.2 + Spring Boot 4.0 + JPA + QueryDSL
- **Frontend**: Next.js 16 (App Router) + React 19 + TypeScript + Tailwind CSS 4
- **인증**: JWT + OAuth2 (Kakao, Google, Naver)
- **실시간**: WebSocket/STOMP

---

## 핵심 철학

### 1. 경계가 명확한 도메인 주도 설계 (Bounded Context DDD)

백엔드는 `boundedContexts/` 아래 도메인별로 완전히 분리한다.
각 바운디드 컨텍스트는 자체적인 계층을 갖는다:

```
boundedContexts/{domain}/
├── in/        # 입력 어댑터 (Controller)
├── out/       # 출력 어댑터 (Repository)
├── app/       # 애플리케이션 서비스 (Facade)
├── domain/    # 도메인 엔티티 + 확장 함수
├── dto/       # 데이터 전송 객체
├── event/     # 도메인 이벤트
└── config/    # 모듈 설정
```

**규칙:**

- 바운디드 컨텍스트 간 직접 참조 금지. 이벤트 또는 글로벌 서비스를 통해 소통
- 도메인 로직은 엔티티 또는 확장 함수에 위치. 서비스에 비즈니스 로직을 넣지 않는다
- Facade 패턴으로 유스케이스를 조율한다 (`*Facade`)

### 2. Kotlin 확장 함수를 활용한 도메인 표현

정책, 속성 접근, 명령을 확장 함수로 분리하여 도메인 모델을 풍부하게 표현한다:

```kotlin
// 정책: PostPolicyExtensions.kt
fun Post.checkActorCanModify(actor: Member) { ... }

// 속성: PostLikeExtensions.kt
val Post.likesCount: Long get() = ...

// 명령: PostLikeExtensions.kt
fun Post.toggleLike(actor: Member) { ... }
```

**규칙:**

- `domain/{entity}Extensions/` 디렉토리에 용도별로 분리
- 정책 검증은 throwing(`BusinessException`)과 non-throwing(`RsData<Void>`) 두 가지 방식 제공

### 3. 일관된 API 응답 체계

모든 API 응답은 `RsData<T>` 봉투 패턴을 따른다:

```kotlin
data class RsData<T>(
    val resultCode: String,   // "200-1", "401-1", "403-2"
    val statusCode: Int,      // resultCode에서 자동 파싱
    val msg: String,          // 사용자 대면 메시지
    val data: T               // 응답 페이로드
)
```

**규칙:**

- resultCode 형식: `"{HTTP상태코드}-{순번}"` (예: "200-1", "404-1")
- 예외는 `BusinessException(resultCode, msg)`으로 발생
- 글로벌 `@ControllerAdvice`가 모든 예외를 `RsData`로 변환

### 4. 프론트엔드는 최소한의 전역 상태

- **Context API만 사용** (Redux/Zustand 금지) — 전역 상태는 인증 정보 정도로 최소화
- 로컬 상태(`useState`)를 우선, 서버 상태는 API 호출로 해결
- 커스텀 훅으로 로직 재사용 (`usePost`, `useAuth`)

### 5. 타입 안전성이 곧 문서

- 백엔드 OpenAPI 스키마에서 생성된 `schema.d.ts`를 프론트엔드에서 사용
- `openapi-fetch`로 타입 안전한 API 호출
- Zod 스키마로 폼 유효성 검증
- TypeScript strict 모드 필수

---

## 네이밍 규칙

### Backend (Kotlin)

| 대상              | 패턴                                       | 예시                      |
| ----------------- | ------------------------------------------ | ------------------------- |
| Controller        | `ApiV1{Domain}Controller`                  | `ApiV1PostController`     |
| Admin Controller  | `ApiV1Adm{Domain}Controller`               | `ApiV1AdmPostController`  |
| Facade            | `{Domain}Facade`                           | `PostFacade`              |
| Repository        | `{Entity}Repository`                       | `PostRepository`          |
| Custom Repository | `{Entity}RepositoryCustom`                 | `PostRepositoryCustom`    |
| Repository Impl   | `{Entity}RepositoryImpl`                   | `PostRepositoryImpl`      |
| 확장 함수 파일    | `{Entity}{역할}Extensions.kt`              | `PostPolicyExtensions.kt` |
| 요청 DTO          | `{Action}ReqBody` (Controller 내부 클래스) | `PostWriteReqBody`        |
| 이벤트            | `{Entity}{Action}Event`                    | `PostCommentWrittenEvent` |

### Frontend (TypeScript/React)

| 대상                   | 패턴                         | 예시                             |
| ---------------------- | ---------------------------- | -------------------------------- |
| 페이지                 | `page.tsx` (App Router 규약) | `app/p/[id]/page.tsx`            |
| 컴포넌트               | PascalCase                   | `PostWriteButton.tsx`            |
| 훅                     | `use{Name}.ts`               | `usePost.ts`                     |
| 라우트 스코프 컴포넌트 | `_components/`               | `app/p/[id]/_components/`        |
| 라우트 스코프 훅       | `_hooks/`                    | `app/p/[id]/_hooks/`             |
| 도메인 로직            | `domain/{name}/`             | `domain/post/hooks/usePost.ts`   |
| 전역 서비스            | `global/{name}/`             | `global/auth/hooks/useAuth.tsx`  |
| UI 컴포넌트            | kebab-case (shadcn 규약)     | `components/ui/alert-dialog.tsx` |

---

## 스타일링 규칙

### CSS 아키텍처

- **Tailwind CSS v4** + CSS 변수 기반 테마 시스템
- oklch 색공간 사용 (더 넓은 색 범위, 균일한 밝기 인식)
- 다크모드: `next-themes` + CSS `.dark` 클래스
- 코드 하이라이팅: 라이트(Prism 기본)/다크(Okaidia) 테마 직접 정의

### 컴포넌트 스타일링

- shadcn/ui 컴포넌트를 기반으로 확장 (직접 수정 가능)
- **CVA (Class Variance Authority)** 로 variant 정의
- `cn()` 유틸리티 (`clsx` + `tailwind-merge`)로 클래스 병합
- Pretendard 폰트 패밀리 (한국어 최적화)

### 디자인 토큰

테마 색상은 CSS 변수로 정의하고, Tailwind에서 `bg-background`, `text-foreground` 등으로 참조:

```
background, foreground, card, popover, primary, secondary,
muted, accent, destructive, border, input, ring
```

---

## 기술적 규칙

### Backend

- **JPA Lazy Loading 기본**: 모든 연관관계는 `LAZY` 페칭
- **Batch Fetch Size 100**: N+1 문제 방지
- **DynamicUpdate**: 변경된 필드만 UPDATE
- **PostBody 분리 패턴**: 대용량 콘텐츠는 별도 엔티티로 분리하고 Lazy 로딩
- **Attribute(EAV) 패턴**: `*Attr` 엔티티로 동적 속성 관리 (좋아요 수, 댓글 수, 조회수)
- **Virtual Thread 활성화** (Java 24)
- **이벤트 발행**: Outbox/Inbox 패턴으로 트랜잭션 안전성 보장

### Frontend

- **React Compiler 활성화** (자동 메모이제이션)
- **App Router 전용** — Pages Router 사용 금지
- **`"use client"`는 필요한 곳에만**: 서버 컴포넌트 기본
- **HOC 패턴으로 인증 가드**: `withLogin`, `withAdmin`, `withLogout`
- **Sonner로 토스트 알림**: `toast.error()`, `toast.success()`
- **STOMP WebSocket**: SockJS 폴백 + 자동 재연결 + 구독 큐

### Import 정렬 (Prettier 자동 적용)

```
1. next 프레임워크
2. react
3. 서드파티 모듈
4. @/lib/backend/client
5. @/lib/*
6. @/components/ui/*
7. lucide-react
8. 로컬 파일 (./)
```

---

## 코드 품질 체크

```bash
# 프론트엔드
cd front && pnpm check   # format → tsc → lint 순차 실행

# 백엔드
cd back && ./gradlew build
```

---

## 지켜야 할 원칙 (롤모델 프로젝트로서)

1. **일관성 > 편의성**: 기존 패턴을 따른다. 새로운 패턴 도입 시 기존 코드도 함께 마이그레이션
2. **계층 침범 금지**: Controller에서 Repository 직접 호출하지 않는다. 반드시 Facade/Service를 거친다
3. **도메인 로직은 도메인에**: Service/Controller에 비즈니스 로직을 넣지 않는다
4. **타입이 곧 문서**: any 타입 금지. OpenAPI 스키마와 Zod로 계약을 명시한다
5. **과잉 엔지니어링 금지**: 현재 필요한 만큼만 구현한다. 가상의 미래 요구사항을 위한 추상화 금지
6. **커밋 메시지는 한국어**: 변경의 "왜"를 설명한다
7. **shadcn/ui 컴포넌트를 기반으로**: UI 컴포넌트를 직접 만들기 전에 shadcn에 있는지 확인
8. **CSS 변수 기반 테마**: 하드코딩된 색상 금지. 반드시 테마 토큰을 사용

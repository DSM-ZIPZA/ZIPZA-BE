# ZIPZA-BE System Architecture

최종보고서에는 아래 두 가지를 넣는 것을 권장합니다.

- 전체 시스템 아키텍처: 프론트엔드, 백엔드, DB, Redis, 외부 API 의존성을 한눈에 보여줌
- 주요 처리 흐름: 사용자가 분석을 요청했을 때 인증, 저장, 외부 API 조회, 분석 결과 생성 과정을 보여줌

## Backend System Architecture

```mermaid
flowchart LR
    Client[Frontend Client<br/>Web or App]

    subgraph Backend[ZIPZA-BE Backend<br/>Spring Boot 3.5 + Kotlin + Java 17]
        Security[Spring Security<br/>CORS + Stateless Session]
        JwtFilter[JWT Authentication Filter]
        Exception[Global Exception Handler<br/>Exception Filter]

        subgraph ApiLayer[API Layer]
            AuthApi[Auth Controller]
            UserApi[User Controller]
            AnalysisApi[Analysis Controllers]
            RegistryApi[Registry OCR Controller]
            BuildingApi[Building Ledger Controller]
            TradeApi[Rent Trade Controller]
            ReportApi[Report Controllers]
            ReminderApi[Reminder Controller]
        end

        subgraph DomainLayer[Domain Service Layer]
            OAuthService[Kakao OAuth2 User Service]
            UserService[User Service]
            AnalysisService[Analysis Services<br/>Price / Rights / Building / Contract<br/>Guarantee / Recovery / Fraud]
            RegistryService[Registry OCR & PDF Services]
            BuildingService[Building Ledger Services]
            TradeService[Rent Trade Service]
            ReportService[Diagnosis Report<br/>Manual Check Services]
            ReminderService[Reminder Service]
            GeminiSummary[Gemini Summary Service]
        end

        subgraph Persistence[Persistence Layer]
            JpaRepo[Spring Data JPA Repositories]
            RedisService[Token Blacklist Service]
        end

        Feign[OpenFeign Clients]
    end

    subgraph Storage[Storage]
        MySQL[(MySQL<br/>JPA Entities)]
        Redis[(Redis<br/>JWT Blacklist)]
    end

    subgraph External[External Services]
        Kakao[Kakao OAuth2]
        Apick[APICK API<br/>Building / Land / Registry PDF]
        GoogleVision[Google Vision OCR]
        Gemini[Google Gemini API<br/>AI Summary]
        Molit[MOLIT RTMS API<br/>Rent Trade Data]
    end

    Client -->|REST API / JSON| Security
    Security --> Exception
    Security --> JwtFilter
    JwtFilter --> ApiLayer

    AuthApi --> OAuthService
    UserApi --> UserService
    AnalysisApi --> AnalysisService
    RegistryApi --> RegistryService
    BuildingApi --> BuildingService
    TradeApi --> TradeService
    ReportApi --> ReportService
    ReminderApi --> ReminderService

    AnalysisService --> JpaRepo
    AnalysisService --> GeminiSummary
    RegistryService --> JpaRepo
    BuildingService --> JpaRepo
    TradeService --> JpaRepo
    ReportService --> JpaRepo
    ReminderService --> JpaRepo
    UserService --> JpaRepo
    OAuthService --> JpaRepo

    JwtFilter --> RedisService
    RedisService --> Redis
    JpaRepo --> MySQL

    OAuthService --> Kakao
    GeminiSummary --> Feign
    RegistryService --> Feign
    BuildingService --> Feign
    TradeService --> Feign

    Feign --> Apick
    Feign --> GoogleVision
    Feign --> Gemini
    Feign --> Molit
```

## Main Analysis Flow

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant FE as Frontend
    participant API as ZIPZA-BE API
    participant Security as Spring Security / JWT Filter
    participant Service as Analysis Services
    participant DB as MySQL
    participant Redis as Redis
    participant External as External APIs<br/>APICK / MOLIT / Vision / Gemini

    User->>FE: 매물 정보 및 계약 정보 입력
    FE->>API: 분석 요청 REST API 호출<br/>Authorization: Bearer JWT
    API->>Security: 인증 필터 실행
    Security->>Redis: JWT 블랙리스트 확인
    Redis-->>Security: 토큰 상태 반환
    Security-->>API: 인증 사용자 식별

    API->>Service: 분석 요청 생성 및 분석 시작
    Service->>DB: User / Property / AnalysisRequest 저장
    Service->>External: 등기부, 건축물대장, 실거래가, OCR, AI 요약 요청
    External-->>Service: 외부 데이터 응답

    Service->>Service: 시세 / 권리 / 건축물 / 보증 / 회수 / 사기패턴 분석
    Service->>DB: 분석 결과 및 진단 리포트 저장
    Service-->>API: 분석 결과 DTO 반환
    API-->>FE: JSON 응답
    FE-->>User: 진단 결과 화면 표시
```

## Report Description Example

ZIPZA-BE 백엔드는 Spring Boot 기반의 계층형 아키텍처로 구성된다. 클라이언트 요청은 Spring Security와 JWT 인증 필터를 거쳐 API Controller로 전달되며, 각 Controller는 도메인 Service에 처리를 위임한다. Service 계층은 JPA Repository를 통해 MySQL에 사용자, 매물, 분석 요청, 등기부, 건축물대장, 분석 결과, 진단 리포트 데이터를 저장한다.

외부 연동은 OpenFeign Client를 통해 분리되어 있으며, APICK API는 건축물대장/토지대장/등기부 PDF 발급에 사용되고, Google Vision OCR은 등기부 OCR 처리에 사용된다. MOLIT RTMS API는 전월세 실거래가 조회에 사용되며, Gemini API는 진단 리포트의 AI 요약 생성에 사용된다. Redis는 로그아웃된 JWT 토큰의 블랙리스트 저장소로 사용되어 stateless 인증 구조를 보완한다.

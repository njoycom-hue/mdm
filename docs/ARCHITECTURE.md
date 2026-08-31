# 아키텍처

## 시스템 구성

```
[피감독자 폰: Android]                [보호자 폰: Android]
  - AccessibilityService                 - Compose 대시보드
  - DeviceAdminReceiver                   - FCM 알림 수신
  - VpnService (도메인 차단)
  - NotificationListenerService
  - UsageStatsManager
  - UploadWorker (WorkManager, 배치 업로드)
        |
        |  HTTPS (배치 5~15분 간격)
        v
[백엔드: Ktor on OCI Always Free A1]
  - Auth (JWT)
  - Pairing (보호자-피감독자 연결 코드)
  - Usage/Event ingestion API
  - Keyword Alert -> FCM push
        |
        v
[PostgreSQL] (self-hosted on same VM)
```

## 데이터 흐름 원칙
- 클라이언트에서 배치로 모아 5~15분마다 업로드 (실시간 스트리밍 아님) → 서버 부하/DB 쓰기 최소화
- 키워드 매치 이벤트만 예외적으로 즉시 업로드 + FCM 푸시 (긴급성 있는 항목)
- 원문 텍스트는 클라이언트에서 키워드 매칭 후 폐기, 매치된 키워드명/시간/앱 패키지명만 전송

## Android 모듈 구조 (`android/app/src/main/java/com/safecircle/app`)
- `admin/` - DeviceAdminReceiver, 관리자 권한 해제 방지
- `accessibility/` - MonitorAccessibilityService (앱 실행 감지, 화면 텍스트 키워드 매칭)
- `vpn/` - DomainFilterVpnService (도메인 블랙리스트 기반 차단)
- `notification/` - KeywordNotificationListenerService
- `telephony/` - CallStateReceiver (통화 이벤트만, 녹음/CALL_LOG 미사용)
- `usage/` - UsageStatsRepository
- `sync/` - UploadWorker (WorkManager 배치 업로드)
- `network/` - Retrofit ApiService, DTO
- `onboarding/` - 동의 플로우

## 백엔드 모듈 구조 (`backend/src/main/kotlin/com/safecircle/backend`)
- `routes/AuthRoutes.kt` - 회원가입/로그인 (JWT)
- `routes/PairingRoutes.kt` - 보호자-피감독자 페어링 코드 발급/연결
- `routes/UsageRoutes.kt` - 배치 사용통계 업로드
- `routes/AlertRoutes.kt` - 키워드/이벤트 알림 수신 → FCM 발송
- `db/Tables.kt` - Exposed ORM 테이블 정의
- `services/FcmService.kt` - Firebase Admin SDK 연동 (푸시 전용, Firestore 미사용)

## 배포 (OCI Always Free)
- Ampere A1 인스턴스 1대: PostgreSQL + Ktor 서버 (Docker Compose)
- Cloudflare: DNS, TLS, 기초 DDoS 방어
- Cloudflare R2: 리포트/로그 파일 저장 (선택)

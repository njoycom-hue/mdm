# SafeCircle (가칭)

도박 등 중독 회복을 돕기 위해, 본인의 명시적 동의 하에 보호자가 스마트폰 사용을 함께 관리·감독할 수 있는 Android 앱.

> ⚠️ 이 프로젝트는 **피감독자(회복 당사자) 본인의 자발적 동의**를 전제로 설계됩니다. 동의 없는 감시 기능은 통신비밀보호법·스토킹처벌법 위반 소지가 있습니다. 자세한 내용은 [`docs/LEGAL.md`](docs/LEGAL.md) 참고.

## 구조

```
android/   - Kotlin Android 앱 (보호자용 / 피감독자용, 역할 기반 단일 앱)
backend/   - Ktor(Kotlin) 백엔드 API 서버
docs/      - 아키텍처, 법적 검토 문서
```

## 핵심 설계 원칙

1. **정책 준수 우선**: SMS/CALL_LOG 위험권한을 직접 선언하지 않고, AccessibilityService 화면 텍스트 캡처(키워드 매칭) + 통화 상태 이벤트로 대체 → Google Play 정식 등록 가능한 범위 유지
2. **원문 미저장**: 문자/알림은 설정된 키워드가 매치된 경우에만 이벤트로 전송, 전체 대화 내용은 서버에 저장하지 않음
3. **투명성**: 상시 포그라운드 알림으로 모니터링 상태 고지, 온보딩에서 명시적 동의 화면 필수
4. **비용 최소화**: OCI Always Free(Ampere A1) 위에 자체 백엔드 배포, FCM으로 푸시 (규모 무관 무료)

## 아키텍처 요약

| 기능 | 구현 방식 | 필요 권한 |
|---|---|---|
| 앱 사용시간 통계 | `UsageStatsManager` | PACKAGE_USAGE_STATS |
| 특정 앱 차단 | `DeviceAdminReceiver` + `AccessibilityService` | BIND_DEVICE_ADMIN, BIND_ACCESSIBILITY_SERVICE |
| 유해 사이트 차단 | `VpnService` (도메인 블랙리스트) | BIND_VPN_SERVICE |
| 문자/알림 키워드 감지 | `NotificationListenerService` + Accessibility 화면 텍스트 | BIND_NOTIFICATION_LISTENER_SERVICE |
| 통화 이벤트(번호/시간) | `TelephonyCallback`/`PhoneStateListener` | READ_PHONE_STATE |
| 위치 확인 | `FusedLocationProviderClient` | ACCESS_FINE_LOCATION (신고 대상 가능성, LEGAL.md 참고) |

자세한 내용은 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), 비용/수익 전략은 커밋 히스토리 및 팀 채널 참고.

## 로컬 개발

```bash
# 백엔드
cd backend
docker compose up -d db
./gradlew run

# 안드로이드
cd android
./gradlew assembleDebug
```

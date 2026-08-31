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

## 구현 상태

- ✅ 인증(JWT 발급/검증, bcrypt 해싱), 회원가입/로그인
- ✅ 페어링(피감독자 코드 발급 → 보호자 입력으로 연결), TTL 만료 처리
- ✅ 동의 기록 API, 로컬 Room 큐 + `WorkManager` 배치 업로드/삭제
- ✅ 키워드/차단앱/차단도메인 정책 서버 저장(`WardSettings`) + 클라이언트 주기 동기화(`SettingsSyncWorker`)
- ✅ 키워드 매치 시 FCM 즉시 푸시 (페어링된 보호자 전원에게)
- ✅ 접근성 서비스 기반 앱 차단(`GLOBAL_ACTION_HOME`)
- ✅ VPN 기반 DNS 도메인 차단 — 전체 트래픽이 아니라 가짜 DNS 서버 주소만 라우팅해 질의만 가로채고, 나머지 트래픽은 일반 네트워크로 직접 나감 (`docs/ARCHITECTURE.md` VPN 섹션 참고)
- ✅ 권한 설정 화면(사용정보/접근성/기기관리자/알림접근/VPN 유도), 재부팅 후 VPN 자동 재시작
- ✅ 보호자용 대시보드 UI — 연결된 피보호자 목록, 최근 24시간 사용시간 요약, 키워드 알림 내역, 키워드/차단앱/차단도메인 정책 편집(`GuardianDashboardActivity`, `WardDetailActivity` + 백엔드 `/v1/guardian/*` 라우트)
- ⬜ 통화 방향(발신/수신) 구분 정교화 — 현재는 CALL_LOG 없이 상태 브로드캐스트만으로 완료/부재중만 구분
- ⬜ Firebase 실 프로젝트 연결 — `android/app/google-services.json`은 플레이스홀더이므로 실제 배포 전 교체 필요

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

## CI/배포

- `.github/workflows/build.yml` — 매 푸시마다 안드로이드 `assembleDebug` + 백엔드 `compileKotlin` 실행, 최신 디버그 APK를 `latest-debug` 릴리즈에 덮어씀 ([다운로드](https://github.com/njoycom-hue/mdm/releases/tag/latest-debug))
- `.github/workflows/deploy-oci.yml` — 백엔드를 OCI 인스턴스(duruone과 동일 서버)에 SSH로 배포 (수동 트리거). 이미지는 GitHub Actions 러너(무료 공용 인프라)에서 buildx+QEMU로 linux/arm64로 크로스 빌드한 뒤 tar로 옮겨 OCI 서버에서는 `docker load` + `docker compose up -d`만 함 — 운영 서버는 컴파일을 전혀 하지 않아 배포 중 CPU 스파이크가 없음. Docker Compose로 완전히 컨테이너화되어 있어 다른 서버/클라우드로 옮길 때도 `docker-compose.yml` + `.env` + 이미지만 있으면 재현됨. 배포 전 저장소 Secrets에 `OCI_HOST`, `OCI_USER`, `OCI_SSH_KEY`, `DATABASE_PASSWORD`, `JWT_SECRET` 등록 필요 (개인키가 passphrase로 잠겨있으면 `OCI_SSH_PASSPHRASE`도)
- `.github/workflows/deploy-oci-nginx-setup.yml` — 배포된 백엔드를 `https://mdm.duruone.com`으로 외부에 노출하는 nginx 리버스 프록시 설정 (최초 1회, 수동 트리거). duruone의 기존 Cloudflare Origin 인증서를 재사용함. **Cloudflare DNS에 `mdm.duruone.com` A레코드 추가는 이 워크플로우가 아니라 사용자가 직접 해야 함**

package com.safecircle.backend.services

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import java.io.FileInputStream

/**
 * 푸시 전용. Firestore 등 데이터 저장소는 쓰지 않는다 (docs/ARCHITECTURE.md 참고) —
 * 자체 PostgreSQL이 단일 진실 공급원이고, FCM은 알림 전송 채널로만 사용한다.
 */
object FcmService {

    private var initialized = false

    /** serviceAccountPath가 비어있으면 조용히 스킵한다 — 로컬 개발 시 Firebase 자격증명 없이도 서버가 뜨도록. */
    fun init(serviceAccountPath: String?) {
        if (serviceAccountPath.isNullOrBlank() || FirebaseApp.getApps().isNotEmpty()) return
        val credentials = GoogleCredentials.fromStream(FileInputStream(serviceAccountPath))
        val options = FirebaseOptions.builder().setCredentials(credentials).build()
        FirebaseApp.initializeApp(options)
        initialized = true
    }

    fun sendKeywordAlert(guardianFcmToken: String, wardName: String, matchedKeywords: List<String>) {
        if (!initialized) return
        val message = Message.builder()
            .setToken(guardianFcmToken)
            .setNotification(
                Notification.builder()
                    .setTitle("SafeCircle 알림")
                    .setBody("$wardName 님 기기에서 ${matchedKeywords.joinToString(", ")} 관련 활동이 감지되었습니다.")
                    .build()
            )
            .build()
        FirebaseMessaging.getInstance().send(message)
    }

    /**
     * 관리자 권한 해제 시도, 감시 대상 앱 실행, 신규 앱 설치, 장시간 무활동 등
     * profileType별로 다른 종류의 활동 알림을 보호자에게 즉시 보낸다.
     */
    fun sendActivityAlert(guardianFcmToken: String, wardName: String, type: String, detail: String) {
        if (!initialized) return
        val (title, body) = when (type) {
            "DEVICE_ADMIN_DISABLE_REQUESTED" ->
                "SafeCircle 이탈 시도 감지" to "$wardName 님이 관리자 권한 해제(삭제 전 단계)를 시도했습니다."
            "WATCHED_APP_LAUNCHED" ->
                "앱 실행 알림" to "$wardName 님이 감시 대상 앱(${detail.ifBlank { "알 수 없음" }})을 실행했습니다."
            "APP_INSTALLED" ->
                "신규 앱 설치 알림" to "$wardName 님 기기에 새 앱(${detail.ifBlank { "알 수 없음" }})이 설치되었습니다."
            "INACTIVITY_DETECTED" ->
                "무활동 감지" to "$wardName 님 기기에서 오랫동안 사용 활동이 없습니다. 안부를 확인해주세요."
            else ->
                "SafeCircle 알림" to "$wardName 님 기기에서 활동이 감지되었습니다."
        }
        val message = Message.builder()
            .setToken(guardianFcmToken)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .build()
        FirebaseMessaging.getInstance().send(message)
    }
}

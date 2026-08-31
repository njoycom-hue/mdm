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
}

package com.safecircle.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.safecircle.app.MainActivity
import com.safecircle.app.R
import com.safecircle.app.SafeCircleApp
import com.safecircle.app.auth.TokenStore
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.RegisterFcmTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!TokenStore(this).isLoggedIn()) return // 로그인 전에는 서버에 보낼 사용자 컨텍스트가 없음
        CoroutineScope(Dispatchers.IO).launch {
            runCatching { ApiClient.get(applicationContext).service.registerFcmToken(RegisterFcmTokenRequest(token)) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            android.content.Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this, SafeCircleApp.MONITORING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }
}

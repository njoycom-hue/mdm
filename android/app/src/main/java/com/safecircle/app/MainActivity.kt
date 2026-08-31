package com.safecircle.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.safecircle.app.auth.AuthActivity
import com.safecircle.app.auth.TokenStore
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.RegisterFcmTokenRequest
import com.safecircle.app.onboarding.ConsentActivity
import com.safecircle.app.onboarding.ConsentStore
import com.safecircle.app.pairing.PairingActivity
import com.safecircle.app.permissions.PermissionSetupActivity
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.SettingsSyncWorker
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tokenStore = TokenStore(this)
        if (!tokenStore.isLoggedIn()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        val role = tokenStore.role()
        if (role == "WARD" && !ConsentStore(this).hasConsented()) {
            startActivity(Intent(this, ConsentActivity::class.java))
            finish()
            return
        }

        if (role == "WARD") {
            EventQueue.schedulePeriodicUpload(this)
            SettingsSyncWorker.schedulePeriodicSync(this)
        }
        registerFcmTokenIfNeeded()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen(
                        role = role ?: "WARD",
                        onOpenPermissionSetup = { startActivity(Intent(this, PermissionSetupActivity::class.java)) },
                        onOpenPairing = { startActivity(Intent(this, PairingActivity::class.java)) }
                    )
                }
            }
        }
    }

    private fun registerFcmTokenIfNeeded() {
        lifecycleScope.launch {
            runCatching { FirebaseMessaging.getInstance().token.await() }
                .onSuccess { token ->
                    runCatching { ApiClient.get(this@MainActivity).service.registerFcmToken(RegisterFcmTokenRequest(token)) }
                }
        }
    }
}

@Composable
private fun HomeScreen(role: String, onOpenPermissionSetup: () -> Unit, onOpenPairing: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SafeCircle")
        if (role == "WARD") {
            Text("이 기기는 보호자와 연결되어 회복을 지원받고 있습니다.")
            Button(onClick = onOpenPermissionSetup) { Text("권한 설정 점검") }
            Button(onClick = onOpenPairing) { Text("보호자 연결 코드 보기") }
        } else {
            Text("연결된 피보호자의 상태 대시보드는 추후 구현 예정입니다.")
            Button(onClick = onOpenPairing) { Text("피보호자 연결하기") }
        }
    }
}

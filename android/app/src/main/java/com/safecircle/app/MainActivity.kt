package com.safecircle.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
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
import com.safecircle.app.guardian.GuardianDashboardActivity
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.RegisterFcmTokenRequest
import com.safecircle.app.onboarding.ConsentActivity
import com.safecircle.app.onboarding.ConsentStore
import com.safecircle.app.pairing.PairingActivity
import com.safecircle.app.permissions.PermissionSetupActivity
import com.safecircle.app.sync.EventQueue
import com.safecircle.app.sync.SettingsSyncWorker
import com.safecircle.app.ui.components.PrimaryButton
import com.safecircle.app.ui.components.ScreenColumn
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SecondaryButton
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
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
            SafeCircleTheme {
                Surface {
                    HomeScreen(
                        role = role ?: "WARD",
                        onOpenPermissionSetup = { startActivity(Intent(this, PermissionSetupActivity::class.java)) },
                        onOpenPairing = { startActivity(Intent(this, PairingActivity::class.java)) },
                        onOpenDashboard = { startActivity(Intent(this, GuardianDashboardActivity::class.java)) }
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
private fun HomeScreen(
    role: String,
    onOpenPermissionSetup: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenDashboard: () -> Unit
) {
    ScreenScaffold(title = "SafeCircle") { padding ->
        ScreenColumn(modifier = Modifier.padding(padding), spacing = Spacing.lg) {
            SectionCard(title = if (role == "WARD") "함께하고 있어요" else "보호자 홈") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(
                        imageVector = if (role == "WARD") Icons.Filled.Shield else Icons.Filled.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = if (role == "WARD") {
                            "이 기기는 보호자와 연결되어 회복을 지원받고 있습니다."
                        } else {
                            "연결된 피보호자의 상태를 확인하고 정책을 관리하세요."
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (role == "WARD") {
                PrimaryButton("권한 설정 점검", onOpenPermissionSetup)
                SecondaryButton("보호자 연결 코드 보기", onOpenPairing)
            } else {
                PrimaryButton("대시보드 열기", onOpenDashboard)
                SecondaryButton("피보호자 추가 연결하기", onOpenPairing)
            }
        }
    }
}

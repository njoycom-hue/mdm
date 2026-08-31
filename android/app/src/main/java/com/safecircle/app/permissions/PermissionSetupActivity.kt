package com.safecircle.app.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safecircle.app.admin.AppDeviceAdminReceiver
import com.safecircle.app.vpn.DomainFilterVpnService

/**
 * 사용시간 통계/차단/VPN 필터링/알림 감지가 동작하려면 사용자가 시스템 설정에서
 * 각 권한을 직접 승인해야 한다. Play 정책상 앱이 자동으로 켤 수 없다.
 */
class PermissionSetupActivity : ComponentActivity() {

    private val vpnPrepareLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            startService(Intent(this, DomainFilterVpnService::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionSetupScreen(
                        onOpenUsageAccess = { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                        onOpenAccessibility = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        onOpenDeviceAdmin = ::requestDeviceAdmin,
                        onOpenNotificationAccess = { startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")) },
                        onEnableVpn = ::requestVpnPermission
                    )
                }
            }
        }
    }

    private fun requestDeviceAdmin() {
        val componentName = ComponentName(this, AppDeviceAdminReceiver::class.java)
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "SafeCircle이 관리자 권한 해제를 방지하려면 필요합니다.")
        startActivity(intent)
    }

    private fun requestVpnPermission() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            startService(Intent(this, DomainFilterVpnService::class.java))
        }
    }
}

@Composable
private fun PermissionSetupScreen(
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onEnableVpn: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("아래 4가지를 모두 켜야 SafeCircle이 정상 동작합니다.")

        Text("1. 사용정보 접근 — 앱별 사용시간 확인")
        Button(onClick = onOpenUsageAccess) { Text("사용정보 접근 설정 열기") }

        Text("2. 접근성 서비스 — 앱 차단, 키워드 감지")
        Button(onClick = onOpenAccessibility) { Text("접근성 설정 열기") }

        Text("3. 기기 관리자 — 무단 해제 방지")
        Button(onClick = onOpenDeviceAdmin) { Text("기기 관리자 활성화") }

        Text("4. 알림 접근 — 문자/은행 알림 키워드 감지")
        Button(onClick = onOpenNotificationAccess) { Text("알림 접근 설정 열기") }

        Text("5. VPN — 유해 사이트 차단")
        Button(onClick = onEnableVpn) { Text("VPN 필터 켜기") }
    }
}

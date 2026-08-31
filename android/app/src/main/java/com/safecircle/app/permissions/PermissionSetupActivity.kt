package com.safecircle.app.permissions

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.safecircle.app.admin.AppDeviceAdminReceiver
import com.safecircle.app.ui.components.ScreenColumn
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SecondaryButton
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import com.safecircle.app.vpn.DomainFilterVpnService

/**
 * 사용시간 통계/차단/VPN 필터링/알림 감지가 동작하려면 사용자가 시스템 설정에서
 * 각 권한을 직접 승인해야 한다. Play 정책상 앱이 자동으로 켤 수 없다.
 *
 * 각 항목의 실제 승인 여부는 앱이 포그라운드로 돌아올 때마다(onResume) 시스템에서
 * 다시 조회해 화면에 반영한다 — 설정 화면에서 뭘 눌렀는지 사용자가 직접 판단할
 * 필요 없이, 이 화면으로 돌아오기만 하면 체크 표시로 바로 확인 가능하다.
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
            SafeCircleTheme {
                Surface {
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

private data class PermissionStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val actionLabel: String,
    val granted: Boolean,
    val onClick: () -> Unit
)

/** 화면이 포그라운드로 돌아올 때(onResume)마다 증가하는 값. 이 값이 바뀔 때 권한 상태를 다시 읽는다. */
@Composable
private fun rememberResumeSignal(): Int {
    var signal by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) signal++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return signal
}

@Composable
private fun PermissionSetupScreen(
    onOpenUsageAccess: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenDeviceAdmin: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onEnableVpn: () -> Unit
) {
    val context = LocalContext.current
    val resumeSignal = rememberResumeSignal()
    val statuses = remember(resumeSignal) { loadPermissionStatuses(context) }

    val steps = listOf(
        PermissionStep(Icons.Filled.BarChart, "사용정보 접근", "앱별 사용시간을 확인하는 데 필요합니다", "설정 열기", statuses.usageAccess, onOpenUsageAccess),
        PermissionStep(Icons.Filled.Accessibility, "접근성 서비스", "앱 차단과 키워드 감지에 필요합니다", "설정 열기", statuses.accessibility, onOpenAccessibility),
        PermissionStep(Icons.Filled.AdminPanelSettings, "기기 관리자", "무단으로 앱을 해제하지 못하도록 막습니다", "활성화", statuses.deviceAdmin, onOpenDeviceAdmin),
        PermissionStep(Icons.Filled.Notifications, "알림 접근", "문자·은행 알림의 키워드를 감지합니다", "설정 열기", statuses.notificationAccess, onOpenNotificationAccess),
        PermissionStep(Icons.Filled.VpnLock, "VPN 필터", "유해 사이트 접속을 차단합니다", "필터 켜기", statuses.vpn, onEnableVpn),
    )
    val doneCount = steps.count { it.granted }

    ScreenScaffold(title = "권한 설정") { padding ->
        ScreenColumn(modifier = Modifier.padding(padding), spacing = Spacing.md) {
            Text(
                "${doneCount}/${steps.size} 완료 — 설정 화면에서 켠 뒤 이 화면으로 돌아오면 자동으로 확인됩니다.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SectionCard {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            "설정 화면에서 목록 중 \"SafeCircle\"을 찾아 눌러 켜주세요.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "토글이 안 눌리면: 설정 > 앱 > SafeCircle > 우측 상단 점 3개(⋮) > " +
                                "\"제한된 설정 허용\"을 먼저 탭한 뒤 다시 시도하세요 (스토어 밖에서 설치한 앱은 " +
                                "안드로이드가 접근성·알림 권한을 기본적으로 막아둡니다).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            steps.forEachIndexed { index, step ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        StepBadge(index + 1, step.granted)
                        Icon(
                            imageVector = step.icon,
                            contentDescription = null,
                            tint = if (step.granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(step.title, style = MaterialTheme.typography.titleMedium)
                            Text(step.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (step.granted) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text("완료됨", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        SecondaryButton(step.actionLabel, step.onClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBadge(number: Int, granted: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (granted) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (granted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

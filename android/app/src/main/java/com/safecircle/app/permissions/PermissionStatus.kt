package com.safecircle.app.permissions

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.NotificationManagerCompat
import com.safecircle.app.accessibility.MonitorAccessibilityService
import com.safecircle.app.admin.AppDeviceAdminReceiver

/** 각 권한이 실제로 켜져 있는지 시스템에서 직접 조회한다 (사용자가 설정 화면에 다녀온 뒤 재확인용). */
data class PermissionStatuses(
    val usageAccess: Boolean,
    val accessibility: Boolean,
    val deviceAdmin: Boolean,
    val notificationAccess: Boolean,
    val vpn: Boolean
)

fun loadPermissionStatuses(context: Context): PermissionStatuses = PermissionStatuses(
    usageAccess = isUsageAccessGranted(context),
    accessibility = isAccessibilityServiceEnabled(context),
    deviceAdmin = isDeviceAdminActive(context),
    notificationAccess = isNotificationListenerEnabled(context),
    vpn = isVpnPrepared(context)
)

@Suppress("DEPRECATION")
private fun isUsageAccessGranted(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        android.os.Process.myUid(),
        context.packageName
    )
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = ComponentName(context, MonitorAccessibilityService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}

private fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isAdminActive(ComponentName(context, AppDeviceAdminReceiver::class.java))
}

private fun isNotificationListenerEnabled(context: Context): Boolean =
    context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

private fun isVpnPrepared(context: Context): Boolean = VpnService.prepare(context) == null

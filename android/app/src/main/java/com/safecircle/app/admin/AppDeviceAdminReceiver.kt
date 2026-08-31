package com.safecircle.app.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import com.safecircle.app.sync.ActivityReporter

/** 관리자 권한 해제 방지 및 정책 적용 진입점. */
class AppDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // 실제 해제(=삭제 전 단계)가 확정되기 전, 사용자가 시도한 시점에 바로 보호자에게 알린다.
        ActivityReporter.report(context, "DEVICE_ADMIN_DISABLE_REQUESTED")
        return "모니터링을 해제하면 보호자에게 알림이 전송됩니다. 계속하시겠습니까?"
    }
}

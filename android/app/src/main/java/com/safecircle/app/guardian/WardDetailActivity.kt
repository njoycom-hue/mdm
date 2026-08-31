package com.safecircle.app.guardian

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.ActivityAlertSummary
import com.safecircle.app.network.dto.AppTimeLimitSummary
import com.safecircle.app.network.dto.AppUsageSummary
import com.safecircle.app.network.dto.KeywordAlertSummary
import com.safecircle.app.network.dto.PROFILE_ADDICT
import com.safecircle.app.network.dto.PROFILE_CHILD
import com.safecircle.app.network.dto.PROFILE_ELDERLY
import com.safecircle.app.network.dto.WardSettingsRequest
import com.safecircle.app.network.dto.WardSettingsResponse
import com.safecircle.app.ui.components.EmptyStateText
import com.safecircle.app.ui.components.PrimaryButton
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SecondaryButton
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 특정 피보호자의 프로필(아동/중독자/부모님)에 맞춰 감시 정책을 편집한다.
 * - 아동: 유해 사이트 차단 + 앱별 하루 사용시간 제한
 * - 중독자: 특정 앱 차단 + 실행 시 알림만 받을 앱(신규 설치는 자동 알림)
 * - 부모님: 위험(보이스피싱 등) 키워드 감지 + 장시간 무활동 자동 알림
 */
class WardDetailActivity : ComponentActivity() {

    private lateinit var wardId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wardId = intent.getStringExtra(EXTRA_WARD_ID) ?: run { finish(); return }
        val wardEmail = intent.getStringExtra(EXTRA_WARD_EMAIL) ?: wardId

        setContent {
            SafeCircleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WardDetailScreen(
                        wardEmail = wardEmail,
                        loadUsage = ::loadUsage,
                        loadAlerts = ::loadAlerts,
                        loadActivityAlerts = ::loadActivityAlerts,
                        loadSettings = ::loadSettings,
                        loadTimeLimits = ::loadTimeLimits,
                        onSave = ::save
                    )
                }
            }
        }
    }

    private fun loadUsage(onResult: (List<AppUsageSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardUsage(wardId) }.onSuccess(onResult)
        }
    }

    private fun loadAlerts(onResult: (List<KeywordAlertSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardAlerts(wardId) }.onSuccess(onResult)
        }
    }

    private fun loadActivityAlerts(onResult: (List<ActivityAlertSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardActivityAlerts(wardId) }.onSuccess(onResult)
        }
    }

    private fun loadSettings(onResult: (WardSettingsResponse) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardSettings(wardId) }.onSuccess(onResult)
        }
    }

    private fun loadTimeLimits(onResult: (List<AppTimeLimitSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardTimeLimits(wardId) }.onSuccess(onResult)
        }
    }

    private fun save(
        profileType: String,
        keywords: List<String>,
        blockedPackages: List<String>,
        blockedDomains: List<String>,
        watchedPackages: List<String>,
        timeLimits: List<AppTimeLimitSummary>,
        onDone: () -> Unit
    ) {
        lifecycleScope.launch {
            runCatching {
                val service = ApiClient.get(this@WardDetailActivity).service
                service.updateWardSettings(
                    wardId,
                    WardSettingsRequest(profileType, keywords, blockedPackages, blockedDomains, watchedPackages)
                )
                service.updateWardTimeLimits(wardId, timeLimits)
            }
                .onSuccess { Toast.makeText(this@WardDetailActivity, "정책이 저장되었습니다", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this@WardDetailActivity, it.message, Toast.LENGTH_SHORT).show() }
            onDone()
        }
    }

    companion object {
        const val EXTRA_WARD_ID = "extra_ward_id"
        const val EXTRA_WARD_EMAIL = "extra_ward_email"
    }
}

private val PHISHING_KEYWORDS = listOf("검찰청", "금융감독원", "계좌이체", "안전계좌", "압류", "개인정보 확인", "지급정지")
private const val DEFAULT_MANUAL_TIME_LIMIT_MINUTES = 30

@Composable
private fun WardDetailScreen(
    wardEmail: String,
    loadUsage: ((List<AppUsageSummary>) -> Unit) -> Unit,
    loadAlerts: ((List<KeywordAlertSummary>) -> Unit) -> Unit,
    loadActivityAlerts: ((List<ActivityAlertSummary>) -> Unit) -> Unit,
    loadSettings: ((WardSettingsResponse) -> Unit) -> Unit,
    loadTimeLimits: ((List<AppTimeLimitSummary>) -> Unit) -> Unit,
    onSave: (
        profileType: String,
        keywords: List<String>,
        blockedPackages: List<String>,
        blockedDomains: List<String>,
        watchedPackages: List<String>,
        timeLimits: List<AppTimeLimitSummary>,
        onDone: () -> Unit
    ) -> Unit
) {
    var profileType by remember { mutableStateOf(PROFILE_ADDICT) }
    var usage by remember { mutableStateOf<List<AppUsageSummary>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<KeywordAlertSummary>>(emptyList()) }
    var activityAlerts by remember { mutableStateOf<List<ActivityAlertSummary>>(emptyList()) }
    var keywordsInput by remember { mutableStateOf("") }
    val blockedPackages = remember { mutableListOf<String>().toMutableStateList() }
    val watchedPackages = remember { mutableListOf<String>().toMutableStateList() }
    val timeLimits = remember { mutableStateMapOf<String, Int>() }
    var manualPackageInput by remember { mutableStateOf("") }
    var manualWatchedPackageInput by remember { mutableStateOf("") }
    var manualTimeLimitPackageInput by remember { mutableStateOf("") }
    var blockedDomainsInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadUsage { usage = it }
        loadAlerts { alerts = it }
        loadActivityAlerts { activityAlerts = it }
        loadSettings {
            profileType = it.profileType
            keywordsInput = it.keywords.joinToString(", ")
            blockedPackages.clear()
            blockedPackages.addAll(it.blockedPackages)
            watchedPackages.clear()
            watchedPackages.addAll(it.watchedPackages)
            blockedDomainsInput = it.blockedDomains.joinToString(", ")
        }
        loadTimeLimits { limits ->
            timeLimits.clear()
            limits.forEach { timeLimits[it.packageName] = it.dailyLimitMinutes }
        }
    }

    // 최근 사용 목록에 없어도 이미 차단/감시/시간제한 설정된 패키지는 선택지에서 빠지지 않도록
    // 사용시간 목록과 기존 설정을 합쳐 하나의 선택 가능한 앱 목록을 만든다.
    val pickableApps = remember(usage, blockedPackages.toList(), watchedPackages.toList(), timeLimits.toMap()) {
        val fromUsage = usage.map { AppUsageSummary(it.packageName, it.appLabel, it.totalForegroundMillis) }
        val extraPackages = (blockedPackages + watchedPackages + timeLimits.keys)
            .distinct()
            .filter { pkg -> fromUsage.none { it.packageName == pkg } }
            .map { AppUsageSummary(it, it, 0L) }
        (fromUsage + extraPackages).sortedByDescending { it.totalForegroundMillis }
    }

    ScreenScaffold(title = wardEmail) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionCard(title = "프로필") {
                Text(
                    "피보호자 상황에 맞는 프로필을 고르면 그에 맞는 기능만 아래에 표시됩니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(selected = profileType == PROFILE_CHILD, onClick = { profileType = PROFILE_CHILD }, label = { Text("아동") })
                    FilterChip(selected = profileType == PROFILE_ADDICT, onClick = { profileType = PROFILE_ADDICT }, label = { Text("중독 회복") })
                    FilterChip(selected = profileType == PROFILE_ELDERLY, onClick = { profileType = PROFILE_ELDERLY }, label = { Text("부모님") })
                }
            }

            SectionCard(title = "최근 24시간 사용시간") {
                if (usage.isEmpty()) {
                    EmptyStateText("데이터가 아직 없습니다.")
                } else {
                    val maxMillis = usage.maxOf { it.totalForegroundMillis }.coerceAtLeast(1)
                    usage.forEach { UsageRow(it, maxMillis) }
                }
            }

            if (profileType == PROFILE_CHILD) {
                SectionCard(title = "유해 사이트 차단") {
                    Text(
                        "쉼표로 구분해 여러 도메인을 입력할 수 있습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = blockedDomainsInput,
                        onValueChange = { blockedDomainsInput = it },
                        label = { Text("차단 도메인") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SectionCard(title = "앱별 사용시간 제한") {
                    Text(
                        "하루 사용시간(분)을 입력하면 초과 시 해당 앱을 홈으로 튕겨냅니다. 0 또는 빈 값은 제한 없음입니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pickableApps.isEmpty()) {
                        EmptyStateText(
                            "아직 피보호자 기기의 사용시간 데이터가 도착하지 않았습니다(최초 동기화까지 " +
                                "몇 분 걸릴 수 있어요). 기다리지 않고 지금 바로 설정하려면 아래에 패키지명을 " +
                                "직접 입력해 추가하세요."
                        )
                    } else {
                        pickableApps.forEach { app ->
                            TimeLimitRow(
                                app = app,
                                minutes = timeLimits[app.packageName],
                                onMinutesChange = { minutes ->
                                    if (minutes == null || minutes <= 0) timeLimits.remove(app.packageName)
                                    else timeLimits[app.packageName] = minutes
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = manualTimeLimitPackageInput,
                        onValueChange = { manualTimeLimitPackageInput = it },
                        label = { Text("목록에 없는 앱 (패키지명)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton("30분 제한으로 추가", onClick = {
                        val pkg = manualTimeLimitPackageInput.trim()
                        if (pkg.isNotEmpty()) timeLimits[pkg] = DEFAULT_MANUAL_TIME_LIMIT_MINUTES
                        manualTimeLimitPackageInput = ""
                    })
                }
            }

            if (profileType == PROFILE_ADDICT) {
                SectionCard(title = "차단할 앱 선택") {
                    if (pickableApps.isEmpty()) {
                        EmptyStateText(
                            "아직 피보호자 기기의 사용시간 데이터가 도착하지 않았습니다(최초 동기화까지 " +
                                "몇 분 걸릴 수 있어요). 기다리지 않고 지금 바로 차단하려면 아래에 패키지명을 " +
                                "직접 입력해 추가하세요."
                        )
                    } else {
                        Text(
                            "체크한 앱은 실행 즉시 홈으로 이동시켜 완전히 막습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        pickableApps.forEach { app ->
                            AppPickRow(
                                app = app,
                                checked = blockedPackages.contains(app.packageName),
                                onCheckedChange = { checked ->
                                    if (checked) { if (!blockedPackages.contains(app.packageName)) blockedPackages.add(app.packageName) }
                                    else blockedPackages.remove(app.packageName)
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = manualPackageInput,
                        onValueChange = { manualPackageInput = it },
                        label = { Text("목록에 없는 앱 (패키지명)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton("추가", onClick = {
                        val pkg = manualPackageInput.trim()
                        if (pkg.isNotEmpty() && !blockedPackages.contains(pkg)) blockedPackages.add(pkg)
                        manualPackageInput = ""
                    })
                }

                SectionCard(title = "실행 시 알림만 받을 앱") {
                    Text(
                        "막지는 않되, 피보호자가 이 앱을 실행하면 그때마다 보호자에게 바로 알립니다. " +
                            "새 앱이 설치될 때도 자동으로 알림이 갑니다(별도 설정 불필요).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pickableApps.isEmpty()) {
                        EmptyStateText(
                            "아직 피보호자 기기의 사용시간 데이터가 도착하지 않았습니다. 아래에 패키지명을 " +
                                "직접 입력해 지금 바로 추가할 수 있습니다."
                        )
                    } else {
                        pickableApps.forEach { app ->
                            AppPickRow(
                                app = app,
                                checked = watchedPackages.contains(app.packageName),
                                onCheckedChange = { checked ->
                                    if (checked) { if (!watchedPackages.contains(app.packageName)) watchedPackages.add(app.packageName) }
                                    else watchedPackages.remove(app.packageName)
                                }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = manualWatchedPackageInput,
                        onValueChange = { manualWatchedPackageInput = it },
                        label = { Text("목록에 없는 앱 (패키지명)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryButton("추가", onClick = {
                        val pkg = manualWatchedPackageInput.trim()
                        if (pkg.isNotEmpty() && !watchedPackages.contains(pkg)) watchedPackages.add(pkg)
                        manualWatchedPackageInput = ""
                    })
                }
            }

            SectionCard(title = if (profileType == PROFILE_ELDERLY) "위험 키워드 감지" else "감시 키워드") {
                if (profileType == PROFILE_ELDERLY) {
                    Text(
                        "보이스피싱에서 자주 쓰이는 표현이 화면에 감지되면 즉시 알립니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SecondaryButton("보이스피싱 의심 키워드 빠르게 추가", onClick = {
                        val current = splitCsv(keywordsInput)
                        val merged = (current + PHISHING_KEYWORDS).distinct()
                        keywordsInput = merged.joinToString(", ")
                    })
                    Text(
                        "12시간 이상 기기 사용이 없으면 무활동으로 보고 자동으로 알림이 갑니다(별도 설정 불필요).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "쉼표로 구분해 여러 개를 입력할 수 있습니다. 화면에서 매치되면 알림이 갑니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = keywordsInput,
                    onValueChange = { keywordsInput = it },
                    label = { Text("키워드") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionCard(title = "최근 키워드 알림") {
                if (alerts.isEmpty()) {
                    EmptyStateText("감지된 알림이 없습니다.")
                } else {
                    alerts.forEach { AlertRow(it) }
                }
            }

            SectionCard(title = "이탈/활동 알림") {
                if (activityAlerts.isEmpty()) {
                    EmptyStateText("감지된 활동이 없습니다.")
                } else {
                    activityAlerts.forEach { ActivityAlertRow(it) }
                }
            }

            if (isSaving) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = Spacing.sm))
                }
            } else {
                PrimaryButton("정책 저장", onClick = {
                    isSaving = true
                    val timeLimitPayload = timeLimits.map { (pkg, minutes) ->
                        val label = pickableApps.firstOrNull { it.packageName == pkg }?.appLabel ?: pkg
                        AppTimeLimitSummary(pkg, label, minutes)
                    }
                    onSave(
                        profileType,
                        splitCsv(keywordsInput),
                        blockedPackages.toList(),
                        splitCsv(blockedDomainsInput),
                        watchedPackages.toList(),
                        timeLimitPayload
                    ) { isSaving = false }
                })
            }
        }
    }
}

@Composable
private fun UsageRow(usage: AppUsageSummary, maxMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(usage.appLabel.ifBlank { usage.packageName }, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatDuration(usage.totalForegroundMillis),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        val fraction = (usage.totalForegroundMillis.toFloat() / maxMillis.toFloat()).coerceIn(0.02f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AppPickRow(app: AppUsageSummary, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appLabel.ifBlank { app.packageName }, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (app.totalForegroundMillis > 0) "${app.packageName} · ${formatDuration(app.totalForegroundMillis)}" else app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeLimitRow(app: AppUsageSummary, minutes: Int?, onMinutesChange: (Int?) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(app.appLabel.ifBlank { app.packageName }, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (app.totalForegroundMillis > 0) "오늘 ${formatDuration(app.totalForegroundMillis)} 사용" else app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedTextField(
            value = minutes?.toString() ?: "",
            onValueChange = { onMinutesChange(it.toIntOrNull()) },
            label = { Text("분") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp)
        )
    }
}

@Composable
private fun AlertRow(alert: KeywordAlertSummary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                alert.matchedKeywords.joinToString(", "),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${alert.sourceApp} · ${formatTimestamp(alert.occurredAtEpochMs)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivityAlertRow(alert: ActivityAlertSummary) {
    val (icon, label) = when (alert.type) {
        "DEVICE_ADMIN_DISABLE_REQUESTED" -> Icons.Filled.AdminPanelSettings to "관리자 권한 해제(삭제 전 단계) 시도"
        "WATCHED_APP_LAUNCHED" -> Icons.Filled.PlayCircle to "감시 대상 앱 실행: ${alert.detail}"
        "APP_INSTALLED" -> Icons.Filled.NewReleases to "신규 앱 설치: ${alert.detail}"
        "INACTIVITY_DETECTED" -> Icons.Filled.Bedtime to "장시간 무활동 감지"
        else -> Icons.Filled.NotificationsActive to alert.type
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatTimestamp(alert.occurredAtEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun splitCsv(input: String): List<String> =
    input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

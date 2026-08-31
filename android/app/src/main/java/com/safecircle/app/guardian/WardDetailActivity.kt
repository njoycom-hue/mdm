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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.AppUsageSummary
import com.safecircle.app.network.dto.KeywordAlertSummary
import com.safecircle.app.network.dto.WardSettingsRequest
import com.safecircle.app.network.dto.WardSettingsResponse
import com.safecircle.app.ui.components.EmptyStateText
import com.safecircle.app.ui.components.PrimaryButton
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/** 특정 피보호자의 최근 사용시간/키워드 알림을 보고, 감시 정책(키워드/차단앱/차단도메인)을 편집한다. */
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
                        loadSettings = ::loadSettings,
                        onSaveSettings = ::saveSettings
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

    private fun loadSettings(onResult: (WardSettingsResponse) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@WardDetailActivity).service.wardSettings(wardId) }.onSuccess(onResult)
        }
    }

    private fun saveSettings(
        keywords: List<String>,
        blockedPackages: List<String>,
        blockedDomains: List<String>,
        onDone: () -> Unit
    ) {
        lifecycleScope.launch {
            runCatching {
                ApiClient.get(this@WardDetailActivity).service.updateWardSettings(
                    wardId,
                    WardSettingsRequest(keywords, blockedPackages, blockedDomains)
                )
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

@Composable
private fun WardDetailScreen(
    wardEmail: String,
    loadUsage: ((List<AppUsageSummary>) -> Unit) -> Unit,
    loadAlerts: ((List<KeywordAlertSummary>) -> Unit) -> Unit,
    loadSettings: ((WardSettingsResponse) -> Unit) -> Unit,
    onSaveSettings: (keywords: List<String>, blockedPackages: List<String>, blockedDomains: List<String>, onDone: () -> Unit) -> Unit
) {
    var usage by remember { mutableStateOf<List<AppUsageSummary>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<KeywordAlertSummary>>(emptyList()) }
    var keywordsInput by remember { mutableStateOf("") }
    var blockedPackagesInput by remember { mutableStateOf("") }
    var blockedDomainsInput by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loadUsage { usage = it }
        loadAlerts { alerts = it }
        loadSettings {
            keywordsInput = it.keywords.joinToString(", ")
            blockedPackagesInput = it.blockedPackages.joinToString(", ")
            blockedDomainsInput = it.blockedDomains.joinToString(", ")
        }
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
            SectionCard(title = "최근 24시간 사용시간") {
                if (usage.isEmpty()) {
                    EmptyStateText("데이터가 아직 없습니다.")
                } else {
                    val maxMillis = usage.maxOf { it.totalForegroundMillis }.coerceAtLeast(1)
                    usage.forEach { UsageRow(it, maxMillis) }
                }
            }

            SectionCard(title = "최근 키워드 알림") {
                if (alerts.isEmpty()) {
                    EmptyStateText("감지된 알림이 없습니다.")
                } else {
                    alerts.forEach { AlertRow(it) }
                }
            }

            SectionCard(title = "감시 정책 편집") {
                Text(
                    "쉼표로 구분해 여러 개를 입력할 수 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = keywordsInput,
                    onValueChange = { keywordsInput = it },
                    label = { Text("키워드") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = blockedPackagesInput,
                    onValueChange = { blockedPackagesInput = it },
                    label = { Text("차단 앱 패키지명") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = blockedDomainsInput,
                    onValueChange = { blockedDomainsInput = it },
                    label = { Text("차단 도메인") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (isSaving) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.padding(vertical = Spacing.sm))
                    }
                } else {
                    PrimaryButton("정책 저장", onClick = {
                        isSaving = true
                        onSaveSettings(splitCsv(keywordsInput), splitCsv(blockedPackagesInput), splitCsv(blockedDomainsInput)) {
                            isSaving = false
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun UsageRow(usage: AppUsageSummary, maxMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(usage.packageName, style = MaterialTheme.typography.bodyMedium)
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

private fun splitCsv(input: String): List<String> =
    input.split(",").map { it.trim() }.filter { it.isNotEmpty() }

private fun formatDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
}

private fun formatTimestamp(epochMs: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(epochMs))

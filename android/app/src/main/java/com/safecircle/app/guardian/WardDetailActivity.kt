package com.safecircle.app.guardian

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.AppUsageSummary
import com.safecircle.app.network.dto.KeywordAlertSummary
import com.safecircle.app.network.dto.WardSettingsRequest
import com.safecircle.app.network.dto.WardSettingsResponse
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
            MaterialTheme {
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

    private fun saveSettings(keywords: List<String>, blockedPackages: List<String>, blockedDomains: List<String>) {
        lifecycleScope.launch {
            runCatching {
                ApiClient.get(this@WardDetailActivity).service.updateWardSettings(
                    wardId,
                    WardSettingsRequest(keywords, blockedPackages, blockedDomains)
                )
            }
                .onSuccess { Toast.makeText(this@WardDetailActivity, "정책이 저장되었습니다", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(this@WardDetailActivity, it.message, Toast.LENGTH_SHORT).show() }
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
    onSaveSettings: (keywords: List<String>, blockedPackages: List<String>, blockedDomains: List<String>) -> Unit
) {
    var usage by remember { mutableStateOf<List<AppUsageSummary>>(emptyList()) }
    var alerts by remember { mutableStateOf<List<KeywordAlertSummary>>(emptyList()) }
    var keywordsInput by remember { mutableStateOf("") }
    var blockedPackagesInput by remember { mutableStateOf("") }
    var blockedDomainsInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        loadUsage { usage = it }
        loadAlerts { alerts = it }
        loadSettings {
            keywordsInput = it.keywords.joinToString(", ")
            blockedPackagesInput = it.blockedPackages.joinToString(", ")
            blockedDomainsInput = it.blockedDomains.joinToString(", ")
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(wardEmail)

        Text("최근 24시간 사용시간")
        if (usage.isEmpty()) Text("데이터가 아직 없습니다.")
        usage.forEach { Text("${it.packageName} — ${formatDuration(it.totalForegroundMillis)}") }

        Text("최근 키워드 알림")
        if (alerts.isEmpty()) Text("감지된 알림이 없습니다.")
        alerts.forEach {
            Text("[${it.sourceApp}] ${it.matchedKeywords.joinToString(", ")} — ${formatTimestamp(it.occurredAtEpochMs)}")
        }

        Text("감시 정책 편집 (쉼표로 구분)")
        OutlinedTextField(value = keywordsInput, onValueChange = { keywordsInput = it }, label = { Text("키워드") })
        OutlinedTextField(value = blockedPackagesInput, onValueChange = { blockedPackagesInput = it }, label = { Text("차단 앱 패키지명") })
        OutlinedTextField(value = blockedDomainsInput, onValueChange = { blockedDomainsInput = it }, label = { Text("차단 도메인") })
        Button(onClick = {
            onSaveSettings(splitCsv(keywordsInput), splitCsv(blockedPackagesInput), splitCsv(blockedDomainsInput))
        }) { Text("정책 저장") }
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

package com.safecircle.app.usage

import android.os.Bundle
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.AppUsageSummary
import com.safecircle.app.ui.components.EmptyStateText
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 피보호자 본인이 보호자에게 보이는 것과 똑같은 최근 24시간 앱별 사용시간을 확인하는 화면.
 * 무엇이 감시되는지 스스로 확인할 수 있게 해 투명성을 지키려는 목적(docs/LEGAL.md 참고).
 */
class MyUsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeCircleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MyUsageScreen(loadUsage = ::loadUsage)
                }
            }
        }
    }

    private fun loadUsage(onResult: (List<AppUsageSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@MyUsageActivity).service.myUsage() }.onSuccess(onResult)
        }
    }
}

@Composable
private fun MyUsageScreen(loadUsage: ((List<AppUsageSummary>) -> Unit) -> Unit) {
    var usage by remember { mutableStateOf<List<AppUsageSummary>>(emptyList()) }
    LaunchedEffect(Unit) { loadUsage { usage = it } }

    ScreenScaffold(title = "내 사용시간") { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionCard(title = "최근 24시간") {
                Text(
                    "보호자에게도 똑같이 보이는 정보입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (usage.isEmpty()) {
                    EmptyStateText("데이터가 아직 없습니다.")
                } else {
                    val maxMillis = usage.maxOf { it.totalForegroundMillis }.coerceAtLeast(1)
                    usage.forEach { MyUsageRow(it, maxMillis) }
                }
            }
        }
    }
}

@Composable
private fun MyUsageRow(usage: AppUsageSummary, maxMillis: Long) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(usage.appLabel.ifBlank { usage.packageName }, style = MaterialTheme.typography.bodyMedium)
            Text(
                formatMyUsageDuration(usage.totalForegroundMillis),
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

private fun formatMyUsageDuration(millis: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    return if (minutes < 60) "${minutes}분" else "${minutes / 60}시간 ${minutes % 60}분"
}

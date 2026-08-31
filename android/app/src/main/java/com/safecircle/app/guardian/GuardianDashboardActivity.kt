package com.safecircle.app.guardian

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.safecircle.app.network.dto.WardSummary
import com.safecircle.app.pairing.PairingActivity
import com.safecircle.app.ui.components.CenteredLoading
import com.safecircle.app.ui.components.EmptyStateText
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SecondaryButton
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 보호자가 연결된 피보호자 목록을 보고, 각각의 상세(사용시간/알림/정책)로 이동하는 진입 화면. */
class GuardianDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeCircleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DashboardScreen(
                        loadWards = ::loadWards,
                        onOpenWard = { ward ->
                            startActivity(
                                Intent(this, WardDetailActivity::class.java)
                                    .putExtra(WardDetailActivity.EXTRA_WARD_ID, ward.wardId)
                                    .putExtra(WardDetailActivity.EXTRA_WARD_EMAIL, ward.email)
                            )
                        },
                        onAddWard = { startActivity(Intent(this, PairingActivity::class.java)) }
                    )
                }
            }
        }
    }

    private fun loadWards(onResult: (List<WardSummary>) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@GuardianDashboardActivity).service.myWards() }
                .onSuccess(onResult)
                .onFailure { Toast.makeText(this@GuardianDashboardActivity, it.message, Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
private fun DashboardScreen(
    loadWards: ((List<WardSummary>) -> Unit) -> Unit,
    onOpenWard: (WardSummary) -> Unit,
    onAddWard: () -> Unit
) {
    var wards by remember { mutableStateOf<List<WardSummary>?>(null) }
    LaunchedEffect(Unit) { loadWards { wards = it } }

    ScreenScaffold(title = "연결된 피보호자") { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            when {
                wards == null -> CenteredLoading(modifier = Modifier.fillMaxWidth().padding(top = Spacing.xl))
                wards!!.isEmpty() -> EmptyWardsState()
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(wards!!) { ward -> WardListItem(ward, onClick = { onOpenWard(ward) }) }
                }
            }
            SecondaryButton("피보호자 추가 연결", onAddWard)
        }
    }
}

@Composable
private fun EmptyWardsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Icon(
            imageVector = Icons.Filled.GroupOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(40.dp)
        )
        EmptyStateText("아직 연결된 피보호자가 없습니다.")
    }
}

@Composable
private fun WardListItem(ward: WardSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(ward.email, style = MaterialTheme.typography.titleMedium)
                Text(
                    "연결일 ${formatDate(ward.pairedAtEpochMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMs))

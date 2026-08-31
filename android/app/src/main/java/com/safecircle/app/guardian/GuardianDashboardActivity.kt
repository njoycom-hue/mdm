package com.safecircle.app.guardian

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.WardSummary
import com.safecircle.app.pairing.PairingActivity
import kotlinx.coroutines.launch

/** 보호자가 연결된 피보호자 목록을 보고, 각각의 상세(사용시간/알림/정책)로 이동하는 진입 화면. */
class GuardianDashboardActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
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
    var wards by remember { mutableStateOf<List<WardSummary>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { loadWards { wards = it } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("연결된 피보호자")
        if (wards.isEmpty()) {
            Text("아직 연결된 피보호자가 없습니다.")
        } else {
            LazyColumn {
                items(wards) { ward ->
                    Column(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenWard(ward) }.padding(vertical = 12.dp)
                    ) {
                        Text(ward.email)
                    }
                    Divider()
                }
            }
        }
        Button(onClick = onAddWard) { Text("피보호자 추가 연결") }
    }
}

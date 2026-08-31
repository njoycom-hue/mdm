package com.safecircle.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safecircle.app.onboarding.ConsentActivity
import com.safecircle.app.onboarding.ConsentStore

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ConsentStore(this).hasConsented()) {
            startActivity(Intent(this, ConsentActivity::class.java))
            finish()
            return
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HomeScreen()
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("SafeCircle")
        Text("설정 마법사와 보호자 대시보드는 추후 구현 예정입니다.")
        // TODO: 권한 상태 점검 화면 (Accessibility/Device Admin/VPN/Notification Listener 활성화 유도)
        Button(onClick = { /* TODO: 권한 안내 플로우로 이동 */ }) {
            Text("권한 설정 시작")
        }
    }
}

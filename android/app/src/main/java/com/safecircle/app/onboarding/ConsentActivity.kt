package com.safecircle.app.onboarding

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.BuildConfig
import com.safecircle.app.MainActivity
import com.safecircle.app.R
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.ConsentEventRequest
import kotlinx.coroutines.launch

/**
 * 피감독자 본인이 직접 승인해야 하는 동의 화면. 보호자가 대신 진행할 수 없다.
 * 통신비밀보호법 대응을 위한 필수 게이트 — docs/LEGAL.md 참고.
 */
class ConsentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConsentScreen(onAgree = ::onAgree)
                }
            }
        }
    }

    private fun onAgree() {
        ConsentStore(this).recordConsent()
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)

        lifecycleScope.launch {
            runCatching {
                ApiClient.get(this@ConsentActivity).service.postConsent(
                    ConsentEventRequest(deviceId = deviceId, appVersion = BuildConfig.VERSION_NAME)
                )
            }
            // 동의 기록 전송이 실패해도 로컬에는 이미 기록되었으니 온보딩은 계속 진행한다.
            // 다음 배치 업로드/재시도 시점에 서버 기록을 다시 시도할 수 있도록 별도 워커로 옮기는 게 이상적.
            startActivity(Intent(this@ConsentActivity, MainActivity::class.java))
            finish()
        }
    }
}

@Composable
private fun ConsentScreen(onAgree: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResourceCompat(R.string.consent_title))
        Text(stringResourceCompat(R.string.consent_body))
        Button(onClick = onAgree) {
            Text(stringResourceCompat(R.string.consent_agree))
        }
    }
}

@Composable
private fun stringResourceCompat(id: Int): String =
    androidx.compose.ui.platform.LocalContext.current.getString(id)

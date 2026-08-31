package com.safecircle.app.onboarding

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.safecircle.app.MainActivity
import com.safecircle.app.R

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
        // TODO: 백엔드에 동의 이벤트 기록 (POST /consents) — timestamp, deviceId, appVersion
        startActivity(Intent(this, MainActivity::class.java))
        finish()
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

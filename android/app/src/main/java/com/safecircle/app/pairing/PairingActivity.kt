package com.safecircle.app.pairing

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.safecircle.app.MainActivity
import com.safecircle.app.auth.TokenStore
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.ClaimPairingRequest
import kotlinx.coroutines.launch

/** WARD는 코드를 발급받아 보호자에게 전달하고, GUARDIAN은 그 코드를 입력해 연결한다. */
class PairingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = TokenStore(this).role()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (role == "GUARDIAN") {
                        GuardianClaimScreen(onClaim = ::claimCode)
                    } else {
                        WardIssueScreen(onIssue = ::issueCode)
                    }
                }
            }
        }
    }

    private fun issueCode(onResult: (String) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@PairingActivity).service.issuePairingCode() }
                .onSuccess { onResult(it.code) }
                .onFailure { Toast.makeText(this@PairingActivity, it.message, Toast.LENGTH_SHORT).show() }
        }
    }

    private fun claimCode(code: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@PairingActivity).service.claimPairingCode(ClaimPairingRequest(code)) }
                .onSuccess {
                    Toast.makeText(this@PairingActivity, "연결되었습니다", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@PairingActivity, MainActivity::class.java))
                    finish()
                }
                .onFailure { Toast.makeText(this@PairingActivity, "코드가 올바르지 않거나 만료되었습니다", Toast.LENGTH_SHORT).show() }
        }
    }
}

@Composable
private fun WardIssueScreen(onIssue: (onResult: (String) -> Unit) -> Unit) {
    var code by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.padding(24.dp)) {
        Text("보호자에게 아래 코드를 알려주세요 (10분간 유효)")
        Text(code ?: "코드 발급 중...")
        Button(onClick = { onIssue { code = it } }) { Text("코드 새로 받기") }
    }
}

@Composable
private fun GuardianClaimScreen(onClaim: (String) -> Unit) {
    var input by remember { mutableStateOf("") }
    Column(modifier = Modifier.padding(24.dp)) {
        Text("피보호자가 알려준 6자리 코드를 입력하세요")
        OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("코드") })
        Button(onClick = { onClaim(input) }) { Text("연결하기") }
    }
}

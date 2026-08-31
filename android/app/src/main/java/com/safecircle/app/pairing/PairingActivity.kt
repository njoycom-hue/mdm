package com.safecircle.app.pairing

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.MainActivity
import com.safecircle.app.auth.TokenStore
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.ClaimPairingRequest
import com.safecircle.app.ui.components.CenteredLoading
import com.safecircle.app.ui.components.PrimaryButton
import com.safecircle.app.ui.components.ScreenColumn
import com.safecircle.app.ui.components.ScreenScaffold
import com.safecircle.app.ui.components.SectionCard
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch

/** WARD는 코드를 발급받아 보호자에게 전달하고, GUARDIAN은 그 코드를 입력해 연결한다. */
class PairingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val role = TokenStore(this).role()

        setContent {
            SafeCircleTheme {
                Surface {
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

    private fun claimCode(code: String, onDone: (String?) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@PairingActivity).service.claimPairingCode(ClaimPairingRequest(code)) }
                .onSuccess {
                    onDone(null)
                    Toast.makeText(this@PairingActivity, "연결되었습니다", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@PairingActivity, MainActivity::class.java))
                    finish()
                }
                .onFailure { onDone("코드가 올바르지 않거나 만료되었습니다") }
        }
    }
}

@Composable
private fun WardIssueScreen(onIssue: (onResult: (String) -> Unit) -> Unit) {
    var code by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { onIssue { code = it } }

    ScreenScaffold(title = "보호자 연결") { padding ->
        ScreenColumn(modifier = Modifier.padding(padding), spacing = Spacing.lg) {
            Text(
                "보호자에게 아래 코드를 알려주세요",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            SectionCard {
                if (code == null) {
                    CenteredLoading(modifier = Modifier.fillMaxWidth())
                } else {
                    Text(
                        text = code!!.chunked(1).joinToString(" "),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "10분간 유효합니다",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            PrimaryButton("코드 새로 받기", onClick = { code = null; onIssue { code = it } })
        }
    }
}

@Composable
private fun GuardianClaimScreen(onClaim: (String, onDone: (String?) -> Unit) -> Unit) {
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ScreenScaffold(title = "피보호자 연결") { padding ->
        ScreenColumn(modifier = Modifier.padding(padding), spacing = Spacing.lg) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Text(
                "피보호자가 알려준 6자리 코드를 입력하세요",
                style = MaterialTheme.typography.bodyLarge
            )
            OutlinedTextField(
                value = input,
                onValueChange = { if (it.length <= 6) input = it.filter(Char::isDigit) },
                label = { Text("코드") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(vertical = Spacing.sm))
                } else {
                    PrimaryButton(
                        "연결하기",
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onClaim(input) { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        },
                        enabled = input.length == 6
                    )
                }
            }
        }
    }
}

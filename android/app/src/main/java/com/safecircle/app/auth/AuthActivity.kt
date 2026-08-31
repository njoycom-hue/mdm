package com.safecircle.app.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.LoginRequest
import com.safecircle.app.network.dto.RegisterRequest
import com.safecircle.app.onboarding.ConsentActivity
import com.safecircle.app.pairing.PairingActivity
import com.safecircle.app.ui.components.PrimaryButton
import com.safecircle.app.ui.components.Spacing
import com.safecircle.app.ui.theme.SafeCircleTheme
import kotlinx.coroutines.launch

/** 회원가입/로그인. 역할(GUARDIAN/WARD)은 가입 시 선택하며, 이후 온보딩 경로가 달라진다. */
class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SafeCircleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthScreen(
                        onLogin = { email, password, onDone -> login(email, password, onDone) },
                        onRegister = { email, password, role, onDone -> register(email, password, role, onDone) }
                    )
                }
            }
        }
    }

    private fun login(email: String, password: String, onDone: (String?) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@AuthActivity).service.login(LoginRequest(email, password)) }
                .onSuccess { auth ->
                    TokenStore(this@AuthActivity).saveSession(auth.token, auth.userId, auth.role)
                    onDone(null)
                    routeAfterAuth(auth.role)
                }
                .onFailure { onDone(it.message ?: "로그인에 실패했습니다") }
        }
    }

    private fun register(email: String, password: String, role: String, onDone: (String?) -> Unit) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@AuthActivity).service.register(RegisterRequest(email, password, role)) }
                .onSuccess { auth ->
                    TokenStore(this@AuthActivity).saveSession(auth.token, auth.userId, auth.role)
                    onDone(null)
                    routeAfterAuth(auth.role)
                }
                .onFailure { onDone(it.message ?: "가입에 실패했습니다") }
        }
    }

    private fun routeAfterAuth(role: String) {
        val next = if (role == "WARD") ConsentActivity::class.java else PairingActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthScreen(
    onLogin: (email: String, password: String, onDone: (String?) -> Unit) -> Unit,
    onRegister: (email: String, password: String, role: String, onDone: (String?) -> Unit) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("WARD") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun submit() {
        errorMessage = null
        isLoading = true
        val onDone: (String?) -> Unit = { error ->
            isLoading = false
            errorMessage = error
        }
        if (isRegisterMode) onRegister(email, password, role, onDone) else onLogin(email, password, onDone)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = "SafeCircle",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = if (isRegisterMode) "회원가입" else "로그인",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("이메일") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("비밀번호") },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "비밀번호 숨기기" else "비밀번호 보기"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isRegisterMode) {
            Text("역할 선택", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FilterChip(
                    selected = role == "WARD",
                    onClick = { role = "WARD" },
                    label = { Text("본인(회복 지원 대상)") }
                )
                FilterChip(
                    selected = role == "GUARDIAN",
                    onClick = { role = "GUARDIAN" },
                    label = { Text("보호자") }
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
            } else {
                PrimaryButton(
                    text = if (isRegisterMode) "가입하고 시작하기" else "로그인",
                    onClick = ::submit,
                    enabled = email.isNotBlank() && password.isNotBlank()
                )
            }
        }

        TextButton(
            onClick = { isRegisterMode = !isRegisterMode; errorMessage = null },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isRegisterMode) "이미 계정이 있어요" else "계정이 없어요, 가입할게요")
        }
    }
}

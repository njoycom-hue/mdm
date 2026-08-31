package com.safecircle.app.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.safecircle.app.network.ApiClient
import com.safecircle.app.network.dto.LoginRequest
import com.safecircle.app.network.dto.RegisterRequest
import com.safecircle.app.onboarding.ConsentActivity
import com.safecircle.app.pairing.PairingActivity
import kotlinx.coroutines.launch

/** 회원가입/로그인. 역할(GUARDIAN/WARD)은 가입 시 선택하며, 이후 온보딩 경로가 달라진다. */
class AuthActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxWidth()) {
                    AuthScreen(
                        onLogin = { email, password -> login(email, password) },
                        onRegister = { email, password, role -> register(email, password, role) }
                    )
                }
            }
        }
    }

    private fun login(email: String, password: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@AuthActivity).service.login(LoginRequest(email, password)) }
                .onSuccess { auth ->
                    TokenStore(this@AuthActivity).saveSession(auth.token, auth.userId, auth.role)
                    routeAfterAuth(auth.role)
                }
                .onFailure { showError(it.message) }
        }
    }

    private fun register(email: String, password: String, role: String) {
        lifecycleScope.launch {
            runCatching { ApiClient.get(this@AuthActivity).service.register(RegisterRequest(email, password, role)) }
                .onSuccess { auth ->
                    TokenStore(this@AuthActivity).saveSession(auth.token, auth.userId, auth.role)
                    routeAfterAuth(auth.role)
                }
                .onFailure { showError(it.message) }
        }
    }

    private fun routeAfterAuth(role: String) {
        val next = if (role == "WARD") ConsentActivity::class.java else PairingActivity::class.java
        startActivity(Intent(this, next))
        finish()
    }

    private fun showError(message: String?) {
        Toast.makeText(this, message ?: "요청에 실패했습니다", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AuthScreen(
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, password: String, role: String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("WARD") }

    Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(if (isRegisterMode) "회원가입" else "로그인")

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("이메일") })
        OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("비밀번호") })

        if (isRegisterMode) {
            Text("역할 선택")
            Row {
                TextButton(onClick = { role = "WARD" }) { Text(if (role == "WARD") "● 본인(회복 지원 대상)" else "○ 본인(회복 지원 대상)") }
                TextButton(onClick = { role = "GUARDIAN" }) { Text(if (role == "GUARDIAN") "● 보호자" else "○ 보호자") }
            }
        }

        Button(onClick = { if (isRegisterMode) onRegister(email, password, role) else onLogin(email, password) }) {
            Text(if (isRegisterMode) "가입하고 시작하기" else "로그인")
        }

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(if (isRegisterMode) "이미 계정이 있어요" else "계정이 없어요, 가입할게요")
        }
    }
}

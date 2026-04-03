package com.example.facecheck.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.example.facecheck.MainActivity
import com.example.facecheck.R
import com.example.facecheck.ui.theme.FaceCheckTheme
import kotlinx.coroutines.delay

class LoginActivity : ComponentActivity() {

    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loginViewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val role = prefs.getString("user_role", "")
        if (role == "teacher" || role == "student") {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContent {
            FaceCheckTheme {
                LoginScreen(loginViewModel)
            }
        }
    }
}

@Composable
fun LoginScreen(viewModel: LoginViewModel) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isStudentLogin by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }
    var loginSuccessHandled by remember { mutableStateOf(false) }
    var showSuccessAnimation by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.observeAsState()

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginUiState.Success -> {
                if (loginSuccessHandled) return@LaunchedEffect
                loginSuccessHandled = true
                Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()
                val prefsEditor = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit()
                prefsEditor.putString("user_role", state.role)
                prefsEditor.putString("access_token", state.accessToken)
                prefsEditor.putString("refresh_token", state.refreshToken)
                if (state.role == "teacher") {
                    prefsEditor.putLong("teacher_id", state.userId)
                } else {
                    prefsEditor.putLong("student_id", state.userId)
                }
                if (rememberMe) {
                    prefsEditor.putString("saved_username", username)
                    prefsEditor.putString("saved_password", password)
                    prefsEditor.putBoolean("remember_me", true)
                } else {
                    prefsEditor.remove("saved_username")
                    prefsEditor.remove("saved_password")
                    prefsEditor.remove("remember_me")
                }
                prefsEditor.apply()
                showSuccessAnimation = true
            }
            is LoginUiState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                loginSuccessHandled = false
            }
            else -> {}
        }
    }

    LaunchedEffect(showSuccessAnimation) {
        if (showSuccessAnimation) {
            delay(1400)
            context.startActivity(Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        rememberMe = prefs.getBoolean("remember_me", false)
        if (rememberMe) {
            username = prefs.getString("saved_username", "") ?: ""
            password = prefs.getString("saved_password", "") ?: ""
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.logo_whitebackground),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "多模式签到系统",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(48.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(if (isStudentLogin) "学号" else "用户名") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("密码") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                    Text("记住密码", fontSize = 14.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isStudentLogin) "学生登录" else "教师登录",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isStudentLogin,
                        onCheckedChange = { isStudentLogin = it }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.login(username, password, isStudentLogin) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = uiState !is LoginUiState.Loading
            ) {
                if (uiState is LoginUiState.Loading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("登录", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = { context.startActivity(Intent(context, RegisterActivity::class.java)) }) {
                Text("还没有账号？立即注册")
            }
            }
            if (showSuccessAnimation) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Black.copy(alpha = 0.45f)
                    ) {}
                    AndroidView(
                        factory = { ctx ->
                            LottieAnimationView(ctx).apply {
                                setAnimation("lottie/telegram.json")
                                repeatCount = 0
                                repeatMode = LottieDrawable.RESTART
                                playAnimation()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                }
            }
        }
    }
}

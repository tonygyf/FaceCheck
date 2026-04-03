package com.example.facecheck.ui.student

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.facecheck.api.ApiService
import com.example.facecheck.api.AppealRequest
import com.example.facecheck.api.MySubmissionsResponse
import com.example.facecheck.api.RetrofitClient
import com.example.facecheck.ui.theme.FaceCheckTheme
import com.example.facecheck.utils.SessionManager

class StudentAppealActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val studentId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("student_id", -1L)
        val apiService = RetrofitClient.getApiService()
        val apiKey = SessionManager(this).apiKey
        setContent {
            FaceCheckTheme {
                StudentAppealScreen(
                    studentId = studentId,
                    apiService = apiService,
                    apiKey = apiKey,
                    onBack = { finish() },
                    onToast = { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentAppealScreen(
    studentId: Long,
    apiService: ApiService,
    apiKey: String,
    onBack: () -> Unit,
    onToast: (String) -> Unit
) {
    val items = remember { mutableStateListOf<MySubmissionsResponse.Item>() }
    val appealInput = remember { mutableStateMapOf<Long, String>() }
    var loading by remember { mutableStateOf(false) }

    fun loadData() {
        if (studentId <= 0) return
        loading = true
        apiService.getMySubmissions(apiKey, studentId).enqueue(object : retrofit2.Callback<MySubmissionsResponse> {
            override fun onResponse(
                call: retrofit2.Call<MySubmissionsResponse>,
                response: retrofit2.Response<MySubmissionsResponse>
            ) {
                loading = false
                items.clear()
                if (response.isSuccessful && response.body()?.success == true) {
                    val source = response.body()?.data ?: emptyList()
                    items.addAll(source.filter { it.finalResult.equals("PENDING_REVIEW", true) || it.finalResult.equals("REJECTED", true) })
                }
            }

            override fun onFailure(call: retrofit2.Call<MySubmissionsResponse>, t: Throwable) {
                loading = false
            }
        })
    }

    fun submitAppeal(submissionId: Long) {
        val reason = (appealInput[submissionId] ?: "").trim()
        if (reason.isEmpty()) {
            onToast("请先填写申诉内容")
            return
        }
        apiService.appealSubmission(apiKey, submissionId, AppealRequest(studentId, reason))
            .enqueue(object : retrofit2.Callback<com.example.facecheck.api.ApiResponse> {
                override fun onResponse(
                    call: retrofit2.Call<com.example.facecheck.api.ApiResponse>,
                    response: retrofit2.Response<com.example.facecheck.api.ApiResponse>
                ) {
                    if (response.isSuccessful && response.body()?.isSuccess == true) {
                        onToast("申诉已提交")
                        loadData()
                    } else {
                        onToast("申诉提交失败")
                    }
                }

                override fun onFailure(call: retrofit2.Call<com.example.facecheck.api.ApiResponse>, t: Throwable) {
                    onToast("网络异常: ${t.message}")
                }
            })
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("签到申诉") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (loading) {
                Text("加载中...", style = MaterialTheme.typography.bodyMedium)
            }
            if (!loading && items.isEmpty()) {
                Text("当前没有待申诉记录", style = MaterialTheme.typography.bodyMedium)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(item.title ?: "签到任务", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("状态：${item.finalResult ?: "-"}", style = MaterialTheme.typography.bodySmall)
                            Text("原因：${item.reason ?: "-"}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = appealInput[item.id] ?: "",
                                onValueChange = { appealInput[item.id] = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("申诉内容") },
                                minLines = 2
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(onClick = { submitAppeal(item.id) }) {
                                    Text("提交申诉")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

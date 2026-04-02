package com.example.facecheck.ui.checkin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facecheck.api.CheckinTaskRequest
import com.example.facecheck.api.RetrofitClient
import com.example.facecheck.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CreateCheckinTaskActivity : AppCompatActivity() {

    private var classId: Long = -1
    private var teacherId: Long = -1
    private var className: String? = null
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)
        classId = intent.getLongExtra("CLASS_ID", -1)
        className = intent.getStringExtra("CLASS_NAME")
        teacherId = sessionManager.getTeacherId()

        if (classId == -1L || teacherId == -1L) {
            Toast.makeText(this, "无效的班级或教师信息", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            MaterialTheme {
                CreateCheckinTaskScreen(
                    className = className ?: "未知班级",
                    onPublish = { request ->
                        publishCheckinTask(request)
                    }
                )
            }
        }
    }

    private fun publishCheckinTask(request: CheckinTaskRequest) {
        // Add static data
        request.classId = classId
        request.teacherId = teacherId

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.getApiService().createCheckinTask(sessionManager.getApiKey(), request).execute()
                runOnUiThread {
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@CreateCheckinTaskActivity, "任务发布成功!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@CreateCheckinTaskActivity, "发布失败: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@CreateCheckinTaskActivity, "网络错误: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCheckinTaskScreen(className: String, onPublish: (CheckinTaskRequest) -> Unit) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    var startAt by remember { mutableStateOf(sdf.format(Date())) }
    var endAt by remember { mutableStateOf("") }
    var isPublished by remember { mutableStateOf(true) }
    var locationLat by remember { mutableStateOf("") }
    var locationLng by remember { mutableStateOf("") }
    var locationRadiusM by remember { mutableStateOf("100") }
    var passwordPlain by remember { mutableStateOf("") }
    var gestureSequence by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("发布签到任务 - $className") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("时间和状态", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = startAt,
                onValueChange = { startAt = it },
                label = { Text("开始时间 (yyyy-MM-dd HH:mm:ss)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = endAt,
                onValueChange = { endAt = it },
                label = { Text("结束时间 (yyyy-MM-dd HH:mm:ss)") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("立即发布")
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = isPublished,
                    onCheckedChange = { isPublished = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("地理位置（可选）", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = locationLat,
                onValueChange = { locationLat = it },
                label = { Text("纬度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = locationLng,
                onValueChange = { locationLng = it },
                label = { Text("经度") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = locationRadiusM,
                onValueChange = { locationRadiusM = it },
                label = { Text("半径（米）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("高级选项（可选）", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = passwordPlain,
                onValueChange = { passwordPlain = it },
                label = { Text("数字密码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = gestureSequence,
                onValueChange = { gestureSequence = it },
                label = { Text("手势序列 (e.g., 1,2,3,6,9)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "任务标题不能为空", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val request = CheckinTaskRequest().apply {
                        this.title = title
                        this.startAt = startAt
                        this.endAt = if(endAt.isBlank()) null else endAt
                        this.status = if(isPublished) "ACTIVE" else "DRAFT"
                        this.locationLat = locationLat.toDoubleOrNull()
                        this.locationLng = locationLng.toDoubleOrNull()
                        this.locationRadiusM = locationRadiusM.toIntOrNull()
                        this.passwordPlain = if(passwordPlain.isBlank()) null else passwordPlain
                        this.gestureSequence = if(gestureSequence.isBlank()) null else gestureSequence
                    }
                    onPublish(request)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("发布任务", fontSize = 16.sp)
            }
        }
    }
}

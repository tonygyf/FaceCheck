package com.example.facecheck.ui.task

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.facecheck.api.CheckinTaskRequest
import com.example.facecheck.api.RetrofitClient
import com.example.facecheck.data.model.Classroom
import com.example.facecheck.database.DatabaseHelper
import com.example.facecheck.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PublishTaskActivity : ComponentActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)
        setContent {
            MaterialTheme {
                PublishTaskWizard(sessionManager)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishTaskWizard(sessionManager: SessionManager) {
    val context = LocalContext.current
    val dbHelper = remember { DatabaseHelper(context) }
    val teacherId = sessionManager.getTeacherId()

    val classroomsState = produceState<List<Classroom>>(initialValue = emptyList(), teacherId) {
        value = withContext(Dispatchers.IO) {
            dbHelper.getAllClassroomsWithStudentCountAsList(teacherId)
        }
    }

    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }

    if (selectedClassroom == null) {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("选择班级") })
            }
        ) { paddingValues ->
            ClassSelectionStep(
                paddingValues = paddingValues,
                classrooms = classroomsState.value,
                onClassroomSelected = { classroom ->
                    selectedClassroom = classroom
                }
            )
        }
    } else {
        CreateCheckinTaskScreen(
            className = selectedClassroom!!.name,
            onPublish = { request ->
                publishCheckinTask(context, sessionManager, selectedClassroom!!.id, request) {
                    (context as? Activity)?.finish()
                }
            },
            onBack = { selectedClassroom = null } // Allow navigating back to class selection
        )
    }
}

private fun publishCheckinTask(context: android.content.Context, sessionManager: SessionManager, classId: Long, request: CheckinTaskRequest, onFinish: () -> Unit) {
    request.classId = classId
    request.teacherId = sessionManager.getTeacherId()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.getApiService().createCheckinTask(sessionManager.apiKey, request).execute()
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "任务发布成功!", Toast.LENGTH_SHORT).show()
                    onFinish()
                } else {
                    Toast.makeText(context, "发布失败: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "网络错误: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

@Composable
fun ClassSelectionStep(paddingValues: PaddingValues, classrooms: List<Classroom>, onClassroomSelected: (Classroom) -> Unit) {
    LazyColumn(
        modifier = Modifier.padding(paddingValues),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "请选择要发布任务的班级:",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(classrooms) { classroom ->
            ClassroomCard(classroom = classroom, onClick = { onClassroomSelected(classroom) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassroomCard(classroom: Classroom, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = CardDefaults.shape
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = classroom.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${classroom.year}级",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "学生人数: ${classroom.studentCount}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCheckinTaskScreen(className: String, onPublish: (CheckinTaskRequest) -> Unit, onBack: () -> Unit) {
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
            TopAppBar(
                title = { Text("发布签到任务 - $className") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
                        this.endAt = if (endAt.isBlank()) {
                            // 如果结束时间为空，则默认为开始时间后一小时
                            try {
                                val startDate = sdf.parse(startAt)
                                val calendar = java.util.Calendar.getInstance()
                                calendar.time = startDate
                                calendar.add(java.util.Calendar.HOUR_OF_DAY, 1)
                                sdf.format(calendar.time)
                            } catch (e: java.text.ParseException) {
                                endAt // 如果解析失败，则保持原样（可能为空）
                            }
                        } else {
                            endAt
                        }
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

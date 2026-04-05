package com.example.facecheck.ui.task

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.facecheck.api.CheckinTaskRequest
import com.example.facecheck.api.RetrofitClient
import com.example.facecheck.data.model.Classroom
import com.example.facecheck.database.DatabaseHelper
import com.example.facecheck.ui.theme.FaceCheckTheme
import com.example.facecheck.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt

class PublishTaskActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val classId = intent.getLongExtra("CLASS_ID", -1)
        val teacherId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("teacher_id", -1)

        setContent {
            FaceCheckTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PublishTaskScreen(
                        classId = classId,
                        teacherId = teacherId,
                        onBack = { finish() },
                        onPublished = {
                            Toast.makeText(this, "发布成功", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishTaskScreen(
    classId: Long,
    teacherId: Long,
    onBack: () -> Unit,
    onPublished: () -> Unit
) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    var locationEnabled by remember { mutableStateOf(false) }
    var locationLat by remember { mutableStateOf<Double?>(null) }
    var locationLng by remember { mutableStateOf<Double?>(null) }
    var locationAddress by remember { mutableStateOf("未选择") }
    var locationRadius by remember { mutableStateOf(100) }

    var gestureEnabled by remember { mutableStateOf(false) }
    var gestureSequence by remember { mutableStateOf<String?>(null) }

    var passwordEnabled by remember { mutableStateOf(false) }
    var passwordText by remember { mutableStateOf("") }

    var classrooms by remember { mutableStateOf<List<Classroom>>(emptyList()) }
    var selectedClassroom by remember { mutableStateOf<Classroom?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val apiService = remember { RetrofitClient.getApiService() }
    val sessionManager = remember { SessionManager(context) }

    val mapPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val lat = if (data?.hasExtra("latitude") == true) data.getDoubleExtra("latitude", 0.0) else null
            val lng = if (data?.hasExtra("longitude") == true) data.getDoubleExtra("longitude", 0.0) else null
            locationLat = lat
            locationLng = lng
            val rawAddress = data?.getStringExtra("address")?.trim().orEmpty()
            locationAddress = if (lat != null && lng != null) {
                val coordText = String.format(Locale.getDefault(), "已选坐标：%.6f, %.6f", lat, lng)
                if (rawAddress.isBlank() || rawAddress.contains("无法获取") || rawAddress.contains("解析")) {
                    coordText
                } else {
                    "$rawAddress（$coordText）"
                }
            } else {
                if (rawAddress.isBlank()) "未选择" else rawAddress
            }
        }
    }

    LaunchedEffect(Unit) {
        val dbHelper = DatabaseHelper(context)
        classrooms = dbHelper.getAllClassrooms(teacherId)
        if (classId != -1L) {
            selectedClassroom = classrooms.find { it.id == classId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("发布考勤任务") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    value = selectedClassroom?.name ?: "请选择班级",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("班级 *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    classrooms.forEach { classroom ->
                        DropdownMenuItem(
                            text = { Text(classroom.name) },
                            onClick = {
                                selectedClassroom = classroom
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务标题 *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = startTime,
                onValueChange = {},
                label = { Text("开始时间 *") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDateTimePicker(context, sdf) { startTime = it } }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择开始时间")
                    }
                }
            )

            OutlinedTextField(
                value = endTime,
                onValueChange = {},
                label = { Text("结束时间 (不填则为开始后1小时)") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDateTimePicker(context, sdf) { endTime = it } }) {
                        Icon(Icons.Default.DateRange, contentDescription = "选择结束时间")
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("立即发布 (ACTIVE)", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("地理位置签到", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = locationEnabled, onCheckedChange = { locationEnabled = it })
            }

            if (locationEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "当前位置：$locationAddress",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "签到范围：${locationRadius}米",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Slider(
                            value = locationRadius.toFloat(),
                            onValueChange = { locationRadius = it.toInt() },
                            valueRange = 50f..500f,
                            steps = 8
                        )
                        Button(
                            onClick = {
                                val intent = Intent(context, MapPickerActivity::class.java)
                                mapPickerLauncher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("选择位置")
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("手势签到", style = MaterialTheme.typography.bodyLarge)
                }
                Switch(checked = gestureEnabled, onCheckedChange = { gestureEnabled = it })
            }

            if (gestureEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            if (gestureSequence != null) "已设置手势" else "请绘制手势密码 (至少连接2个点)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        GestureLockPad(
                            onSequenceComplete = { sequence -> gestureSequence = sequence },
                            onCleared = { gestureSequence = null },
                            modifier = Modifier.size(240.dp)
                        )
                        if (gestureSequence != null) {
                            Button(
                                onClick = { gestureSequence = null },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Text("清除手势")
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("密码签到", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = passwordEnabled, onCheckedChange = { passwordEnabled = it })
            }

            if (passwordEnabled) {
                OutlinedTextField(
                    value = passwordText,
                    onValueChange = { passwordText = it },
                    label = { Text("签到密码") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            HorizontalDivider()

            Button(
                onClick = {
                    if (isPublishing) return@Button
                    if (teacherId <= 0) {
                        Toast.makeText(context, "教师身份无效，请重新登录", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedClassroom == null) {
                        Toast.makeText(context, "请选择班级", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (title.isBlank()) {
                        Toast.makeText(context, "请填写任务标题", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (startTime.isBlank()) {
                        Toast.makeText(context, "请选择开始时间", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (locationEnabled && (locationLat == null || locationLng == null)) {
                        Toast.makeText(context, "已开启地理位置签到，请先在地图中确认有效位置", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalEndTime = if (endTime.isBlank()) {
                        try {
                            val cal = Calendar.getInstance()
                            cal.time = sdf.parse(startTime)!!
                            cal.add(Calendar.HOUR, 1)
                            sdf.format(cal.time)
                        } catch (e: Exception) {
                            startTime
                        }
                    } else endTime

                    val request = CheckinTaskRequest().apply {
                        this.classId = selectedClassroom!!.id
                        this.teacherId = teacherId
                        this.title = title
                        this.startAt = startTime
                        this.endAt = finalEndTime
                        this.status = if (isActive) "ACTIVE" else "DRAFT"
                        this.locationLat = if (locationEnabled) locationLat else null
                        this.locationLng = if (locationEnabled) locationLng else null
                        this.locationRadiusM = if (locationEnabled) locationRadius else null
                        this.gestureSequence = if (gestureEnabled) gestureSequence else null
                        this.passwordPlain = if (passwordEnabled && passwordText.isNotBlank()) passwordText else null
                    }

                    isPublishing = true
                    apiService.createCheckinTask(sessionManager.apiKey, request)
                        .enqueue(object : retrofit2.Callback<com.example.facecheck.api.ApiCreateResponse> {
                            override fun onResponse(
                                call: retrofit2.Call<com.example.facecheck.api.ApiCreateResponse>,
                                response: retrofit2.Response<com.example.facecheck.api.ApiCreateResponse>
                            ) {
                                isPublishing = false
                                val body = response.body()
                                if (response.isSuccessful && (body == null || body.isOk())) {
                                    onPublished()
                                    return
                                }
                                val bodyError = body?.error?.takeIf { it.isNotBlank() }
                                val rawError = try {
                                    response.errorBody()?.string()
                                } catch (_: Exception) {
                                    null
                                }
                                Toast.makeText(
                                    context,
                                    "发布失败: ${bodyError ?: rawError ?: "服务器返回异常"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            override fun onFailure(
                                call: retrofit2.Call<com.example.facecheck.api.ApiCreateResponse>,
                                t: Throwable
                            ) {
                                isPublishing = false
                                Toast.makeText(context, "发布失败: ${t.message}", Toast.LENGTH_LONG).show()
                            }
                        })
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !isPublishing
            ) {
                Text(if (isPublishing) "发布中..." else "确认发布", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun GestureLockPad(
    onSequenceComplete: (String) -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    var currentPos by remember { mutableStateOf<Offset?>(null) }
    val centers = remember {
        mutableStateListOf<Offset>().also { list -> repeat(9) { list.add(Offset.Zero) } }
    }

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSurface = MaterialTheme.colorScheme.surfaceVariant

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        selectedIds.clear()
                        onCleared()
                        currentPos = offset
                        centers.forEachIndexed { index, center ->
                            if (offsetDistance(offset, center) < 60f) {
                                if (!selectedIds.contains(index + 1)) selectedIds.add(index + 1)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        currentPos = change.position
                        centers.forEachIndexed { index, center ->
                            if (offsetDistance(change.position, center) < 60f) {
                                if (!selectedIds.contains(index + 1)) selectedIds.add(index + 1)
                            }
                        }
                    },
                    onDragEnd = {
                        currentPos = null
                        if (selectedIds.size >= 2) {
                            onSequenceComplete(selectedIds.joinToString("-"))
                        }
                    },
                    onDragCancel = { currentPos = null }
                )
            }
    ) {
        val cellSize = size.width / 3f
        val radius = cellSize * 0.25f
        val dotRadius = radius * 0.3f

        for (row in 0..2) {
            for (col in 0..2) {
                centers[row * 3 + col] = Offset(
                    x = col * cellSize + cellSize / 2f,
                    y = row * cellSize + cellSize / 2f
                )
            }
        }

        for (i in 0 until selectedIds.size - 1) {
            drawLine(
                color = colorPrimary.copy(alpha = 0.6f),
                start = centers[selectedIds[i] - 1],
                end = centers[selectedIds[i + 1] - 1],
                strokeWidth = 6f
            )
        }

        if (selectedIds.isNotEmpty() && currentPos != null) {
            drawLine(
                color = colorPrimary.copy(alpha = 0.4f),
                start = centers[selectedIds.last() - 1],
                end = currentPos!!,
                strokeWidth = 6f
            )
        }

        for (i in 0..8) {
            val center = centers[i]
            val isSelected = selectedIds.contains(i + 1)
            drawCircle(
                color = if (isSelected) colorPrimary.copy(alpha = 0.2f) else colorSurface,
                radius = radius,
                center = center
            )
            drawCircle(
                color = if (isSelected) colorPrimary else Color.Gray,
                radius = dotRadius,
                center = center
            )
        }
    }
}

private fun offsetDistance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

private fun showDateTimePicker(
    context: Context,
    sdf: SimpleDateFormat,
    onSelected: (String) -> Unit
) {
    val now = Calendar.getInstance()
    DatePickerDialog(
        context,
        { _, year, month, day ->
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val cal = Calendar.getInstance()
                    cal.set(year, month, day, hour, minute, 0)
                    onSelected(sdf.format(cal.time))
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        },
        now.get(Calendar.YEAR),
        now.get(Calendar.MONTH),
        now.get(Calendar.DAY_OF_MONTH)
    ).show()
}

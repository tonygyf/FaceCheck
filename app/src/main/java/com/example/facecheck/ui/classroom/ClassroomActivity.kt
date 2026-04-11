package com.example.facecheck.ui.classroom

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.example.facecheck.data.model.Student
import com.example.facecheck.ui.theme.FaceCheckTheme
import com.example.facecheck.utils.Constants
import de.hdodenhof.circleimageview.CircleImageView
import java.io.File

class ClassroomActivity : ComponentActivity() {
    private val studentViewModel: StudentViewModel by viewModels {
        StudentViewModelFactory(application)
    }
    private var classroomId: Long = -1

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        classroomId = intent.getLongExtra("classroom_id", -1)
        if (classroomId == -1L) {
            Toast.makeText(this, "班级信息无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        studentViewModel.loadStudentsForClass(classroomId)

        setContent {
            FaceCheckTheme {
                val students by studentViewModel.students.observeAsState(emptyList())
                val actionResult by studentViewModel.actionResult.observeAsState()
                val errorMessage by studentViewModel.errorMessage.observeAsState()
                LaunchedEffect(actionResult) {
                    if (actionResult == true) {
                        Toast.makeText(this@ClassroomActivity, "学生信息已保存", Toast.LENGTH_SHORT).show()
                    }
                }
                LaunchedEffect(errorMessage) {
                    val message = errorMessage?.trim().orEmpty()
                    if (message.isNotEmpty()) {
                        Toast.makeText(this@ClassroomActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("学生展示墙") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                                }
                            }
                        )
                    }
                ) { padding ->
                    ClassroomShowcaseContent(
                        students = students,
                        onUpdateStudent = { studentId, name, sid, gender ->
                            studentViewModel.updateStudent(studentId, classroomId, name, sid, gender, "")
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassroomShowcaseContent(
    students: List<Student>,
    onUpdateStudent: (studentId: Long, name: String, sid: String, gender: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = remember(students) { buildStats(students) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    val pageBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surface
        )
    )

    Box(
        modifier = modifier.background(pageBrush)
    ) {
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("当前班级暂无学生", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "学生加入后会自动展示在这里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            OverviewPanel(stats = stats)
            Spacer(modifier = Modifier.height(10.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(students, key = { it.id }) { student ->
                    StudentShowcaseCard(
                        student = student,
                        onClick = { selectedStudent = student },
                    )
                }
            }
        }
    }

    selectedStudent?.let { student ->
        StudentDetailDialog(
            student = student,
            onDismiss = { selectedStudent = null },
            onSave = { newName, newSid, newGender ->
                student.name = newName
                student.sid = newSid
                student.gender = newGender
                onUpdateStudent(student.id, newName, newSid, newGender)
                selectedStudent = null
            },
        )
    }
}

@Composable
private fun OverviewPanel(stats: StudentStats) {
    val cardBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f),
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.68f)
        )
    )
    Surface(
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(cardBrush)
                .padding(14.dp)
        ) {
            Text(
                text = "班级总览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(
                    icon = Icons.Default.DateRange,
                    text = "总人数 ${stats.total}",
                    modifier = Modifier.weight(1f)
                )
                StatChip(
                    icon = Icons.Default.LocationOn,
                    text = "有头像 ${stats.withAvatar}",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip(icon = Icons.Default.Check, text = "男 ${stats.male}", modifier = Modifier.weight(1f))
                StatChip(icon = Icons.Default.Check, text = "女 ${stats.female}", modifier = Modifier.weight(1f))
                StatChip(icon = Icons.Default.Lock, text = "未知 ${stats.unknown}", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.82f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
            Text(text = text, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StudentShowcaseCard(student: Student, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StudentAvatar(avatarUri = student.avatarUri, name = student.name)
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = student.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = student.sid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PillTag(text = normalizeGender(student.gender), modifier = Modifier.weight(1f))
                PillTag(text = if (student.avatarUri.isNullOrBlank()) "无头像" else "已上传头像", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PillTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StudentAvatar(avatarUri: String?, name: String, size: Dp = 44.dp) {
    val initial = name.trim().take(1).ifEmpty { "?" }
    val seed = (name.hashCode() and 0xFF)
    val hue = seed / 255f
    val fallbackBg = Color.hsv(hue * 360f, 0.25f, 0.95f)
    val resolvedAvatarModel = remember(avatarUri) { resolveAvatarModel(avatarUri) }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(fallbackBg)
            .border(1.dp, Color.White.copy(alpha = 0.9f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedAvatarModel != null) {
            AndroidView(
                factory = { context ->
                    CircleImageView(context).apply {
                        borderWidth = 0
                    }
                },
                update = { imageView ->
                    Glide.with(imageView.context)
                        .load(resolvedAvatarModel)
                        .centerCrop()
                        .into(imageView)
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1F2937),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StudentDetailDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (name: String, sid: String, gender: String) -> Unit,
) {
    val context = LocalContext.current
    var isEditing by remember(student.id) { mutableStateOf(false) }
    var name by remember(student.id) { mutableStateOf(student.name ?: "") }
    var sid by remember(student.id) { mutableStateOf(student.sid ?: "") }
    var gender by remember(student.id) { mutableStateOf(student.gender ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "编辑学生信息" else "学生详情") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StudentAvatar(
                    avatarUri = student.avatarUri,
                    name = name.ifBlank { student.name ?: "?" },
                    size = 116.dp,
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (isEditing) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("姓名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sid,
                        onValueChange = { sid = it },
                        label = { Text("学号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("性别") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    StudentDetailInfo(label = "姓名", value = student.name ?: "未填写")
                    StudentDetailInfo(label = "学号", value = student.sid ?: "未填写")
                    StudentDetailInfo(label = "性别", value = normalizeGender(student.gender))
                    StudentDetailInfo(
                        label = "头像",
                        value = if (student.avatarUri.isNullOrBlank()) "未上传" else "已上传",
                    )
                }
            }
        },
        confirmButton = {
            if (isEditing) {
                TextButton(onClick = {
                    val trimmedName = name.trim()
                    val trimmedSid = sid.trim()
                    val trimmedGender = gender.trim().ifEmpty { "未知" }
                    if (trimmedName.isEmpty() || trimmedSid.isEmpty()) {
                        Toast.makeText(context, "姓名和学号不能为空", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(trimmedName, trimmedSid, trimmedGender)
                    }
                }) {
                    Text("保存")
                }
            } else {
                TextButton(onClick = { isEditing = true }) {
                    Text("编辑")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun StudentDetailInfo(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun resolveAvatarModel(avatarUri: String?): Any? {
    val raw = avatarUri?.trim().orEmpty()
    if (raw.isEmpty()) return null
    return when {
        raw.startsWith("http://", ignoreCase = true) -> raw
        raw.startsWith("https://", ignoreCase = true) -> raw
        raw.startsWith("file://", ignoreCase = true) -> raw
        raw.startsWith("content://", ignoreCase = true) -> raw
        raw.startsWith("/") -> File(raw)
        else -> Constants.CDN_BASE_URL + raw.trimStart('/')
    }
}

private data class StudentStats(
    val total: Int,
    val withAvatar: Int,
    val male: Int,
    val female: Int,
    val unknown: Int
)

private fun buildStats(students: List<Student>): StudentStats {
    var withAvatar = 0
    var male = 0
    var female = 0
    var unknown = 0
    for (s in students) {
        if (!s.avatarUri.isNullOrBlank()) withAvatar++
        when (normalizeGender(s.gender)) {
            "男" -> male++
            "女" -> female++
            else -> unknown++
        }
    }
    return StudentStats(
        total = students.size,
        withAvatar = withAvatar,
        male = male,
        female = female,
        unknown = unknown
    )
}

private fun normalizeGender(gender: String?): String {
    return when (gender?.trim()?.uppercase()) {
        "M", "MALE", "男" -> "男"
        "F", "FEMALE", "女" -> "女"
        else -> "未知"
    }
}

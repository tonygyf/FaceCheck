package com.example.facecheck.ui.classroom

import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.example.facecheck.R
import com.example.facecheck.data.model.Classroom
import com.example.facecheck.database.DatabaseHelper
import com.example.facecheck.ui.checkin.AttendanceTaskStatusChips
import com.google.android.material.bottomsheet.BottomSheetDialog

data class ClassTaskCheckinRow(
    val taskId: Long,
    val title: String,
    val status: String,
    val startAt: String,
    val signedStudentCount: Int,
)

object ClassroomCheckinStatusSheet {

    @JvmStatic
    @JvmOverloads
    fun show(
        fragment: Fragment,
        classroom: Classroom,
        db: DatabaseHelper,
        dimBehind: View? = null,
    ) {
        if (!fragment.isAdded) return
        val activity = fragment.activity ?: return
        showInternal(
            activity = activity,
            classroom = classroom,
            db = db,
            lifecycleOwner = fragment.viewLifecycleOwner,
            viewModelStoreOwner = fragment,
            savedStateRegistryOwner = fragment,
            dimBehind = dimBehind,
        )
    }

    @JvmStatic
    fun show(activity: AppCompatActivity, classroom: Classroom, db: DatabaseHelper) {
        showInternal(
            activity = activity,
            classroom = classroom,
            db = db,
            lifecycleOwner = activity,
            viewModelStoreOwner = activity,
            savedStateRegistryOwner = activity,
            dimBehind = null,
        )
    }

    private fun showInternal(
        activity: FragmentActivity,
        classroom: Classroom,
        db: DatabaseHelper,
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        dimBehind: View?,
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val rows = loadRows(db, classroom.getId())
        val studentCap = classroom.getStudentCount()

        val dialog = BottomSheetDialog(activity)
        dialog.setOnShowListener { d ->
            val bottomSheetDialog = d as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet,
            )
            bottomSheet?.setBackgroundResource(R.drawable.bg_bottom_sheet_rounded)
        }

        val content = LayoutInflater.from(activity).inflate(R.layout.dialog_classroom_checkin_status, null, false)
        ComposeViewTreeOwnersHelper.apply(content, lifecycleOwner, viewModelStoreOwner, savedStateRegistryOwner)
        dialog.setContentView(content)
// ✅ 关键修复：setContentView 之后 window 已存在，把 owners 补挂到 decorView
        //    ComposeView.onAttachedToWindow 会向上遍历到 decorView，必须在那层也能找到 owner
        dialog.window?.decorView?.let { decor ->
            ComposeViewTreeOwnersHelper.apply(
                decor, lifecycleOwner, viewModelStoreOwner, savedStateRegistryOwner
            )
        }
        dimBehind?.alpha = 0.82f
        dialog.setOnDismissListener {
            dimBehind?.alpha = 1f
        }

        dialog.window?.let { w ->
            w.setDimAmount(0.45f)
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }

        val compose = content.findViewById<ComposeView>(R.id.compose_classroom_checkin_status)
        if (compose == null) {
            dialog.dismiss()
            return
        }
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        compose.setContent {
            MaterialTheme {
                Surface(color = Color.Transparent) {
                    ClassroomCheckinStatusSheetBody(
                        className = classroom.getName() ?: "",
                        year = classroom.getYear(),
                        studentCount = studentCap,
                        rows = rows,
                    )
                }
            }
        }
        dialog.show()
    }

    private fun loadRows(db: DatabaseHelper, classId: Long): List<ClassTaskCheckinRow> {
        val out = ArrayList<ClassTaskCheckinRow>()
        val c = db.getAllCheckinTasksByClass(classId) ?: return out
        try {
            if (!c.moveToFirst()) return out
            val idIx = c.getColumnIndexOrThrow("id")
            val titleIx = c.getColumnIndexOrThrow("title")
            val statusIx = c.getColumnIndexOrThrow("status")
            val startIx = c.getColumnIndexOrThrow("startAt")
            do {
                val tid = c.getLong(idIx)
                val signed = db.countDistinctStudentsSubmittedForTask(tid)
                out.add(
                    ClassTaskCheckinRow(
                        taskId = tid,
                        title = c.getString(titleIx) ?: "",
                        status = c.getString(statusIx) ?: "",
                        startAt = c.getString(startIx) ?: "",
                        signedStudentCount = signed,
                    ),
                )
            } while (c.moveToNext())
        } finally {
            c.close()
        }
        return out
    }
}

@Composable
private fun ClassroomCheckinStatusSheetBody(
    className: String,
    year: Int,
    studentCount: Int,
    rows: List<ClassTaskCheckinRow>,
) {
    val config = LocalConfiguration.current
    val maxFromScreen = (config.screenHeightDp * 0.58f).dp
    val maxListHeight = minOf(maxFromScreen, 560.dp)

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxListHeight),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = "考勤签到情况",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = buildString {
                        append(className)
                        append(" · ")
                        append(year)
                        append("级")
                        if (studentCount > 0) {
                            append(" · 班级人数 ")
                            append(studentCount)
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 6.dp),
                )
                HorizontalDivider(
                    modifier = Modifier.padding(top = 12.dp),
                    color = Color(0xFFE2E8F0),
                )
            }
        }
        if (rows.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = "暂无考勤任务。发布任务并同步后，将在此显示各任务签到人数。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            items(rows, key = { it.taskId }) { row ->
                ClassTaskCheckinCard(row = row, classStudentCount = studentCount)
            }
        }
    }
}

@Composable
private fun ClassTaskCheckinCard(row: ClassTaskCheckinRow, classStudentCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFC7D2FE)),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = row.title.ifBlank { "未命名任务" },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            AttendanceTaskStatusChips(status = row.status, startAt = row.startAt)
            val signedText = if (classStudentCount > 0) {
                "已签到 ${row.signedStudentCount} / $classStudentCount 人"
            } else {
                "已签到 ${row.signedStudentCount} 人（班级人数未同步）"
            }
            Text(
                text = signedText,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569),
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

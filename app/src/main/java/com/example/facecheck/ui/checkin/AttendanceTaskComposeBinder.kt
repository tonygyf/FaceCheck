package com.example.facecheck.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp

object AttendanceTaskComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView, status: String?, startAt: String?) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TaskMeta(status = status ?: "", startAt = startAt ?: "")
        }
    }
}

/** 供其它界面（如班级签到情况表）复用考勤任务状态胶囊样式 */
@Composable
fun AttendanceTaskStatusChips(status: String, startAt: String) {
    TaskMeta(status = status, startAt = startAt)
}

@Composable
private fun TaskMeta(status: String, startAt: String) {
    val isActive = status.equals("ACTIVE", true)
    val statusBg = if (isActive) Color(0xFFE8F5E9) else Color(0xFFF2F4F7)
    val statusText = if (isActive) Color(0xFF2E7D32) else Color(0xFF667085)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = status.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = statusText,
            modifier = Modifier
                .background(statusBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        if (startAt.isNotBlank()) {
            Text(
                text = startAt.replace("T", " ").take(16),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B),
                modifier = Modifier
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

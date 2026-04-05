package com.example.facecheck.ui.classroom

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

/**
 * 班级卡片顶部信息条：与考勤任务的 ACTIVE/CLOSED 胶囊区分，采用「班级」主标识 + 任务统计。
 */
object ClassroomCardComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView, totalTasks: Int, activeTasks: Int) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            ClassroomMetaChips(totalTasks = totalTasks, activeTasks = activeTasks)
        }
    }
}

@Composable
private fun ClassroomMetaChips(totalTasks: Int, activeTasks: Int) {
    val indigoBg = Color(0xFFEEF2FF)
    val indigoText = Color(0xFF3730A3)
    val slateBg = Color(0xFFF1F5F9)
    val slateText = Color(0xFF475569)
    val amberBg = Color(0xFFFFFBEB)
    val amberText = Color(0xFFB45309)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "班级",
            style = MaterialTheme.typography.labelMedium,
            color = indigoText,
            modifier = Modifier
                .background(indigoBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        Text(
            text = if (totalTasks <= 0) "暂无考勤任务" else "${totalTasks} 个考勤任务",
            style = MaterialTheme.typography.labelMedium,
            color = slateText,
            modifier = Modifier
                .background(slateBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
        if (activeTasks > 0) {
            Text(
                text = "${activeTasks} 进行中",
                style = MaterialTheme.typography.labelMedium,
                color = amberText,
                modifier = Modifier
                    .background(amberBg, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

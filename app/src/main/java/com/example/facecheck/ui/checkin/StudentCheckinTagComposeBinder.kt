package com.example.facecheck.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp

object StudentCheckinTagComposeBinder {
    @JvmStatic
    fun bind(composeView: ComposeView, tags: List<TagItem>) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            TagRow(tags = tags)
        }
    }

    data class TagItem(
        val text: String,
        val required: Boolean
    )
}

@Composable
private fun TagRow(tags: List<StudentCheckinTagComposeBinder.TagItem>) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tags.forEach { tag ->
            val (bg, fg) = resolveTagColors(tag)
            Text(
                text = tag.text,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                modifier = Modifier
                    .background(bg, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

private fun resolveTagColors(tag: StudentCheckinTagComposeBinder.TagItem): Pair<Color, Color> {
    return when (tag.text) {
        "基础签到" -> Pair(Color(0xFFEFF6FF), Color(0xFF1D4ED8))
        "混合签到" -> Pair(Color(0xFFEDE9FE), Color(0xFF6D28D9))
        "定位" -> Pair(Color(0xFFDCFCE7), Color(0xFF166534))
        "手势" -> Pair(Color(0xFFFFEDD5), Color(0xFF9A3412))
        "密码" -> Pair(Color(0xFFFCE7F3), Color(0xFF9D174D))
        "人脸" -> Pair(Color(0xFFE0F2FE), Color(0xFF0C4A6E))
        else -> if (tag.required) {
            Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        } else {
            Pair(Color(0xFFF3F4F6), Color(0xFF374151))
        }
    }
}

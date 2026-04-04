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
            val bg = if (tag.required) Color(0xFF2196F3) else Color(0xFFFFFFFF)
            val fg = if (tag.required) Color.White else Color(0xFF1565C0)
            Text(
                text = if (tag.required) "${tag.text} *" else tag.text,
                style = MaterialTheme.typography.labelMedium,
                color = fg,
                modifier = Modifier
                    .background(bg, RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

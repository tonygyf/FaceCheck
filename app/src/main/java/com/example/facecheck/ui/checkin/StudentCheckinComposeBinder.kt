package com.example.facecheck.ui.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

object StudentCheckinComposeBinder {
    @JvmStatic
    fun bindGesturePad(
        composeView: ComposeView,
        readonly: Boolean,
        initialValue: String?,
        onSequenceChanged: ((String) -> Unit)?
    ) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            StudentGesturePad(
                readonly = readonly,
                initialValue = initialValue.orEmpty(),
                onSequenceChanged = onSequenceChanged
            )
        }
    }
}

@Composable
private fun StudentGesturePad(
    readonly: Boolean,
    initialValue: String,
    onSequenceChanged: ((String) -> Unit)?
) {
    val selectedIds = remember { mutableStateListOf<Int>() }
    var currentPos by remember { mutableStateOf<Offset?>(null) }
    val centers = remember { mutableStateListOf<Offset>().also { repeat(9) { _ -> it.add(Offset.Zero) } } }

    if (selectedIds.isEmpty() && initialValue.isNotBlank()) {
        initialValue.split("-").mapNotNull { it.toIntOrNull() }.forEach { selectedIds.add(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7FAFF), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (readonly) "手势答案（只读）" else "请绘制手势（至少连接2个点）",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF475467)
        )

        Canvas(
            modifier = Modifier
                .size(240.dp)
                .aspectRatio(1f)
                .pointerInput(readonly) {
                    if (readonly) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedIds.clear()
                            currentPos = offset
                            centers.forEachIndexed { index, center ->
                                if (offsetDistance(offset, center) < 60f && !selectedIds.contains(index + 1)) {
                                    selectedIds.add(index + 1)
                                }
                            }
                        },
                        onDrag = { change, _ ->
                            currentPos = change.position
                            centers.forEachIndexed { index, center ->
                                if (offsetDistance(change.position, center) < 60f && !selectedIds.contains(index + 1)) {
                                    selectedIds.add(index + 1)
                                }
                            }
                        },
                        onDragEnd = {
                            currentPos = null
                            if (selectedIds.size >= 2) {
                                onSequenceChanged?.invoke(selectedIds.joinToString("-"))
                            }
                        },
                        onDragCancel = { currentPos = null }
                    )
                }
        ) {
            val cellSize = size.width / 3f
            val radius = cellSize * 0.24f
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
                    color = Color(0xFF2196F3).copy(alpha = 0.6f),
                    start = centers[selectedIds[i] - 1],
                    end = centers[selectedIds[i + 1] - 1],
                    strokeWidth = 6f
                )
            }

            if (!readonly && selectedIds.isNotEmpty() && currentPos != null) {
                drawLine(
                    color = Color(0xFF2196F3).copy(alpha = 0.4f),
                    start = centers[selectedIds.last() - 1],
                    end = currentPos!!,
                    strokeWidth = 6f
                )
            }

            for (i in 0..8) {
                val center = centers[i]
                val isSelected = selectedIds.contains(i + 1)
                drawCircle(
                    color = if (isSelected) Color(0xFF2196F3).copy(alpha = 0.20f) else Color(0xFFE5E7EB),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = if (isSelected) Color(0xFF2196F3) else Color(0xFF94A3B8),
                    radius = dotRadius,
                    center = center
                )
            }
        }

        if (selectedIds.size >= 2) {
            Row(
                modifier = Modifier
                    .background(Color(0xFFEFF6FF), RoundedCornerShape(999.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "已连接: ${selectedIds.joinToString("-")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1D4ED8)
                )
            }
        }
    }
}

private fun offsetDistance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}

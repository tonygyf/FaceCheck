package com.example.facecheck.ui.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.NumberPicker
import java.util.Calendar

interface AttendanceCalendarActionListener {
    fun onPrevMonthClick()
    fun onNextMonthClick()
    fun onDayClick(dayOfMonth: Int)
    fun onYearMonthPicked(year: Int, month: Int)
    fun onToggleGroupMode()
    fun onCycleStatusFilter()
}

object AttendanceCalendarComposeBinder {

    @JvmStatic
    fun bind(
        composeView: ComposeView,
        year: Int,
        month: Int,
        selectedDay: Int,
        dateStatusMap: Map<Int, Int>,
        prevMonthHasActive: Boolean,
        nextMonthHasActive: Boolean,
        groupedByStatus: Boolean,
        statusFilterMode: Int,
        listener: AttendanceCalendarActionListener
    ) {
        composeView.setContent {
            MaterialTheme {
                AttendanceMonthCalendar(
                    year = year,
                    month = month,
                    selectedDay = selectedDay,
                    dateStatusMap = dateStatusMap,
                    prevMonthHasActive = prevMonthHasActive,
                    nextMonthHasActive = nextMonthHasActive,
                    groupedByStatus = groupedByStatus,
                    statusFilterMode = statusFilterMode,
                    listener = listener
                )
            }
        }
    }
}

@Composable
private fun AttendanceMonthCalendar(
    year: Int,
    month: Int,
    selectedDay: Int,
    dateStatusMap: Map<Int, Int>,
    prevMonthHasActive: Boolean,
    nextMonthHasActive: Boolean,
    groupedByStatus: Boolean,
    statusFilterMode: Int,
    listener: AttendanceCalendarActionListener
) {
    val monthTitle = "${year}年${month + 1}月"
    val weekTitles = listOf("一", "二", "三", "四", "五", "六", "日")
    val firstOffset = firstDayOffset(year, month)
    val daysInMonth = daysInMonth(year, month)
    val totalCells = ((firstOffset + daysInMonth + 6) / 7) * 7
    val rowCount = totalCells / 7
    var showPickerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 统一的头部，根据 groupedByStatus 切换内容
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (groupedByStatus) {
                // 按状态组织：使用箭头切换筛选条件
                ArrowButton("<", showDot = false) { listener.onCycleStatusFilter() }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { listener.onCycleStatusFilter() }
                ) {
                    Text(
                        text = when (statusFilterMode) {
                            1 -> "仅ACTIVE"
                            2 -> "仅CLOSED"
                            else -> "全部"
                        },
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                TextButton(onClick = { listener.onToggleGroupMode() }) {
                    Text("按日期组织", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                ArrowButton(">", showDot = false) { listener.onCycleStatusFilter() }
            } else {
                // 按日期组织
                ArrowButton("<", prevMonthHasActive) { listener.onPrevMonthClick() }
                TextButton(
                    modifier = Modifier.weight(1f),
                    onClick = { showPickerDialog = true }
                ) {
                    Text(monthTitle, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { listener.onToggleGroupMode() }) {
                    Text("按状态组织", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
                ArrowButton(">", nextMonthHasActive) { listener.onNextMonthClick() }
            }
        }

        // 日历部分，仅在非状态分组模式下显示
        if (!groupedByStatus) {
            // 星期标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekTitles.forEach { title ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            // 日期单元格
            repeat(rowCount) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { col ->
                        val index = row * 7 + col
                        val day = index - firstOffset + 1
                        DayCell(
                            day = if (day in 1..daysInMonth) day else null,
                            selected = day == selectedDay,
                            statusColorInt = dateStatusMap[day],
                            onClick = { if (day in 1..daysInMonth) listener.onDayClick(day) }
                        )
                    }
                }
            }
        }

        // 年月选择对话框
        if (showPickerDialog) {
            YearMonthPickerDialog(
                currentYear = year,
                currentMonth = month,
                onDismiss = { showPickerDialog = false },
                onPick = { pickedYear, pickedMonth ->
                    showPickerDialog = false
                    listener.onYearMonthPicked(pickedYear, pickedMonth)
                }
            )
        }
    }
}

@Composable
private fun ArrowButton(
    text: String,
    showDot: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (showDot) {
            Box(
                modifier = Modifier
                    .align(if (text == "<") Alignment.TopStart else Alignment.TopEnd)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
                    .size(6.dp)
                    .background(Color.Red, CircleShape)
            )
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: Int?,
    selected: Boolean,
    statusColorInt: Int?,
    onClick: () -> Unit
) {
    val selectedBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val selectedBorder = MaterialTheme.colorScheme.primary
    val defaultTextColor = MaterialTheme.colorScheme.onSurface
    val statusDotColor = if (statusColorInt != null) Color(statusColorInt) else Color.Transparent

    Box(
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .padding(2.dp)
            .then(
                if (day != null) {
                    Modifier
                        .border(
                            width = if (selected) 1.dp else 0.dp,
                            color = if (selected) selectedBorder else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .background(
                            color = if (selected) selectedBackground else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onClick() }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Text(
                text = day.toString(),
                color = defaultTextColor,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
            if (statusColorInt != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 5.dp, end = 5.dp)
                        .size(6.dp)
                        .background(
                            color = statusDotColor,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

private fun firstDayOffset(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
}

private fun daysInMonth(year: Int, month: Int): Int {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)
    return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
}

@Composable
private fun YearMonthPickerDialog(
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onPick: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var pickedYear by remember { mutableStateOf(currentYear) }
    var pickedMonth by remember { mutableStateOf(currentMonth + 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onPick(pickedYear, pickedMonth - 1) }) {
                Text(text = "确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "取消")
            }
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = {
                        NumberPicker(context).apply {
                            minValue = currentYear - 5
                            maxValue = currentYear + 5
                            value = currentYear
                            wrapSelectorWheel = false
                            setOnValueChangedListener { _, _, newVal ->
                                pickedYear = newVal
                            }
                        }
                    },
                    update = {
                        it.minValue = currentYear - 5
                        it.maxValue = currentYear + 5
                        if (it.value != pickedYear) it.value = pickedYear
                    }
                )
                Text(
                    text = "年",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = {
                        NumberPicker(context).apply {
                            minValue = 1
                            maxValue = 12
                            value = currentMonth + 1
                            wrapSelectorWheel = true
                            setOnValueChangedListener { _, _, newVal ->
                                pickedMonth = newVal
                            }
                        }
                    },
                    update = {
                        it.minValue = 1
                        it.maxValue = 12
                        if (it.value != pickedMonth) it.value = pickedMonth
                    }
                )
                Text(
                    text = "月",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

package com.example.facecheck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.facecheck.R

object HomeStatsComposeBinder {
    @JvmStatic
    fun bind(
        composeView: ComposeView,
        firstValue: String,
        firstLabel: String,
        secondValue: String,
        secondLabel: String,
        thirdValue: String,
        thirdLabel: String,
        roleLabel: String,
        syncHint: String,
        onClassroomClick: Runnable,
        onAttendanceClick: Runnable,
        onSyncClick: Runnable,
    ) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            HomeDashboard(
                firstValue = firstValue,
                firstLabel = firstLabel,
                secondValue = secondValue,
                secondLabel = secondLabel,
                thirdValue = thirdValue,
                thirdLabel = thirdLabel,
                roleLabel = roleLabel,
                syncHint = syncHint,
                onClassroomClick = { onClassroomClick.run() },
                onAttendanceClick = { onAttendanceClick.run() },
                onSyncClick = { onSyncClick.run() }
            )
        }
    }
}

@Composable
private fun HomeDashboard(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
    thirdValue: String,
    thirdLabel: String,
    roleLabel: String,
    syncHint: String,
    onClassroomClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        HeroSection(roleLabel = roleLabel)
        StatsRow(
            firstValue = firstValue,
            firstLabel = firstLabel,
            secondValue = secondValue,
            secondLabel = secondLabel,
            thirdValue = thirdValue,
            thirdLabel = thirdLabel
        )
        QuickActions(
            roleLabel = roleLabel,
            onClassroomClick = onClassroomClick,
            onAttendanceClick = onAttendanceClick,
            onSyncClick = onSyncClick
        )
        SyncSection(syncHint = syncHint, onSyncClick = onSyncClick)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeroSection(roleLabel: String) {
    val primaryColor = colorResource(id = R.color.primary)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = roleLabel,
            style = MaterialTheme.typography.labelLarge,
            color = primaryColor
        )
        Text(
            text = "欢迎回到 FaceCheck",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "首页已升级为 Compose 视图，统计、快捷入口和同步状态集中展示。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsRow(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
    thirdValue: String,
    thirdLabel: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(modifier = Modifier.weight(1f), value = firstValue, label = firstLabel)
        StatCard(modifier = Modifier.weight(1f), value = secondValue, label = secondLabel)
        StatCard(modifier = Modifier.weight(1f), value = thirdValue, label = thirdLabel)
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickActions(
    roleLabel: String,
    onClassroomClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    val isTeacher = roleLabel.contains("教师")
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "快捷入口",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                ActionListItem(
                    icon = Icons.Default.AccountBox,
                    title = if (isTeacher) "班级管理" else "我的班级",
                    subtitle = if (isTeacher) "管理您的授课班级与学生" else "查看已加入的班级",
                    onClick = onClassroomClick
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                ActionListItem(
                    icon = Icons.Default.DateRange,
                    title = if (isTeacher) "考勤管理" else "我的考勤",
                    subtitle = if (isTeacher) "发布并审核签到任务" else "参与签到与查看记录",
                    onClick = onAttendanceClick
                )
                HorizontalDivider(modifier = Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                ActionListItem(
                    icon = Icons.Default.Refresh,
                    title = "数据同步",
                    subtitle = "与云端同步最新数据",
                    onClick = onSyncClick
                )
            }
        }
    }
}

@Composable
private fun ActionListItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val primaryColor = colorResource(id = R.color.primary)
    // 学习通风格的轻微淡蓝色底色圆底图标
    val iconBgColor = Color(0xFFE3F2FD) // Light blue 50
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = primaryColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "进入",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SyncSection(syncHint: String, onSyncClick: () -> Unit) {
    val primaryColor = colorResource(id = R.color.primary)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "数据同步",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = syncHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            )
            Button(
                onClick = onSyncClick,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text("立即同步", color = Color.White)
            }
        }
    }
}

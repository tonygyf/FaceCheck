package com.example.facecheck.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroCard(roleLabel = roleLabel)
        StatsRow(
            firstValue = firstValue,
            firstLabel = firstLabel,
            secondValue = secondValue,
            secondLabel = secondLabel,
            thirdValue = thirdValue,
            thirdLabel = thirdLabel
        )
        QuickActions(
            onClassroomClick = onClassroomClick,
            onAttendanceClick = onAttendanceClick,
            onSyncClick = onSyncClick
        )
        SyncCard(syncHint = syncHint, onSyncClick = onSyncClick)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun HeroCard(roleLabel: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp)),
        color = Color(0xFF1E3A8A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFFBFDBFE)
            )
            Text(
                text = "欢迎回到 FaceCheck",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = "首页已升级为 Compose 视图，统计、快捷入口和同步状态集中展示。",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE2E8F0)
            )
        }
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
    ElevatedCard(modifier = modifier) {
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
                fontWeight = FontWeight.SemiBold
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
    onClassroomClick: () -> Unit,
    onAttendanceClick: () -> Unit,
    onSyncClick: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "快捷入口",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onClassroomClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("班级")
                }
                FilledTonalButton(
                    onClick = onAttendanceClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("考勤")
                }
                OutlinedButton(
                    onClick = onSyncClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("同步")
                }
            }
        }
    }
}

@Composable
private fun SyncCard(syncHint: String, onSyncClick: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "数据同步",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = syncHint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSyncClick) {
                Text("立即同步")
            }
        }
    }
}

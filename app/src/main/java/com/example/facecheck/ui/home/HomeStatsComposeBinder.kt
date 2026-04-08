package com.example.facecheck.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    ) {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        composeView.setContent {
            HomeStatsRow(
                firstValue = firstValue,
                firstLabel = firstLabel,
                secondValue = secondValue,
                secondLabel = secondLabel,
                thirdValue = thirdValue,
                thirdLabel = thirdLabel
            )
        }
    }
}

@Composable
private fun HomeStatsRow(
    firstValue: String,
    firstLabel: String,
    secondValue: String,
    secondLabel: String,
    thirdValue: String,
    thirdLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

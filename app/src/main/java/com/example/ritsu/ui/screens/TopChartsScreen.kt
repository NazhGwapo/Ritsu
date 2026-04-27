package com.example.ritsu.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ritsu.ui.cards.ChartListItem
import com.example.ritsu.ui.cards.TopChartItem

@Composable
fun TopChartsScreen(
    onChartClick: (TopChartItem) -> Unit,
    topCharts: List<TopChartItem>
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(topCharts) { chart ->
                ChartListItem(chart = chart, onClick = { onChartClick(chart) })
            }
        }
    }
}

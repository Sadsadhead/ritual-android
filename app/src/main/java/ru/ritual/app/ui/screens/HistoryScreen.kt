package ru.ritual.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ru.ritual.app.domain.model.RunRecord
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime

@Composable
fun HistoryScreen(records: List<RunRecord>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
            bottom = 20.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text("ИСТОРИЯ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(.5f))
            Spacer(Modifier.height(3.dp))
            Text("Ваш ритм", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink).padding(15.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text("НА ЭТОЙ НЕДЕЛЕ", style = MaterialTheme.typography.labelMedium, color = Lime)
                    Spacer(Modifier.height(3.dp))
                    Text("12", style = MaterialTheme.typography.headlineLarge, color = androidx.compose.ui.graphics.Color.White)
                    Text("алгоритмов завершено", style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color.White.copy(.65f))
                }
                Spacer(Modifier.weight(1f))
                Text("＋ 20%", style = MaterialTheme.typography.titleMedium, color = Lime)
            }
            Spacer(Modifier.height(14.dp))
            Text("Последние", style = MaterialTheme.typography.headlineMedium)
        }
        items(records) { record ->
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (record.percent == 100) Lime else MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Check, null, tint = Ink)
                }
                Spacer(Modifier.padding(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(record.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("${record.finishedAt} · ${record.duration}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                }
                Text("${record.percent}%", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

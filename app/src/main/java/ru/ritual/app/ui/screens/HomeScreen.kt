package ru.ritual.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.ui.AppUiState
import ru.ritual.app.ui.components.ChecklistCard
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime

private enum class HomeCatalogTab(val title: String, val symbol: String) {
    Catalog("Каталог", "▦"),
    Recent("Недавние", "↺"),
    Favorites("Избранные", "♥"),
}

@Composable
fun HomeScreen(
    state: AppUiState,
    checklists: List<Checklist>,
    recentChecklistIds: List<String>,
    onQueryChange: (String) -> Unit,
    onChecklistClick: (String) -> Unit,
    onChecklistEdit: (String) -> Unit,
    onChecklistDelete: (String) -> Unit,
    onChecklistFavorite: (String) -> Unit,
    onChecklistDuplicate: (String) -> Unit,
    onActiveRunClick: (String) -> Unit,
    onFinishAllRuns: () -> Unit,
    onAiClick: () -> Unit,
    onCreateClick: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Checklist?>(null) }
    var showFinishAllConfirmation by remember { mutableStateOf(false) }
    var selectedTabName by rememberSaveable { mutableStateOf(HomeCatalogTab.Catalog.name) }
    val selectedTab = HomeCatalogTab.entries.firstOrNull { it.name == selectedTabName } ?: HomeCatalogTab.Catalog
    val visibleChecklists = remember(checklists, recentChecklistIds, selectedTab) {
        when (selectedTab) {
            HomeCatalogTab.Catalog -> checklists
            HomeCatalogTab.Recent -> {
                val byId = checklists.associateBy(Checklist::id)
                recentChecklistIds.mapNotNull(byId::get)
            }
            HomeCatalogTab.Favorites -> checklists.filter(Checklist::isFavorite)
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp,
            bottom = 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Смысл, тег, этап…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, Modifier.size(19.dp)) },
                    trailingIcon = if (state.query.isBlank()) null else {{
                        Icon(
                            Icons.Outlined.Close,
                            "Очистить поиск",
                            Modifier.size(18.dp).clickable { onQueryChange("") },
                        )
                    }},
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.size(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(
                        modifier = Modifier.size(39.dp).clip(RoundedCornerShape(10.dp)).background(Ink).clickable(onClick = onCreateClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Создать вручную", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier.size(39.dp).clip(RoundedCornerShape(10.dp)).background(Lime).clickable(onClick = onAiClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = "Создать с ИИ", tint = Ink, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        if (state.activeRuns.isNotEmpty() && state.preferences.showActiveRunOnHome) {
            item {
                ActiveRunsStrip(
                    runs = state.activeRuns,
                    showRange = state.preferences.showProgressRange,
                    onClick = onActiveRunClick,
                    onFinishAll = { showFinishAllConfirmation = true },
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
                    .clip(RoundedCornerShape(11.dp)).background(MaterialTheme.colorScheme.surface).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                HomeCatalogTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    Row(
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Ink else Color.Transparent)
                            .clickable { selectedTabName = tab.name }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(tab.symbol, style = MaterialTheme.typography.labelLarge, color = if (selected) Lime else Ink.copy(.55f))
                        Spacer(Modifier.size(5.dp))
                        Text(tab.title, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else Ink)
                    }
                }
            }
        }
        if (visibleChecklists.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 18.dp)
                        .clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(when (selectedTab) { HomeCatalogTab.Favorites -> "♡"; HomeCatalogTab.Recent -> "↺"; else -> "⌕" }, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        when {
                            state.query.isNotBlank() -> "Ничего не найдено"
                            selectedTab == HomeCatalogTab.Favorites -> "Избранных алгоритмов пока нет"
                            selectedTab == HomeCatalogTab.Recent -> "Запущенные алгоритмы появятся здесь"
                            else -> "Алгоритмов пока нет"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        } else {
            items(visibleChecklists, key = Checklist::id) { checklist ->
                ChecklistCard(
                    checklist = checklist,
                    onClick = { onChecklistClick(checklist.id) },
                    onEdit = { onChecklistEdit(checklist.id) },
                    onDelete = { pendingDelete = checklist },
                    onToggleFavorite = { onChecklistFavorite(checklist.id) },
                    onDuplicate = { onChecklistDuplicate(checklist.id) },
                    compact = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                )
            }
        }
    }

    pendingDelete?.let { checklist ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Удалить алгоритм?") },
            text = { Text("«${checklist.title}» будет удалён с этого устройства.") },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Отмена") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onChecklistDelete(checklist.id)
                    },
                ) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
        )
    }

    if (showFinishAllConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishAllConfirmation = false },
            title = { Text("Завершить все алгоритмы?") },
            text = { Text("Прогресс всех запущенных алгоритмов будет удалён.") },
            dismissButton = {
                TextButton(onClick = { showFinishAllConfirmation = false }) { Text("Отмена") }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishAllConfirmation = false
                    onFinishAllRuns()
                }) { Text("Завершить все", color = MaterialTheme.colorScheme.error) }
            },
        )
    }
}

@Composable
private fun ActiveRunsStrip(
    runs: List<ru.ritual.app.domain.model.ActiveAlgorithmRun>,
    showRange: Boolean,
    onClick: (String) -> Unit,
    onFinishAll: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (runs.size == 1) "В работе · 1" else "В работе · ${runs.size}",
                style = MaterialTheme.typography.labelLarge,
                color = Ink.copy(.62f),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onFinishAll, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.StopCircle, "Завершить все", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items(runs, key = { it.algorithmId }) { active ->
                Column(
                    modifier = Modifier.width(72.dp).clickable { onClick(active.algorithmId) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawCircle(color = Ink.copy(alpha = .1f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f))
                            drawArc(
                                color = Color(active.accentArgb),
                                startAngle = -90f,
                                sweepAngle = 360f * active.maxPercent / 100f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 9f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                            )
                            if (active.minPercent != active.maxPercent) {
                                drawArc(
                                    color = Ink,
                                    startAngle = -90f,
                                    sweepAngle = 360f * active.minPercent / 100f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                                )
                            }
                        }
                        Box(Modifier.size(47.dp).clip(CircleShape).background(Color(active.accentArgb)), contentAlignment = Alignment.Center) {
                            Text(active.emoji, style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            if (showRange && active.minPercent != active.maxPercent) {
                                "${active.minPercent}–${active.maxPercent}%"
                            } else "${active.maxPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.BottomCenter).clip(RoundedCornerShape(7.dp)).background(Ink)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        active.title,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Ink,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

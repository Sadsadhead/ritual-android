package ru.ritual.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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

@Composable
fun HomeScreen(
    state: AppUiState,
    categories: List<String>,
    checklists: List<Checklist>,
    onQueryChange: (String) -> Unit,
    onCategoryClick: (String) -> Unit,
    onChecklistClick: (String) -> Unit,
    onChecklistEdit: (String) -> Unit,
    onChecklistDelete: (String) -> Unit,
    onAiClick: () -> Unit,
    onCreateClick: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Checklist?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 14.dp,
            bottom = 18.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ДОБРОЕ УТРО", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(.52f))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Что сделаем сегодня?",
                        style = MaterialTheme.typography.headlineLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Ink).clickable(onClick = onCreateClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "Создать вручную", tint = Color.White)
                }
            }
        }
        item {
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Найти инструкцию") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(48.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categories.forEach { category ->
                    val selected = state.selectedCategory == category
                    Text(
                        category,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) Ink else MaterialTheme.colorScheme.surface)
                            .clickable { onCategoryClick(category) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
        if (state.query.isBlank() && state.selectedCategory == "Все") {
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Ink)
                        .clickable(onClick = onAiClick)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Lime),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null, tint = Ink, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("✦ ИИ", style = MaterialTheme.typography.labelMedium, color = Lime)
                        Text("Задача → шаги", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Открыть", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(if (state.query.isBlank()) "Ваши алгоритмы" else "Результаты", style = MaterialTheme.typography.headlineMedium)
                Text("${checklists.size}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground.copy(.45f))
            }
        }
        if (checklists.isEmpty()) {
            item {
                Text(
                    "Ничего не найдено. Попробуйте другой запрос.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(.55f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 32.dp),
                )
            }
        } else {
            items(checklists, key = Checklist::id) { checklist ->
                ChecklistCard(
                    checklist = checklist,
                    onClick = { onChecklistClick(checklist.id) },
                    onEdit = { onChecklistEdit(checklist.id) },
                    onDelete = { pendingDelete = checklist },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
}

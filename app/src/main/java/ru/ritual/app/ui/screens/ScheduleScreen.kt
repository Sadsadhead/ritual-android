package ru.ritual.app.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.ritual.app.domain.model.AppPreferences
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.RecurrenceRule
import ru.ritual.app.domain.model.RepeatFrequency
import ru.ritual.app.domain.model.ScheduleItem
import ru.ritual.app.domain.model.ScheduleAiSuggestion
import ru.ritual.app.domain.model.ScheduleItemType
import ru.ritual.app.domain.model.ScheduleOccurrence
import ru.ritual.app.domain.model.ScheduleViewMode
import ru.ritual.app.domain.model.occurrencesBetween
import ru.ritual.app.ui.theme.Apricot
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lavender
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.ui.theme.Sky
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import androidx.core.content.ContextCompat

private val ruLocale = Locale.forLanguageTag("ru")
private val dateLabelFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", ruLocale)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private data class ScopeAction(val occurrence: ScheduleOccurrence, val delete: Boolean)
private data class EditRequest(val occurrence: ScheduleOccurrence?, val wholeSeries: Boolean)

@Composable
fun ScheduleScreen(
    items: List<ScheduleItem>,
    algorithms: List<Checklist>,
    preferences: AppPreferences,
    isImprovingWithAi: Boolean,
    aiSuggestion: ScheduleAiSuggestion?,
    aiError: String?,
    onSave: (ScheduleItem, Long?, Boolean) -> Unit,
    onDelete: (ScheduleItem, Long, Boolean) -> Unit,
    onRunAlgorithm: (String) -> Unit,
    onImproveWithAi: (String, String, ScheduleItemType, String, List<String>, String) -> Unit,
    onClearAi: () -> Unit,
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }
    var viewMode by remember(preferences.calendarDefaultView) { mutableStateOf(preferences.calendarDefaultView) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var editRequest by remember { mutableStateOf<EditRequest?>(null) }
    var scopeAction by remember { mutableStateOf<ScopeAction?>(null) }

    val filteredItems = remember(items, selectedCategory, selectedTag, preferences.calendarShowNotes) {
        items.filter { item ->
            (preferences.calendarShowNotes || item.type != ScheduleItemType.Note) &&
                (selectedCategory == null || item.category == selectedCategory) &&
                (selectedTag == null || selectedTag in item.tags)
        }
    }
    val categories = remember(items) { items.map(ScheduleItem::category).filter(String::isNotBlank).distinct().sorted() }
    val tags = remember(items) { items.flatMap(ScheduleItem::tags).distinct().sorted() }
    val dayOccurrences = remember(filteredItems, selectedDate) {
        filteredItems.occurrencesOn(selectedDate)
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("РАСПИСАНИЕ", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.5f))
                Text("Ваше время", style = MaterialTheme.typography.headlineLarge)
            }
            IconButton(onClick = { selectedDate = LocalDate.now(); visibleMonth = YearMonth.now() }) {
                Icon(Icons.Outlined.Schedule, "Сегодня")
            }
            IconButton(onClick = { editRequest = EditRequest(null, true) }) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, "Создать", tint = Color.White, modifier = Modifier.size(19.dp))
                }
            }
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(ScheduleViewMode.entries) { mode ->
                FilterChip(selected = viewMode == mode, onClick = { viewMode = mode }, label = { Text(mode.title) })
            }
        }

        if (categories.isNotEmpty() || tags.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item {
                    AssistChip(
                        onClick = { selectedCategory = null; selectedTag = null },
                        label = { Text(if (selectedCategory == null && selectedTag == null) "Все" else "Сбросить") },
                    )
                }
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category.takeUnless { it == selectedCategory } },
                        label = { Text(category) },
                    )
                }
                items(tags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag.takeUnless { it == selectedTag } },
                        label = { Text("#$tag") },
                        leadingIcon = { Icon(Icons.Outlined.Tag, null, Modifier.size(14.dp)) },
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                when (viewMode) {
                    ScheduleViewMode.Month -> MonthView(
                        month = visibleMonth,
                        selectedDate = selectedDate,
                        items = filteredItems,
                        mondayFirst = preferences.calendarWeekStartsMonday,
                        onMonthChange = { delta ->
                            val next = visibleMonth.plusMonths(delta)
                            visibleMonth = next
                            selectedDate = next.atDay(selectedDate.dayOfMonth.coerceAtMost(next.lengthOfMonth()))
                        },
                        onSelect = { selectedDate = it },
                    )
                    ScheduleViewMode.Week -> WeekView(selectedDate, filteredItems, { selectedDate = it }) { selectedDate = selectedDate.plusWeeks(it) }
                    ScheduleViewMode.Day -> DayHeader(selectedDate) { selectedDate = selectedDate.plusDays(it) }
                    ScheduleViewMode.Year -> YearHeatmap(
                        year = selectedDate.year,
                        items = filteredItems,
                        highlightCurrentWeek = preferences.calendarHighlightCurrentWeek,
                        onMonth = { month ->
                            visibleMonth = YearMonth.of(selectedDate.year, month)
                            selectedDate = visibleMonth.atDay(1)
                            viewMode = ScheduleViewMode.Month
                        },
                        onYearChange = { selectedDate = selectedDate.plusYears(it) },
                    )
                }
            }

            if (viewMode != ScheduleViewMode.Year) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedDate.format(dateLabelFormatter).replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        Text("${dayOccurrences.size}", style = MaterialTheme.typography.labelLarge, color = Ink.copy(.45f))
                    }
                }
                if (dayOccurrences.isEmpty()) {
                    item {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("Свободный день", style = MaterialTheme.typography.titleMedium)
                            Text("Добавьте заметку, напоминание или мероприятие.", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.52f))
                            Spacer(Modifier.height(7.dp))
                            OutlinedButton(onClick = { editRequest = EditRequest(null, true) }) { Icon(Icons.Outlined.Add, null); Text("Добавить") }
                        }
                    }
                } else {
                    items(dayOccurrences, key = { "${it.item.id}-${it.startMillis}" }) { occurrence ->
                        ScheduleCard(
                            occurrence = occurrence,
                            algorithm = algorithms.firstOrNull { it.id == occurrence.item.algorithmId },
                            onEdit = {
                                if (occurrence.item.recurrence.frequency == RepeatFrequency.None) editRequest = EditRequest(occurrence, true)
                                else scopeAction = ScopeAction(occurrence, false)
                            },
                            onDelete = {
                                if (occurrence.item.recurrence.frequency == RepeatFrequency.None) onDelete(occurrence.item, occurrence.startMillis, true)
                                else scopeAction = ScopeAction(occurrence, true)
                            },
                            onRunAlgorithm = onRunAlgorithm,
                        )
                    }
                }
            }
        }
    }

    editRequest?.let { request ->
        ScheduleEditorSheet(
            occurrence = request.occurrence,
            wholeSeries = request.wholeSeries,
            defaultDate = selectedDate,
            algorithms = algorithms,
            categories = (categories + algorithms.map(Checklist::category) + "Другое").distinct(),
            offerSystemCalendar = preferences.calendarOfferSystemExport,
            isImprovingWithAi = isImprovingWithAi,
            aiSuggestion = aiSuggestion,
            aiError = aiError,
            onImproveWithAi = onImproveWithAi,
            onConsumeAi = onClearAi,
            onDismiss = { onClearAi(); editRequest = null },
            onSave = { item, original, whole -> onClearAi(); onSave(item, original, whole); editRequest = null },
        )
    }

    scopeAction?.let { action ->
        AlertDialog(
            onDismissRequest = { scopeAction = null },
            title = { Text(if (action.delete) "Удалить событие" else "Изменить событие") },
            text = { Text("Это событие входит в повторяющуюся цепочку.") },
            confirmButton = {
                TextButton(onClick = {
                    if (action.delete) onDelete(action.occurrence.item, action.occurrence.startMillis, false)
                    else editRequest = EditRequest(action.occurrence, false)
                    scopeAction = null
                }) { Text("Только это") }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (action.delete) onDelete(action.occurrence.item, action.occurrence.startMillis, true)
                    else editRequest = EditRequest(action.occurrence, true)
                    scopeAction = null
                }) { Text("Всю цепочку") }
            },
        )
    }
}

@Composable
private fun MonthView(
    month: YearMonth,
    selectedDate: LocalDate,
    items: List<ScheduleItem>,
    mondayFirst: Boolean,
    onMonthChange: (Long) -> Unit,
    onSelect: (LocalDate) -> Unit,
) {
    val firstDay = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    val gridStart = month.atDay(1).with(TemporalAdjusters.previousOrSame(firstDay))
    val dates = (0L until 42L).map(gridStart::plusDays)
    val counts = remember(items, month) {
        val from = month.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val until = month.atEndOfMonth().atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        items.occurrencesBetween(from, until).groupingBy { it.startMillis.localDate() }.eachCount()
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Предыдущий месяц") }
            Text(
                month.month.getDisplayName(TextStyle.FULL_STANDALONE, ruLocale).replaceFirstChar(Char::uppercase) + " ${month.year}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { onMonthChange(1) }) { Icon(Icons.Outlined.ChevronRight, "Следующий месяц") }
        }
        val weekdays = (0L until 7L).map { LocalDate.of(2024, 1, if (mondayFirst) 1 else 7).plusDays(it) }
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEach { day ->
                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, ruLocale), style = MaterialTheme.typography.labelSmall, color = Ink.copy(.45f), modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        dates.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val selected = date == selectedDate
                    val count = counts[date] ?: 0
                    Column(
                        Modifier.weight(1f).aspectRatio(.9f).padding(2.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Ink else Color.Transparent).clickable { onSelect(date) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text("${date.dayOfMonth}", style = MaterialTheme.typography.labelLarge, color = when { selected -> Color.White; date.month != month.month -> Ink.copy(.25f); else -> Ink })
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            repeat(count.coerceAtMost(3)) { Box(Modifier.size(3.dp).clip(CircleShape).background(if (selected) Lime else Lavender)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekView(selected: LocalDate, items: List<ScheduleItem>, onSelect: (LocalDate) -> Unit, onMove: (Long) -> Unit) {
    val monday = selected.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMove(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Предыдущая неделя") }
            Text("Неделя ${monday.dayOfMonth}–${monday.plusDays(6).dayOfMonth}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            IconButton(onClick = { onMove(1) }) { Icon(Icons.Outlined.ChevronRight, "Следующая неделя") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(7) { index ->
                val date = monday.plusDays(index.toLong())
                val count = items.occurrencesOn(date).size
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (date == selected) Ink else MaterialTheme.colorScheme.background)
                        .clickable { onSelect(date) }.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.NARROW, ruLocale), style = MaterialTheme.typography.labelSmall, color = if (date == selected) Color.White.copy(.65f) else Ink.copy(.45f))
                    Text("${date.dayOfMonth}", style = MaterialTheme.typography.titleMedium, color = if (date == selected) Color.White else Ink)
                    Box(Modifier.size(if (count > 0) 5.dp else 3.dp).clip(CircleShape).background(if (count > 0) Lime else Color.Transparent))
                }
            }
        }
    }
}

@Composable
private fun DayHeader(selected: LocalDate, onMove: (Long) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onMove(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Предыдущий день") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(selected.dayOfWeek.getDisplayName(TextStyle.FULL, ruLocale).replaceFirstChar(Char::uppercase), style = MaterialTheme.typography.labelLarge, color = Ink.copy(.5f))
            Text(selected.format(dateLabelFormatter), style = MaterialTheme.typography.titleLarge)
        }
        IconButton(onClick = { onMove(1) }) { Icon(Icons.Outlined.ChevronRight, "Следующий день") }
    }
}

@Composable
private fun YearHeatmap(
    year: Int,
    items: List<ScheduleItem>,
    highlightCurrentWeek: Boolean,
    onMonth: (Int) -> Unit,
    onYearChange: (Long) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val firstDay = LocalDate.of(year, 1, 1)
    val lastDay = LocalDate.of(year, 12, 31)
    val gridStart = firstDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val gridEnd = lastDay.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val weeks = remember(year) {
        generateSequence(gridStart) { previous -> previous.plusWeeks(1).takeIf { it <= gridEnd } }.toList()
    }
    val from = firstDay.atStartOfDay(zone).toInstant().toEpochMilli()
    val until = lastDay.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
    val byDay = remember(items, year) {
        items.occurrencesBetween(from, until).groupingBy { it.startMillis.localDate() }.eachCount()
    }
    val total = byDay.values.sum()
    val currentWeek = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val quarterWeeks = remember(weeks) {
        (0 until 4).map { quarter ->
            weeks.filter { weekStart -> (weekStart.plusDays(3).monthValue - 1) / 3 == quarter }
        }
    }
    val heatColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .58f),
        Color(0xFFE2F3A2),
        Lime,
        Color(0xFFA5D84D),
        Color(0xFF4D8C3E),
    )
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onYearChange(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Предыдущий год") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$year · неделя = ячейка", style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Text("$total записей и напоминаний", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.45f))
            }
            IconButton(onClick = { onYearChange(1) }) { Icon(Icons.Outlined.ChevronRight, "Следующий год") }
        }
        Spacer(Modifier.height(8.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface).padding(10.dp)) {
            Text("Каждая ячейка — одна неделя", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.44f))
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 4.dp
                val labelWidth = 22.dp
                val cellSize = ((maxWidth - labelWidth - gap * 13) / 14).coerceAtLeast(14.dp)
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    quarterWeeks.forEachIndexed { quarterIndex, quarter ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(gap)) {
                            Text(
                                listOf("I", "II", "III", "IV")[quarterIndex],
                                style = MaterialTheme.typography.labelSmall,
                                color = Ink.copy(.42f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(labelWidth),
                            )
                            repeat(14) { weekIndex ->
                                val weekStart = quarter.getOrNull(weekIndex)
                                if (weekStart == null) {
                                    Spacer(Modifier.size(cellSize))
                                } else {
                                    val daysInYear = (0L..6L).map(weekStart::plusDays).filter { it.year == year }
                                    val count = daysInYear.sumOf { byDay[it] ?: 0 }
                                    val level = when {
                                        count == 0 -> 0
                                        count == 1 -> 1
                                        count <= 3 -> 2
                                        count <= 6 -> 3
                                        else -> 4
                                    }
                                    val isCurrent = highlightCurrentWeek && weekStart == currentWeek
                                    val first = daysInYear.first()
                                    val last = daysInYear.last()
                                    Box(
                                        Modifier
                                            .size(cellSize)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(heatColors[level])
                                            .then(if (isCurrent) Modifier.border(2.dp, Ink, RoundedCornerShape(4.dp)) else Modifier)
                                            .semantics {
                                                contentDescription = "${first.dayOfMonth} ${first.month.getDisplayName(TextStyle.SHORT, ruLocale)} — ${last.dayOfMonth} ${last.month.getDisplayName(TextStyle.SHORT, ruLocale)}: $count"
                                            }
                                            .clickable { onMonth(daysInYear[daysInYear.size / 2].monthValue) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("меньше", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.42f))
            heatColors.forEach { color -> Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color)) }
            Text("больше", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.42f))
        }
    }
}

@Composable
private fun ScheduleCard(
    occurrence: ScheduleOccurrence,
    algorithm: Checklist?,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRunAlgorithm: (String) -> Unit,
) {
    val item = occurrence.item
    val accent = Color(item.colorArgb)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)) {
        Box(Modifier.width(5.dp).height(if (algorithm != null) 122.dp else 94.dp).background(accent))
        Column(Modifier.weight(1f).padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (item.type) { ScheduleItemType.Reminder -> Icons.Outlined.Notifications; ScheduleItemType.Event -> Icons.Outlined.Event; ScheduleItemType.Note -> Icons.AutoMirrored.Outlined.Notes },
                    null, tint = accent, modifier = Modifier.size(17.dp),
                )
                Spacer(Modifier.size(6.dp))
                Text(if (item.allDay) "Весь день" else occurrence.startMillis.localTime(), style = MaterialTheme.typography.labelLarge, color = Ink.copy(.55f))
                if (item.recurrence.frequency != RepeatFrequency.None) {
                    Spacer(Modifier.size(6.dp)); Icon(Icons.Outlined.Repeat, null, tint = Ink.copy(.35f), modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.Edit, "Редактировать", Modifier.size(15.dp)) }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) { Icon(Icons.Outlined.DeleteOutline, "Удалить", Modifier.size(15.dp)) }
            }
            Text(item.title.ifBlank { item.type.title }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall, color = Ink.copy(.52f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (algorithm != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { onRunAlgorithm(algorithm.id) }, modifier = Modifier.height(34.dp), contentPadding = PaddingValues(horizontal = 9.dp)) {
                    Icon(Icons.Outlined.PlayArrow, null, Modifier.size(15.dp)); Spacer(Modifier.size(4.dp)); Text("${algorithm.emoji} ${algorithm.title}", maxLines = 1)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleEditorSheet(
    occurrence: ScheduleOccurrence?,
    wholeSeries: Boolean,
    defaultDate: LocalDate,
    algorithms: List<Checklist>,
    categories: List<String>,
    offerSystemCalendar: Boolean,
    isImprovingWithAi: Boolean,
    aiSuggestion: ScheduleAiSuggestion?,
    aiError: String?,
    onImproveWithAi: (String, String, ScheduleItemType, String, List<String>, String) -> Unit,
    onConsumeAi: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (ScheduleItem, Long?, Boolean) -> Unit,
) {
    val context = LocalContext.current
    val source = occurrence?.item
    val editStart = if (source != null && !wholeSeries) occurrence.startMillis else source?.startMillis
    val initialDate = editStart?.localDate() ?: defaultDate
    val initialTime = editStart?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() } ?: LocalTime.now().withSecond(0).withNano(0)
    var title by remember(source, wholeSeries) { mutableStateOf(source?.title.orEmpty()) }
    var description by remember(source, wholeSeries) { mutableStateOf(source?.description.orEmpty()) }
    var type by remember(source, wholeSeries) { mutableStateOf(source?.type ?: ScheduleItemType.Reminder) }
    var date by remember(source, wholeSeries) { mutableStateOf(initialDate) }
    var hour by remember(source, wholeSeries) { mutableStateOf("%02d".format(initialTime.hour)) }
    var minute by remember(source, wholeSeries) { mutableStateOf("%02d".format(initialTime.minute)) }
    var duration by remember(source, wholeSeries) {
        val initialDuration = source?.let { current -> current.endMillis?.minus(current.startMillis) } ?: 3_600_000L
        mutableStateOf((initialDuration / 60_000L).coerceAtLeast(1L).toString())
    }
    var allDay by remember(source, wholeSeries) { mutableStateOf(source?.allDay ?: false) }
    var category by remember(source, wholeSeries) { mutableStateOf(source?.category ?: "Другое") }
    var tags by remember(source, wholeSeries) { mutableStateOf(source?.tags?.joinToString(", ").orEmpty()) }
    var algorithmId by remember(source, wholeSeries) { mutableStateOf(source?.algorithmId) }
    var frequency by remember(source, wholeSeries) { mutableStateOf(if (wholeSeries) source?.recurrence?.frequency ?: RepeatFrequency.None else RepeatFrequency.None) }
    var interval by remember(source, wholeSeries) { mutableStateOf(if (wholeSeries) (source?.recurrence?.interval ?: 1).toString() else "1") }
    var weekdays by remember(source, wholeSeries) { mutableStateOf(if (wholeSeries) source?.recurrence?.weekdays.orEmpty() else emptyList()) }
    var monthDays by remember(source, wholeSeries) { mutableStateOf(if (wholeSeries) source?.recurrence?.monthDays?.joinToString(", ").orEmpty() else "") }
    var untilDate by remember(source, wholeSeries) { mutableStateOf(if (wholeSeries) source?.recurrence?.untilEpochDay?.let(LocalDate::ofEpochDay) else null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showUntilPicker by remember { mutableStateOf(false) }
    var typeMenu by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    var repeatMenu by remember { mutableStateOf(false) }
    var algorithmMenu by remember { mutableStateOf(false) }
    var showAiPreferences by remember { mutableStateOf(false) }
    var aiPreferences by remember { mutableStateOf("") }
    val palette = listOf(Apricot, Lavender, Sky, Lime, Color(0xFFFFC8D6), Color(0xFFBFE7D2))
    var selectedColorArgb by remember(source, wholeSeries) {
        mutableIntStateOf(source?.colorArgb ?: when (source?.type ?: ScheduleItemType.Reminder) {
            ScheduleItemType.Reminder -> Apricot.toArgb()
            ScheduleItemType.Event -> Lavender.toArgb()
            ScheduleItemType.Note -> Sky.toArgb()
        })
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val exactAlarmPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    val enteredTags = tags.split(',', ';').map(String::trim).map { it.removePrefix("#") }.filter(String::isNotBlank).distinct()
    val suggestions = algorithms.sortedByDescending { algorithm ->
        (if (algorithm.category == category) 4 else 0) + algorithm.tags.count { it in enteredTags }
    }.filter { it.category == category || it.tags.any(enteredTags::contains) }.take(3)

    LaunchedEffect(aiSuggestion) {
        aiSuggestion?.let { suggestion ->
            title = suggestion.title
            description = suggestion.description
            category = suggestion.category
            tags = suggestion.tags.joinToString(", ")
            onConsumeAi()
        }
    }

    fun buildItem(): ScheduleItem {
        val start = date.atTime(hour.toIntOrNull()?.coerceIn(0, 23) ?: 9, minute.toIntOrNull()?.coerceIn(0, 59) ?: 0)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val durationMillis = duration.toLongOrNull()?.coerceAtLeast(1L)?.times(60_000L) ?: 3_600_000L
        return (source ?: ScheduleItem(title = "", startMillis = start)).copy(
            title = title.trim(), description = description.trim(), type = type, startMillis = start,
            endMillis = if (type == ScheduleItemType.Note && allDay) null else start + durationMillis,
            allDay = allDay, category = category, tags = enteredTags, algorithmId = algorithmId,
            recurrence = if (wholeSeries) RecurrenceRule(
                frequency = frequency,
                interval = interval.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                weekdays = weekdays,
                monthDays = monthDays.split(',', ';').mapNotNull { it.trim().toIntOrNull()?.takeIf { day -> day in 1..31 } }.distinct(),
                untilEpochDay = untilDate?.toEpochDay(),
            ) else source?.recurrence ?: RecurrenceRule(),
            colorArgb = selectedColorArgb,
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(Color(selectedColorArgb))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(Color.White.copy(.7f)), contentAlignment = Alignment.Center) {
                        Icon(
                            when (type) { ScheduleItemType.Reminder -> Icons.Outlined.Notifications; ScheduleItemType.Event -> Icons.Outlined.Event; ScheduleItemType.Note -> Icons.AutoMirrored.Outlined.Notes },
                            null,
                            tint = Ink,
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (source == null) "Новое в расписании" else if (wholeSeries) "Редактирование цепочки" else "Редактирование события", style = MaterialTheme.typography.titleLarge)
                        Text("Можно связать с алгоритмом", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.55f))
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    ScheduleItemType.entries.forEach { value ->
                        val selected = type == value
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (selected) Ink else Color.Transparent)
                                .clickable {
                                    type = value
                                    if (source == null) selectedColorArgb = when (value) {
                                        ScheduleItemType.Reminder -> Apricot.toArgb()
                                        ScheduleItemType.Event -> Lavender.toArgb()
                                        ScheduleItemType.Note -> Sky.toArgb()
                                    }
                                }.padding(horizontal = 5.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                when (value) { ScheduleItemType.Reminder -> Icons.Outlined.Notifications; ScheduleItemType.Event -> Icons.Outlined.Event; ScheduleItemType.Note -> Icons.AutoMirrored.Outlined.Notes },
                                null,
                                Modifier.size(15.dp),
                                tint = if (selected) Lime else Ink,
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(value.title, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else Ink, maxLines = 1)
                        }
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Цвет", style = MaterialTheme.typography.labelLarge, color = Ink.copy(.55f))
                    Spacer(Modifier.weight(1f))
                    palette.forEach { color ->
                        Box(
                            Modifier.padding(start = 7.dp).size(if (selectedColorArgb == color.toArgb()) 27.dp else 23.dp)
                                .clip(CircleShape).background(color)
                                .then(if (selectedColorArgb == color.toArgb()) Modifier.border(2.dp, Ink, CircleShape) else Modifier)
                                .clickable { selectedColorArgb = color.toArgb() },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    title,
                    { title = it },
                    label = { Text("Название") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showAiPreferences = true },
                            enabled = !isImprovingWithAi,
                        ) { Icon(Icons.Outlined.AutoAwesome, "Улучшить название с ИИ", Modifier.size(18.dp)) }
                    },
                    shape = RoundedCornerShape(11.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(selectedColorArgb), focusedLabelColor = Ink),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
            item {
                OutlinedTextField(
                    description,
                    { description = it },
                    label = { Text("Описание") },
                    trailingIcon = {
                        IconButton(
                            onClick = { showAiPreferences = true },
                            enabled = !isImprovingWithAi,
                        ) { Icon(Icons.Outlined.AutoAwesome, "Улучшить описание с ИИ", Modifier.size(18.dp)) }
                    },
                    shape = RoundedCornerShape(11.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(selectedColorArgb), focusedLabelColor = Ink),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
            }
            item {
                Button(
                    onClick = { showAiPreferences = true },
                    enabled = !isImprovingWithAi && (title.isNotBlank() || description.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColorArgb), contentColor = Ink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) {
                    if (isImprovingWithAi) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = Ink)
                    else Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(17.dp), tint = Ink)
                    Spacer(Modifier.size(7.dp))
                    Text(if (isImprovingWithAi) "YandexGPT улучшает…" else "Улучшить с помощью ИИ")
                }
                if (!aiError.isNullOrBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(aiError, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.CalendarMonth, null); Spacer(Modifier.size(5.dp)); Text(date.format(dateLabelFormatter)) }
                    Spacer(Modifier.size(8.dp)); Text("Весь день"); Spacer(Modifier.size(5.dp)); Switch(allDay, { allDay = it })
                }
            }
            if (!allDay) item {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Ч") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(82.dp), singleLine = true)
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, label = { Text("Мин") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.width(94.dp), singleLine = true)
                    }
                    OutlinedTextField(duration, { duration = it.filter(Char::isDigit).take(4) }, label = { Text("Длительность, мин") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            }
            item {
                Box {
                    OutlinedTextField(category, {}, readOnly = true, label = { Text("Категория") }, trailingIcon = { IconButton({ categoryMenu = true }) { Icon(Icons.Outlined.ExpandMore, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(categoryMenu, { categoryMenu = false }) { categories.forEach { value -> DropdownMenuItem({ Text(value) }, { category = value; categoryMenu = false }) } }
                }
            }
            item { OutlinedTextField(tags, { tags = it }, label = { Text("Теги") }, leadingIcon = { Icon(Icons.Outlined.Tag, null) }, placeholder = { Text("работа, важно") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
            item {
                Box {
                    val algorithm = algorithms.firstOrNull { it.id == algorithmId }
                    OutlinedTextField(algorithm?.let { "${it.emoji} ${it.title}" } ?: "Не прикреплён", {}, readOnly = true, label = { Text("Алгоритм") }, trailingIcon = { IconButton({ algorithmMenu = true }) { Icon(Icons.Outlined.ExpandMore, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(algorithmMenu, { algorithmMenu = false }) {
                        DropdownMenuItem({ Text("Без алгоритма") }, { algorithmId = null; algorithmMenu = false })
                        algorithms.forEach { value -> DropdownMenuItem({ Text("${value.emoji} ${value.title}") }, { algorithmId = value.id; algorithmMenu = false }) }
                    }
                }
            }
            if (suggestions.isNotEmpty()) item {
                Column {
                    Text("Подходящие алгоритмы", style = MaterialTheme.typography.labelLarge, color = Ink.copy(.55f))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        suggestions.forEach { algorithm -> AssistChip(onClick = { algorithmId = algorithm.id }, label = { Text("${algorithm.emoji} ${algorithm.title}", maxLines = 1) }) }
                    }
                }
            }
            if (wholeSeries) item {
                Box {
                    OutlinedTextField(frequency.title, {}, readOnly = true, label = { Text("Повтор") }, leadingIcon = { Icon(Icons.Outlined.Repeat, null) }, trailingIcon = { IconButton({ repeatMenu = true }) { Icon(Icons.Outlined.ExpandMore, null) } }, modifier = Modifier.fillMaxWidth())
                    DropdownMenu(repeatMenu, { repeatMenu = false }) { RepeatFrequency.entries.forEach { value -> DropdownMenuItem({ Text(value.title) }, { frequency = value; repeatMenu = false }) } }
                }
            }
            if (wholeSeries && frequency != RepeatFrequency.None) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(interval, { interval = it.filter(Char::isDigit).take(2) }, label = { Text("Интервал повтора") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (frequency == RepeatFrequency.Weekly) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            DayOfWeek.entries.forEach { day ->
                                FilterChip(selected = day.value in weekdays, onClick = { weekdays = if (day.value in weekdays) weekdays - day.value else (weekdays + day.value).sorted() }, label = { Text(day.getDisplayName(TextStyle.NARROW, ruLocale)) })
                            }
                        }
                    }
                    if (frequency == RepeatFrequency.Monthly) OutlinedTextField(monthDays, { monthDays = it }, label = { Text("Числа месяца") }, placeholder = { Text("1, 15, 28") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { showUntilPicker = true }) { Text(untilDate?.format(dateLabelFormatter) ?: "Дата окончания") }
                        if (untilDate != null) TextButton(onClick = { untilDate = null }) { Text("Без окончания") }
                    }
                }
            }
            item {
                val built = buildItem()
                Button(
                    onClick = {
                        if (
                            built.type == ScheduleItemType.Reminder &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        if (
                            built.type == ScheduleItemType.Reminder &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            !context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                        ) {
                            runCatching {
                                exactAlarmPermission.launch(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:${context.packageName}")),
                                )
                            }
                        }
                        onSave(built, occurrence?.originalStartMillis, wholeSeries)
                    },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) { Text("Сохранить") }
                if (offerSystemCalendar && type != ScheduleItemType.Note) {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(onClick = { openSystemCalendar(context, buildItem()) }, enabled = title.isNotBlank(), modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, Modifier.size(17.dp)); Spacer(Modifier.size(6.dp)); Text("Добавить в системный календарь")
                    }
                }
            }
        }
    }

    if (showAiPreferences) {
        AlertDialog(
            onDismissRequest = { showAiPreferences = false },
            icon = { Icon(Icons.Outlined.AutoAwesome, null, tint = Ink) },
            title = { Text("Пожелания к улучшению") },
            text = {
                Column {
                    Text("Можно ничего не писать — ИИ сам сделает запись яснее и аккуратнее.", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.58f))
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = aiPreferences,
                        onValueChange = { aiPreferences = it },
                        label = { Text("Необязательно") },
                        placeholder = { Text("Короче, официальнее, добавить подготовку…") },
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            dismissButton = { TextButton(onClick = { showAiPreferences = false }) { Text("Отмена") } },
            confirmButton = {
                Button(
                    onClick = {
                        showAiPreferences = false
                        onImproveWithAi(title, description, type, category, enteredTags, aiPreferences.trim())
                        aiPreferences = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                ) { Text("Улучшить") }
            },
        )
    }

    if (showDatePicker) ScheduleDatePicker(date, { date = it; showDatePicker = false }, { showDatePicker = false })
    if (showUntilPicker) ScheduleDatePicker(untilDate ?: date, { untilDate = it; showUntilPicker = false }, { showUntilPicker = false })
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ScheduleDatePicker(initial: LocalDate, onSelect: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onSelect(Instant.ofEpochMilli(it).atZone(ZoneId.of("UTC")).toLocalDate()) } }) { Text("Готово") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    ) { DatePicker(state = state) }
}

private fun List<ScheduleItem>.occurrencesOn(date: LocalDate): List<ScheduleOccurrence> {
    val zone = ZoneId.systemDefault()
    return occurrencesBetween(
        date.atStartOfDay(zone).toInstant().toEpochMilli(),
        date.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli(),
        zone,
    )
}

private fun Long.localDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
private fun Long.localTime(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)

private fun openSystemCalendar(context: Context, item: ScheduleItem) {
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, item.startMillis)
        .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, item.endMillis ?: item.startMillis + 3_600_000L)
        .putExtra(CalendarContract.Events.TITLE, item.title)
        .putExtra(CalendarContract.Events.DESCRIPTION, item.description)
        .putExtra(CalendarContract.Events.ALL_DAY, item.allDay)
    item.recurrence.toRRule()?.let { intent.putExtra(CalendarContract.Events.RRULE, it) }
    runCatching { context.startActivity(intent) }
}

private fun RecurrenceRule.toRRule(): String? {
    val frequencyPart = when (frequency) {
        RepeatFrequency.None -> return null
        RepeatFrequency.Daily -> "DAILY"
        RepeatFrequency.Weekdays, RepeatFrequency.Weekly -> "WEEKLY"
        RepeatFrequency.Monthly -> "MONTHLY"
        RepeatFrequency.Yearly -> "YEARLY"
    }
    val days = when (frequency) {
        RepeatFrequency.Weekdays -> listOf(1, 2, 3, 4, 5)
        RepeatFrequency.Weekly -> weekdays
        else -> emptyList()
    }
    val dayCodes = listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")
    return buildString {
        append("FREQ=$frequencyPart;INTERVAL=${interval.coerceAtLeast(1)}")
        if (days.isNotEmpty()) append(";BYDAY=${days.mapNotNull { dayCodes.getOrNull(it - 1) }.joinToString(",")}")
        if (monthDays.isNotEmpty()) append(";BYMONTHDAY=${monthDays.joinToString(",")}")
        untilEpochDay?.let { append(";UNTIL=${LocalDate.ofEpochDay(it).format(DateTimeFormatter.BASIC_ISO_DATE)}T235959Z") }
    }
}

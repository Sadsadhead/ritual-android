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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.ChecklistStep
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GeneratedStep
import ru.ritual.app.domain.model.StepType
import ru.ritual.app.ui.components.MarkdownText
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lavender
import ru.ritual.app.ui.theme.Lime

private enum class DiffKind { Added, Changed, Removed, Unchanged }

private data class StepDiff(
    val kind: DiffKind,
    val old: ChecklistStep?,
    val new: GeneratedStep?,
)

@Composable
fun ImprovementReviewScreen(
    original: Checklist,
    proposal: GeneratedChecklist,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
    val oldById = original.steps.associateBy(ChecklistStep::id)
    val newById = proposal.steps.associateBy(GeneratedStep::id)
    val diffs = proposal.steps.map { new ->
        val old = oldById[new.id]
        StepDiff(if (old == null) DiffKind.Added else if (old.matches(new)) DiffKind.Unchanged else DiffKind.Changed, old, new)
    } + original.steps.filter { it.id !in newById }.map { StepDiff(DiffKind.Removed, it, null) }
    val added = diffs.count { it.kind == DiffKind.Added }
    val changed = diffs.count { it.kind == DiffKind.Changed }
    val removed = diffs.count { it.kind == DiffKind.Removed }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Закрыть сравнение") }
            Column(Modifier.weight(1f)) {
                Text("ИИ‑УЛУЧШЕНИЕ", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.48f))
                Text("Проверьте изменения", style = MaterialTheme.typography.titleLarge)
            }
            Icon(Icons.Outlined.AutoFixHigh, null, tint = Ink)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)).background(Ink).padding(14.dp)) {
                    Text("${proposal.symbol} ${proposal.title}", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text("+$added добавлено   ~$changed изменено   −$removed удалено", style = MaterialTheme.typography.bodyMedium, color = Lime)
                    if (original.title != proposal.title || original.description != proposal.description || original.category != proposal.category || original.tags != proposal.tags) {
                        Spacer(Modifier.height(9.dp))
                        Text("Метаданные тоже обновлены", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(.66f))
                        if (original.title != proposal.title) DiffLine("Название", original.title, proposal.title, dark = true)
                        if (original.description != proposal.description) DiffLine("Описание", original.description, proposal.description, dark = true)
                        if (original.category != proposal.category) DiffLine("Категория", original.category, proposal.category, dark = true)
                        if (original.tags != proposal.tags) DiffLine("Теги", original.tags.joinToString(), proposal.tags.joinToString(), dark = true)
                        if (original.durationMinutes != proposal.estimatedDurationMinutes) {
                            DiffLine("Длительность", "${original.durationMinutes} мин", "${proposal.estimatedDurationMinutes} мин", dark = true)
                        }
                    }
                }
            }
            items(diffs, key = { "${it.kind}-${it.old?.id ?: it.new?.id}" }) { diff ->
                DiffCard(diff)
            }
        }

        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.height(46.dp)) { Text("Оставить текущий") }
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).height(46.dp),
            ) {
                Text("Сохранить улучшенный")
            }
        }
    }
}

@Composable
private fun DiffCard(diff: StepDiff) {
    val background = when (diff.kind) {
        DiffKind.Added -> Lime.copy(alpha = .34f)
        DiffKind.Changed -> Lavender.copy(alpha = .34f)
        DiffKind.Removed -> MaterialTheme.colorScheme.error.copy(alpha = .09f)
        DiffKind.Unchanged -> MaterialTheme.colorScheme.surface
    }
    val icon = when (diff.kind) {
        DiffKind.Added -> Icons.Outlined.Add
        DiffKind.Changed -> Icons.Outlined.Edit
        DiffKind.Removed -> Icons.Outlined.DeleteOutline
        DiffKind.Unchanged -> null
    }
    val label = when (diff.kind) {
        DiffKind.Added -> "ДОБАВЛЕНО"
        DiffKind.Changed -> "ИЗМЕНЕНО"
        DiffKind.Removed -> "УДАЛЕНО"
        DiffKind.Unchanged -> "БЕЗ ИЗМЕНЕНИЙ"
    }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).background(background).padding(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Box(Modifier.size(26.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(.62f)), contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (diff.kind == DiffKind.Removed) MaterialTheme.colorScheme.error else Ink)
                }
                Spacer(Modifier.size(7.dp))
            }
            Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.copy(.55f))
            Spacer(Modifier.weight(1f))
            Text(diff.new?.type ?: diff.old?.type?.name.orEmpty(), style = MaterialTheme.typography.labelSmall, color = Ink.copy(.42f))
        }
        Spacer(Modifier.height(5.dp))
        when (diff.kind) {
            DiffKind.Added -> {
                Text(diff.new?.title.orEmpty(), style = MaterialTheme.typography.titleMedium)
                if (!diff.new?.description.isNullOrBlank()) MarkdownText(diff.new!!.description, style = MaterialTheme.typography.bodySmall, color = Ink.copy(.66f))
            }
            DiffKind.Removed -> {
                Text(diff.old?.title.orEmpty(), style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough), color = MaterialTheme.colorScheme.error)
                Text(diff.old?.description.orEmpty(), style = MaterialTheme.typography.bodySmall, color = Ink.copy(.48f), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            DiffKind.Changed -> {
                val old = diff.old!!
                val new = diff.new!!
                if (old.title != new.title) DiffLine("Название", old.title, new.title)
                if (old.description != new.description) DiffLine("Описание", old.description, new.description)
                if (old.type.apiName() != new.type) DiffLine("Тип", old.type.apiName(), new.type)
                if (old.options != new.options) DiffLine("Варианты", old.options.joinToString(), new.options.joinToString())
                if (old.checklistItems != new.checklistItems) DiffLine("Чек‑лист", old.checklistItems.joinToString(), new.checklistItems.joinToString())
                if (old.timerSeconds != new.timerSeconds) DiffLine("Таймер", old.timerSeconds?.toString().orEmpty(), new.timerSeconds?.toString().orEmpty())
            }
            DiffKind.Unchanged -> Text(diff.new?.title.orEmpty(), style = MaterialTheme.typography.titleMedium, color = Ink.copy(.7f))
        }
    }
}

@Composable
private fun DiffLine(label: String, old: String, new: String, dark: Boolean = false) {
    Column(Modifier.padding(top = 5.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (dark) Color.White.copy(.5f) else Ink.copy(.45f))
        if (old.isNotBlank()) Text("− $old", style = MaterialTheme.typography.bodySmall, color = if (dark) Color(0xFFFFB5B0) else MaterialTheme.colorScheme.error, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (new.isNotBlank()) Text("+ $new", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), color = if (dark) Lime else Color(0xFF2F6F3D), maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

private fun ChecklistStep.matches(new: GeneratedStep): Boolean =
    title == new.title && description == new.description && type.apiName() == new.type &&
        options == new.options && checklistItems == new.checklistItems && timerSeconds == new.timerSeconds

private fun StepType.apiName(): String = when (this) {
    StepType.Information -> "INFORMATION"
    StepType.Warning -> "WARNING"
    StepType.Checkbox -> "CHECKBOX"
    StepType.YesNo -> "YES_NO"
    StepType.SingleChoice -> "SINGLE_CHOICE"
    StepType.MultipleChoice -> "MULTIPLE_CHOICE"
    StepType.Timer -> "TIMER"
    StepType.Final -> "FINAL"
}

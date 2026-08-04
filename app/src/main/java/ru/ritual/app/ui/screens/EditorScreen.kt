package ru.ritual.app.ui.screens

import android.Manifest
import android.app.Activity
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.ForkRight
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.zIndex
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.core.content.ContextCompat
import ru.ritual.app.domain.model.AttachmentType
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.ChecklistStep
import ru.ritual.app.domain.model.StepAttachment
import ru.ritual.app.domain.model.StepType
import ru.ritual.app.media.VideoCircleRecorderActivity
import ru.ritual.app.media.SquarePhotoRecorderActivity
import ru.ritual.app.media.VoiceNoteRecorder
import ru.ritual.app.ui.theme.Apricot
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lavender
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.ui.theme.Sky
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.AlgorithmMetadataSuggestion
import ru.ritual.app.domain.model.MetadataTarget
import ru.ritual.app.domain.model.inferBranchPlacements
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.roundToInt

private enum class AttachmentKind(val label: String) {
    Photo("Фото"), VideoCircle("Видео"), Audio("Аудио"), File("Файл")
}

@Immutable
private data class DraftAttachment(val uri: Uri, val kind: AttachmentKind, val name: String = uri.lastPathSegment ?: kind.label)

@Immutable
private data class DraftStep(
    val title: String,
    val kind: String,
    val accent: Color,
    val description: String = "",
    val note: String = "",
    val attachments: List<DraftAttachment> = emptyList(),
    val checklistItems: List<String> = emptyList(),
    val options: List<String> = emptyList(),
    val timerSeconds: Int = 300,
    val id: String = UUID.randomUUID().toString(),
    val parentConditionId: String? = null,
    val parentOptionIndex: Int? = null,
)

@Immutable
private data class BlockType(
    val label: String,
    val symbol: String,
    val description: String,
    val accent: Color,
)

private data class BranchPlacement(val conditionId: String, val optionIndex: Int)

@Immutable
private data class FlowDropTarget(
    val key: String,
    val conditionId: String? = null,
    val optionIndex: Int? = null,
    val anchorStepId: String? = null,
    val afterAnchor: Boolean = true,
)

private val blockTypes = listOf(
    BlockType("Действие", "✓", "Обычный шаг с подтверждением", Sky),
    BlockType("Информация", "i", "Текст, инструкция или пояснение", Sky),
    BlockType("Чек-лист", "☑", "Несколько независимо отмечаемых пунктов", Lime),
    BlockType("Условие · Да / Нет", "◇", "Мгновенный переход после ответа", Lavender),
    BlockType("Один вариант", "○", "Выбор одного варианта", Lavender),
    BlockType("Таймер", "◷", "Фоновый отсчёт с уведомлениями", Apricot),
    BlockType("Предупреждение", "!", "Важная информация перед действием", Apricot),
    BlockType("Финал", "■", "Завершение алгоритма", Lime),
)

@Composable
fun EditorScreen(
    initialChecklist: GeneratedChecklist? = null,
    existingChecklist: Checklist? = null,
    isGeneratingMetadata: Boolean = false,
    metadataTarget: MetadataTarget? = null,
    metadataSuggestion: AlgorithmMetadataSuggestion? = null,
    isImprovingAlgorithm: Boolean = false,
    improvementStage: String? = null,
    improvementOriginal: Checklist? = null,
    improvementProposal: GeneratedChecklist? = null,
    improvementError: String? = null,
    onRequestMetadata: (String, String, List<String>, MetadataTarget) -> Unit = { _, _, _, _ -> },
    onConsumeMetadataSuggestion: () -> Unit = {},
    onImprove: (Checklist, String) -> Unit = { _, _ -> },
    onDiscardImprovement: () -> Unit = {},
    onAcceptImprovement: () -> Unit = {},
    onSave: (Checklist) -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableIntStateOf(0) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showValidationError by remember { mutableStateOf(false) }
    var previewChecklist by remember { mutableStateOf<Checklist?>(null) }
    var pendingImprovement by remember { mutableStateOf<Checklist?>(null) }
    var improvementPreferences by rememberSaveable { mutableStateOf("") }
    var pendingBranchPlacement by remember { mutableStateOf<BranchPlacement?>(null) }
    var algorithmTitle by remember(initialChecklist, existingChecklist) {
        mutableStateOf(initialChecklist?.title ?: existingChecklist?.title.orEmpty())
    }
    var algorithmDescription by remember(initialChecklist, existingChecklist) {
        mutableStateOf(initialChecklist?.description ?: existingChecklist?.description.orEmpty())
    }
    var algorithmCategory by remember(initialChecklist, existingChecklist) {
        mutableStateOf(
            initialChecklist?.category?.takeIf(String::isNotBlank)
                ?: existingChecklist?.category?.takeIf(String::isNotBlank)
                ?: "Другое",
        )
    }
    var algorithmTags by remember(initialChecklist, existingChecklist) {
        mutableStateOf((initialChecklist?.tags ?: existingChecklist?.tags.orEmpty()).joinToString(", "))
    }
    val algorithmAccent = initialChecklist?.accentArgb?.let(::Color) ?: existingChecklist?.accent
    val algorithmSymbol = initialChecklist?.symbol?.takeIf(String::isNotBlank)
        ?: existingChecklist?.emoji?.takeIf(String::isNotBlank)
    val steps = remember(initialChecklist, existingChecklist) {
        mutableStateListOf<DraftStep>().apply {
            if (initialChecklist != null) {
                val branchPlacements = initialChecklist.inferBranchPlacements()
                addAll(initialChecklist.steps.mapIndexed { index, step ->
                    val placement = branchPlacements[step.id]
                    DraftStep(
                        id = step.id,
                        title = step.title,
                        kind = if (step.checklistItems.isNotEmpty()) "Чек-лист" else step.type.toEditorLabel(),
                        accent = when {
                            step.type == "FINAL" -> Lime
                            step.type == "WARNING" -> Apricot
                            step.type == "YES_NO" || step.type.contains("CHOICE") -> Lavender
                            index % 2 == 0 -> Sky
                            else -> Apricot
                        },
                        description = step.description,
                        checklistItems = step.checklistItems,
                        options = step.options,
                        timerSeconds = step.timerSeconds ?: 300,
                        parentConditionId = placement?.first,
                        parentOptionIndex = placement?.second,
                    )
                })
            } else if (existingChecklist != null) {
                addAll(existingChecklist.steps.map { step ->
                    val label = if (step.checklistItems.isNotEmpty()) "Чек-лист" else step.type.toEditorLabel()
                    DraftStep(
                        id = step.id,
                        title = step.title,
                        kind = label,
                        accent = blockTypes.firstOrNull { it.label == label }?.accent ?: Sky,
                        description = step.description,
                        note = step.note,
                        attachments = step.attachments.map { attachment ->
                            DraftAttachment(
                                uri = Uri.parse(attachment.uri),
                                kind = when (attachment.type) {
                                    AttachmentType.Photo -> AttachmentKind.Photo
                                    AttachmentType.VideoCircle -> AttachmentKind.VideoCircle
                                    AttachmentType.Audio -> AttachmentKind.Audio
                                    AttachmentType.File -> AttachmentKind.File
                                },
                                name = attachment.name,
                            )
                        },
                        checklistItems = step.checklistItems,
                        options = step.options,
                        timerSeconds = step.timerSeconds ?: 300,
                        parentConditionId = step.parentConditionId,
                        parentOptionIndex = step.parentOptionIndex,
                    )
                })
            } else {
                // Ручное создание всегда начинается с чистого листа.
            }
        }
    }

    LaunchedEffect(initialChecklist, existingChecklist) {
        if (initialChecklist == null && existingChecklist == null) clearDraft(context)
    }

    LaunchedEffect(metadataSuggestion, metadataTarget) {
        val suggestion = metadataSuggestion ?: return@LaunchedEffect
        when (metadataTarget) {
            MetadataTarget.Title -> algorithmTitle = suggestion.title
            MetadataTarget.Description -> algorithmDescription = suggestion.description
            MetadataTarget.Classification -> {
                algorithmCategory = suggestion.category
                algorithmTags = suggestion.tags.joinToString(", ")
            }
            null -> Unit
        }
        onConsumeMetadataSuggestion()
    }

    if (improvementOriginal != null && improvementProposal != null) {
        ImprovementReviewScreen(
            original = improvementOriginal,
            proposal = improvementProposal,
            onDismiss = onDiscardImprovement,
            onAccept = onAcceptImprovement,
        )
        return
    }

    previewChecklist?.let { preview ->
        RunnerScreen(checklist = preview, onClose = { previewChecklist = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Закрыть") }
            Text(
                if (existingChecklist == null) "Новый алгоритм" else existingChecklist.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (steps.isNotEmpty()) {
                IconButton(onClick = { showResetDialog = true }) {
                    Icon(Icons.Outlined.DeleteSweep, "Начать заново")
                }
                IconButton(
                    onClick = {
                        previewChecklist = buildChecklist(
                            algorithmTitle.ifBlank { "Демо алгоритма" },
                            algorithmDescription,
                            algorithmCategory,
                            algorithmTags.split(',', ';').map(String::trim).map { it.removePrefix("#") }.filter(String::isNotBlank).distinct(),
                            steps,
                            algorithmAccent,
                            algorithmSymbol,
                            null,
                        )
                    },
                ) {
                    Icon(Icons.Outlined.PlayArrow, "Демо без сохранения")
                }
            }
            IconButton(
                onClick = {
                    if (algorithmTitle.isBlank() || steps.isEmpty()) {
                        showValidationError = true
                    } else {
                        onSave(
                            buildChecklist(
                                algorithmTitle,
                                algorithmDescription,
                                algorithmCategory,
                                algorithmTags.split(',', ';').map(String::trim).map { it.removePrefix("#") }.filter(String::isNotBlank).distinct(),
                                steps,
                                algorithmAccent,
                                algorithmSymbol,
                                existingChecklist?.id,
                            ),
                        )
                    }
                },
                enabled = steps.isNotEmpty(),
            ) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Check, "Сохранить", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        Button(
            onClick = {
                if (algorithmTitle.isBlank() || steps.isEmpty()) {
                    showValidationError = true
                } else {
                    pendingImprovement = buildChecklist(
                            algorithmTitle,
                            algorithmDescription,
                            algorithmCategory,
                            algorithmTags.split(',', ';').map(String::trim).map { it.removePrefix("#") }.filter(String::isNotBlank).distinct(),
                            steps,
                            algorithmAccent,
                            algorithmSymbol,
                            existingChecklist?.id,
                        )
                }
            },
            enabled = steps.isNotEmpty() && !isImprovingAlgorithm,
            colors = ButtonDefaults.buttonColors(containerColor = Lime.copy(.52f), contentColor = Ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp).height(46.dp),
        ) {
            if (isImprovingAlgorithm) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.size(7.dp))
                Text(improvementStage ?: "Улучшаю…")
            } else {
                Icon(Icons.Outlined.AutoFixHigh, null, Modifier.size(18.dp))
                Spacer(Modifier.size(7.dp))
                Text("Улучшить с помощью ИИ")
            }
        }
        if (improvementError != null) {
            Text(
                improvementError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(3.dp),
        ) {
            listOf("Этапы" to Icons.Outlined.DragIndicator, "Схема" to Icons.Outlined.ForkRight).forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (selectedMode == index) Ink else Color.Transparent)
                        .clickable { selectedMode = index }
                        .padding(vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.second, null, tint = if (selectedMode == index) Color.White else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(item.first, style = MaterialTheme.typography.labelLarge, color = if (selectedMode == index) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        if (selectedMode == 0) {
            StepsEditor(
                title = algorithmTitle,
                onTitleChange = { algorithmTitle = it },
                description = algorithmDescription,
                onDescriptionChange = { algorithmDescription = it },
                category = algorithmCategory,
                onCategoryChange = { algorithmCategory = it },
                tags = algorithmTags,
                onTagsChange = { algorithmTags = it },
                isGeneratingMetadata = isGeneratingMetadata,
                metadataTarget = metadataTarget,
                onGenerateTitle = { onRequestMetadata(algorithmTitle, algorithmDescription, steps.map(DraftStep::title), MetadataTarget.Title) },
                onGenerateDescription = { onRequestMetadata(algorithmTitle, algorithmDescription, steps.map(DraftStep::title), MetadataTarget.Description) },
                onClassify = { onRequestMetadata(algorithmTitle, algorithmDescription, steps.map(DraftStep::title), MetadataTarget.Classification) },
                steps = steps,
                onEdit = { editingIndex = it },
                onDelete = { index -> removeDraftStep(steps, index) },
                onAdd = { showTypePicker = true },
            )
        } else {
            FlowEditor(
                steps = steps,
                onEdit = { editingIndex = it },
                onMove = { stepId, target -> moveDraftStep(steps, stepId, target) },
                onDelete = { index -> removeDraftStep(steps, index) },
                onAddToBranch = { conditionId, optionIndex ->
                    pendingBranchPlacement = BranchPlacement(conditionId, optionIndex)
                    showTypePicker = true
                },
                onAdd = {
                    pendingBranchPlacement = null
                    showTypePicker = true
                },
            )
        }
    }

    if (showTypePicker) {
        BlockTypePicker(
            onDismiss = { showTypePicker = false; pendingBranchPlacement = null },
            onSelect = { type ->
                val requestedPlacement = pendingBranchPlacement
                val insertIndex = if (requestedPlacement != null) {
                    val lastBranchIndex = steps.indexOfLast {
                        it.parentConditionId == requestedPlacement.conditionId &&
                            (it.parentOptionIndex ?: 0) == requestedPlacement.optionIndex
                    }
                    if (lastBranchIndex >= 0) lastBranchIndex + 1
                    else (steps.indexOfFirst { it.id == requestedPlacement.conditionId } + 1).coerceAtLeast(0)
                } else if (steps.lastOrNull()?.kind == "Финал") steps.lastIndex else steps.size
                val previous = steps.getOrNull(insertIndex - 1)
                val placementConditionId = when {
                    requestedPlacement != null -> requestedPlacement.conditionId
                    previous?.isBranchCondition() == true -> previous.id
                    previous?.parentConditionId != null -> previous.parentConditionId
                    else -> null
                }
                val placementOptionIndex = when {
                    requestedPlacement != null -> requestedPlacement.optionIndex
                    previous?.isBranchCondition() == true -> 0
                    previous?.parentConditionId != null -> 0
                    else -> null
                }
                steps.add(
                    insertIndex,
                    DraftStep(
                        title = "",
                        kind = type.label,
                        accent = type.accent,
                        checklistItems = if (type.label == "Чек-лист") listOf("") else emptyList(),
                        options = when {
                            type.label.contains("Условие") -> listOf("Да", "Нет")
                            type.label.contains("вариант") -> listOf("", "")
                            else -> emptyList()
                        },
                        parentConditionId = placementConditionId,
                        parentOptionIndex = placementOptionIndex,
                    ),
                )
                showTypePicker = false
                pendingBranchPlacement = null
                editingIndex = insertIndex
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Начать с нуля?") },
            text = { Text("Все этапы текущего черновика будут удалены.") },
            confirmButton = {
                TextButton(onClick = {
                    steps.clear()
                    clearDraft(context)
                    editingIndex = null
                    selectedMode = 0
                    showResetDialog = false
                }) { Text("Очистить") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Отмена") } },
        )
    }

    if (showValidationError) {
        AlertDialog(
            onDismissRequest = { showValidationError = false },
            title = { Text("Не хватает данных") },
            text = { Text("Укажите название алгоритма и добавьте хотя бы один этап.") },
            confirmButton = { TextButton(onClick = { showValidationError = false }) { Text("Понятно") } },
        )
    }

    pendingImprovement?.let { checklist ->
        AlertDialog(
            onDismissRequest = { pendingImprovement = null },
            icon = {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Lime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoFixHigh, null, tint = Ink)
                }
            },
            title = { Text("Как улучшить алгоритм?") },
            text = {
                Column {
                    Text(
                        "Пожелание можно оставить пустым — тогда YandexGPT проведёт полный аудит самостоятельно.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(.62f),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = improvementPreferences,
                        onValueChange = { improvementPreferences = it },
                        label = { Text("Пожелания, необязательно") },
                        placeholder = { Text("Например: короче, больше проверок, мягче тон…") },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            dismissButton = { TextButton(onClick = { pendingImprovement = null }) { Text("Отмена") } },
            confirmButton = {
                Button(
                    onClick = {
                        pendingImprovement = null
                        onImprove(checklist, improvementPreferences.trim())
                        improvementPreferences = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(17.dp), tint = Lime)
                    Spacer(Modifier.size(6.dp))
                    Text("Улучшить")
                }
            },
        )
    }

    editingIndex?.let { index ->
        steps.getOrNull(index)?.let { step ->
            StepEditSheet(
                step = step,
                onDismiss = { editingIndex = null },
                onSave = { updated ->
                    steps[index] = updated
                    steps.indices.forEach { childIndex ->
                        val child = steps[childIndex]
                        if (child.parentConditionId == updated.id) {
                            steps[childIndex] = if (updated.isBranchCondition()) {
                                child.copy(parentOptionIndex = (child.parentOptionIndex ?: 0).coerceIn(0, (updated.options.size - 1).coerceAtLeast(0)))
                            } else {
                                child.copy(
                                    parentConditionId = updated.parentConditionId,
                                    parentOptionIndex = updated.parentOptionIndex,
                                )
                            }
                        }
                    }
                    editingIndex = null
                },
                onDelete = {
                    removeDraftStep(steps, index)
                    editingIndex = null
                },
            )
        }
    }
}

private fun DraftStep.isBranchCondition(): Boolean =
    kind.contains("Условие") || kind == "Один вариант"

private fun removeDraftStep(steps: SnapshotStateList<DraftStep>, index: Int) {
    val removed = steps.getOrNull(index) ?: return
    steps.indices.forEach { childIndex ->
        val child = steps[childIndex]
        if (child.parentConditionId == removed.id) {
            steps[childIndex] = child.copy(
                parentConditionId = removed.parentConditionId,
                parentOptionIndex = removed.parentOptionIndex,
            )
        }
    }
    steps.removeAt(index)
}

private fun moveDraftStep(steps: SnapshotStateList<DraftStep>, stepId: String, target: FlowDropTarget) {
    val from = steps.indexOfFirst { it.id == stepId }
    if (from < 0 || target.conditionId == stepId || target.anchorStepId == stepId) return
    val moved = steps.removeAt(from).copy(
        parentConditionId = target.conditionId,
        parentOptionIndex = target.optionIndex,
    )
    val insertion = if (target.conditionId != null) {
        val lastInBranch = steps.indexOfLast {
            it.parentConditionId == target.conditionId && (it.parentOptionIndex ?: 0) == (target.optionIndex ?: 0)
        }
        if (lastInBranch >= 0) lastInBranch + 1
        else (steps.indexOfFirst { it.id == target.conditionId } + 1).coerceAtLeast(0)
    } else {
        val anchorIndex = target.anchorStepId?.let { id -> steps.indexOfFirst { it.id == id } } ?: steps.size
        if (anchorIndex < 0) steps.size else anchorIndex + if (target.afterAnchor) 1 else 0
    }
    steps.add(insertion.coerceIn(0, steps.size), moved)
}

private fun String.toEditorLabel(): String = when (this) {
    "YES_NO" -> "Условие · Да / Нет"
    "SINGLE_CHOICE" -> "Один вариант"
    "MULTIPLE_CHOICE" -> "Один вариант"
    "INFORMATION" -> "Информация"
    "WARNING" -> "Предупреждение"
    "TIMER" -> "Таймер"
    "FINAL" -> "Финал"
    else -> "Действие"
}

private fun StepType.toEditorLabel(): String = when (this) {
    StepType.YesNo -> "Условие · Да / Нет"
    StepType.SingleChoice -> "Один вариант"
    StepType.MultipleChoice -> "Один вариант"
    StepType.Information -> "Информация"
    StepType.Warning -> "Предупреждение"
    StepType.Timer -> "Таймер"
    StepType.Final -> "Финал"
    StepType.Checkbox -> "Действие"
}

private fun buildChecklist(
    title: String,
    description: String,
    category: String,
    tags: List<String>,
    steps: List<DraftStep>,
    preferredAccent: Color?,
    preferredSymbol: String?,
    existingId: String?,
): Checklist = Checklist(
    id = existingId ?: "user-${UUID.randomUUID()}",
    title = title.trim(),
    description = description.trim(),
    category = category,
    durationMinutes = steps.sumOf { step ->
        if (step.kind == "Таймер") (step.timerSeconds / 60).coerceAtLeast(1) else 2
    }.coerceAtLeast(1),
    accent = preferredAccent ?: steps.firstOrNull()?.accent ?: Sky,
    emoji = preferredSymbol ?: blockTypes.firstOrNull { it.label == steps.firstOrNull()?.kind }?.symbol ?: "◇",
    tags = tags,
    steps = steps.mapIndexed { index, step ->
        ChecklistStep(
            id = step.id,
            eyebrow = if (step.kind == "Финал") "ГОТОВО" else "%02d · %s".format(index + 1, step.kind.uppercase()),
            title = step.title,
            description = step.description,
            type = when {
                step.kind == "Предупреждение" -> StepType.Warning
                step.kind == "Информация" -> StepType.Information
                step.kind.contains("Условие") -> StepType.YesNo
                step.kind == "Один вариант" -> StepType.SingleChoice
                step.kind == "Таймер" -> StepType.Timer
                step.kind == "Финал" -> StepType.Final
                else -> StepType.Checkbox
            },
            timerSeconds = step.timerSeconds.takeIf { step.kind == "Таймер" },
            checklistItems = step.checklistItems,
            options = step.options,
            note = step.note,
            attachments = step.attachments.map { attachment ->
                StepAttachment(
                    uri = attachment.uri.toString(),
                    type = when (attachment.kind) {
                        AttachmentKind.Photo -> AttachmentType.Photo
                        AttachmentKind.VideoCircle -> AttachmentType.VideoCircle
                        AttachmentKind.Audio -> AttachmentType.Audio
                        AttachmentKind.File -> AttachmentType.File
                    },
                    name = attachment.name,
                )
            },
            parentConditionId = step.parentConditionId,
            parentOptionIndex = step.parentOptionIndex,
        )
    },
)

private fun clearDraft(context: Context) {
    context.getSharedPreferences("editor_draft", Context.MODE_PRIVATE).edit().remove("steps").apply()
}

@Composable
private fun StepsEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    isGeneratingMetadata: Boolean,
    metadataTarget: MetadataTarget?,
    onGenerateTitle: () -> Unit,
    onGenerateDescription: () -> Unit,
    onClassify: () -> Unit,
    steps: SnapshotStateList<DraftStep>,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    onAdd: () -> Unit,
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var draggedId by remember { mutableStateOf<String?>(null) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    val editorCategories = listOf("Ежедневное", "Дом", "Кухня", "Здоровье", "Работа", "Учёба", "Путешествия", "Финансы", "Авто", "Хобби", "Другое")
    LazyColumn(
        state = listState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "editor-header") {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface).padding(12.dp),
            ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Lime), contentAlignment = Alignment.Center) {
                    Text("✦", style = MaterialTheme.typography.titleMedium, color = Ink)
                }
                Spacer(Modifier.size(9.dp))
                Column {
                    Text("Основа алгоритма", style = MaterialTheme.typography.titleLarge)
                    Text("Название, смысл и быстрый поиск", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.5f))
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Название алгоритма") },
                singleLine = true,
                trailingIcon = {
                    AiFieldButton(
                        loading = isGeneratingMetadata && metadataTarget == MetadataTarget.Title,
                        busy = isGeneratingMetadata,
                        description = "Создать название с помощью ИИ",
                        onClick = onGenerateTitle,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Краткое описание") },
                minLines = 2,
                maxLines = 3,
                trailingIcon = {
                    AiFieldButton(
                        loading = isGeneratingMetadata && metadataTarget == MetadataTarget.Description,
                        busy = isGeneratingMetadata,
                        description = "Создать описание с помощью ИИ",
                        onClick = onGenerateDescription,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            Box {
                OutlinedTextField(
                    value = category,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Категория") },
                    trailingIcon = {
                        IconButton(onClick = { categoryMenuExpanded = true }) {
                            Icon(Icons.Outlined.ExpandMore, "Выбрать категорию")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().clickable { categoryMenuExpanded = true },
                )
                DropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                    modifier = Modifier.fillMaxWidth(.86f),
                ) {
                    editorCategories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = { onCategoryChange(item); categoryMenuExpanded = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = tags,
                onValueChange = onTagsChange,
                label = { Text("Теги") },
                placeholder = { Text("быстро, утро, кухня") },
                leadingIcon = { Icon(Icons.Outlined.Tag, null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(7.dp))
            OutlinedButton(
                onClick = onClassify,
                enabled = !isGeneratingMetadata,
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Lime.copy(.42f), contentColor = Ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(42.dp),
            ) {
                if (isGeneratingMetadata && metadataTarget == MetadataTarget.Classification) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AutoAwesome, null, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.size(7.dp))
                Text("ИИ: определить категорию и теги")
            }
            }
            Spacer(Modifier.height(13.dp))
            Text("Содержание", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(5.dp))
            Text("Удерживайте ручку ≡ справа и перемещайте этап.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(.55f))
        }
        itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
            var dragOffset by remember(step.id) { mutableFloatStateOf(0f) }
            val swipeOffset = remember(step.id) { Animatable(0f) }
            val density = LocalDensity.current
            val revealWidthPx = with(density) { 82.dp.toPx() }
            val isDragging = draggedId == step.id
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFE8E6)),
            ) {
                TextButton(
                    onClick = { onDelete(index) },
                    modifier = Modifier.align(Alignment.CenterEnd).width(82.dp).fillMaxSize(),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFB3261E)),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(3.dp))
                    Text("Удалить", style = MaterialTheme.typography.labelMedium)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset { IntOffset(swipeOffset.value.roundToInt(), 0) }
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else 0f
                            alpha = if (isDragging) .88f else 1f
                            shadowElevation = if (isDragging) 12f else 0f
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .draggable(
                            orientation = Orientation.Horizontal,
                            state = rememberDraggableState { delta ->
                                coroutineScope.launch {
                                    swipeOffset.snapTo((swipeOffset.value + delta).coerceIn(-revealWidthPx, 0f))
                                }
                            },
                            onDragStopped = {
                                coroutineScope.launch {
                                    swipeOffset.animateTo(
                                        if (swipeOffset.value < -revealWidthPx * .35f) -revealWidthPx else 0f,
                                        spring(dampingRatio = .78f, stiffness = 520f),
                                    )
                                }
                            },
                        )
                        .clickable {
                            if (swipeOffset.value < 0f) coroutineScope.launch { swipeOffset.animateTo(0f) }
                            else onEdit(index)
                        }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(step.accent), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = Ink)
                }
                Spacer(Modifier.size(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(step.kind, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.52f))
                    val parent = steps.firstOrNull { it.id == step.parentConditionId }
                    if (parent != null) {
                        val branchName = parent.options.getOrNull(step.parentOptionIndex ?: 0)
                            ?: "Ветка ${(step.parentOptionIndex ?: 0) + 1}"
                        Text(
                            "↳ $branchName",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (step.note.isNotBlank() || step.attachments.isNotEmpty() || step.checklistItems.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            buildString {
                                if (step.note.isNotBlank()) append("Заметка")
                                if (step.note.isNotBlank() && step.attachments.isNotEmpty()) append(" · ")
                                if (step.attachments.isNotEmpty()) append("${step.attachments.size} влож.")
                                if ((step.note.isNotBlank() || step.attachments.isNotEmpty()) && step.checklistItems.isNotEmpty()) append(" · ")
                                if (step.checklistItems.isNotEmpty()) append("${step.checklistItems.size} пункт.")
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isDragging) step.accent.copy(.55f) else MaterialTheme.colorScheme.background)
                        .pointerInput(step.id, steps.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedId = step.id
                                    dragOffset = 0f
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffset = 0f
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffset = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount.y

                                    val layout = listState.layoutInfo
                                    val currentInfo = layout.visibleItemsInfo.firstOrNull { it.key == step.id }
                                    if (currentInfo != null) {
                                        val translatedTop = currentInfo.offset + dragOffset
                                        val translatedBottom = translatedTop + currentInfo.size
                                        val translatedCenter = (translatedTop + translatedBottom) / 2f
                                        val targetInfo = layout.visibleItemsInfo.firstOrNull { info ->
                                            info.key != step.id &&
                                                steps.any { candidate -> candidate.id == info.key } &&
                                                translatedCenter >= info.offset &&
                                                translatedCenter <= info.offset + info.size
                                        }
                                        if (targetInfo != null) {
                                            val from = steps.indexOfFirst { it.id == step.id }
                                            val to = steps.indexOfFirst { it.id == targetInfo.key }
                                            if (from >= 0 && to >= 0 && from != to) {
                                                dragOffset += currentInfo.offset - targetInfo.offset
                                                steps.add(to, steps.removeAt(from))
                                            }
                                        }

                                        val scrollDelta = when {
                                            translatedTop < layout.viewportStartOffset ->
                                                (translatedTop - layout.viewportStartOffset).coerceAtLeast(-28f)
                                            translatedBottom > layout.viewportEndOffset ->
                                                (translatedBottom - layout.viewportEndOffset).coerceAtMost(28f)
                                            else -> 0f
                                        }
                                        if (scrollDelta != 0f) {
                                            coroutineScope.launch { listState.scrollBy(scrollDelta) }
                                        }
                                    }
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.DragIndicator, "Удерживайте и перетащите", tint = MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
                }
            }
        }
        item(key = "editor-add") { AddStepButton(onAdd) }
    }
}

@Composable
private fun AiFieldButton(loading: Boolean, busy: Boolean, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = !busy) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Outlined.AutoAwesome, description, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun FlowEditor(
    steps: List<DraftStep>,
    onEdit: (Int) -> Unit,
    onMove: (stepId: String, target: FlowDropTarget) -> Unit,
    onDelete: (Int) -> Unit,
    onAddToBranch: (conditionId: String, optionIndex: Int) -> Unit,
    onAdd: () -> Unit,
) {
    var draggedStepId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dragPoint by remember { mutableStateOf(Offset.Zero) }
    var activeTarget by remember { mutableStateOf<FlowDropTarget?>(null) }
    val dropBounds = remember { mutableStateMapOf<FlowDropTarget, Rect>() }
    val updateDrag: (Offset) -> Unit = { delta ->
        dragOffset += delta
        dragPoint += delta
        val dragged = steps.firstOrNull { it.id == draggedStepId }
        activeTarget = dropBounds.entries
            .asSequence()
            .filter { (target, _) ->
                target.conditionId != draggedStepId &&
                    target.anchorStepId != draggedStepId &&
                    (dragged?.isBranchCondition() != true || target.conditionId == null)
            }
            .minByOrNull { (_, bounds) ->
                val dx = when {
                    dragPoint.x < bounds.left -> bounds.left - dragPoint.x
                    dragPoint.x > bounds.right -> dragPoint.x - bounds.right
                    else -> 0f
                }
                val dy = when {
                    dragPoint.y < bounds.top -> bounds.top - dragPoint.y
                    dragPoint.y > bounds.bottom -> dragPoint.y - bounds.bottom
                    else -> 0f
                }
                dx * dx + dy * dy
            }
            ?.key
    }
    val finishDrag = {
        val stepId = draggedStepId
        val target = activeTarget
        if (stepId != null && target != null) onMove(stepId, target)
        draggedStepId = null
        activeTarget = null
        dragOffset = Offset.Zero
        dragPoint = Offset.Zero
    }
    val cancelDrag = {
        draggedStepId = null
        activeTarget = null
        dragOffset = Offset.Zero
        dragPoint = Offset.Zero
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Блок-схема", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        if (draggedStepId != null) "Отпустите блок в подсвеченной точке."
                        else "Удерживайте блок и перенесите его в нужный узел.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(.55f),
                    )
                }
                if (steps.isNotEmpty()) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Lime.copy(.65f)), contentAlignment = Alignment.Center) {
                        Text("${steps.size}", style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (steps.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("Схема пока пустая", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Добавьте первый блок и выберите его тип.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
                Spacer(Modifier.height(14.dp))
                AddStepButton(onAdd)
            } else {
                StartPill()
                val rootSteps = steps.filter { it.parentConditionId == null }
                FlowDropZone(
                    target = FlowDropTarget(
                        key = "root-start",
                        anchorStepId = rootSteps.firstOrNull()?.id,
                        afterAnchor = false,
                    ),
                    dragActive = draggedStepId != null,
                    selected = activeTarget?.key == "root-start",
                    onBounds = { target, bounds -> dropBounds[target] = bounds },
                )
                rootSteps.forEachIndexed { rootIndex, step ->
                    BranchAwareFlowNode(
                        step = step,
                        steps = steps,
                        onEdit = onEdit,
                        draggedStepId = draggedStepId,
                        dragOffset = dragOffset,
                        activeTarget = activeTarget,
                        onDragStart = { stepId, center ->
                            draggedStepId = stepId
                            dragOffset = Offset.Zero
                            dragPoint = center
                            activeTarget = null
                        },
                        onDrag = updateDrag,
                        onDragEnd = finishDrag,
                        onDragCancel = cancelDrag,
                        onDropBounds = { target, bounds -> dropBounds[target] = bounds },
                        onDelete = onDelete,
                        onAddToBranch = onAddToBranch,
                    )
                    val target = FlowDropTarget(
                        key = "root-after-${step.id}",
                        anchorStepId = step.id,
                        afterAnchor = true,
                    )
                    FlowDropZone(
                        target = target,
                        dragActive = draggedStepId != null,
                        selected = activeTarget == target,
                        onBounds = { dropTarget, bounds -> dropBounds[dropTarget] = bounds },
                    )
                }
                EndPill()
                Spacer(Modifier.height(18.dp))
                AddStepButton(onAdd)
            }
        }
    }
}

@Composable
private fun BranchAwareFlowNode(
    step: DraftStep,
    steps: List<DraftStep>,
    onEdit: (Int) -> Unit,
    draggedStepId: String?,
    dragOffset: Offset,
    activeTarget: FlowDropTarget?,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDropBounds: (FlowDropTarget, Rect) -> Unit,
    onDelete: (Int) -> Unit,
    onAddToBranch: (String, Int) -> Unit,
) {
    val index = steps.indexOfFirst { it.id == step.id }
    if (!step.isBranchCondition()) {
        FlowNode(
            step = step,
            number = index + 1,
            onClick = { onEdit(index) },
            isDragging = draggedStepId == step.id,
            dragOffset = dragOffset,
            onDragStart = { center -> onDragStart(step.id, center) },
            onDrag = onDrag,
            onDragEnd = onDragEnd,
            onDragCancel = onDragCancel,
            onDelete = { onDelete(index) },
        )
        return
    }

    DecisionNode(
        step = step,
        onClick = { onEdit(index) },
        isDragging = draggedStepId == step.id,
        dragOffset = dragOffset,
        onDragStart = { center -> onDragStart(step.id, center) },
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        onDelete = { onDelete(index) },
    )
    BranchConnector()
    val labels = step.options.ifEmpty {
        if (step.kind.contains("Условие")) listOf("Да", "Нет") else listOf("", "")
    }
    BranchOptionsCarousel(
        condition = step,
        labels = labels,
        steps = steps,
        onEdit = onEdit,
        draggedStepId = draggedStepId,
        dragOffset = dragOffset,
        activeTarget = activeTarget,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onDragEnd = onDragEnd,
        onDragCancel = onDragCancel,
        onDropBounds = onDropBounds,
        onDelete = onDelete,
        onAddToBranch = onAddToBranch,
    )
    MergeConnector()
}

@Composable
private fun BranchOptionsCarousel(
    condition: DraftStep,
    labels: List<String>,
    steps: List<DraftStep>,
    onEdit: (Int) -> Unit,
    draggedStepId: String?,
    dragOffset: Offset,
    activeTarget: FlowDropTarget?,
    onDragStart: (String, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDropBounds: (FlowDropTarget, Rect) -> Unit,
    onDelete: (Int) -> Unit,
    onAddToBranch: (String, Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val laneWidth = (maxWidth - 7.dp) / 2
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(labels, key = { index, _ -> "${condition.id}-$index" }) { optionIndex, label ->
                val branchSteps = steps.filter {
                    it.parentConditionId == condition.id && (it.parentOptionIndex ?: 0) == optionIndex
                }
                val focused = optionIndex in firstVisible..(firstVisible + 1)
                val scale by animateFloatAsState(if (focused) 1f else .92f, label = "branch-scale")
                Column(
                    modifier = Modifier.width(laneWidth).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (focused) 1f else .62f }
                        .then(
                            if (draggedStepId != null) Modifier.border(
                                1.dp,
                                if (activeTarget?.conditionId == condition.id && activeTarget.optionIndex == optionIndex) Lime else Ink.copy(.14f),
                                RoundedCornerShape(11.dp),
                            ) else Modifier,
                        ).padding(horizontal = if (draggedStepId != null) 3.dp else 0.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BranchLabel(
                        text = label.ifBlank { "Вариант ${optionIndex + 1}" }.uppercase(),
                        color = if (optionIndex == 0) Lime else Apricot,
                        modifier = Modifier.fillMaxWidth(),
                        onAdd = { onAddToBranch(condition.id, optionIndex) },
                    )
                    if (branchSteps.isEmpty()) {
                        Text(
                            "Ветка пока пустая",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(.42f),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(.55f)).padding(horizontal = 4.dp, vertical = 14.dp),
                        )
                    } else {
                        branchSteps.forEachIndexed { branchIndex, child ->
                            FlowNode(
                                step = child,
                                number = steps.indexOfFirst { it.id == child.id } + 1,
                                compact = true,
                                onClick = { onEdit(steps.indexOfFirst { it.id == child.id }) },
                                isDragging = draggedStepId == child.id,
                                dragOffset = dragOffset,
                                onDragStart = { center -> onDragStart(child.id, center) },
                                onDrag = onDrag,
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel,
                                onDelete = { onDelete(steps.indexOfFirst { it.id == child.id }) },
                            )
                            if (branchIndex < branchSteps.lastIndex) FlowConnector()
                        }
                    }
                    val dropTarget = FlowDropTarget(
                        key = "branch-${condition.id}-$optionIndex",
                        conditionId = condition.id,
                        optionIndex = optionIndex,
                    )
                    FlowDropZone(
                        target = dropTarget,
                        dragActive = draggedStepId != null,
                        selected = activeTarget == dropTarget,
                        compact = true,
                        onBounds = onDropBounds,
                    )
                }
            }
        }
    }
    if (labels.size > 2) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            labels.indices.forEach { index ->
                Box(
                    Modifier.padding(horizontal = 2.dp).size(if (index in firstVisible..firstVisible + 1) 7.dp else 5.dp)
                        .clip(CircleShape).background(if (index in firstVisible..firstVisible + 1) Ink else Ink.copy(.2f)),
                )
            }
            Spacer(Modifier.size(6.dp))
            Text("ещё ${labels.size - 2} →", style = MaterialTheme.typography.labelSmall, color = Ink.copy(.5f))
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun FlowNode(
    step: DraftStep,
    number: Int,
    compact: Boolean = false,
    onClick: () -> Unit,
    isDragging: Boolean,
    dragOffset: Offset,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var centerInRoot by remember(step.id) { mutableStateOf(Offset.Zero) }
    val lift by animateFloatAsState(if (isDragging) 1.045f else 1f, label = "flow-node-lift")
    Box(modifier = if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(.82f)) {
        if (isDragging) {
            Box(
                Modifier.matchParentSize().clip(RoundedCornerShape(12.dp))
                    .background(step.accent.copy(.13f)).border(2.dp, step.accent, RoundedCornerShape(12.dp)),
            )
        }
        Box(
            Modifier.fillMaxWidth().zIndex(if (isDragging) 8f else 0f)
                .onGloballyPositioned { coordinates -> centerInRoot = coordinates.boundsInRoot().center }
                .graphicsLayer {
                    translationX = if (isDragging) dragOffset.x else 0f
                    translationY = if (isDragging) dragOffset.y else 0f
                    scaleX = lift
                    scaleY = lift
                    shadowElevation = if (isDragging) 22f else 0f
                    rotationZ = if (isDragging) (dragOffset.x / 180f).coerceIn(-2.2f, 2.2f) else 0f
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onClick)
                    .pointerInput(step.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { onDragStart(centerInRoot) },
                            onDragCancel = onDragCancel,
                            onDragEnd = onDragEnd,
                            onDrag = { change, amount -> change.consume(); onDrag(amount) },
                        )
                    }
                    .padding(if (compact) 9.dp else 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(if (compact) 21.dp else 25.dp).clip(RoundedCornerShape(6.dp)).background(step.accent), contentAlignment = Alignment.Center) {
                    Text("$number", style = MaterialTheme.typography.labelSmall, color = Ink)
                }
                Spacer(Modifier.size(7.dp))
                Column(Modifier.weight(1f)) {
                    Text(step.title.ifBlank { "Без названия" }, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    if (!compact) Text(step.kind, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.5f))
                }
                Spacer(Modifier.size(14.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                Icon(Icons.Outlined.Close, "Удалить блок", tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun FlowDropZone(
    target: FlowDropTarget,
    dragActive: Boolean,
    selected: Boolean,
    compact: Boolean = false,
    onBounds: (FlowDropTarget, Rect) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth(if (compact) 1f else .82f)
            .height(if (dragActive) 34.dp else 22.dp)
            .onGloballyPositioned { onBounds(target, it.boundsInRoot()) },
        contentAlignment = Alignment.Center,
    ) {
        if (dragActive) {
            Box(
                Modifier.fillMaxWidth().height(if (selected) 25.dp else 18.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) Lime.copy(.72f) else MaterialTheme.colorScheme.surfaceVariant)
                    .border(if (selected) 2.dp else 1.dp, if (selected) Ink else Ink.copy(.16f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.DragIndicator, "Переместить сюда", tint = Ink.copy(if (selected) .9f else .35f), modifier = Modifier.size(15.dp))
            }
        } else {
            FlowConnector()
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BlockTypePicker(onDismiss: () -> Unit, onSelect: (BlockType) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            item {
                Text("Тип нового блока", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text("Тип определяет поведение блока при выполнении.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                Spacer(Modifier.height(7.dp))
            }
            itemsIndexed(blockTypes, key = { _, type -> type.label }) { _, type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(type) }
                        .padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(30.dp).clip(RoundedCornerShape(7.dp)).background(type.accent), contentAlignment = Alignment.Center) {
                        Text(type.symbol, style = MaterialTheme.typography.labelLarge, color = Ink)
                    }
                    Spacer(Modifier.size(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(type.label, style = MaterialTheme.typography.titleMedium)
                        Text(
                            type.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(.55f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun StepEditSheet(
    step: DraftStep,
    onDismiss: () -> Unit,
    onSave: (DraftStep) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(step) { mutableStateOf(step.title) }
    var description by remember(step) { mutableStateOf(step.description) }
    var note by remember(step) { mutableStateOf(step.note) }
    var kind by remember(step) { mutableStateOf(step.kind) }
    val checklistItems = remember(step) { mutableStateListOf<String>().apply { addAll(step.checklistItems) } }
    val options = remember(step) { mutableStateListOf<String>().apply { addAll(step.options) } }
    var timerSeconds by remember(step) { mutableIntStateOf(step.timerSeconds.coerceAtLeast(1)) }
    val attachments = remember(step) { mutableStateListOf<DraftAttachment>().apply { addAll(step.attachments) } }
    val voiceRecorder = remember(context) { VoiceNoteRecorder(context) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordingStartedAt by remember { mutableLongStateOf(0L) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    var mediaError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(voiceRecorder) {
        onDispose { voiceRecorder.cancel() }
    }
    LaunchedEffect(isRecordingVoice, recordingStartedAt) {
        while (isRecordingVoice) {
            recordingElapsedMs = SystemClock.elapsedRealtime() - recordingStartedAt
            delay(200)
        }
    }
    val photoRecorder = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getStringExtra(SquarePhotoRecorderActivity.EXTRA_URI)?.let(Uri::parse)
            val name = result.data?.getStringExtra(SquarePhotoRecorderActivity.EXTRA_NAME)
            if (uri != null) attachments.add(DraftAttachment(uri, AttachmentKind.Photo, name ?: "photo.jpg"))
        }
    }
    val startPhotoCapture = {
        runCatching { photoRecorder.launch(Intent(context, SquarePhotoRecorderActivity::class.java)) }
            .onFailure { mediaError = "Не удалось открыть камеру" }
        Unit
    }
    val photoCameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startPhotoCapture() else mediaError = "Для фото нужен доступ к камере"
    }
    val videoCircleRecorder = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.getStringExtra(VideoCircleRecorderActivity.EXTRA_URI)?.let(Uri::parse)
            val name = result.data?.getStringExtra(VideoCircleRecorderActivity.EXTRA_NAME)
            if (uri != null) attachments.add(DraftAttachment(uri, AttachmentKind.VideoCircle, name ?: "video.mp4"))
        }
    }
    val startVideoCapture = {
        runCatching {
            videoCircleRecorder.launch(Intent(context, VideoCircleRecorderActivity::class.java))
        }.onFailure {
            mediaError = "Не удалось открыть камеру"
        }
        Unit
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.CAMERA] == true) startVideoCapture() else mediaError = "Для записи видео нужен доступ к камере"
    }
    val startVoiceRecording = {
        runCatching {
            voiceRecorder.start()
            recordingStartedAt = SystemClock.elapsedRealtime()
            recordingElapsedMs = 0L
            isRecordingVoice = true
        }.onFailure { mediaError = "Не удалось начать запись голоса" }
        Unit
    }
    val microphonePermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startVoiceRecording() else mediaError = "Для голосовой заметки нужен доступ к микрофону"
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { persistUri(context, uri); attachments.add(DraftAttachment(uri, AttachmentKind.File)) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(step.accent).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Color.White.copy(.65f)), contentAlignment = Alignment.Center) {
                        Text(blockTypes.firstOrNull { it.label == kind }?.symbol ?: "◇", style = MaterialTheme.typography.titleLarge, color = Ink)
                    }
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("Этап", style = MaterialTheme.typography.headlineMedium, color = Ink)
                        Text("Текст, логика и материалы — в одном блоке", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.58f))
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text("Тип блока", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    blockTypes.forEach { type ->
                        Text(
                            "${type.symbol}  ${type.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (kind == type.label) Color.White else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (kind == type.label) Ink else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    kind = type.label
                                    if (kind == "Чек-лист" && checklistItems.isEmpty()) checklistItems.add("")
                                    if (kind.contains("Условие")) {
                                        options.clear()
                                        options.addAll(listOf("Да", "Нет"))
                                    } else if (kind.contains("вариант") && options.isEmpty()) {
                                        options.addAll(listOf("", ""))
                                    }
                                }
                                .padding(horizontal = 11.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            if (kind == "Чек-лист" || checklistItems.isNotEmpty()) {
                item {
                    Text("Пункты чек-листа", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(5.dp))
                    Text("Пользователь сможет отмечать их галочками.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                }
                itemsIndexed(checklistItems) { index, item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = item,
                            onValueChange = { checklistItems[index] = it },
                            label = { Text("Пункт ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { checklistItems.removeAt(index) }) { Icon(Icons.Outlined.Close, "Удалить пункт") }
                    }
                }
                item {
                    OutlinedButton(onClick = { checklistItems.add("") }, shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) {
                        Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Пункт")
                    }
                }
            }
            if (kind.contains("вариант") || kind.contains("Условие")) {
                item { Text("Варианты ответа", style = MaterialTheme.typography.titleMedium) }
                itemsIndexed(options) { index, option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = option,
                            onValueChange = { options[index] = it },
                            label = { Text("Вариант ${index + 1}") },
                            singleLine = true,
                            enabled = !kind.contains("Условие"),
                            shape = RoundedCornerShape(9.dp),
                            modifier = Modifier.weight(1f),
                        )
                        if (!kind.contains("Условие")) {
                            IconButton(onClick = { options.removeAt(index) }) { Icon(Icons.Outlined.Close, "Удалить вариант") }
                        }
                    }
                }
                if (!kind.contains("Условие")) {
                    item {
                        OutlinedButton(onClick = { options.add("") }, shape = RoundedCornerShape(9.dp), modifier = Modifier.fillMaxWidth().height(42.dp)) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Вариант")
                        }
                    }
                }
            }
            if (kind == "Таймер") {
                item {
                    Text("Длительность", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    TimerWheelPicker(
                        totalSeconds = timerSeconds,
                        onDurationChange = { timerSeconds = it.coerceAtLeast(1) },
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Прокручивайте часы, минуты и секунды. Уведомления придут за 5 мин, за 1 мин и при завершении.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(.55f),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание действия") },
                    minLines = 3,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Личная заметка") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Notes, null) },
                    placeholder = { Text("Совет, предупреждение или комментарий") },
                    minLines = 2,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (kind != "Чек-лист") {
                item {
                    Text("Медиаконтент", style = MaterialTheme.typography.titleMedium)
                    Text("Записи сохраняются только на этом устройстве.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AttachmentButton("Фото", Icons.Outlined.PhotoCamera) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                startPhotoCapture()
                            } else {
                                photoCameraPermission.launch(Manifest.permission.CAMERA)
                            }
                        }
                        AttachmentButton("Кружок", Icons.Outlined.Videocam) {
                            if (
                                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                            ) {
                                startVideoCapture()
                            } else {
                                cameraPermission.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
                            }
                        }
                        AttachmentButton(
                            if (isRecordingVoice) "Стоп ${formatRecordingTime(recordingElapsedMs)}" else "Голос",
                            if (isRecordingVoice) Icons.Outlined.StopCircle else Icons.Outlined.Mic,
                        ) {
                            if (isRecordingVoice) {
                                val output = voiceRecorder.stop()
                                isRecordingVoice = false
                                if (output != null && output.file.length() > 0L) {
                                    attachments.add(DraftAttachment(output.uri, AttachmentKind.Audio, output.file.name))
                                } else {
                                    mediaError = "Голосовая заметка слишком короткая"
                                }
                            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                startVoiceRecording()
                            } else {
                                microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        AttachmentButton("Файл", Icons.Outlined.AttachFile) { filePicker.launch(arrayOf("*/*")) }
                    }
                    mediaError?.let {
                        Spacer(Modifier.height(7.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (kind != "Чек-лист" && attachments.isNotEmpty()) {
                itemsIndexed(attachments) { index, attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            when (attachment.kind) {
                                AttachmentKind.Photo -> Icons.Outlined.PhotoCamera
                                AttachmentKind.VideoCircle -> Icons.Outlined.Videocam
                                AttachmentKind.Audio -> Icons.Outlined.Mic
                                AttachmentKind.File -> Icons.Outlined.AttachFile
                            },
                            null,
                        )
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(attachment.kind.label, style = MaterialTheme.typography.titleMedium)
                            Text(attachment.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                        }
                        IconButton(onClick = { attachments.removeAt(index) }) { Icon(Icons.Outlined.Close, "Удалить вложение") }
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        onSave(
                            step.copy(
                                title = title.trim(),
                                description = description.trim(),
                                note = note.trim(),
                                kind = kind,
                                attachments = if (kind == "Чек-лист") emptyList() else attachments.toList(),
                                checklistItems = if (kind == "Чек-лист") checklistItems.map(String::trim).filter(String::isNotBlank) else emptyList(),
                                options = when {
                                    kind.contains("Условие") -> listOf("Да", "Нет")
                                    kind == "Один вариант" -> options.map(String::trim).let { values ->
                                        if (values.isEmpty()) listOf("", "") else values
                                    }
                                    kind.contains("вариант") -> options.map(String::trim)
                                    else -> emptyList()
                                },
                                timerSeconds = if (kind == "Таймер") timerSeconds.coerceAtLeast(1) else 300,
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                ) { Text("Сохранить изменения") }
                Spacer(Modifier.height(6.dp))
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.DeleteOutline, null)
                    Spacer(Modifier.size(7.dp))
                    Text("Удалить этап")
                }
            }
        }
    }
}

private fun persistUri(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

@Composable
private fun AttachmentButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(9.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(42.dp),
        ) {
            Icon(icon, label, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun formatRecordingTime(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1_000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun TimerWheelPicker(totalSeconds: Int, onDurationChange: (Int) -> Unit) {
    val safeTotal = totalSeconds.coerceIn(1, 86_399)
    val hours = safeTotal / 3_600
    val minutes = (safeTotal % 3_600) / 60
    val seconds = safeTotal % 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Ink)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(Lime), contentAlignment = Alignment.Center) {
                Text("◷", style = MaterialTheme.typography.titleMedium, color = Ink)
            }
            Spacer(Modifier.size(9.dp))
            Text("Таймер", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Spacer(Modifier.weight(1f))
            Text(
                "%02d:%02d:%02d".format(hours, minutes, seconds),
                style = MaterialTheme.typography.titleLarge,
                color = Lime,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberWheel(
                label = "часы",
                values = 0..23,
                selected = hours,
                onSelected = { onDurationChange((it * 3_600 + minutes * 60 + seconds).coerceAtLeast(1)) },
                modifier = Modifier.weight(1f),
            )
            Text(":", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(.45f))
            NumberWheel(
                label = "минуты",
                values = 0..59,
                selected = minutes,
                onSelected = { onDurationChange((hours * 3_600 + it * 60 + seconds).coerceAtLeast(1)) },
                modifier = Modifier.weight(1f),
            )
            Text(":", style = MaterialTheme.typography.headlineMedium, color = Color.White.copy(.45f))
            NumberWheel(
                label = "секунды",
                values = 0..59,
                selected = seconds,
                onSelected = { onDurationChange((hours * 3_600 + minutes * 60 + it).coerceAtLeast(1)) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumberWheel(
    label: String,
    values: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(values.first, values.last))
    val flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center)

    LaunchedEffect(listState, values) {
        snapshotFlow {
            val center = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
            listState.layoutInfo.visibleItemsInfo.minByOrNull { item ->
                kotlin.math.abs(item.offset + item.size / 2 - center)
            }?.index
        }
            .distinctUntilChanged()
            .collect { index -> index?.let { onSelected(values.first + it) } }
    }
    LaunchedEffect(selected) {
        val target = (selected - values.first).coerceIn(0, values.count() - 1)
        if (!listState.isScrollInProgress && listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
        }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().height(132.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color.White.copy(.1f))
                    .border(1.dp, Lime.copy(.65f), RoundedCornerShape(9.dp)),
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = 44.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(values.count()) { index ->
                    val value = values.first + index
                    Text(
                        text = "%02d".format(value),
                        style = if (value == selected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(if (value == selected) 1f else .3f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().height(44.dp).padding(top = 9.dp),
                    )
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = Lime.copy(.82f))
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DecisionNode(
    step: DraftStep,
    onClick: () -> Unit,
    isDragging: Boolean,
    dragOffset: Offset,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDelete: () -> Unit,
) {
    var centerInRoot by remember(step.id) { mutableStateOf(Offset.Zero) }
    val lift by animateFloatAsState(if (isDragging) 1.045f else 1f, label = "decision-lift")
    Box(Modifier.fillMaxWidth(.82f)) {
        if (isDragging) {
            Box(
                Modifier.matchParentSize().clip(RoundedCornerShape(12.dp))
                    .background(Lavender.copy(.16f)).border(2.dp, Lavender, RoundedCornerShape(12.dp)),
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth().zIndex(if (isDragging) 8f else 0f)
                .onGloballyPositioned { centerInRoot = it.boundsInRoot().center }
                .graphicsLayer {
                    translationX = if (isDragging) dragOffset.x else 0f
                    translationY = if (isDragging) dragOffset.y else 0f
                    scaleX = lift
                    scaleY = lift
                    shadowElevation = if (isDragging) 22f else 0f
                }
                .clip(RoundedCornerShape(12.dp)).background(Lavender)
                .clickable(onClick = onClick)
                .pointerInput(step.id) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { onDragStart(centerInRoot) },
                        onDragCancel = onDragCancel,
                        onDragEnd = onDragEnd,
                        onDrag = { change, amount -> change.consume(); onDrag(amount) },
                    )
                }
                .padding(12.dp),
        ) {
            Column(Modifier.align(Alignment.Center)) {
                Text("УСЛОВИЕ", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.55f))
                Spacer(Modifier.height(5.dp))
                Text(step.title.ifBlank { "Без названия" }, style = MaterialTheme.typography.titleLarge, color = Ink)
            }
            IconButton(onClick = onDelete, modifier = Modifier.align(Alignment.TopEnd).size(24.dp)) {
                Icon(Icons.Outlined.Close, "Удалить условие", tint = Color(0xFFE53935), modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable private fun StartPill() {
    Text("▶", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(7.dp)).background(Ink).padding(horizontal = 11.dp, vertical = 6.dp))
}

@Composable private fun EndPill() {
    Row(Modifier.clip(RoundedCornerShape(7.dp)).background(Ink).padding(horizontal = 11.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Check, null, tint = Lime, modifier = Modifier.size(15.dp))
        Spacer(Modifier.size(6.dp))
        Text("■", style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable private fun BranchLabel(text: String, color: Color, modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Box(modifier = modifier.clip(RoundedCornerShape(7.dp)).background(color).padding(start = 7.dp, end = 25.dp, top = 6.dp, bottom = 7.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onAdd, modifier = Modifier.align(Alignment.BottomEnd).size(22.dp)) {
            Icon(Icons.Outlined.Add, "Добавить блок в ветку", tint = Ink, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable private fun FlowConnector() {
    Canvas(Modifier.size(width = 18.dp, height = 22.dp)) {
        drawLine(Ink.copy(.32f), Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth = 3f, cap = StrokeCap.Round)
    }
}

@Composable private fun BranchConnector() {
    Canvas(Modifier.fillMaxWidth(.55f).height(30.dp)) {
        val center = size.width / 2
        drawLine(Ink.copy(.35f), Offset(center, 0f), Offset(center, size.height * .42f), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width * .2f, size.height * .42f), Offset(size.width * .8f, size.height * .42f), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width * .2f, size.height * .42f), Offset(size.width * .2f, size.height), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width * .8f, size.height * .42f), Offset(size.width * .8f, size.height), 4f, cap = StrokeCap.Round)
    }
}

@Composable private fun MergeConnector() {
    Canvas(Modifier.fillMaxWidth(.55f).height(32.dp)) {
        drawLine(Ink.copy(.35f), Offset(size.width * .2f, 0f), Offset(size.width * .2f, size.height * .58f), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width * .8f, 0f), Offset(size.width * .8f, size.height * .58f), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width * .2f, size.height * .58f), Offset(size.width * .8f, size.height * .58f), 4f, cap = StrokeCap.Round)
        drawLine(Ink.copy(.35f), Offset(size.width / 2, size.height * .58f), Offset(size.width / 2, size.height), 4f, cap = StrokeCap.Round)
    }
}

@Composable private fun AddStepButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
        shape = RoundedCornerShape(9.dp),
        modifier = Modifier.fillMaxWidth().height(44.dp),
    ) {
        Icon(Icons.Outlined.Add, null)
        Spacer(Modifier.size(8.dp))
        Text("Этап")
    }
}

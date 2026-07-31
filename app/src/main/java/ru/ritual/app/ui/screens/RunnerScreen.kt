package ru.ritual.app.ui.screens

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import androidx.core.content.ContextCompat
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.AttachmentType
import ru.ritual.app.domain.model.StepAttachment
import ru.ritual.app.domain.model.ChecklistStep
import ru.ritual.app.domain.model.StepType
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.timer.ChecklistTimerService
import kotlin.math.ceil

@Composable
fun RunnerScreen(
    checklist: Checklist,
    tapNavigation: Boolean = true,
    keepScreenAwake: Boolean = false,
    autoPlayVideoNotes: Boolean = false,
    onClose: () -> Unit,
) {
    val activity = LocalContext.current as? Activity
    var currentIndex by rememberSaveable { mutableIntStateOf(0) }
    val navigationHistory = remember { mutableStateListOf<Int>() }
    val checkedItems = remember { mutableStateMapOf<String, Set<Int>>() }
    val selectedOptions = remember { mutableStateMapOf<String, Set<Int>>() }
    val step = checklist.steps[currentIndex]
    val nextIndex = checklist.steps.nextStepAfter(currentIndex)
    val navigateTo: (Int?) -> Unit = { target ->
        if (target == null) {
            onClose()
        } else {
            navigationHistory.add(currentIndex)
            currentIndex = target
        }
    }
    val navigateBack = {
        if (navigationHistory.isNotEmpty()) currentIndex = navigationHistory.removeAt(navigationHistory.lastIndex)
    }
    DisposableEffect(activity, keepScreenAwake) {
        if (keepScreenAwake) activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
    val progress by animateFloatAsState((currentIndex + 1f) / checklist.steps.size, label = "progress")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(checklist.accent)
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 10.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            )
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIcon(Icons.Outlined.Close, "Закрыть", onClose)
            Spacer(Modifier.weight(1f))
            Text("${currentIndex + 1} из ${checklist.steps.size}", style = MaterialTheme.typography.labelLarge, color = Ink.copy(.65f))
            Spacer(Modifier.weight(1f))
            CircleIcon(Icons.Outlined.KeyboardArrowDown, "Свернуть приложение", { activity?.moveTaskToBack(true) })
            Spacer(Modifier.size(5.dp))
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            checklist.steps.indices.forEach { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(if (index <= currentIndex) Ink else Ink.copy(.16f)),
                )
            }
        }
        Spacer(Modifier.height(26.dp))
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(currentIndex, tapNavigation) {
                if (tapNavigation) {
                    detectTapGestures { offset ->
                        if (offset.x < size.width * .28f) {
                            navigateBack()
                        } else if (step.type != StepType.YesNo && step.type != StepType.SingleChoice) {
                            navigateTo(if (step.type == StepType.Final) null else nextIndex)
                        }
                    }
                }
            },
        ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
            Text(step.eyebrow, style = MaterialTheme.typography.labelMedium, color = Ink.copy(.58f))
            Spacer(Modifier.height(6.dp))
            Text(step.title, style = MaterialTheme.typography.headlineLarge, color = Ink)
            Spacer(Modifier.height(12.dp))
            Text(step.description, style = MaterialTheme.typography.bodyLarge, color = Ink.copy(.72f))
            if (step.note.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    step.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink.copy(.72f),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(.48f)).padding(11.dp),
                )
            }
            if (step.attachments.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                StepMedia(attachments = step.attachments, autoPlayVideoNotes = autoPlayVideoNotes)
            }
            Spacer(Modifier.height(20.dp))
            if (step.type == StepType.YesNo) {
                YesNoChoice { answer ->
                    val optionIndex = if (answer) 0 else 1
                    navigateTo(checklist.steps.firstStepInBranch(step.id, optionIndex) ?: nextIndex)
                }
            } else if (step.type == StepType.SingleChoice) {
                ChoiceBlock(
                    options = step.options,
                    selected = selectedOptions[step.id].orEmpty(),
                    multiple = false,
                    onSelect = { selectedIndex ->
                        selectedOptions[step.id] = setOf(selectedIndex)
                        navigateTo(checklist.steps.firstStepInBranch(step.id, selectedIndex) ?: nextIndex)
                    },
                )
            } else if (step.type == StepType.MultipleChoice) {
                ChoiceBlock(
                    options = step.options,
                    selected = selectedOptions[step.id].orEmpty(),
                    multiple = true,
                    onSelect = { selectedIndex ->
                        val current = selectedOptions[step.id].orEmpty()
                        selectedOptions[step.id] = if (selectedIndex in current) current - selectedIndex else current + selectedIndex
                    },
                )
            } else if (step.checklistItems.isNotEmpty()) {
                ChecklistBlock(
                    items = step.checklistItems,
                    checked = checkedItems[step.id].orEmpty(),
                    onToggle = { index ->
                        val current = checkedItems[step.id].orEmpty()
                        checkedItems[step.id] = if (index in current) current - index else current + index
                    },
                )
            } else {
                when (step.type) {
                    StepType.YesNo -> Unit
                    StepType.Timer -> TimerBlock(step.timerSeconds ?: 60, step.title)
                    StepType.Final -> FinalMark()
                    else -> CheckMark()
                }
            }
        }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (navigationHistory.isNotEmpty()) {
                Button(
                    onClick = navigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(.72f), contentColor = Ink),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(46.dp),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Назад", modifier = Modifier.size(18.dp))
                }
            }
            if (step.type != StepType.YesNo && step.type != StepType.SingleChoice) {
                Button(
                    onClick = {
                        navigateTo(if (step.type == StepType.Final) null else nextIndex)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp),
                ) {
                    Icon(
                        if (step.type == StepType.Final || nextIndex == null) Icons.Outlined.Check else Icons.AutoMirrored.Outlined.ArrowForward,
                        if (step.type == StepType.Final || nextIndex == null) "Завершить" else "Дальше",
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

private fun List<ChecklistStep>.firstStepInBranch(conditionId: String, optionIndex: Int): Int? =
    indices.firstOrNull { index ->
        this[index].parentConditionId == conditionId &&
            (this[index].parentOptionIndex ?: 0) == optionIndex
    }

private fun List<ChecklistStep>.nextStepAfter(index: Int, visited: Set<Int> = emptySet()): Int? {
    if (index !in indices || index in visited) return null
    val current = this[index]
    val sibling = indices.firstOrNull { candidateIndex ->
        candidateIndex > index &&
            this[candidateIndex].parentConditionId == current.parentConditionId &&
            (this[candidateIndex].parentOptionIndex ?: 0) == (current.parentOptionIndex ?: 0)
    }
    if (sibling != null) return sibling
    val parentIndex = current.parentConditionId?.let { conditionId -> indexOfFirst { it.id == conditionId } }
        ?.takeIf { it >= 0 }
        ?: return null
    return nextStepAfter(parentIndex, visited + index)
}

@Composable
private fun ChoiceBlock(options: List<String>, selected: Set<Int>, multiple: Boolean, onSelect: (Int) -> Unit) {
    if (options.isEmpty()) {
        Text("Для этого этапа не заданы варианты", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.55f))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        options.forEachIndexed { index, option ->
            val active = index in selected
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (active) Ink else Color.White.copy(.62f))
                    .clickable { onSelect(index) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (multiple) if (active) "☑" else "☐" else if (active) "●" else "○", color = if (active) Color.White else Ink)
                Spacer(Modifier.size(9.dp))
                Text(option, style = MaterialTheme.typography.titleMedium, color = if (active) Color.White else Ink)
            }
        }
    }
}

@Composable
private fun StepMedia(attachments: List<StepAttachment>, autoPlayVideoNotes: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        attachments.forEach { attachment ->
            when (attachment.type) {
                AttachmentType.VideoCircle -> VideoCircleNote(attachment, autoPlayVideoNotes)
                AttachmentType.Audio -> AudioVoiceNote(attachment)
                AttachmentType.Photo -> PhotoNote(attachment)
                AttachmentType.File -> FileNote(attachment)
            }
        }
    }
}

@Composable
private fun VideoCircleNote(attachment: StepAttachment, autoPlay: Boolean) {
    val context = LocalContext.current
    var player by remember(attachment.uri) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(attachment.uri) {
        onDispose {
            player?.release()
            player = null
        }
    }
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(CircleShape).background(Ink.copy(.12f))
                .clickable {
                    player?.let { activePlayer ->
                        if (activePlayer.isPlaying) { activePlayer.pause(); isPlaying = false } else { activePlayer.start(); isPlaying = true }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { currentContext ->
                    TextureView(currentContext).also { texture ->
                        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                                val mediaPlayer = MediaPlayer().apply {
                                    setDataSource(context, Uri.parse(attachment.uri))
                                    setSurface(Surface(surfaceTexture))
                                    isLooping = true
                                    setOnPreparedListener { prepared ->
                                        applyCenterCropTransform(texture, prepared.videoWidth, prepared.videoHeight)
                                        prepared.seekTo(1)
                                        if (autoPlay) {
                                            prepared.start()
                                            isPlaying = true
                                        }
                                    }
                                    setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                        applyCenterCropTransform(texture, videoWidth, videoHeight)
                                    }
                                    setOnCompletionListener { isPlaying = false }
                                    prepareAsync()
                                }
                                player = mediaPlayer
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                                player?.let { applyCenterCropTransform(texture, it.videoWidth, it.videoHeight) }
                            }

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                player?.release()
                                player = null
                                isPlaying = false
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                        }
                    }
                },
            )
            Box(Modifier.size(38.dp).clip(CircleShape).background(Ink.copy(.76f)), contentAlignment = Alignment.Center) {
                Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (isPlaying) "Пауза" else "Воспроизвести", tint = Color.White)
            }
        }
    }
}

private fun applyCenterCropTransform(texture: TextureView, videoWidth: Int, videoHeight: Int) {
    if (texture.width <= 0 || texture.height <= 0 || videoWidth <= 0 || videoHeight <= 0) return
    val viewWidth = texture.width.toFloat()
    val viewHeight = texture.height.toFloat()
    val scale = maxOf(viewWidth / videoWidth, viewHeight / videoHeight)
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale
    texture.setTransform(
        Matrix().apply {
            setScale(
                scaledWidth / viewWidth,
                scaledHeight / viewHeight,
                viewWidth / 2f,
                viewHeight / 2f,
            )
        },
    )
}

@Composable
private fun AudioVoiceNote(attachment: StepAttachment) {
    val context = LocalContext.current
    var player by remember(attachment.uri) { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    DisposableEffect(attachment.uri) {
        onDispose { player?.release(); player = null }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.64f))
            .clickable {
                val activePlayer = player ?: runCatching { MediaPlayer.create(context, Uri.parse(attachment.uri)) }.getOrNull()?.also {
                    player = it
                    it.setOnCompletionListener { isPlaying = false; it.seekTo(0) }
                }
                activePlayer?.let {
                    if (it.isPlaying) { it.pause(); isPlaying = false } else { it.start(); isPlaying = true }
                }
            }.padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
            Icon(if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, if (isPlaying) "Пауза" else "Голосовая заметка", tint = Color.White)
        }
        Spacer(Modifier.size(10.dp))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(18) { index ->
                Box(Modifier.width(3.dp).height((8 + (index * 7 % 18)).dp).clip(CircleShape).background(Ink.copy(if (isPlaying) .72f else .32f)))
            }
        }
        Text("Голос", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.58f))
    }
}

@Composable
private fun PhotoNote(attachment: StepAttachment) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp)),
        factory = { context -> ImageView(context).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { it.setImageURI(Uri.parse(attachment.uri)) },
    )
}

@Composable
private fun FileNote(attachment: StepAttachment) {
    Text(
        "📎 ${attachment.name}",
        style = MaterialTheme.typography.bodyMedium,
        color = Ink,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(.58f)).padding(11.dp),
    )
}

@Composable
private fun CheckMark() {
    Box(
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(.62f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Outlined.Check, contentDescription = null, tint = Ink, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun FinalMark() {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink).padding(16.dp),
    ) {
        Text("100%\nвыполнено", style = MaterialTheme.typography.headlineLarge, color = Color.White)
    }
}

@Composable
private fun YesNoChoice(onSelected: (Boolean) -> Unit) {
    Text("Выберите вариант — переход произойдёт сразу", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.56f))
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(true to "Да", false to "Нет").forEach { (value, label) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(.62f))
                    .clickable { onSelected(value) }
                    .padding(13.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = MaterialTheme.typography.titleMedium, color = Ink)
            }
        }
    }
}

@Composable
private fun ChecklistBlock(items: List<String>, checked: Set<Int>, onToggle: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items.forEachIndexed { index, item ->
            val isChecked = index in checked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isChecked) Ink else Color.White.copy(.62f))
                    .clickable { onToggle(index) }
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isChecked) Color.White else Ink.copy(.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isChecked) Icon(Icons.Outlined.Check, null, tint = Ink, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.size(9.dp))
                Text(item, style = MaterialTheme.typography.titleMedium, color = if (isChecked) Color.White else Ink)
            }
        }
        Text(
            "${checked.size} из ${items.size}",
            style = MaterialTheme.typography.labelLarge,
            color = Ink.copy(.55f),
            modifier = Modifier.align(Alignment.End),
        )
    }
}

@Composable
private fun TimerBlock(initialSeconds: Int, title: String) {
    val context = LocalContext.current
    var secondsLeft by rememberSaveable(initialSeconds) { mutableIntStateOf(initialSeconds) }
    var isRunning by rememberSaveable { mutableStateOf(false) }
    var deadlineMillis by rememberSaveable { mutableLongStateOf(0L) }

    val beginTimer = {
        val duration = if (secondsLeft > 0) secondsLeft else initialSeconds
        secondsLeft = duration
        deadlineMillis = System.currentTimeMillis() + duration * 1_000L
        isRunning = true
        ChecklistTimerService.start(context, duration, title)
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        beginTimer()
    }

    LaunchedEffect(isRunning, deadlineMillis) {
        while (isRunning) {
            secondsLeft = ceil(
                ((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)) / 1_000.0,
            ).toInt()
            if (secondsLeft <= 0) {
                isRunning = false
                break
            }
            delay(250L)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Ink).padding(15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("ТАЙМЕР", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(.5f))
            Text(
                "%02d:%02d".format(secondsLeft / 60, secondsLeft % 60),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
        }
        Spacer(Modifier.weight(1f))
        CircleIcon(
            if (isRunning) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
            if (isRunning) "Пауза" else "Старт",
            {
                if (isRunning) {
                    secondsLeft = ceil(
                        ((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)) / 1_000.0,
                    ).toInt()
                    isRunning = false
                    ChecklistTimerService.stop(context)
                } else if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    beginTimer()
                }
            },
            light = true,
        )
    }
}

@Composable
private fun CircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    light: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (light) Color.White else Color.White.copy(.6f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = Ink, modifier = Modifier.size(19.dp))
    }
}

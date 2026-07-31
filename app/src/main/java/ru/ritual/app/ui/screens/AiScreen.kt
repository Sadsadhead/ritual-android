package ru.ritual.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GenerationCharacter

@Composable
fun AiScreen(
    hasYandexCredentials: Boolean,
    isSavingKey: Boolean,
    onSaveCredentials: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    isGenerating: Boolean,
    generationStage: String?,
    generatedChecklist: GeneratedChecklist?,
    generationError: String?,
    generationNotifications: Boolean,
    onGenerate: (String, Int, GenerationCharacter) -> Unit,
    onEditDraft: () -> Unit,
) {
    val context = LocalContext.current
    var prompt by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableIntStateOf(1) }
    var characterIndex by rememberSaveable { mutableIntStateOf(0) }
    var characterMenuExpanded by remember { mutableStateOf(false) }
    var apiKey by rememberSaveable { mutableStateOf("") }
    var folderId by rememberSaveable { mutableStateOf("") }
    var keyVisible by rememberSaveable { mutableStateOf(false) }
    var pendingGeneration by remember { mutableStateOf<(() -> Unit)?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingGeneration?.invoke()
        pendingGeneration = null
    }
    val startGeneration = {
        val action = { onGenerate(prompt, detail, GenerationCharacter.entries[characterIndex]) }
        if (
            generationNotifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingGeneration = action
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            action()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = 20.dp,
            )
            .padding(horizontal = 16.dp),
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(Lime).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Ink)
        }
        Spacer(Modifier.height(10.dp))
        Text("Создать из идеи", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            "Опишите бытовую задачу обычными словами — приложение превратит её в понятный маршрут.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(.62f),
        )
        Spacer(Modifier.height(14.dp))
        if (!hasYandexCredentials) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Lime).padding(7.dp)) {
                        Icon(Icons.Outlined.Key, null, tint = Ink)
                    }
                    Spacer(Modifier.padding(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Подключите YandexGPT", style = MaterialTheme.typography.titleMedium)
                        Text("Ключ и ID каталога останутся на устройстве", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(.55f))
                    }
                }
                Spacer(Modifier.height(9.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API-ключ") },
                    placeholder = { Text("AQVN…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { keyVisible = !keyVisible }) {
                            Icon(if (keyVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Показать ключ")
                        }
                    },
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = folderId,
                    onValueChange = { folderId = it },
                    label = { Text("ID каталога") },
                    placeholder = { Text("b1g…") },
                    singleLine = true,
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        onSaveCredentials(apiKey, folderId)
                    },
                    enabled = apiKey.isNotBlank() && folderId.isNotBlank() && !isSavingKey,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    if (isSavingKey) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Подключить YandexGPT")
                    }
                }
                Text(
                    "Расширенные настройки и удаление ключа →",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(.58f),
                    modifier = Modifier.clickable(onClick = onOpenSettings).padding(top = 12.dp, bottom = 2.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Lime.copy(alpha = .55f)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CheckCircle, null, tint = Ink)
                Spacer(Modifier.padding(6.dp))
                Text("YandexGPT подключён", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("Изменить", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clickable(onClick = onOpenSettings).padding(6.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Что нужно сделать?") },
            placeholder = { Text("Например: собрать чемодан на три дня у моря…") },
            minLines = 4,
            shape = RoundedCornerShape(11.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(13.dp))
        Text("Характер генерации", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(7.dp))
        val character = GenerationCharacter.entries[characterIndex]
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { characterMenuExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(character.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        character.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(.58f),
                    )
                }
                Icon(Icons.Outlined.ExpandMore, "Выбрать характер")
            }
            DropdownMenu(
                expanded = characterMenuExpanded,
                onDismissRequest = { characterMenuExpanded = false },
                modifier = Modifier.fillMaxWidth(.88f),
            ) {
                GenerationCharacter.entries.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(.58f),
                                )
                            }
                        },
                        onClick = {
                            characterIndex = index
                            characterMenuExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Text("Насколько подробно?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Кратко", "Оптимально", "Подробно").forEachIndexed { index, label ->
                val selected = detail == index
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) Ink else MaterialTheme.colorScheme.surface)
                        .clickable { detail = index }
                        .padding(vertical = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (isGenerating) {
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Lime.copy(.38f)).padding(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Ink)
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text("YandexGPT работает", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(generationStage ?: "Создаю алгоритм…", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.62f))
                    }
                }
                Spacer(Modifier.height(9.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Ink, trackColor = Ink.copy(.12f))
                Text("Можно свернуть приложение — сообщим о результате.", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.55f), modifier = Modifier.padding(top = 7.dp))
            }
            Spacer(Modifier.height(10.dp))
        }
        Button(
            onClick = startGeneration,
            enabled = hasYandexCredentials && prompt.isNotBlank() && !isGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) {
            if (isGenerating) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.padding(5.dp))
                Text(generationStage ?: "Создаю алгоритм…")
            } else {
                Icon(Icons.Outlined.AutoAwesome, null)
                Spacer(Modifier.padding(5.dp))
                Text("Сгенерировать алгоритм")
            }
        }
        if (generationError != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                generationError,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.error.copy(.08f)).padding(10.dp),
            )
        }
        if (generatedChecklist != null) {
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Lime.copy(.45f)).padding(12.dp)) {
                Text("ЧЕРНОВИК ГОТОВ", style = MaterialTheme.typography.labelMedium, color = Ink.copy(.6f))
                Spacer(Modifier.height(6.dp))
                Text(generatedChecklist.title, style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(5.dp))
                Text("${generatedChecklist.steps.size} шагов · ~${generatedChecklist.estimatedDurationMinutes} мин", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.62f))
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onEditDraft,
                    colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Проверить и отредактировать") }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Перед сохранением вы увидите результат и сможете изменить любой шаг.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(.5f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

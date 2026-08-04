package ru.ritual.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
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
import ru.ritual.app.ui.components.MarkdownText

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
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Ink, Color(0xFF34364A))))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Lime), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, null, tint = Ink)
                }
                Spacer(Modifier.size(11.dp))
                Column {
                    Text("Создать из идеи", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("YandexGPT · проектирование и проверка", style = MaterialTheme.typography.labelMedium, color = Lime)
                }
            }
            Spacer(Modifier.height(11.dp))
            Text(
                "Опишите результат — ИИ спроектирует этапы, развилки, предупреждения и проверит маршрут.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(.68f),
            )
        }
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
            minLines = 5,
            leadingIcon = { Text("✦", style = MaterialTheme.typography.titleLarge, color = Ink) },
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(13.dp))
        Text("Характер", style = MaterialTheme.typography.titleLarge)
        Text("Меняет структуру, тон и глубину результата", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.5f))
        Spacer(Modifier.height(7.dp))
        val characterColors = listOf(Lime, Color(0xFFFFD7A8), Color(0xFFBFD9FF), Color(0xFFFFD2DC), Color(0xFFC9F0DD), Color(0xFFD9CEFF))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(GenerationCharacter.entries) { index, item ->
                val selected = characterIndex == index
                Column(
                    Modifier.width(156.dp).height(116.dp).clip(RoundedCornerShape(14.dp))
                        .background(if (selected) characterColors[index] else MaterialTheme.colorScheme.surface)
                        .then(if (selected) Modifier.border(2.dp, Ink, RoundedCornerShape(14.dp)) else Modifier)
                        .clickable { characterIndex = index }
                        .padding(11.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.symbol, style = MaterialTheme.typography.titleLarge, color = Ink)
                        Spacer(Modifier.size(7.dp))
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = Ink.copy(.6f), maxLines = 3)
                }
            }
        }
        Spacer(Modifier.height(13.dp))
        Text("Насколько подробно?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Кратко\n4–6", "Оптимально\n7–9", "Подробно\n10–14").forEachIndexed { index, label ->
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
                        .padding(vertical = 9.dp),
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
                Text("${generatedChecklist.symbol} ${generatedChecklist.title}", style = MaterialTheme.typography.headlineMedium, color = Ink)
                Spacer(Modifier.height(5.dp))
                Text("${generatedChecklist.steps.size} шагов · ~${generatedChecklist.estimatedDurationMinutes} мин", style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.62f))
                if (generatedChecklist.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    MarkdownText(generatedChecklist.description, style = MaterialTheme.typography.bodyMedium, color = Ink.copy(.72f))
                }
                val branches = generatedChecklist.steps.count { it.type == "YES_NO" || it.type == "SINGLE_CHOICE" }
                val warnings = generatedChecklist.steps.count { it.type == "WARNING" }
                Spacer(Modifier.height(8.dp))
                Text("◇ $branches развилок   ⚠ $warnings предупреждений", style = MaterialTheme.typography.labelLarge, color = Ink.copy(.62f))
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

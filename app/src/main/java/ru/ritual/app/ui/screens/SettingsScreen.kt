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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ru.ritual.app.ui.theme.Ink
import ru.ritual.app.ui.theme.Lime

@Composable
fun SettingsScreen(
    hasYandexCredentials: Boolean,
    onSaveCredentials: (String, String) -> Unit,
    onDeleteCredentials: () -> Unit,
    tapNavigation: Boolean,
    onTapNavigationChange: (Boolean) -> Unit,
    keepScreenAwake: Boolean,
    onKeepScreenAwakeChange: (Boolean) -> Unit,
    autoPlayVideoNotes: Boolean,
    onAutoPlayVideoNotesChange: (Boolean) -> Unit,
    generationNotifications: Boolean,
    onGenerationNotificationsChange: (Boolean) -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var folderId by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(hasYandexCredentials) {
        if (hasYandexCredentials) {
            apiKey = ""
            folderId = ""
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
        Text("НАСТРОЙКИ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onBackground.copy(.5f))
        Spacer(Modifier.height(3.dp))
        Text("Всё под контролем", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(14.dp))

        Column(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Ink).padding(15.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(Lime), contentAlignment = Alignment.Center) {
                    Icon(if (hasYandexCredentials) Icons.Outlined.Check else Icons.Outlined.Lock, null, tint = Ink)
                }
                Spacer(Modifier.padding(7.dp))
                Column {
                    Text("YandexGPT", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(
                        if (hasYandexCredentials) "Доступ защищён и готов" else "Доступ ещё не настроен",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasYandexCredentials) Lime else Color.White.copy(.55f),
                    )
                }
            }
            Spacer(Modifier.height(13.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text(if (hasYandexCredentials) "Новый API-ключ" else "API-ключ сервисного аккаунта") },
                placeholder = { Text("AQVN…") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Показать или скрыть")
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = folderId,
                onValueChange = { folderId = it },
                label = { Text("ID каталога Yandex Cloud") },
                placeholder = { Text("b1g…") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSaveCredentials(apiKey, folderId)
                },
                enabled = apiKey.isNotBlank() && folderId.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp),
            ) {
                Text(if (hasYandexCredentials) "Заменить доступ" else "Сохранить на устройстве")
            }
            if (hasYandexCredentials) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onDeleteCredentials,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Удалить данные доступа")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Интерфейс и выполнение", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        PreferenceSwitch(
            icon = Icons.Outlined.TouchApp,
            title = "Навигация тапами",
            text = "Касание справа открывает следующий этап, слева — предыдущий.",
            checked = tapNavigation,
            onCheckedChange = onTapNavigationChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.ScreenLockPortrait,
            title = "Не выключать экран",
            text = "Экран остаётся активным во время выполнения алгоритма.",
            checked = keepScreenAwake,
            onCheckedChange = onKeepScreenAwakeChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.PlayCircleOutline,
            title = "Автозапуск видео",
            text = "Видеозаметки запускаются автоматически при открытии этапа.",
            checked = autoPlayVideoNotes,
            onCheckedChange = onAutoPlayVideoNotesChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.NotificationsActive,
            title = "Готовность алгоритма",
            text = "Показывать системный прогресс и уведомлять после генерации.",
            checked = generationNotifications,
            onCheckedChange = onGenerationNotificationsChange,
        )

        Spacer(Modifier.height(12.dp))
        Text("Безопасность", style = MaterialTheme.typography.titleLarge)
        SecurityRow(
            icon = Icons.Outlined.Security,
            title = "Android Keystore",
            text = "Ключ шифрования создаётся аппаратным хранилищем устройства и не покидает его.",
        )
        SecurityRow(
            icon = Icons.Outlined.CloudOff,
            title = "Без облачной копии",
            text = "API-ключ и ID каталога исключены из резервных копий и экспорта чек-листов.",
        )
        Spacer(Modifier.height(18.dp))
        Text("Важно", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Используйте API-ключ сервисного аккаунта с ролью ai.languageModels.user и областью yc.ai.languageModels.execute. Ограничьте срок действия и удаляйте ключ, когда он не нужен.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(.64f),
        )
    }
}

@Composable
private fun PreferenceSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.padding(7.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(.56f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SecurityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.padding(8.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(.58f))
        }
    }
}

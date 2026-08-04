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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.FilterChip
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
import ru.ritual.app.domain.model.ScheduleViewMode

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
    calendarWeekStartsMonday: Boolean,
    onCalendarWeekStartsMondayChange: (Boolean) -> Unit,
    calendarDefaultView: ScheduleViewMode,
    onCalendarDefaultViewChange: (ScheduleViewMode) -> Unit,
    calendarShowNotes: Boolean,
    onCalendarShowNotesChange: (Boolean) -> Unit,
    calendarOfferSystemExport: Boolean,
    onCalendarOfferSystemExportChange: (Boolean) -> Unit,
    showActiveRunOnHome: Boolean,
    onShowActiveRunOnHomeChange: (Boolean) -> Unit,
    showProgressRange: Boolean,
    onShowProgressRangeChange: (Boolean) -> Unit,
    confirmBeforeStopping: Boolean,
    onConfirmBeforeStoppingChange: (Boolean) -> Unit,
    compactAlgorithmCards: Boolean,
    onCompactAlgorithmCardsChange: (Boolean) -> Unit,
    calendarShowWeekNumbers: Boolean,
    onCalendarShowWeekNumbersChange: (Boolean) -> Unit,
    calendarHighlightCurrentWeek: Boolean,
    onCalendarHighlightCurrentWeekChange: (Boolean) -> Unit,
) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    var folderId by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }
    var showCredentialEditor by rememberSaveable { mutableStateOf(!hasYandexCredentials) }

    LaunchedEffect(hasYandexCredentials) {
        if (hasYandexCredentials) {
            apiKey = ""
            folderId = ""
            showCredentialEditor = false
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
        Text("Настройки", style = MaterialTheme.typography.headlineLarge)
        Text("Только то, что влияет на работу приложения", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(.52f))
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Lime.copy(.42f)).padding(11.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GestureChip("↔", "этапы", Modifier.weight(1f))
            GestureChip("↓", "свернуть", Modifier.weight(1f))
            GestureChip("тап", if (tapNavigation) "включён" else "выключен", Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))

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
            if (hasYandexCredentials && !showCredentialEditor) {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedButton(
                        onClick = { showCredentialEditor = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) { Text("Изменить") }
                    OutlinedButton(
                        onClick = onDeleteCredentials,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFFB4AB)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(40.dp),
                    ) { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(17.dp)); Spacer(Modifier.size(5.dp)); Text("Удалить") }
                }
            } else {
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
        }

        Spacer(Modifier.height(12.dp))
        Text("Выполнение", style = MaterialTheme.typography.titleLarge)
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
            title = "Фоновая генерация",
            text = "Показывать прогресс и сообщать, когда YandexGPT закончит работу.",
            checked = generationNotifications,
            onCheckedChange = onGenerationNotificationsChange,
        )

        Spacer(Modifier.height(12.dp))
        Text("Главная и уведомления", style = MaterialTheme.typography.titleLarge)
        PreferenceSwitch(
            icon = Icons.Outlined.PlayCircleOutline,
            title = "Активный алгоритм на главной",
            text = "Показывать незавершённый алгоритм кружком с прогрессом.",
            checked = showActiveRunOnHome,
            onCheckedChange = onShowActiveRunOnHomeChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.Schedule,
            title = "Диапазон прогресса",
            text = "Для ветвлений показывать минимальный и максимальный путь и процент.",
            checked = showProgressRange,
            onCheckedChange = onShowProgressRangeChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.Security,
            title = "Подтверждать остановку",
            text = "Спрашивать подтверждение перед удалением прогресса выполнения.",
            checked = confirmBeforeStopping,
            onCheckedChange = onConfirmBeforeStoppingChange,
        )
        Spacer(Modifier.height(12.dp))
        Text("Расписание", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(5.dp))
        Text("Вид при открытии", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground.copy(.55f))
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ScheduleViewMode.entries.forEach { mode ->
                FilterChip(
                    selected = calendarDefaultView == mode,
                    onClick = { onCalendarDefaultViewChange(mode) },
                    label = { Text(mode.title) },
                )
            }
        }
        PreferenceSwitch(
            icon = Icons.Outlined.CalendarMonth,
            title = "Неделя с понедельника",
            text = "Первым столбцом календаря будет понедельник.",
            checked = calendarWeekStartsMonday,
            onCheckedChange = onCalendarWeekStartsMondayChange,
        )
        PreferenceSwitch(
            icon = Icons.AutoMirrored.Outlined.Notes,
            title = "Заметки в календаре",
            text = "Показывать дневниковые записи рядом с событиями.",
            checked = calendarShowNotes,
            onCheckedChange = onCalendarShowNotesChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.EventAvailable,
            title = "Системный календарь",
            text = "Предлагать экспорт мероприятий в календарь устройства.",
            checked = calendarOfferSystemExport,
            onCheckedChange = onCalendarOfferSystemExportChange,
        )
        PreferenceSwitch(
            icon = Icons.Outlined.EventAvailable,
            title = "Выделять текущую неделю",
            text = "Обводить текущую неделю и сразу прокручивать карту к ней.",
            checked = calendarHighlightCurrentWeek,
            onCheckedChange = onCalendarHighlightCurrentWeekChange,
        )

        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Security, null, Modifier.size(20.dp), tint = Ink.copy(.7f))
            Spacer(Modifier.size(9.dp))
            Text("Ключ YandexGPT защищён Android Keystore и не попадает в резервные копии.", style = MaterialTheme.typography.bodySmall, color = Ink.copy(.58f))
        }
    }
}

@Composable
private fun GestureChip(symbol: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(10.dp)).background(Color.White.copy(.58f)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(symbol, style = MaterialTheme.typography.titleMedium, color = Ink)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Ink.copy(.55f))
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface).clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Lime.copy(.42f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.padding(7.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(.5f), maxLines = 2)
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

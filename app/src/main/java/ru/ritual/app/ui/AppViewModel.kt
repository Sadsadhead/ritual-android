package ru.ritual.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.ritual.app.data.ChecklistRepository
import ru.ritual.app.data.ActiveRunStore
import ru.ritual.app.data.AlgorithmUsageStore
import ru.ritual.app.data.ScheduleRepository
import ru.ritual.app.data.security.SecureApiKeyStore
import ru.ritual.app.data.network.YandexGptChecklistService
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.AlgorithmSuggestion
import ru.ritual.app.domain.model.ActiveAlgorithmRun
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GenerationCharacter
import ru.ritual.app.domain.model.AlgorithmMetadataSuggestion
import ru.ritual.app.domain.model.AppPreferences
import ru.ritual.app.domain.model.MetadataTarget
import ru.ritual.app.domain.model.ScheduleItem
import ru.ritual.app.domain.model.ScheduleItemType
import ru.ritual.app.domain.model.ScheduleAiSuggestion
import ru.ritual.app.domain.model.ScheduleViewMode
import ru.ritual.app.domain.model.toChecklistReplacing
import ru.ritual.app.notification.GenerationNotifier
import ru.ritual.app.notification.ActiveAlgorithmNotifier
import ru.ritual.app.notification.GenerationKeepAliveService
import ru.ritual.app.widget.WidgetUpdater
import java.util.UUID

data class AppUiState(
    val query: String = "",
    val selectedCategory: String = "Все",
    val hasYandexCredentials: Boolean = false,
    val keyMessage: String? = null,
    val isSavingKey: Boolean = false,
    val isGenerating: Boolean = false,
    val generationStage: String? = null,
    val generatedChecklist: GeneratedChecklist? = null,
    val generationError: String? = null,
    val checklists: List<Checklist> = emptyList(),
    val scheduleItems: List<ScheduleItem> = emptyList(),
    val preferences: AppPreferences = AppPreferences(),
    val isGeneratingMetadata: Boolean = false,
    val metadataTarget: MetadataTarget? = null,
    val metadataSuggestion: AlgorithmMetadataSuggestion? = null,
    val activeRuns: List<ActiveAlgorithmRun> = emptyList(),
    val isImprovingAlgorithm: Boolean = false,
    val improvementStage: String? = null,
    val improvementOriginal: Checklist? = null,
    val improvementProposal: GeneratedChecklist? = null,
    val improvementError: String? = null,
    val isImprovingScheduleItem: Boolean = false,
    val scheduleAiSuggestion: ScheduleAiSuggestion? = null,
    val scheduleAiError: String? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChecklistRepository(application)
    private val scheduleRepository = ScheduleRepository(application)
    private val activeRunStore = ActiveRunStore(application)
    private val usageStore = AlgorithmUsageStore(application)
    private val keyStore = SecureApiKeyStore(application)
    private val yandexGptService = YandexGptChecklistService()
    private val preferencesStore = application.getSharedPreferences("app_preferences", 0)
    private val generationNotifier = GenerationNotifier(application)
    private val activeAlgorithmNotifier = ActiveAlgorithmNotifier(application)
    private val mutableState = MutableStateFlow(AppUiState(preferences = loadPreferences()))
    private var improvementRequestToken = 0L
    private var scheduleAiRequestToken = 0L

    init {
        activeAlgorithmNotifier.cancelAll(emptyList())
        activeRunStore.activeRuns.value.forEach { active ->
            val checklist = repository.checklists.value.firstOrNull { it.id == active.algorithmId }
            if (checklist == null) {
                activeRunStore.clear(active.algorithmId)
                activeAlgorithmNotifier.cancel(active.algorithmId)
            } else {
                val normalized = if (active.visitedStepIds.isEmpty() || active.maxPercent == 0) {
                    activeRunStore.update(
                        checklist = checklist,
                        stepIndex = active.currentStepIndex,
                        visitedStepIds = listOfNotNull(checklist.steps.getOrNull(active.currentStepIndex)?.id),
                    )
                } else active
                activeAlgorithmNotifier.show(normalized)
            }
        }
        WidgetUpdater.updateAll(application)
    }

    val state: StateFlow<AppUiState> = combine(
        mutableState,
        keyStore.hasKey,
        repository.checklists,
        scheduleRepository.items,
        activeRunStore.activeRuns,
    ) { state, hasKey, checklists, scheduleItems, activeRuns ->
        state.copy(
            hasYandexCredentials = hasKey,
            checklists = checklists,
            scheduleItems = scheduleItems,
            activeRuns = activeRuns,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), mutableState.value)

    val checklists: List<Checklist> get() = state.value.checklists
    val history get() = repository.history
    val categories: List<String>
        get() = (listOf("Все", "Ежедневное", "Дом", "Кухня", "Здоровье", "Работа", "Учёба", "Путешествия", "Другое") +
            checklists.map(Checklist::category)).distinct()

    fun filteredChecklists(): List<Checklist> {
        val current = state.value
        val terms = current.query
            .lowercase()
            .replace('#', ' ')
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotBlank)
        return checklists.filter { item ->
            terms.isEmpty() || terms.all(item.searchableText()::contains)
        }
    }

    fun recentChecklistIds(): List<String> = usageStore.recentIds()

    fun recommendationsFor(algorithmId: String): List<AlgorithmSuggestion> {
        val byId = checklists.associateBy(Checklist::id)
        return usageStore.suggestionsFor(algorithmId, byId.keys)
            .mapNotNull { ranked -> byId[ranked.algorithmId]?.let { AlgorithmSuggestion(it, ranked.reason) } }
    }

    fun updateQuery(value: String) {
        mutableState.value = mutableState.value.copy(query = value)
    }

    fun selectCategory(value: String) {
        mutableState.value = mutableState.value.copy(selectedCategory = value)
    }

    fun saveYandexCredentials(apiKey: String, folderId: String) {
        val normalizedKey = apiKey.trim().replace("\n", "").replace("\r", "")
        val normalizedFolderId = folderId.trim().replace("\n", "").replace("\r", "")
        if (normalizedKey.isBlank()) {
            mutableState.value = mutableState.value.copy(keyMessage = "Введите API-ключ")
            return
        }
        if (normalizedFolderId.isBlank()) {
            mutableState.value = mutableState.value.copy(keyMessage = "Введите ID каталога Yandex Cloud")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(isSavingKey = true)
            runCatching { keyStore.save(normalizedKey, normalizedFolderId) }
                .onSuccess {
                    mutableState.value = mutableState.value.copy(
                        keyMessage = "Доступ к YandexGPT сохранён и зашифрован",
                        isSavingKey = false,
                    )
                }
                .onFailure {
                    mutableState.value = mutableState.value.copy(
                        keyMessage = "Не удалось сохранить ключ: ${it.message ?: "ошибка хранилища"}",
                        isSavingKey = false,
                    )
                }
        }
    }

    fun deleteYandexCredentials() {
        viewModelScope.launch {
            keyStore.delete()
            mutableState.value = mutableState.value.copy(keyMessage = "Данные YandexGPT полностью удалены")
        }
    }

    fun improveScheduleItem(
        title: String,
        description: String,
        type: ScheduleItemType,
        category: String,
        tags: List<String>,
        preferences: String,
    ) {
        val requestToken = ++scheduleAiRequestToken
        viewModelScope.launch {
            val credentials = keyStore.read()
            if (credentials == null) {
                mutableState.value = mutableState.value.copy(
                    isImprovingScheduleItem = false,
                    scheduleAiError = "Сначала настройте доступ к YandexGPT",
                )
                return@launch
            }
            mutableState.value = mutableState.value.copy(
                isImprovingScheduleItem = true,
                scheduleAiSuggestion = null,
                scheduleAiError = null,
            )
            runCatching {
                yandexGptService.improveScheduleItem(
                    apiKey = credentials.apiKey,
                    folderId = credentials.folderId,
                    title = title.trim(),
                    description = description.trim(),
                    type = type,
                    category = category,
                    tags = tags,
                    preferences = preferences.trim(),
                )
            }.onSuccess { suggestion ->
                if (requestToken == scheduleAiRequestToken) {
                    mutableState.value = mutableState.value.copy(
                        isImprovingScheduleItem = false,
                        scheduleAiSuggestion = suggestion,
                    )
                }
            }.onFailure { error ->
                if (requestToken == scheduleAiRequestToken) {
                    mutableState.value = mutableState.value.copy(
                        isImprovingScheduleItem = false,
                        scheduleAiError = error.message ?: "Не удалось улучшить запись",
                    )
                }
            }
        }
    }

    fun clearScheduleAiSuggestion() {
        scheduleAiRequestToken += 1
        mutableState.value = mutableState.value.copy(
            isImprovingScheduleItem = false,
            scheduleAiSuggestion = null,
            scheduleAiError = null,
        )
    }

    fun generateChecklist(topic: String, detail: Int, character: GenerationCharacter) {
        if (topic.isBlank()) {
            mutableState.value = mutableState.value.copy(generationError = "Опишите, какую инструкцию нужно создать")
            return
        }
        viewModelScope.launch {
            mutableState.value = mutableState.value.copy(
                isGenerating = true,
                generationStage = "Готовлю запрос",
                generationError = null,
                generatedChecklist = null,
            )
            if (mutableState.value.preferences.generationNotifications) generationNotifier.showProgress("Готовлю запрос")
            val credentials = keyStore.read()
            if (credentials == null) {
                mutableState.value = mutableState.value.copy(isGenerating = false, generationStage = null, generationError = "Сначала настройте доступ к YandexGPT")
                generationNotifier.cancel()
                return@launch
            }
            val keepAlive = mutableState.value.preferences.generationNotifications
            if (keepAlive) runCatching { GenerationKeepAliveService.start(getApplication()) }
            runCatching {
                yandexGptService.generate(credentials.apiKey, credentials.folderId, topic.trim(), detail, character) { stage ->
                    mutableState.value = mutableState.value.copy(generationStage = stage)
                    if (mutableState.value.preferences.generationNotifications) generationNotifier.showProgress(stage)
                }
            }
                .onSuccess { generated ->
                    if (keepAlive) runCatching { GenerationKeepAliveService.stop(getApplication()) }
                    mutableState.value = mutableState.value.copy(isGenerating = false, generationStage = null, generatedChecklist = generated)
                    if (mutableState.value.preferences.generationNotifications) generationNotifier.showComplete(generated.title)
                }
                .onFailure { error ->
                    if (keepAlive) runCatching { GenerationKeepAliveService.stop(getApplication()) }
                    mutableState.value = mutableState.value.copy(
                        isGenerating = false,
                        generationStage = null,
                        generationError = error.message ?: "Не удалось создать алгоритм",
                    )
                    if (mutableState.value.preferences.generationNotifications) {
                        generationNotifier.showError(error.message ?: "Не удалось создать алгоритм")
                    }
                }
        }
    }

    fun requestMetadata(
        title: String,
        description: String,
        stepTitles: List<String>,
        target: MetadataTarget,
    ) {
        viewModelScope.launch {
            val credentials = keyStore.read()
            if (credentials == null) {
                mutableState.value = mutableState.value.copy(keyMessage = "Сначала настройте доступ к YandexGPT")
                return@launch
            }
            mutableState.value = mutableState.value.copy(
                isGeneratingMetadata = true,
                metadataTarget = target,
                metadataSuggestion = null,
            )
            runCatching {
                yandexGptService.suggestMetadata(
                    credentials.apiKey,
                    credentials.folderId,
                    title.trim(),
                    description.trim(),
                    stepTitles,
                )
            }.onSuccess { suggestion ->
                mutableState.value = mutableState.value.copy(
                    isGeneratingMetadata = false,
                    metadataSuggestion = suggestion,
                )
            }.onFailure { error ->
                mutableState.value = mutableState.value.copy(
                    isGeneratingMetadata = false,
                    metadataTarget = null,
                    keyMessage = error.message ?: "Не удалось обработать поля с помощью ИИ",
                )
            }
        }
    }

    fun improveChecklist(checklist: Checklist, preferences: String = "") {
        val requestToken = ++improvementRequestToken
        viewModelScope.launch {
            val credentials = keyStore.read()
            if (credentials == null) {
                mutableState.value = mutableState.value.copy(keyMessage = "Сначала настройте доступ к YandexGPT")
                return@launch
            }
            mutableState.value = mutableState.value.copy(
                isImprovingAlgorithm = true,
                improvementStage = "Готовлю аудит",
                improvementOriginal = checklist,
                improvementProposal = null,
                improvementError = null,
            )
            val keepAlive = mutableState.value.preferences.generationNotifications
            if (keepAlive) {
                runCatching { GenerationKeepAliveService.start(getApplication()) }
                generationNotifier.showProgress("Улучшаю существующий алгоритм")
            }
            runCatching {
                yandexGptService.improve(credentials.apiKey, credentials.folderId, checklist, preferences.trim()) { stage ->
                    if (requestToken == improvementRequestToken) {
                        mutableState.value = mutableState.value.copy(improvementStage = stage)
                        if (keepAlive) generationNotifier.showProgress(stage)
                    }
                }
            }.onSuccess { proposal ->
                if (requestToken == improvementRequestToken) {
                    if (keepAlive) runCatching { GenerationKeepAliveService.stop(getApplication()) }
                    mutableState.value = mutableState.value.copy(
                        isImprovingAlgorithm = false,
                        improvementStage = null,
                        improvementProposal = proposal,
                    )
                    if (keepAlive) generationNotifier.showComplete("Улучшение «${checklist.title}» готово")
                }
            }.onFailure { error ->
                if (requestToken == improvementRequestToken) {
                    if (keepAlive) runCatching { GenerationKeepAliveService.stop(getApplication()) }
                    mutableState.value = mutableState.value.copy(
                        isImprovingAlgorithm = false,
                        improvementStage = null,
                        improvementError = error.message ?: "Не удалось улучшить алгоритм",
                    )
                    if (keepAlive) generationNotifier.showError(error.message ?: "Не удалось улучшить алгоритм")
                }
            }
        }
    }

    fun discardImprovement() {
        improvementRequestToken += 1
        if (mutableState.value.isImprovingAlgorithm) {
            runCatching { GenerationKeepAliveService.stop(getApplication()) }
            generationNotifier.cancel()
        }
        mutableState.value = mutableState.value.copy(
            isImprovingAlgorithm = false,
            improvementStage = null,
            improvementOriginal = null,
            improvementProposal = null,
            improvementError = null,
        )
    }

    fun acceptImprovement() {
        val original = mutableState.value.improvementOriginal ?: return
        val proposal = mutableState.value.improvementProposal ?: return
        val improved = proposal.toChecklistReplacing(original)
        repository.save(improved)
        if (state.value.activeRuns.any { it.algorithmId == original.id }) finishRun(original.id)
        WidgetUpdater.updateAlgorithm(getApplication())
        mutableState.value = mutableState.value.copy(
            isImprovingAlgorithm = false,
            improvementStage = null,
            improvementOriginal = null,
            improvementProposal = null,
            improvementError = null,
            keyMessage = "Улучшенная версия «${improved.title}» сохранена",
        )
    }

    fun consumeMetadataSuggestion() {
        mutableState.value = mutableState.value.copy(metadataSuggestion = null, metadataTarget = null)
    }

    fun setTapNavigation(enabled: Boolean) = updatePreferences { it.copy(tapNavigation = enabled) }
    fun setKeepScreenAwake(enabled: Boolean) = updatePreferences { it.copy(keepScreenAwake = enabled) }
    fun setAutoPlayVideoNotes(enabled: Boolean) = updatePreferences { it.copy(autoPlayVideoNotes = enabled) }
    fun setGenerationNotifications(enabled: Boolean) {
        updatePreferences { it.copy(generationNotifications = enabled) }
        if (!enabled) generationNotifier.cancel()
    }
    fun setCalendarWeekStartsMonday(enabled: Boolean) = updatePreferences { it.copy(calendarWeekStartsMonday = enabled) }
    fun setCalendarDefaultView(view: ScheduleViewMode) = updatePreferences { it.copy(calendarDefaultView = view) }
    fun setCalendarShowNotes(enabled: Boolean) = updatePreferences { it.copy(calendarShowNotes = enabled) }
    fun setCalendarOfferSystemExport(enabled: Boolean) = updatePreferences { it.copy(calendarOfferSystemExport = enabled) }

    fun saveScheduleItem(item: ScheduleItem, originalStartMillis: Long? = null, wholeSeries: Boolean = true) {
        if (originalStartMillis != null) scheduleRepository.saveOccurrence(item, originalStartMillis, wholeSeries)
        else scheduleRepository.save(item)
        mutableState.value = mutableState.value.copy(keyMessage = "Событие сохранено")
        WidgetUpdater.updateSchedule(getApplication())
    }

    fun deleteScheduleItem(item: ScheduleItem, occurrenceStartMillis: Long, wholeSeries: Boolean) {
        val isSeries = item.recurrence.frequency != ru.ritual.app.domain.model.RepeatFrequency.None ||
            state.value.scheduleItems.any { it.seriesId == item.id }
        scheduleRepository.delete(item, occurrenceStartMillis, wholeSeries)
        mutableState.value = mutableState.value.copy(keyMessage = if (wholeSeries && isSeries) "Цепочка удалена" else "Событие удалено")
        WidgetUpdater.updateSchedule(getApplication())
    }

    fun clearGeneratedChecklist() {
        mutableState.value = mutableState.value.copy(generatedChecklist = null, generationError = null)
    }

    fun saveChecklist(checklist: Checklist) {
        repository.save(checklist)
        WidgetUpdater.updateAlgorithm(getApplication())
        mutableState.value = mutableState.value.copy(
            generatedChecklist = null,
            generationError = null,
            keyMessage = "Алгоритм «${checklist.title}» сохранён на устройстве",
        )
    }

    fun deleteChecklist(checklistId: String) {
        val title = checklists.firstOrNull { it.id == checklistId }?.title ?: "Алгоритм"
        repository.delete(checklistId)
        if (state.value.activeRuns.any { it.algorithmId == checklistId }) finishRun(checklistId)
        WidgetUpdater.updateAlgorithm(getApplication())
        mutableState.value = mutableState.value.copy(keyMessage = "Алгоритм «$title» удалён")
    }

    fun toggleChecklistFavorite(checklistId: String) {
        val checklist = checklists.firstOrNull { it.id == checklistId } ?: return
        val updated = checklist.copy(isFavorite = !checklist.isFavorite)
        repository.save(updated)
        WidgetUpdater.updateAlgorithm(getApplication())
        mutableState.value = mutableState.value.copy(
            keyMessage = if (updated.isFavorite) "Добавлено в избранное" else "Удалено из избранного",
        )
    }

    fun duplicateChecklist(checklistId: String) {
        val checklist = checklists.firstOrNull { it.id == checklistId } ?: return
        val duplicate = checklist.copy(
            id = "user-${UUID.randomUUID()}",
            title = "${checklist.title} · копия",
            isFavorite = false,
            progress = 0f,
        )
        repository.save(duplicate)
        WidgetUpdater.updateAlgorithm(getApplication())
        mutableState.value = mutableState.value.copy(keyMessage = "Копия алгоритма создана")
    }

    fun updateRun(checklist: Checklist, stepIndex: Int, visitedStepIds: List<String>) {
        if (activeRunStore.activeRuns.value.none { it.algorithmId == checklist.id }) {
            usageStore.recordStart(checklist.id)
        }
        val run = activeRunStore.update(checklist, stepIndex, visitedStepIds)
        activeAlgorithmNotifier.show(run)
        WidgetUpdater.updateAlgorithm(getApplication())
    }

    fun finishRun(algorithmId: String) {
        usageStore.recordFinish(algorithmId)
        activeRunStore.clear(algorithmId)
        activeAlgorithmNotifier.cancel(algorithmId)
        WidgetUpdater.updateAlgorithm(getApplication())
    }

    fun finishAllRuns() {
        val runs = activeRunStore.activeRuns.value
        runs.forEach { usageStore.recordFinish(it.algorithmId) }
        activeRunStore.clearAll()
        activeAlgorithmNotifier.cancelAll(runs)
        WidgetUpdater.updateAlgorithm(getApplication())
    }

    fun setShowActiveRunOnHome(enabled: Boolean) = updatePreferences { it.copy(showActiveRunOnHome = enabled) }
    fun setShowProgressRange(enabled: Boolean) = updatePreferences { it.copy(showProgressRange = enabled) }
    fun setConfirmBeforeStopping(enabled: Boolean) = updatePreferences { it.copy(confirmBeforeStopping = enabled) }
    fun setCompactAlgorithmCards(enabled: Boolean) = updatePreferences { it.copy(compactAlgorithmCards = enabled) }
    fun setCalendarShowWeekNumbers(enabled: Boolean) = updatePreferences { it.copy(calendarShowWeekNumbers = enabled) }
    fun setCalendarHighlightCurrentWeek(enabled: Boolean) = updatePreferences { it.copy(calendarHighlightCurrentWeek = enabled) }

    fun consumeMessage() {
        mutableState.value = mutableState.value.copy(keyMessage = null)
    }

    private fun updatePreferences(transform: (AppPreferences) -> AppPreferences) {
        val updated = transform(mutableState.value.preferences)
        preferencesStore.edit()
            .putBoolean("tap_navigation", updated.tapNavigation)
            .putBoolean("keep_screen_awake", updated.keepScreenAwake)
            .putBoolean("autoplay_video", updated.autoPlayVideoNotes)
            .putBoolean("generation_notifications", updated.generationNotifications)
            .putBoolean("calendar_week_monday", updated.calendarWeekStartsMonday)
            .putString("calendar_default_view", updated.calendarDefaultView.name)
            .putBoolean("calendar_show_notes", updated.calendarShowNotes)
            .putBoolean("calendar_offer_system_export", updated.calendarOfferSystemExport)
            .putBoolean("show_active_run_home", updated.showActiveRunOnHome)
            .putBoolean("show_progress_range", updated.showProgressRange)
            .putBoolean("confirm_before_stopping", updated.confirmBeforeStopping)
            .putBoolean("compact_algorithm_cards", updated.compactAlgorithmCards)
            .putBoolean("calendar_week_numbers", updated.calendarShowWeekNumbers)
            .putBoolean("calendar_highlight_current_week", updated.calendarHighlightCurrentWeek)
            .apply()
        mutableState.value = mutableState.value.copy(preferences = updated)
    }

    private fun loadPreferences() = AppPreferences(
        tapNavigation = preferencesStore.getBoolean("tap_navigation", true),
        keepScreenAwake = preferencesStore.getBoolean("keep_screen_awake", false),
        autoPlayVideoNotes = preferencesStore.getBoolean("autoplay_video", false),
        generationNotifications = preferencesStore.getBoolean("generation_notifications", true),
        calendarWeekStartsMonday = preferencesStore.getBoolean("calendar_week_monday", true),
        calendarDefaultView = runCatching {
            ScheduleViewMode.valueOf(preferencesStore.getString("calendar_default_view", ScheduleViewMode.Month.name) ?: ScheduleViewMode.Month.name)
        }.getOrDefault(ScheduleViewMode.Month),
        calendarShowNotes = preferencesStore.getBoolean("calendar_show_notes", true),
        calendarOfferSystemExport = preferencesStore.getBoolean("calendar_offer_system_export", true),
        showActiveRunOnHome = preferencesStore.getBoolean("show_active_run_home", true),
        showProgressRange = preferencesStore.getBoolean("show_progress_range", true),
        confirmBeforeStopping = preferencesStore.getBoolean("confirm_before_stopping", true),
        compactAlgorithmCards = preferencesStore.getBoolean("compact_algorithm_cards", true),
        calendarShowWeekNumbers = preferencesStore.getBoolean("calendar_week_numbers", true),
        calendarHighlightCurrentWeek = preferencesStore.getBoolean("calendar_highlight_current_week", true),
    )
}

private fun Checklist.searchableText(): String = buildString {
    append(title).append(' ')
    append(description).append(' ')
    append(category).append(' ')
    append(tags.joinToString(" ")).append(' ')
    steps.forEach { step ->
        append(step.eyebrow).append(' ')
        append(step.title).append(' ')
        append(step.description).append(' ')
        append(step.note).append(' ')
        append(step.checklistItems.joinToString(" ")).append(' ')
        append(step.options.joinToString(" ")).append(' ')
        append(step.attachments.joinToString(" ") { it.name }).append(' ')
    }
}.lowercase()

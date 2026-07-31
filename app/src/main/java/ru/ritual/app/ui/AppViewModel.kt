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
import ru.ritual.app.data.security.SecureApiKeyStore
import ru.ritual.app.data.network.YandexGptChecklistService
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GenerationCharacter
import ru.ritual.app.domain.model.AlgorithmMetadataSuggestion
import ru.ritual.app.domain.model.AppPreferences
import ru.ritual.app.domain.model.MetadataTarget
import ru.ritual.app.notification.GenerationNotifier

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
    val preferences: AppPreferences = AppPreferences(),
    val isGeneratingMetadata: Boolean = false,
    val metadataTarget: MetadataTarget? = null,
    val metadataSuggestion: AlgorithmMetadataSuggestion? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChecklistRepository(application)
    private val keyStore = SecureApiKeyStore(application)
    private val yandexGptService = YandexGptChecklistService()
    private val preferencesStore = application.getSharedPreferences("app_preferences", 0)
    private val generationNotifier = GenerationNotifier(application)
    private val mutableState = MutableStateFlow(AppUiState(preferences = loadPreferences()))

    val state: StateFlow<AppUiState> = combine(mutableState, keyStore.hasKey, repository.checklists) { state, hasKey, checklists ->
        state.copy(hasYandexCredentials = hasKey, checklists = checklists)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppUiState())

    val checklists: List<Checklist> get() = state.value.checklists
    val history get() = repository.history
    val categories: List<String>
        get() = (listOf("Все", "Ежедневное", "Дом", "Кухня", "Здоровье", "Работа", "Учёба", "Путешествия", "Другое") +
            checklists.map(Checklist::category)).distinct()

    fun filteredChecklists(): List<Checklist> {
        val current = state.value
        return checklists.filter { item ->
            (current.selectedCategory == "Все" || item.category == current.selectedCategory) &&
                (current.query.isBlank() || item.title.contains(current.query, ignoreCase = true) ||
                    item.description.contains(current.query, ignoreCase = true) ||
                    item.tags.any { it.contains(current.query, ignoreCase = true) })
        }
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
                return@launch
            }
            runCatching {
                yandexGptService.generate(credentials.apiKey, credentials.folderId, topic.trim(), detail, character) { stage ->
                    mutableState.value = mutableState.value.copy(generationStage = stage)
                    if (mutableState.value.preferences.generationNotifications) generationNotifier.showProgress(stage)
                }
            }
                .onSuccess { generated ->
                    mutableState.value = mutableState.value.copy(isGenerating = false, generationStage = null, generatedChecklist = generated)
                    if (mutableState.value.preferences.generationNotifications) generationNotifier.showComplete(generated.title)
                }
                .onFailure { error ->
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

    fun clearGeneratedChecklist() {
        mutableState.value = mutableState.value.copy(generatedChecklist = null, generationError = null)
    }

    fun saveChecklist(checklist: Checklist) {
        repository.save(checklist)
        mutableState.value = mutableState.value.copy(
            generatedChecklist = null,
            generationError = null,
            keyMessage = "Алгоритм «${checklist.title}» сохранён на устройстве",
        )
    }

    fun deleteChecklist(checklistId: String) {
        val title = checklists.firstOrNull { it.id == checklistId }?.title ?: "Алгоритм"
        repository.delete(checklistId)
        mutableState.value = mutableState.value.copy(keyMessage = "Алгоритм «$title» удалён")
    }

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
            .apply()
        mutableState.value = mutableState.value.copy(preferences = updated)
    }

    private fun loadPreferences() = AppPreferences(
        tapNavigation = preferencesStore.getBoolean("tap_navigation", true),
        keepScreenAwake = preferencesStore.getBoolean("keep_screen_awake", false),
        autoPlayVideoNotes = preferencesStore.getBoolean("autoplay_video", false),
        generationNotifications = preferencesStore.getBoolean("generation_notifications", true),
    )
}

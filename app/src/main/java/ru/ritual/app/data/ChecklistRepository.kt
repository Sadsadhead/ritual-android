package ru.ritual.app.data

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import ru.ritual.app.domain.model.AttachmentType
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.ChecklistStep
import ru.ritual.app.domain.model.RunRecord
import ru.ritual.app.domain.model.StepAttachment
import ru.ritual.app.domain.model.StepType
import ru.ritual.app.ui.theme.Apricot
import ru.ritual.app.ui.theme.Lavender
import ru.ritual.app.ui.theme.Lime
import ru.ritual.app.ui.theme.Sky

class ChecklistRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("saved_algorithms", Context.MODE_PRIVATE)
    private val builtInChecklists = listOf(
        Checklist(
            id = "leave-home",
            title = "Перед выходом",
            description = "Спокойно выйти из дома и ничего не забыть",
            category = "Ежедневное",
            durationMinutes = 4,
            accent = Lime,
            emoji = "↗",
            isFavorite = true,
            progress = .63f,
            steps = listOf(
                ChecklistStep("keys", "01 · ВАЖНОЕ", "Ключи с собой?", "Проверьте карманы или отделение сумки. Домофонный ключ тоже на месте.", StepType.YesNo),
                ChecklistStep("windows", "02 · БЕЗОПАСНОСТЬ", "Закройте окна", "Особенно если ожидается дождь или сильный ветер."),
                ChecklistStep("water", "03 · БЕЗОПАСНОСТЬ", "Проверьте воду и плиту", "Краны закрыты, конфорки выключены, рядом ничего не нагревается."),
                ChecklistStep("devices", "04 · ДОМ", "Выключите лишние приборы", "Утюг, фен и зарядки можно отключить от сети."),
                ChecklistStep("ready", "ГОТОВО", "Можно выходить", "Всё проверено. Хорошего дня!", StepType.Final),
            ),
        ),
        Checklist(
            id = "laundry",
            title = "Стирка без сюрпризов",
            description = "От сортировки вещей до правильного режима",
            category = "Дом",
            durationMinutes = 8,
            accent = Sky,
            emoji = "◌",
            steps = listOf(
                ChecklistStep("sort", "01 · ПОДГОТОВКА", "Разделите бельё", "Белое, цветное и тёмное лучше стирать отдельно."),
                ChecklistStep("labels", "02 · ПРОВЕРКА", "Посмотрите ярлыки", "Отложите вещи только для ручной или деликатной стирки.", StepType.YesNo),
                ChecklistStep("load", "03 · ЗАГРУЗКА", "Загрузите барабан", "Оставьте сверху пространство примерно с ладонь."),
                ChecklistStep("detergent", "04 · СРЕДСТВО", "Добавьте средство", "Следуйте дозировке на упаковке."),
                ChecklistStep("mode", "05 · РЕЖИМ", "Выберите программу", "Для смешанных тканей начните с 30 °C и 800 оборотов."),
                ChecklistStep("timer", "06 · ОЖИДАНИЕ", "Запустите машину", "Мы напомним, когда можно доставать бельё.", StepType.Timer, 5),
                ChecklistStep("done", "ГОТОВО", "Стирка запущена", "Самое сложное уже позади.", StepType.Final),
            ),
        ),
        Checklist(
            id = "omelette",
            title = "Идеальный омлет",
            description = "Воздушный завтрак за десять минут",
            category = "Кухня",
            durationMinutes = 10,
            accent = Apricot,
            emoji = "◒",
            steps = listOf(
                ChecklistStep(
                    "ingredients",
                    "01 · ПОДГОТОВКА",
                    "Соберите продукты",
                    "Отмечайте продукты по мере подготовки.",
                    checklistItems = listOf("2 яйца", "30 мл молока", "Щепотка соли", "Кусочек сливочного масла"),
                ),
                ChecklistStep("mix", "02 · ОСНОВА", "Смешайте яйца", "Перемешайте вилкой до однородности, не взбивая в пену."),
                ChecklistStep("pan", "03 · СКОВОРОДА", "Растопите масло", "Средний огонь, масло не должно темнеть."),
                ChecklistStep("cook", "04 · ГОТОВИМ", "Вылейте смесь", "Готовьте 3–4 минуты под крышкой.", StepType.Timer, 4),
                ChecklistStep("done", "ГОТОВО", "Завтрак готов", "Добавьте свежую зелень и подавайте сразу.", StepType.Final),
            ),
        ),
        Checklist(
            id = "trip",
            title = "Короткая поездка",
            description = "Собраться легко и без тревоги",
            category = "Путешествия",
            durationMinutes = 15,
            accent = Lavender,
            emoji = "◇",
            steps = listOf(
                ChecklistStep("docs", "01 · ДОКУМЕНТЫ", "Документы и билеты", "Паспорт, билеты, страховка и бронирования."),
                ChecklistStep("clothes", "02 · ВЕЩИ", "Одежда по погоде", "Соберите комплекты по дням и один запасной."),
                ChecklistStep("care", "03 · УХОД", "Несессер", "Только нужное, жидкости — в небольших флаконах."),
                ChecklistStep("tech", "04 · ТЕХНИКА", "Зарядки и наушники", "Проверьте кабели и зарядите пауэрбанк."),
                ChecklistStep("done", "ГОТОВО", "Чемодан собран", "Закройте его и поставьте у двери.", StepType.Final),
            ),
        ),
    )

    private var userChecklists: List<Checklist> = loadUserChecklists()
    private var deletedBuiltInIds: Set<String> = preferences
        .getStringSet(DELETED_BUILT_INS_KEY, emptySet())
        .orEmpty()
        .toSet()
    private val mutableChecklists = MutableStateFlow(mergedChecklists())
    val checklists: StateFlow<List<Checklist>> = mutableChecklists.asStateFlow()

    fun save(checklist: Checklist) {
        userChecklists = userChecklists.filterNot { it.id == checklist.id } + checklist
        deletedBuiltInIds = deletedBuiltInIds - checklist.id
        preferences.edit().putString(USER_ALGORITHMS_KEY, encode(userChecklists).toString()).apply()
        preferences.edit().putStringSet(DELETED_BUILT_INS_KEY, deletedBuiltInIds).apply()
        mutableChecklists.value = mergedChecklists()
    }

    fun delete(checklistId: String) {
        val isBuiltIn = builtInChecklists.any { it.id == checklistId }
        userChecklists = userChecklists.filterNot { it.id == checklistId }
        if (isBuiltIn) deletedBuiltInIds = deletedBuiltInIds + checklistId
        preferences.edit()
            .putString(USER_ALGORITHMS_KEY, encode(userChecklists).toString())
            .putStringSet(DELETED_BUILT_INS_KEY, deletedBuiltInIds)
            .apply()
        mutableChecklists.value = mergedChecklists()
    }

    private fun mergedChecklists(): List<Checklist> {
        val overriddenIds = userChecklists.mapTo(mutableSetOf(), Checklist::id)
        return userChecklists.asReversed() + builtInChecklists.filterNot {
            it.id in overriddenIds || it.id in deletedBuiltInIds
        }
    }

    val history = listOf(
        RunRecord("Перед выходом", "Сегодня, 08:42", "3 мин", 100),
        RunRecord("Идеальный омлет", "Вчера, 10:16", "11 мин", 100),
        RunRecord("Короткая поездка", "27 июля, 19:05", "9 мин", 62),
    )

    private fun encode(checklists: List<Checklist>) = JSONArray().apply {
        checklists.forEach { checklist ->
            put(
                JSONObject()
                    .put("id", checklist.id)
                    .put("title", checklist.title)
                    .put("description", checklist.description)
                    .put("category", checklist.category)
                    .put("durationMinutes", checklist.durationMinutes)
                    .put("accent", checklist.accent.toArgb())
                    .put("emoji", checklist.emoji)
                    .put("tags", JSONArray(checklist.tags))
                    .put("steps", JSONArray().apply {
                        checklist.steps.forEach { step ->
                            put(
                                JSONObject()
                                    .put("id", step.id)
                                    .put("eyebrow", step.eyebrow)
                                    .put("title", step.title)
                                    .put("description", step.description)
                                    .put("type", step.type.name)
                                    .put("timerSeconds", step.timerSeconds)
                                    .put("checklistItems", JSONArray(step.checklistItems))
                                    .put("options", JSONArray(step.options))
                                    .put("note", step.note)
                                    .put("parentConditionId", step.parentConditionId)
                                    .put("parentOptionIndex", step.parentOptionIndex)
                                    .put("attachments", JSONArray().apply {
                                        step.attachments.forEach { attachment ->
                                            put(
                                                JSONObject()
                                                    .put("uri", attachment.uri)
                                                    .put("type", attachment.type.name)
                                                    .put("name", attachment.name),
                                            )
                                        }
                                    }),
                            )
                        }
                    }),
            )
        }
    }

    private fun loadUserChecklists(): List<Checklist> = runCatching {
        val raw = preferences.getString(USER_ALGORITHMS_KEY, null) ?: return@runCatching emptyList()
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val stepsJson = item.optJSONArray("steps") ?: JSONArray()
                val steps = buildList {
                    for (stepIndex in 0 until stepsJson.length()) {
                        val step = stepsJson.getJSONObject(stepIndex)
                        val attachmentsJson = step.optJSONArray("attachments") ?: JSONArray()
                        add(
                            ChecklistStep(
                                id = step.getString("id"),
                                eyebrow = step.optString("eyebrow"),
                                title = step.optString("title"),
                                description = step.optString("description"),
                                type = runCatching { StepType.valueOf(step.optString("type")) }.getOrDefault(StepType.Checkbox),
                                timerSeconds = step.optInt("timerSeconds", 0).takeIf { it > 0 },
                                checklistItems = step.optJSONArray("checklistItems").toStringList(),
                                options = step.optJSONArray("options").toOptionList(),
                                note = step.optString("note"),
                                parentConditionId = step.optString("parentConditionId")
                                    .takeIf { it.isNotBlank() && it != "null" },
                                parentOptionIndex = step.optInt("parentOptionIndex", -1).takeIf { it >= 0 },
                                attachments = buildList {
                                    for (attachmentIndex in 0 until attachmentsJson.length()) {
                                        val attachment = attachmentsJson.getJSONObject(attachmentIndex)
                                        add(
                                            StepAttachment(
                                                uri = attachment.getString("uri"),
                                                type = runCatching { AttachmentType.valueOf(attachment.getString("type")) }.getOrDefault(AttachmentType.File),
                                                name = attachment.optString("name", "Вложение"),
                                            ),
                                        )
                                    }
                                },
                            ),
                        )
                    }
                }
                if (steps.isNotEmpty()) {
                    add(
                        Checklist(
                            id = item.getString("id"),
                            title = item.optString("title", "Алгоритм"),
                            description = item.optString("description"),
                            category = item.optString("category", "Другое"),
                            durationMinutes = item.optInt("durationMinutes", steps.size * 2).coerceAtLeast(1),
                            accent = Color(item.optInt("accent", Sky.toArgb())),
                            emoji = item.optString("emoji", "◇"),
                            steps = steps,
                            tags = item.optJSONArray("tags").toStringList(),
                        ),
                    )
                }
            }
        }
    }.getOrDefault(emptyList())

    private fun JSONArray?.toStringList(): List<String> = buildList {
        val source = this@toStringList ?: return@buildList
        for (index in 0 until source.length()) source.optString(index).takeIf(String::isNotBlank)?.let(::add)
    }

    private fun JSONArray?.toOptionList(): List<String> = buildList {
        val source = this@toOptionList ?: return@buildList
        for (index in 0 until source.length()) add(source.optString(index))
    }

    private companion object {
        const val USER_ALGORITHMS_KEY = "algorithms_json_v1"
        const val DELETED_BUILT_INS_KEY = "deleted_built_in_algorithms"
    }
}

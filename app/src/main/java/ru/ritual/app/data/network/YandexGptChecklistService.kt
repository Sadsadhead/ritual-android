package ru.ritual.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GeneratedStep
import ru.ritual.app.domain.model.GenerationCharacter
import ru.ritual.app.domain.model.AlgorithmMetadataSuggestion
import ru.ritual.app.domain.model.Checklist
import ru.ritual.app.domain.model.ScheduleAiSuggestion
import ru.ritual.app.domain.model.ScheduleItemType
import ru.ritual.app.domain.model.StepType
import ru.ritual.app.domain.model.firstStepInBranchIndex
import ru.ritual.app.domain.model.nextStepIndexAfter
import java.net.HttpURLConnection
import java.net.URL

class YandexGptChecklistService {
    suspend fun improve(
        apiKey: String,
        folderId: String,
        checklist: Checklist,
        preferences: String = "",
        onStage: (String) -> Unit = {},
    ): GeneratedChecklist = withContext(Dispatchers.IO) {
        val sourceJson = checklist.toGenerationJson()
        onStage("Анализирую текущую структуру")
        val improvedJson = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = IMPROVER_PROMPT,
            user = buildString {
                append("Текущий алгоритм для улучшения:\n$sourceJson")
                append("\n\nПожелания пользователя: ")
                append(preferences.trim().ifBlank { "нет дополнительных пожеланий; улучши по общим критериям" })
            },
            temperature = 0.18,
            maxTokens = 6_000,
            jsonObject = true,
        )
        onStage("Проверяю изменения и переходы")
        var result = parseAlgorithm(improvedJson)
        if (!result.hasRichInteractiveStructure()) {
            onStage("Исправляю ветвления и предупреждения")
            result = parseAlgorithm(
                complete(
                    apiKey = apiKey,
                    folderId = folderId,
                    system = IMPROVEMENT_REPAIR_PROMPT,
                    user = "Исходный алгоритм:\n$sourceJson\n\nПожелания пользователя:\n${preferences.trim().ifBlank { "нет дополнительных пожеланий" }}\n\nПредложенное улучшение:\n$improvedJson",
                    temperature = 0.1,
                    maxTokens = 6_000,
                    jsonObject = true,
                ),
            )
        }
        onStage("Готовлю сравнение версий")
        result
    }

    suspend fun suggestMetadata(
        apiKey: String,
        folderId: String,
        title: String,
        description: String,
        stepTitles: List<String>,
    ): AlgorithmMetadataSuggestion = withContext(Dispatchers.IO) {
        val response = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = """
                Ты редактор и классификатор пользовательских алгоритмов. Верни только JSON без Markdown.
                Создай короткое ясное название, полезное описание в 1–2 предложениях, одну категорию и 3–6 тегов.
                Категория должна быть одной из: Ежедневное, Дом, Кухня, Здоровье, Работа, Учёба,
                Путешествия, Финансы, Авто, Хобби, Другое. Теги — короткие русские слова без #.
                Схема: {"title":"...","description":"...","category":"...","tags":["...","..."]}
            """.trimIndent(),
            user = "Название: $title\nОписание: $description\nЭтапы: ${stepTitles.joinToString("; ")}",
            temperature = 0.25,
            maxTokens = 700,
            jsonObject = true,
        )
        val data = runCatching { JSONObject(response) }
            .getOrElse { throw YandexGptException(502, "YandexGPT не вернул метаданные алгоритма") }
        AlgorithmMetadataSuggestion(
            title = data.optString("title").trim().ifBlank { title.ifBlank { "Новый алгоритм" } },
            description = data.optString("description").trim().ifBlank { description },
            category = data.optString("category").trim().ifBlank { "Другое" },
            tags = buildList {
                val tags = data.optJSONArray("tags") ?: JSONArray()
                for (index in 0 until tags.length()) {
                    tags.optString(index).trim().removePrefix("#").takeIf(String::isNotBlank)?.let(::add)
                }
            }.distinct().take(8),
        )
    }

    suspend fun improveScheduleItem(
        apiKey: String,
        folderId: String,
        title: String,
        description: String,
        type: ScheduleItemType,
        category: String,
        tags: List<String>,
        preferences: String = "",
    ): ScheduleAiSuggestion = withContext(Dispatchers.IO) {
        val response = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = """
                Ты лаконичный редактор персонального расписания. Улучши запись, не меняя её смысл,
                дату, время и тип. Название должно быть коротким и конкретным. Описание — полезным,
                спокойным и до трёх коротких предложений; для напоминания добавь ясный критерий выполнения,
                для мероприятия — цель или подготовку, для заметки — удобную структуру. Подбери одну
                практичную категорию и 2–5 коротких русских тегов без #. Не выдумывай факты, людей,
                адреса и обязательства. Верни только JSON без Markdown:
                {"title":"...","description":"...","category":"...","tags":["...","..."]}
            """.trimIndent(),
            user = "Тип: ${type.title}\nНазвание: $title\nОписание: $description\nКатегория: $category\nТеги: ${tags.joinToString(", ")}\nПожелания пользователя: ${preferences.trim().ifBlank { "нет дополнительных пожеланий" }}",
            temperature = 0.25,
            maxTokens = 800,
            jsonObject = true,
        )
        val data = runCatching { JSONObject(response) }
            .getOrElse { throw YandexGptException(502, "YandexGPT не вернул улучшенную запись") }
        ScheduleAiSuggestion(
            title = data.optString("title").trim().ifBlank { title.ifBlank { type.title } },
            description = data.optString("description").trim().ifBlank { description },
            category = data.optString("category").trim().ifBlank { category.ifBlank { "Другое" } },
            tags = buildList {
                val values = data.optJSONArray("tags") ?: JSONArray()
                for (index in 0 until values.length()) {
                    values.optString(index).trim().removePrefix("#").takeIf(String::isNotBlank)?.let(::add)
                }
            }.distinct().take(6),
        )
    }

    suspend fun generate(
        apiKey: String,
        folderId: String,
        topic: String,
        detail: Int,
        character: GenerationCharacter,
        onStage: (String) -> Unit = {},
    ): GeneratedChecklist = withContext(Dispatchers.IO) {
        onStage("Расширяю исходный запрос")
        val expandedPrompt = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = "$EXPANDER_PROMPT\n\nХарактер результата: ${character.instruction}",
            user = topic,
            temperature = 0.35,
            maxTokens = 1_500,
            jsonObject = false,
        )

        onStage("Проектирую этапы и переходы")
        val draftJson = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = algorithmPrompt(detail, character),
            user = "Исходный запрос пользователя:\n$topic\n\nРасширенное техническое задание:\n$expandedPrompt",
            temperature = 0.2,
            maxTokens = 4_500,
            jsonObject = true,
        )

        onStage("Проверяю логику и исправляю ошибки")
        val verifiedJson = complete(
            apiKey = apiKey,
            folderId = folderId,
            system = "$VERIFIER_PROMPT\n\nСохрани выбранный характер результата: ${character.instruction}",
            user = "Исходная задача:\n$topic\n\nАлгоритм для аудита:\n$draftJson",
            temperature = 0.1,
            maxTokens = 4_500,
            jsonObject = true,
        )

        onStage("Проверяю структуру результата")
        var result = parseAlgorithm(verifiedJson)
        if (!result.hasRichInteractiveStructure()) {
            onStage("Усиливаю ветвления и предупреждения")
            val repairedJson = complete(
                apiKey = apiKey,
                folderId = folderId,
                system = REPAIR_PROMPT,
                user = "Исходная задача:\n$topic\n\nJSON для структурного исправления:\n$verifiedJson",
                temperature = 0.12,
                maxTokens = 5_500,
                jsonObject = true,
            )
            result = parseAlgorithm(repairedJson)
        }
        result
    }

    private fun complete(
        apiKey: String,
        folderId: String,
        system: String,
        user: String,
        temperature: Double,
        maxTokens: Int,
        jsonObject: Boolean,
    ): String {
        val body = JSONObject()
            .put("modelUri", "gpt://${folderId.trim()}/yandexgpt/latest")
            .put(
                "completionOptions",
                JSONObject()
                    .put("stream", false)
                    .put("temperature", temperature)
                    .put("maxTokens", maxTokens.toString()),
            )
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("text", system))
                    .put(JSONObject().put("role", "user").put("text", user)),
            )
        if (jsonObject) body.put("jsonObject", true)

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 90_000
            doOutput = true
            setRequestProperty("Authorization", "Api-Key $apiKey")
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
        }
        try {
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
            val status = connection.responseCode
            val responseBody = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw YandexGptException(status, readableError(status, responseBody))
            return extractText(responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private fun extractText(raw: String): String {
        val response = runCatching { JSONObject(raw) }
            .getOrElse { throw YandexGptException(502, "YandexGPT вернул повреждённый ответ") }
        return response.optJSONObject("result")
            ?.optJSONArray("alternatives")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("text")
            ?.trim()
            ?.removePrefix("```json")
            ?.removePrefix("```")
            ?.removeSuffix("```")
            ?.trim()
            ?.takeIf(String::isNotBlank)
            ?: throw YandexGptException(502, "YandexGPT вернул пустой ответ")
    }

    private fun parseAlgorithm(text: String): GeneratedChecklist {
        val data = runCatching { JSONObject(text) }
            .getOrElse { throw YandexGptException(502, "Проверенный ответ YandexGPT не является алгоритмом") }
        val stepsJson = data.optJSONArray("steps") ?: JSONArray()
        if (stepsJson.length() < 2) throw YandexGptException(502, "В алгоритме должно быть не меньше двух этапов")

        val usedIds = mutableSetOf<String>()
        val parsed = buildList {
            for (index in 0 until stepsJson.length()) {
                val item = stepsJson.optJSONObject(index) ?: continue
                val requestedId = item.optString("id").trim().ifBlank { "step-${index + 1}" }
                val id = if (usedIds.add(requestedId)) requestedId else "step-${index + 1}"
                usedIds.add(id)
                val optionsJson = item.optJSONArray("options") ?: JSONArray()
                val checklistJson = item.optJSONArray("checklistItems") ?: JSONArray()
                val requestedType = item.optString("type", "CHECKBOX").trim().uppercase()
                val type = when {
                    requestedType == "MULTIPLE_CHOICE" -> "SINGLE_CHOICE"
                    requestedType in ALLOWED_TYPES -> requestedType
                    else -> "INFORMATION"
                }
                add(
                    GeneratedStep(
                        id = id,
                        title = item.optString("title").trim().ifBlank { "Этап ${index + 1}" },
                        description = item.optString("description").trim(),
                        type = type,
                        isRequired = item.optBoolean("isRequired", true),
                        options = buildList {
                            for (optionIndex in 0 until optionsJson.length()) {
                                val rawOption = optionsJson.opt(optionIndex)
                                val title = when (rawOption) {
                                    is JSONObject -> rawOption.optString("title")
                                    is String -> rawOption
                                    else -> ""
                                }.trim()
                                if (title.isNotBlank()) add(title)
                            }
                        },
                        defaultNextStepId = item.optString("defaultNextStepId").trim()
                            .takeIf { it.isNotBlank() && it != "null" },
                        checklistItems = buildList {
                            for (checkIndex in 0 until checklistJson.length()) {
                                checklistJson.optString(checkIndex).trim().takeIf(String::isNotBlank)?.let(::add)
                            }
                        },
                        timerSeconds = item.optInt("timerSeconds", 0).takeIf { it > 0 },
                        optionNextStepIds = buildList {
                            for (optionIndex in 0 until optionsJson.length()) {
                                val target = (optionsJson.opt(optionIndex) as? JSONObject)
                                    ?.optString("nextStepId").orEmpty().trim()
                                add(target.takeIf { it.isNotBlank() && it != "null" })
                            }
                        },
                    ),
                )
            }
        }
        if (parsed.size < 2) throw YandexGptException(502, "YandexGPT не создал достаточно этапов")

        val normalized = parsed.mapIndexed { index, step ->
            val normalizedOptions = when {
                step.type == "YES_NO" && step.options.size < 2 -> listOf("Да", "Нет")
                else -> step.options
            }
            val normalizedTargets = List(normalizedOptions.size) { optionIndex ->
                step.optionNextStepIds.getOrNull(optionIndex)
                    ?.takeIf { id -> parsed.any { it.id == id } }
            }
            when {
                index == parsed.lastIndex -> step.copy(type = "FINAL", defaultNextStepId = null, options = emptyList(), optionNextStepIds = emptyList())
                step.type == "FINAL" -> step.copy(type = "INFORMATION", defaultNextStepId = parsed[index + 1].id)
                else -> step.copy(
                    options = normalizedOptions,
                    defaultNextStepId = step.defaultNextStepId
                        ?.takeIf { target -> parsed.any { it.id == target } }
                        ?: parsed[index + 1].id,
                    timerSeconds = if (step.type == "TIMER") step.timerSeconds ?: 300 else step.timerSeconds,
                    optionNextStepIds = normalizedTargets,
                )
            }
        }

        return GeneratedChecklist(
            title = data.optString("title").trim().ifBlank { "Новый алгоритм" },
            description = data.optString("description").trim(),
            category = data.optString("category").trim().ifBlank { "Другое" },
            estimatedDurationMinutes = data.optInt("estimatedDurationMinutes", normalized.size * 2).coerceAtLeast(1),
            steps = normalized,
            accentArgb = parseAccent(data.optString("accentColor")),
            symbol = data.optString("symbol").trim().take(8).ifBlank { "◇" },
            tags = buildList {
                val tags = data.optJSONArray("tags") ?: JSONArray()
                for (index in 0 until tags.length()) {
                    tags.optString(index).trim().removePrefix("#").takeIf(String::isNotBlank)?.let(::add)
                }
            }.distinct().take(8),
        )
    }

    private fun parseAccent(raw: String): Int? = runCatching {
        val hex = raw.trim().removePrefix("#")
        require(hex.length == 6 && hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' })
        (0xFF000000L or hex.toLong(16)).toInt()
    }.getOrNull()

    private fun Checklist.toGenerationJson(): String = JSONObject().apply {
        put("title", title)
        put("description", description)
        put("category", category)
        put("tags", JSONArray(tags))
        put("estimatedDurationMinutes", durationMinutes)
        put("symbol", emoji)
        put("steps", JSONArray().apply {
            steps.forEachIndexed { index, step ->
                val nextId = steps.nextStepIndexAfter(index)?.let { steps[it].id }
                val optionLabels = when {
                    step.type == StepType.YesNo -> listOf("Да", "Нет")
                    else -> step.options
                }
                put(JSONObject().apply {
                    put("id", step.id)
                    put("title", step.title)
                    put("description", step.description)
                    put("type", when (step.type) {
                        StepType.Information -> "INFORMATION"
                        StepType.Warning -> "WARNING"
                        StepType.Checkbox -> "CHECKBOX"
                        StepType.YesNo -> "YES_NO"
                        StepType.SingleChoice -> "SINGLE_CHOICE"
                        StepType.MultipleChoice -> "SINGLE_CHOICE"
                        StepType.Timer -> "TIMER"
                        StepType.Final -> "FINAL"
                    })
                    put("isRequired", true)
                    put("checklistItems", JSONArray(step.checklistItems))
                    put("timerSeconds", step.timerSeconds ?: JSONObject.NULL)
                    put("defaultNextStepId", nextId ?: JSONObject.NULL)
                    put("options", JSONArray().apply {
                        optionLabels.forEachIndexed { optionIndex, label ->
                            val target = steps.firstStepInBranchIndex(step.id, optionIndex)?.let { steps[it].id } ?: nextId
                            put(JSONObject().put("id", "${step.id}-option-$optionIndex").put("title", label).put("nextStepId", target ?: JSONObject.NULL))
                        }
                    })
                })
            }
        })
    }.toString()

    private fun algorithmPrompt(detail: Int, character: GenerationCharacter): String {
        val stepRange = when (detail) {
            0 -> "4–6"
            2 -> "10–14"
            else -> "7–9"
        }
        return """
            Ты системный архитектор интерактивных бытовых алгоритмов. На основе расширенного технического
            задания создай практичный алгоритм из $stepRange этапов. Не копируй техническое задание дословно.
            Выбранный пользователем характер результата: ${character.title}. ${character.instruction}
            Допустимые типы: INFORMATION, WARNING, CHECKBOX, YES_NO, SINGLE_CHOICE, TIMER, FINAL.
            Подбери один тематический цвет accentColor в формате #RRGGBB и один узнаваемый
            symbol: эмодзи или короткий символ, точно соответствующий теме. Цвет должен быть спокойным,
            достаточно светлым для тёмного текста и не сливаться с белым.
            Создай минимум одну настоящую развилку YES_NO или SINGLE_CHOICE. У развилки должно быть минимум два
            варианта с разными nextStepId, и каждая ветка должна содержать осмысленное действие до объединения.
            Добавь минимум один WARNING перед потенциальной ошибкой, риском, необратимым действием или важной проверкой.
            Условия и варианты должны иметь options. Для YES_NO названия вариантов строго «Да» и «Нет».
            Списки вещей оформляй через checklistItems, а не длинное описание.
            Для TIMER указывай timerSeconds. Последний этап всегда FINAL. Все переходы должны вести на существующие ID.
            Не добавляй опасные, незаконные или заведомо бессмысленные действия.

            Используй расширенный Markdown ВНУТРИ строк description: короткие списки, **жирные критерии**,
            `параметры`, > важные пояснения, таблицы только когда они действительно улучшают выбор.
            Markdown должен оставаться валидной JSON-строкой с экранированными переводами строк.
            Добавляй уместные эмодзи и символы в названия ключевых этапов (условие, предупреждение, таймер),
            но не больше одного на название и без визуального шума.

            Верни только JSON без Markdown:
            {"title":"...","description":"...","category":"...","tags":["завтрак","быстро"],"accentColor":"#F4CFA3","symbol":"🍳","estimatedDurationMinutes":10,
            "steps":[{"id":"step-1","title":"🧰 Подготовка","description":"- **Проверьте** комплект\\n- Отметьте готовое","type":"CHECKBOX",
            "isRequired":true,"options":[],"checklistItems":[],"timerSeconds":null,
            "defaultNextStepId":"step-2"}]}
            option: {"id":"option-1","title":"...","nextStepId":"step-2"}.
        """.trimIndent()
    }

    private fun readableError(status: Int, raw: String): String {
        val json = runCatching { JSONObject(raw) }.getOrNull()
        val error = json?.opt("error")
        val apiMessage = when (error) {
            is JSONObject -> error.optString("message")
            is String -> error
            else -> json?.optString("message")
        }?.takeIf(String::isNotBlank)
        return when (status) {
            400 -> apiMessage ?: "YandexGPT не принял запрос. Проверьте ID каталога."
            401 -> "API-ключ отклонён Yandex Cloud. Проверьте или перевыпустите ключ."
            403 -> "Нет доступа к YandexGPT. Нужна роль ai.languageModels.user и область yc.ai.languageModels.execute."
            404 -> "Каталог или модель YandexGPT не найдены. Проверьте ID каталога."
            429 -> "Лимит запросов YandexGPT исчерпан. Проверьте квоты Yandex Cloud."
            else -> apiMessage ?: "Ошибка YandexGPT ($status)"
        }
    }

    private companion object {
        const val ENDPOINT = "https://ai.api.cloud.yandex.net/foundationModels/v1/completion"
        val ALLOWED_TYPES = setOf("INFORMATION", "WARNING", "CHECKBOX", "YES_NO", "SINGLE_CHOICE", "TIMER", "FINAL")

        val EXPANDER_PROMPT = """
            Ты аналитик задач. Преобразуй короткий пользовательский запрос в подробное системное техническое
            задание для генератора пошагового алгоритма. Уточни ожидаемый результат, исходные условия,
            необходимые материалы, порядок действий, проверки, возможные развилки, таймеры, критерии завершения,
            риски и меры безопасности. Найди минимум одну точку принятия решения и опиши две действительно разные
            ветки после неё. Отдельно перечисли предупреждения, частые ошибки и способы восстановления.
            Не выполняй задачу и не создавай JSON-алгоритм. Не задавай пользователю
            вопросы: явно перечисли разумные предположения. Пиши компактно, но исчерпывающе на русском языке.
        """.trimIndent()

        val VERIFIER_PROMPT = """
            Ты независимый аудитор интерактивных алгоритмов. Проверь предложенный JSON относительно исходной задачи:
            полноту, порядок, понятность, безопасность, отсутствие противоречий и повторов, корректность ID и переходов,
            уместность условий, вариантов, чек-листов и таймеров. Самостоятельно исправь все найденные недостатки.
            В результате обязательно должна быть хотя бы одна настоящая развилка YES_NO или SINGLE_CHOICE:
            минимум два варианта ведут на разные существующие ID и обе ветки содержат полезные шаги.
            Обязательно добавь отдельный WARNING. Используй компактный Markdown внутри description и уместные эмодзи.
            Также проверь, что accentColor и symbol семантически подходят к теме; при необходимости замени их.
            Сохрани полезные детали и не добавляй лишних этапов. Последний этап обязан иметь тип FINAL.
            Верни только полный исправленный JSON-алгоритм без отчёта, комментариев и Markdown, в той же схеме.
        """.trimIndent()

        val REPAIR_PROMPT = """
            Ты архитектор графов интерактивных алгоритмов. Верни только полный исправленный JSON без обёртки.
            Сохрани цель, полезные детали, цвет и символ, но обязательно исправь структуру:
            1. Добавь или исправь YES_NO/SINGLE_CHOICE минимум с двумя вариантами, ведущими на разные существующие ID.
            2. После каждого варианта должен быть хотя бы один осмысленный шаг; затем ветви могут объединиться.
            3. Добавь отдельный этап WARNING перед риском или частой ошибкой.
            4. Последний шаг — FINAL, граф без циклов и недостижимых шагов.
            5. В description используй компактный расширенный Markdown; в ключевых названиях — уместные эмодзи/символы.
            Для YES_NO варианты называются «Да» и «Нет». Не ограничивайся переименованием линейных шагов:
            option.nextStepId должны фактически образовывать разные ветви.
        """.trimIndent()

        val IMPROVER_PROMPT = """
            Ты старший редактор и архитектор интерактивных алгоритмов. Улучши переданный JSON, сохранив его цель.
            Верни только полный итоговый JSON в той же схеме, без отчёта и Markdown-обёртки.

            Правила безопасного редактирования:
            - сохраняй id каждого неизменённого или отредактированного существующего блока;
            - новым блокам давай id вида improved-step-N; удалённые блоки просто не включай;
            - исправь порядок, полноту, понятность, ветвления, чек-листы, таймеры и предупреждения;
            - создай минимум одну реальную развилку YES_NO/SINGLE_CHOICE с разными nextStepId;
            - добавь WARNING там, где возможна ошибка, риск или важная проверка;
            - все ссылки ведут на существующие id, граф без циклов, последний блок FINAL;
            - сохрани полезные детали, заметки не выдумывай, медиавложения не описывай и не удаляй;
            - используй компактный Markdown внутри description, уместные эмодзи и один тематический symbol;
            - обнови категорию, теги, длительность и светлый accentColor только если это улучшает результат.
        """.trimIndent()

        val IMPROVEMENT_REPAIR_PROMPT = """
            Исправь предложенное улучшение алгоритма и верни только полный JSON. Сопоставляй его с исходником:
            сохраняй исходные id для существующих блоков и используй improved-step-N только для добавленных.
            Обязательны WARNING и реальная развилка минимум с двумя разными option.nextStepId.
            Удали циклы и битые ссылки, сохрани смысл, Markdown, цвет, символ и последний FINAL.
        """.trimIndent()
    }
}

private fun GeneratedChecklist.hasRichInteractiveStructure(): Boolean {
    val hasWarning = steps.any { it.type == "WARNING" }
    val hasRealBranch = steps.any { step ->
        step.type in setOf("YES_NO", "SINGLE_CHOICE") &&
            step.options.size >= 2 &&
            step.optionNextStepIds.filterNotNull().distinct().size >= 2
    }
    return hasWarning && hasRealBranch
}

class YandexGptException(val statusCode: Int, override val message: String) : Exception(message)

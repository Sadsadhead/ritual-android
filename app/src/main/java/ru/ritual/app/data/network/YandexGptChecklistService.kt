package ru.ritual.app.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.ritual.app.domain.model.GeneratedChecklist
import ru.ritual.app.domain.model.GeneratedStep
import ru.ritual.app.domain.model.GenerationCharacter
import ru.ritual.app.domain.model.AlgorithmMetadataSuggestion
import java.net.HttpURLConnection
import java.net.URL

class YandexGptChecklistService {
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
        parseAlgorithm(verifiedJson)
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
                val requestedType = item.optString("type", "CHECKBOX")
                val type = requestedType.takeIf(ALLOWED_TYPES::contains) ?: "INFORMATION"
                add(
                    GeneratedStep(
                        id = id,
                        title = item.optString("title").trim().ifBlank { "Этап ${index + 1}" },
                        description = item.optString("description").trim(),
                        type = type,
                        isRequired = item.optBoolean("isRequired", true),
                        options = buildList {
                            for (optionIndex in 0 until optionsJson.length()) {
                                val option = optionsJson.optJSONObject(optionIndex)
                                val title = option?.optString("title").orEmpty().trim()
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
                                val target = optionsJson.optJSONObject(optionIndex)
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
            when {
                index == parsed.lastIndex -> step.copy(type = "FINAL", defaultNextStepId = null)
                step.type == "FINAL" -> step.copy(type = "INFORMATION", defaultNextStepId = parsed[index + 1].id)
                else -> step.copy(
                    defaultNextStepId = step.defaultNextStepId
                        ?.takeIf { target -> parsed.any { it.id == target } }
                        ?: parsed[index + 1].id,
                    timerSeconds = if (step.type == "TIMER") step.timerSeconds ?: 300 else step.timerSeconds,
                    optionNextStepIds = step.optionNextStepIds.map { target ->
                        target?.takeIf { id -> parsed.any { it.id == id } }
                    },
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
            Допустимые типы: INFORMATION, CHECKBOX, YES_NO, SINGLE_CHOICE, MULTIPLE_CHOICE, TIMER, FINAL.
            Подбери один тематический цвет accentColor в формате #RRGGBB и один узнаваемый
            symbol: эмодзи или короткий символ, точно соответствующий теме. Цвет должен быть спокойным,
            достаточно светлым для тёмного текста и не сливаться с белым.
            Условия и варианты должны иметь options. Списки вещей оформляй через checklistItems, а не длинное описание.
            Для TIMER указывай timerSeconds. Последний этап всегда FINAL. Все переходы должны вести на существующие ID.
            Не добавляй опасные, незаконные или заведомо бессмысленные действия.

            Верни только JSON без Markdown:
            {"title":"...","description":"...","category":"...","tags":["завтрак","быстро"],"accentColor":"#F4CFA3","symbol":"🍳","estimatedDurationMinutes":10,
            "steps":[{"id":"step-1","title":"...","description":"...","type":"CHECKBOX",
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
        val ALLOWED_TYPES = setOf("INFORMATION", "CHECKBOX", "YES_NO", "SINGLE_CHOICE", "MULTIPLE_CHOICE", "TIMER", "FINAL")

        val EXPANDER_PROMPT = """
            Ты аналитик задач. Преобразуй короткий пользовательский запрос в подробное системное техническое
            задание для генератора пошагового алгоритма. Уточни ожидаемый результат, исходные условия,
            необходимые материалы, порядок действий, проверки, возможные развилки, таймеры, критерии завершения,
            риски и меры безопасности. Не выполняй задачу и не создавай JSON-алгоритм. Не задавай пользователю
            вопросы: явно перечисли разумные предположения. Пиши компактно, но исчерпывающе на русском языке.
        """.trimIndent()

        val VERIFIER_PROMPT = """
            Ты независимый аудитор интерактивных алгоритмов. Проверь предложенный JSON относительно исходной задачи:
            полноту, порядок, понятность, безопасность, отсутствие противоречий и повторов, корректность ID и переходов,
            уместность условий, вариантов, чек-листов и таймеров. Самостоятельно исправь все найденные недостатки.
            Также проверь, что accentColor и symbol семантически подходят к теме; при необходимости замени их.
            Сохрани полезные детали и не добавляй лишних этапов. Последний этап обязан иметь тип FINAL.
            Верни только полный исправленный JSON-алгоритм без отчёта, комментариев и Markdown, в той же схеме.
        """.trimIndent()
    }
}

class YandexGptException(val statusCode: Int, override val message: String) : Exception(message)

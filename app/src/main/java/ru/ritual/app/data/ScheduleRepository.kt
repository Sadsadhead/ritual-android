package ru.ritual.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import ru.ritual.app.domain.model.RecurrenceRule
import ru.ritual.app.domain.model.RepeatFrequency
import ru.ritual.app.domain.model.ScheduleItem
import ru.ritual.app.domain.model.ScheduleItemType
import ru.ritual.app.notification.ScheduleAlarmScheduler
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

class ScheduleRepository(context: Context, syncAlarms: Boolean = true) {
    private val preferences = context.getSharedPreferences("schedule_store", Context.MODE_PRIVATE)
    private val mutableItems = MutableStateFlow(readItems())
    private val alarmScheduler = ScheduleAlarmScheduler(context)
    val items: StateFlow<List<ScheduleItem>> = mutableItems

    init { if (syncAlarms) alarmScheduler.sync(mutableItems.value) }

    fun save(item: ScheduleItem) {
        val updated = mutableItems.value.toMutableList()
        val index = updated.indexOfFirst { it.id == item.id }
        if (index >= 0) updated[index] = item else updated.add(item)
        persist(updated.sortedBy(ScheduleItem::startMillis))
    }

    fun saveOccurrence(item: ScheduleItem, originalStartMillis: Long, wholeSeries: Boolean) {
        if (wholeSeries || item.recurrence.frequency == RepeatFrequency.None) {
            save(item)
            return
        }
        val series = mutableItems.value.firstOrNull { it.id == item.id } ?: return
        exclude(series, originalStartMillis)
        save(
            item.copy(
                id = UUID.randomUUID().toString(),
                recurrence = RecurrenceRule(),
                excludedEpochDays = emptyList(),
                seriesId = series.id,
                originalOccurrenceStartMillis = originalStartMillis,
            ),
        )
    }

    fun delete(item: ScheduleItem, occurrenceStartMillis: Long, wholeSeries: Boolean) {
        if (wholeSeries) {
            persist(mutableItems.value.filterNot { it.id == item.id || it.seriesId == item.id })
        } else if (item.recurrence.frequency != RepeatFrequency.None) {
            exclude(item, occurrenceStartMillis)
        } else {
            persist(mutableItems.value.filterNot { it.id == item.id })
        }
    }

    private fun exclude(item: ScheduleItem, occurrenceStartMillis: Long) {
        val day = Instant.ofEpochMilli(occurrenceStartMillis).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
        save(item.copy(excludedEpochDays = (item.excludedEpochDays + day).distinct()))
    }

    private fun persist(items: List<ScheduleItem>) {
        mutableItems.value = items
        preferences.edit().putString(ITEMS_KEY, encode(items).toString()).apply()
        alarmScheduler.sync(items)
    }

    private fun readItems(): List<ScheduleItem> = runCatching {
        val array = JSONArray(preferences.getString(ITEMS_KEY, "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val source = array.getJSONObject(index)
                val recurrence = source.optJSONObject("recurrence") ?: JSONObject()
                add(
                    ScheduleItem(
                        id = source.getString("id"),
                        title = source.optString("title"),
                        description = source.optString("description"),
                        type = enumValueOrDefault(source.optString("type"), ScheduleItemType.Reminder),
                        startMillis = source.getLong("startMillis"),
                        endMillis = source.optLong("endMillis", -1L).takeIf { it >= 0L },
                        allDay = source.optBoolean("allDay"),
                        category = source.optString("category", "Другое"),
                        tags = source.optJSONArray("tags").strings(),
                        algorithmId = source.optString("algorithmId").takeIf { it.isNotBlank() && it != "null" },
                        recurrence = RecurrenceRule(
                            frequency = enumValueOrDefault(recurrence.optString("frequency"), RepeatFrequency.None),
                            interval = recurrence.optInt("interval", 1).coerceAtLeast(1),
                            weekdays = recurrence.optJSONArray("weekdays").ints(),
                            monthDays = recurrence.optJSONArray("monthDays").ints(),
                            untilEpochDay = recurrence.optLong("untilEpochDay", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
                        ),
                        excludedEpochDays = source.optJSONArray("excludedEpochDays").longs(),
                        seriesId = source.optString("seriesId").takeIf { it.isNotBlank() && it != "null" },
                        originalOccurrenceStartMillis = source.optLong("originalOccurrenceStartMillis", -1L).takeIf { it >= 0L },
                        colorArgb = source.optInt("colorArgb", 0xFFB8A1FF.toInt()),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun encode(items: List<ScheduleItem>) = JSONArray().apply {
        items.forEach { item ->
            put(JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("description", item.description)
                put("type", item.type.name)
                put("startMillis", item.startMillis)
                put("endMillis", item.endMillis ?: JSONObject.NULL)
                put("allDay", item.allDay)
                put("category", item.category)
                put("tags", JSONArray(item.tags))
                put("algorithmId", item.algorithmId ?: JSONObject.NULL)
                put("recurrence", JSONObject().apply {
                    put("frequency", item.recurrence.frequency.name)
                    put("interval", item.recurrence.interval)
                    put("weekdays", JSONArray(item.recurrence.weekdays))
                    put("monthDays", JSONArray(item.recurrence.monthDays))
                    put("untilEpochDay", item.recurrence.untilEpochDay ?: JSONObject.NULL)
                })
                put("excludedEpochDays", JSONArray(item.excludedEpochDays))
                put("seriesId", item.seriesId ?: JSONObject.NULL)
                put("originalOccurrenceStartMillis", item.originalOccurrenceStartMillis ?: JSONObject.NULL)
                put("colorArgb", item.colorArgb)
            })
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
        runCatching { enumValueOf<T>(value) }.getOrDefault(default)

    private fun JSONArray?.strings() = buildList {
        val source = this@strings ?: return@buildList
        for (index in 0 until source.length()) source.optString(index).takeIf(String::isNotBlank)?.let(::add)
    }

    private fun JSONArray?.ints() = buildList {
        val source = this@ints ?: return@buildList
        for (index in 0 until source.length()) add(source.optInt(index))
    }

    private fun JSONArray?.longs() = buildList {
        val source = this@longs ?: return@buildList
        for (index in 0 until source.length()) add(source.optLong(index))
    }

    private companion object { const val ITEMS_KEY = "schedule_items_v1" }
}

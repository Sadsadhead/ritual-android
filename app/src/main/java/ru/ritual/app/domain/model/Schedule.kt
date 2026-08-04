package ru.ritual.app.domain.model

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class ScheduleItemType(val title: String) {
    Reminder("Напоминание"),
    Event("Мероприятие"),
    Note("Заметка"),
}

enum class RepeatFrequency(val title: String) {
    None("Не повторять"),
    Daily("Каждый день"),
    Weekdays("По будням"),
    Weekly("По дням недели"),
    Monthly("По числам месяца"),
    Yearly("Каждый год"),
}

enum class ScheduleViewMode(val title: String) {
    Day("День"), Week("Неделя"), Month("Месяц"), Year("Год")
}

@Immutable
data class ScheduleAiSuggestion(
    val title: String,
    val description: String,
    val category: String,
    val tags: List<String>,
)

@Immutable
data class RecurrenceRule(
    val frequency: RepeatFrequency = RepeatFrequency.None,
    val interval: Int = 1,
    val weekdays: List<Int> = emptyList(),
    val monthDays: List<Int> = emptyList(),
    val untilEpochDay: Long? = null,
)

@Immutable
data class ScheduleItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val type: ScheduleItemType = ScheduleItemType.Reminder,
    val startMillis: Long,
    val endMillis: Long? = null,
    val allDay: Boolean = false,
    val category: String = "Другое",
    val tags: List<String> = emptyList(),
    val algorithmId: String? = null,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val excludedEpochDays: List<Long> = emptyList(),
    val seriesId: String? = null,
    val originalOccurrenceStartMillis: Long? = null,
    val colorArgb: Int = 0xFFB8A1FF.toInt(),
)

@Immutable
data class ScheduleOccurrence(
    val item: ScheduleItem,
    val startMillis: Long,
    val endMillis: Long?,
    val originalStartMillis: Long,
)

fun List<ScheduleItem>.occurrencesBetween(
    fromMillis: Long,
    untilMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<ScheduleOccurrence> = buildList {
    val fromDate = Instant.ofEpochMilli(fromMillis).atZone(zone).toLocalDate()
    val untilDate = Instant.ofEpochMilli(untilMillis).atZone(zone).toLocalDate()
    this@occurrencesBetween.forEach { item ->
        val anchor = Instant.ofEpochMilli(item.startMillis).atZone(zone)
        val anchorDate = anchor.toLocalDate()
        val recurrenceUntil = item.recurrence.untilEpochDay?.let(LocalDate::ofEpochDay)
        val first = maxOf(fromDate, anchorDate)
        val last = minOf(untilDate, recurrenceUntil ?: untilDate)
        if (last < first) return@forEach
        var date = first
        while (!date.isAfter(last)) {
            if (item.matchesOccurrence(anchorDate, date) && date.toEpochDay() !in item.excludedEpochDays) {
                val start = date.atTime(anchor.toLocalTime()).atZone(zone).toInstant().toEpochMilli()
                if (start in fromMillis..untilMillis) {
                    val duration = item.endMillis?.minus(item.startMillis)
                    add(
                        ScheduleOccurrence(
                            item = item,
                            startMillis = start,
                            endMillis = duration?.let(start::plus),
                            originalStartMillis = start,
                        ),
                    )
                }
            }
            date = date.plusDays(1)
        }
    }
}.sortedBy(ScheduleOccurrence::startMillis)

private fun ScheduleItem.matchesOccurrence(anchor: LocalDate, date: LocalDate): Boolean {
    val interval = recurrence.interval.coerceAtLeast(1)
    return when (recurrence.frequency) {
        RepeatFrequency.None -> date == anchor
        RepeatFrequency.Daily -> ChronoUnit.DAYS.between(anchor, date) % interval == 0L
        RepeatFrequency.Weekdays -> date.dayOfWeek.value in DayOfWeek.MONDAY.value..DayOfWeek.FRIDAY.value
        RepeatFrequency.Weekly -> {
            val anchorWeek = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val selected = recurrence.weekdays.ifEmpty { listOf(anchor.dayOfWeek.value) }
            ChronoUnit.WEEKS.between(anchorWeek, dateWeek) % interval == 0L && date.dayOfWeek.value in selected
        }
        RepeatFrequency.Monthly -> {
            val months = ChronoUnit.MONTHS.between(anchor.withDayOfMonth(1), date.withDayOfMonth(1))
            val selected = recurrence.monthDays.ifEmpty { listOf(anchor.dayOfMonth) }
            months % interval == 0L && date.dayOfMonth in selected
        }
        RepeatFrequency.Yearly -> {
            val years = date.year - anchor.year
            years % interval == 0 && date.month == anchor.month && date.dayOfMonth == anchor.dayOfMonth
        }
    }
}

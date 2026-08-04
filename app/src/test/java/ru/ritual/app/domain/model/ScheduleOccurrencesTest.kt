package ru.ritual.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class ScheduleOccurrencesTest {
    private val zone = ZoneId.of("Europe/Moscow")

    @Test
    fun weeklySeriesUsesSelectedBranchesOfWeek() {
        val item = itemAt(LocalDate.of(2026, 8, 3)).copy(
            recurrence = RecurrenceRule(
                frequency = RepeatFrequency.Weekly,
                weekdays = listOf(DayOfWeek.MONDAY.value, DayOfWeek.WEDNESDAY.value),
            ),
        )
        val result = listOf(item).inRange(LocalDate.of(2026, 8, 3), LocalDate.of(2026, 8, 12))
        assertEquals(listOf(3, 5, 10, 12), result.map { it.startMillis.date().dayOfMonth })
    }

    @Test
    fun monthlySeriesRespectsEndDateAndExcludedOccurrence() {
        val item = itemAt(LocalDate.of(2026, 1, 5)).copy(
            recurrence = RecurrenceRule(
                frequency = RepeatFrequency.Monthly,
                monthDays = listOf(5, 20),
                untilEpochDay = LocalDate.of(2026, 3, 20).toEpochDay(),
            ),
            excludedEpochDays = listOf(LocalDate.of(2026, 2, 5).toEpochDay()),
        )
        val result = listOf(item).inRange(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 30))
        assertEquals(
            listOf("2026-01-05", "2026-01-20", "2026-02-20", "2026-03-05", "2026-03-20"),
            result.map { it.startMillis.date().toString() },
        )
    }

    @Test
    fun occurrenceKeepsOriginalDuration() {
        val item = itemAt(LocalDate.of(2026, 8, 1)).copy(
            endMillis = LocalDate.of(2026, 8, 1).atTime(11, 30).atZone(zone).toInstant().toEpochMilli(),
            recurrence = RecurrenceRule(RepeatFrequency.Daily),
        )
        val result = listOf(item).inRange(LocalDate.of(2026, 8, 2), LocalDate.of(2026, 8, 2)).single()
        assertEquals(90 * 60_000L, result.endMillis!! - result.startMillis)
        assertTrue(result.startMillis.date() == LocalDate.of(2026, 8, 2))
    }

    private fun itemAt(date: LocalDate) = ScheduleItem(
        title = "Тест",
        startMillis = date.atTime(LocalTime.of(10, 0)).atZone(zone).toInstant().toEpochMilli(),
    )

    private fun List<ScheduleItem>.inRange(from: LocalDate, to: LocalDate) = occurrencesBetween(
        from.atStartOfDay(zone).toInstant().toEpochMilli(),
        to.atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli(),
        zone,
    )

    private fun Long.date() = java.time.Instant.ofEpochMilli(this).atZone(zone).toLocalDate()
}

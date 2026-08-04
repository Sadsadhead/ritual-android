package ru.ritual.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ru.ritual.app.MainActivity
import ru.ritual.app.R
import ru.ritual.app.data.ActiveRunStore
import ru.ritual.app.data.ChecklistRepository
import ru.ritual.app.data.ScheduleRepository
import ru.ritual.app.domain.model.occurrencesBetween
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object WidgetUpdater {
    fun updateAll(context: Context) {
        updateAlgorithm(context)
        updateSchedule(context)
    }

    fun updateAlgorithm(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        AlgorithmWidgetProvider().onUpdate(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, AlgorithmWidgetProvider::class.java)),
        )
    }

    fun updateSchedule(context: Context) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        ScheduleWidgetProvider().onUpdate(
            appContext,
            manager,
            manager.getAppWidgetIds(ComponentName(appContext, ScheduleWidgetProvider::class.java)),
        )
    }
}

class AlgorithmWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val active = ActiveRunStore(context).activeRuns.value.firstOrNull()
        val fallback = if (active == null) {
            ChecklistRepository(context).checklists.value
                .sortedByDescending { it.isFavorite }
                .firstOrNull()
        } else null
        ids.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_algorithm)
            val algorithmId = active?.algorithmId ?: fallback?.id
            val title = active?.title ?: fallback?.title ?: "Создайте алгоритм"
            val emoji = active?.emoji ?: fallback?.emoji ?: "＋"
            val step = active?.let {
                val percent = if (it.minPercent != it.maxPercent) "${it.minPercent}–${it.maxPercent}%" else "${it.maxPercent}%"
                "$percent · шаг ${it.completedSteps}"
            }
                ?: if (fallback == null) "Нажмите, чтобы открыть" else "Быстрый запуск"
            val progress = active?.minPercent ?: 0
            views.setTextViewText(R.id.widget_algorithm_emoji, emoji)
            views.setTextViewText(R.id.widget_algorithm_title, title)
            views.setTextViewText(R.id.widget_algorithm_status, if (active == null) "▷ $step" else "● $step")
            views.setProgressBar(R.id.widget_algorithm_progress, 100, progress, false)
            views.setViewVisibility(R.id.widget_algorithm_progress, if (active == null) View.GONE else View.VISIBLE)
            views.setOnClickPendingIntent(R.id.widget_algorithm_root, openApp(context, algorithmId, 7100 + widgetId))
            manager.updateAppWidget(widgetId, views)
        }
    }
}

class ScheduleWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        if (ids.isEmpty()) return
        val now = System.currentTimeMillis()
        val upcoming = ScheduleRepository(context, syncAlarms = false).items.value
            .occurrencesBetween(now, now + TimeUnit.DAYS.toMillis(14))
            .take(3)
        val zone = ZoneId.systemDefault()
        val dayFormat = DateTimeFormatter.ofPattern("d MMM")
        val timeFormat = DateTimeFormatter.ofPattern("HH:mm")
        ids.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_schedule)
            views.setTextViewText(
                R.id.widget_schedule_date,
                Instant.ofEpochMilli(now).atZone(zone).format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
            )
            val rowIds = intArrayOf(R.id.widget_schedule_row_1, R.id.widget_schedule_row_2, R.id.widget_schedule_row_3)
            upcoming.forEachIndexed { index, occurrence ->
                val dateTime = Instant.ofEpochMilli(occurrence.startMillis).atZone(zone)
                val prefix = if (occurrence.item.allDay) {
                    dateTime.format(dayFormat)
                } else {
                    "${dateTime.format(dayFormat)} · ${dateTime.format(timeFormat)}"
                }
                views.setViewVisibility(rowIds[index], View.VISIBLE)
                views.setTextViewText(rowIds[index], "$prefix  ${occurrence.item.title}")
            }
            for (index in upcoming.size until rowIds.size) {
                views.setViewVisibility(rowIds[index], if (index == 0) View.VISIBLE else View.GONE)
                if (index == 0) views.setTextViewText(rowIds[index], "Свободно · добавьте событие")
            }
            views.setOnClickPendingIntent(R.id.widget_schedule_root, openSchedule(context, 8100 + widgetId))
            manager.updateAppWidget(widgetId, views)
        }
    }
}

private fun openApp(context: Context, algorithmId: String?, requestCode: Int): PendingIntent =
    PendingIntent.getActivity(
        context,
        requestCode,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_ALGORITHM_ID, algorithmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

private fun openSchedule(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_SCHEDULE),
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
)

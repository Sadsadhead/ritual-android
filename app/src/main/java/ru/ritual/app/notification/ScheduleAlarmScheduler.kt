package ru.ritual.app.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.ritual.app.MainActivity
import ru.ritual.app.R
import ru.ritual.app.data.ScheduleRepository
import ru.ritual.app.domain.model.ScheduleItem
import ru.ritual.app.domain.model.ScheduleItemType
import ru.ritual.app.domain.model.occurrencesBetween
import java.util.concurrent.TimeUnit

class ScheduleAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val store = context.getSharedPreferences("schedule_alarm_store", Context.MODE_PRIVATE)

    fun sync(items: List<ScheduleItem>) {
        store.getStringSet(REQUEST_CODES, emptySet()).orEmpty().forEach { encoded ->
            encoded.toIntOrNull()?.let { requestCode ->
                alarmManager.cancel(alarmIntent(requestCode, "", ""))
            }
        }
        val now = System.currentTimeMillis()
        val until = now + TimeUnit.DAYS.toMillis(366)
        val requestCodes = mutableSetOf<String>()
        items.filter { it.type == ScheduleItemType.Reminder }
            .mapNotNull { item -> listOf(item).occurrencesBetween(now, until).firstOrNull() }
            .forEach { occurrence ->
                val requestCode = ("${occurrence.item.id}:${occurrence.startMillis}").hashCode()
                val intent = alarmIntent(requestCode, occurrence.item.title, occurrence.item.description)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.startMillis, intent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, occurrence.startMillis, intent)
                }
                requestCodes += requestCode.toString()
            }
        store.edit().putStringSet(REQUEST_CODES, requestCodes).apply()
    }

    private fun alarmIntent(requestCode: Int, title: String, description: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ScheduleAlarmReceiver::class.java)
                .setAction(ACTION_NOTIFY)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_DESCRIPTION, description)
                .putExtra(EXTRA_ID, requestCode),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        const val ACTION_NOTIFY = "ru.ritual.app.SCHEDULE_NOTIFY"
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_ID = "notification_id"
        private const val REQUEST_CODES = "request_codes"
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        ) {
            ScheduleRepository(context)
            return
        }
        if (intent.action != ScheduleAlarmScheduler.ACTION_NOTIFY) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Напоминания расписания", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Напоминания из раздела «Расписание»"
                    enableVibration(true)
                },
            )
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val id = intent.getIntExtra(ScheduleAlarmScheduler.EXTRA_ID, 5800)
        NotificationManagerCompat.from(context).notify(
            id,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_TITLE).orEmpty().ifBlank { "Напоминание" })
                .setContentText(intent.getStringExtra(ScheduleAlarmScheduler.EXTRA_DESCRIPTION).orEmpty().ifBlank { "Пора выполнить запланированное" })
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build(),
        )
        // После срабатывания ставим ближайший следующий экземпляр каждой серии.
        ScheduleRepository(context)
    }

    private companion object { const val CHANNEL_ID = "schedule_reminders" }
}

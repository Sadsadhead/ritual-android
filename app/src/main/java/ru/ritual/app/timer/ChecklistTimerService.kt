package ru.ritual.app.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ru.ritual.app.MainActivity
import ru.ritual.app.R
import kotlin.math.ceil

class ChecklistTimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var deadlineMillis = 0L
    private var initialSeconds = 0
    private var timerTitle = "Таймер алгоритма"
    private val announced = mutableSetOf<Int>()

    private val ticker = object : Runnable {
        override fun run() {
            val remaining = remainingSeconds()
            when {
                remaining <= 0 -> finishTimer()
                else -> {
                    notifyMilestoneIfNeeded(300, "Осталось 5 минут")
                    notifyMilestoneIfNeeded(60, "Осталась 1 минута")
                    handler.postDelayed(this, 1_000L)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopTimer()
            ACTION_START -> {
                initialSeconds = intent.getIntExtra(EXTRA_SECONDS, 60).coerceAtLeast(1)
                timerTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty().ifBlank { "Таймер алгоритма" }
                deadlineMillis = System.currentTimeMillis() + initialSeconds * 1_000L
                announced.clear()
                handler.removeCallbacks(ticker)
                ServiceCompat.startForeground(
                    this,
                    ONGOING_ID,
                    ongoingNotification(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
                )
                handler.post(ticker)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun remainingSeconds(): Int = ceil(
        ((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L)) / 1_000.0,
    ).toInt()

    private fun notifyMilestoneIfNeeded(seconds: Int, message: String) {
        if (initialSeconds > seconds && remainingSeconds() <= seconds && announced.add(seconds)) {
            playSignal(finished = false)
            notificationManager().notify(ALERT_ID + seconds, alertNotification(message, finished = false))
        }
    }

    private fun finishTimer() {
        handler.removeCallbacks(ticker)
        playSignal(finished = true)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        notificationManager().notify(ALERT_ID, alertNotification("Время вышло", finished = true))
        stopSelf()
    }

    private fun stopTimer() {
        handler.removeCallbacks(ticker)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun ongoingNotification() = NotificationCompat.Builder(this, TIMER_CHANNEL)
        .setSmallIcon(R.drawable.app_icon)
        .setContentTitle(timerTitle)
        .setContentText("Таймер работает в фоне")
        .setContentIntent(contentIntent())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setWhen(deadlineMillis)
        .setUsesChronometer(true)
        .setChronometerCountDown(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .build()

    private fun alertNotification(message: String, finished: Boolean) =
        NotificationCompat.Builder(this, ALERT_CHANNEL)
            .setSmallIcon(R.drawable.app_icon)
            .setContentTitle(timerTitle)
            .setContentText(message)
            .setContentIntent(contentIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(if (finished) 60_000L else 30_000L)
            .build()

    private fun contentIntent(): PendingIntent = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun playSignal(finished: Boolean) {
        runCatching {
            ToneGenerator(AudioManager.STREAM_ALARM, 90).also { tone ->
                tone.startTone(
                    if (finished) ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD else ToneGenerator.TONE_PROP_BEEP2,
                    if (finished) 1_500 else 600,
                )
                handler.postDelayed({ tone.release() }, if (finished) 1_700L else 800L)
            }
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager().createNotificationChannel(
            NotificationChannel(TIMER_CHANNEL, "Текущий таймер", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Показывает оставшееся время запущенного алгоритма"
                setSound(null, null)
            },
        )
        notificationManager().createNotificationChannel(
            NotificationChannel(ALERT_CHANNEL, "Сигналы таймера", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Предупреждает за 5 минут, за 1 минуту и при завершении"
                enableVibration(true)
            },
        )
    }

    private fun notificationManager() = getSystemService(NotificationManager::class.java)

    companion object {
        private const val ACTION_START = "ru.ritual.app.timer.START"
        private const val ACTION_STOP = "ru.ritual.app.timer.STOP"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_TITLE = "title"
        private const val TIMER_CHANNEL = "checklist_timer"
        private const val ALERT_CHANNEL = "checklist_timer_alerts"
        private const val ONGOING_ID = 4401
        private const val ALERT_ID = 4501

        fun start(context: Context, seconds: Int, title: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, ChecklistTimerService::class.java)
                    .setAction(ACTION_START)
                    .putExtra(EXTRA_SECONDS, seconds)
                    .putExtra(EXTRA_TITLE, title),
            )
        }

        fun stop(context: Context) {
            context.startService(Intent(context, ChecklistTimerService::class.java).setAction(ACTION_STOP))
        }
    }
}

package ru.ritual.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ru.ritual.app.MainActivity
import ru.ritual.app.R

class GenerationKeepAliveService : Service() {
    override fun onCreate() {
        super.onCreate()
        current = this
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Генерация алгоритмов", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Ход и результат создания алгоритмов с помощью YandexGPT"
                },
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopKeepingAlive()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Создаю алгоритм")
                .setContentText("YandexGPT продолжает работу в фоне")
                .setProgress(0, 0, true)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openAppIntent())
                .build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (current === this) current = null
        super.onDestroy()
    }

    private fun stopKeepingAlive() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun openAppIntent() = PendingIntent.getActivity(
        this,
        6202,
        Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_AI),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "algorithm_generation"
        const val NOTIFICATION_ID = 4107
        private const val ACTION_START = "ru.ritual.app.generation.START"
        private const val ACTION_STOP = "ru.ritual.app.generation.STOP"
        @Volatile private var current: GenerationKeepAliveService? = null

        fun start(context: Context) = ContextCompat.startForegroundService(
            context,
            Intent(context, GenerationKeepAliveService::class.java).setAction(ACTION_START),
        )

        fun stop(context: Context) {
            current?.stopKeepingAlive()
                ?: context.stopService(Intent(context, GenerationKeepAliveService::class.java))
        }
    }
}

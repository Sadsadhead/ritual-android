package ru.ritual.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.ritual.app.MainActivity
import ru.ritual.app.R
import ru.ritual.app.domain.model.ActiveAlgorithmRun

class ActiveAlgorithmNotifier(private val context: Context) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Активный алгоритм", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Быстрый возврат к выполняемому алгоритму"
                    setSound(null, null)
                },
            )
        }
    }

    fun show(run: ActiveAlgorithmRun) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val progress = run.minPercent
        val progressLabel = if (run.minPercent != run.maxPercent) "${run.minPercent}–${run.maxPercent}%" else "${run.maxPercent}%"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("${run.emoji} ${run.title}")
                .setContentText("$progressLabel · ${run.completedSteps} шаг · нажмите, чтобы продолжить")
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setContentIntent(openRunIntent(run.algorithmId))
                .build()
        runCatching { NotificationManagerCompat.from(context).notify(notificationId(run.algorithmId), notification) }
    }

    fun cancel(algorithmId: String) = NotificationManagerCompat.from(context).cancel(notificationId(algorithmId))

    fun cancelAll(runs: List<ActiveAlgorithmRun>) {
        runs.forEach { cancel(it.algorithmId) }
        NotificationManagerCompat.from(context).cancel(LEGACY_NOTIFICATION_ID)
    }

    private fun openRunIntent(algorithmId: String) = PendingIntent.getActivity(
        context,
        notificationId(algorithmId),
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(MainActivity.EXTRA_ALGORITHM_ID, algorithmId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val CHANNEL_ID = "active_algorithm"
        const val LEGACY_NOTIFICATION_ID = 4208
        fun notificationId(algorithmId: String) = 0x40000000 or (algorithmId.hashCode() and 0x3FFFFFFF)
    }
}

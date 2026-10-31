package de.soeren.dhbwplan.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import de.soeren.dhbwplan.MainActivity
import de.soeren.dhbwplan.R
import de.soeren.dhbwplan.data.IcsParser
import de.soeren.dhbwplan.data.PlanRepository
import java.util.concurrent.TimeUnit

/** Holt den Plan alle paar Stunden im Hintergrund und meldet Änderungen per Benachrichtigung. */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repo = PlanRepository(applicationContext)
        val course = repo.course ?: return Result.success()
        return try {
            val events = IcsParser.parse(repo.download(course))
            val changes = repo.checkChanges(course, events, repo.deselected(course))
            if (changes != null) notifyChanges(applicationContext, changes)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val CHANNEL = "plan_changes"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("plan-sync", ExistingPeriodicWorkPolicy.KEEP, request)
        }

        fun notifyChanges(context: Context, text: String) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Planänderungen", NotificationManager.IMPORTANCE_DEFAULT)
            )
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) return

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notify)
                .setColor(0xFFFF2BD6.toInt())
                .setContentTitle("Stundenplan geändert")
                .setContentText(text.lineSequence().first())
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
            manager.notify(1, notification)
        }
    }
}

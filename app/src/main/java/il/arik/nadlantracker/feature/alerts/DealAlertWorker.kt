package il.arik.nadlantracker.feature.alerts

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import il.arik.nadlantracker.MainActivity
import il.arik.nadlantracker.NadlanApp
import il.arik.nadlantracker.R
import il.arik.nadlantracker.data.repository.FavoritesRepository

/**
 * Daily check: re-runs every alert-enabled favorite and notifies when the
 * deal count grew since the recorded baseline. The first check after
 * enabling only records the baseline — no notification.
 */
class DealAlertWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as NadlanApp).container
        val favorites = container.favoritesRepository.alertEnabledFavorites()

        favorites.forEach { favorite ->
            val query = favorite.query ?: return@forEach
            val result = runCatching {
                container.dealsRepository.search(query, forceRefresh = true)
            }.getOrNull() ?: return@forEach
            if (result.isStale) return@forEach // no fresh data — keep the old baseline

            val count = result.deals.size
            val baseline = favorite.lastSeenCount
            if (baseline != null && count > baseline) {
                notifyNewDeals(favorite, count - baseline)
            }
            container.favoritesRepository.updateLastSeenCount(favorite.id, count)
        }
        return Result.success()
    }

    private fun notifyNewDeals(favorite: FavoritesRepository.Favorite, newCount: Int) {
        if (Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.alerts_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )
        }

        val tapIntent = PendingIntent.getActivity(
            applicationContext,
            favorite.id.toInt(),
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(favorite.displayName)
            .setContentText(
                applicationContext.getString(R.string.alerts_new_deals, newCount)
            )
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(applicationContext)
            .notify(favorite.id.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "deal_alerts"
        const val WORK_NAME = "deal-alerts"
    }
}

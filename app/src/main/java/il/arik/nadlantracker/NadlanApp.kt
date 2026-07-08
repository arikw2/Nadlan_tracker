package il.arik.nadlantracker

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import il.arik.nadlantracker.di.AppContainer
import il.arik.nadlantracker.feature.alerts.DealAlertWorker
import java.io.File
import java.util.concurrent.TimeUnit
import org.osmdroid.config.Configuration

class NadlanApp : Application(), androidx.work.Configuration.Provider {

    lateinit var container: AppContainer
        private set

    override val workManagerConfiguration: androidx.work.Configuration
        get() = androidx.work.Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Configuration.getInstance().apply {
            // OSM tile policy requires an identifying user agent; cache stays app-private.
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
        scheduleDealAlerts()
    }

    private fun scheduleDealAlerts() {
        val request = PeriodicWorkRequestBuilder<DealAlertWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            DealAlertWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}

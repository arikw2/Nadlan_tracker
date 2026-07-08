package il.arik.nadlantracker

import android.app.Application
import il.arik.nadlantracker.di.AppContainer
import java.io.File
import org.osmdroid.config.Configuration

class NadlanApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Configuration.getInstance().apply {
            // OSM tile policy requires an identifying user agent; cache stays app-private.
            userAgentValue = packageName
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}

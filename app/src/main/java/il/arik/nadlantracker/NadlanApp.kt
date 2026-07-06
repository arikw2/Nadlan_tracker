package il.arik.nadlantracker

import android.app.Application
import il.arik.nadlantracker.di.AppContainer

class NadlanApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

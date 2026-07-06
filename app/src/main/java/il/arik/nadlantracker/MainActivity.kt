package il.arik.nadlantracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import il.arik.nadlantracker.core.ui.theme.NadlanTrackerTheme
import il.arik.nadlantracker.feature.nav.NadlanNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NadlanTrackerTheme {
                // The whole app is Hebrew — pin RTL even on non-Hebrew devices.
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    NadlanNavHost()
                }
            }
        }
    }
}

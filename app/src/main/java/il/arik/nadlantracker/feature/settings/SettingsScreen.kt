package il.arik.nadlantracker.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import il.arik.nadlantracker.R
import il.arik.nadlantracker.feature.results.CenteredMessage

@Composable
fun SettingsScreen() {
    CenteredMessage(stringResource(R.string.settings_title))
}

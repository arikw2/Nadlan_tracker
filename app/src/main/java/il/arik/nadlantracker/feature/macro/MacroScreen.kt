package il.arik.nadlantracker.feature.macro

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import il.arik.nadlantracker.R
import il.arik.nadlantracker.feature.results.CenteredMessage

@Composable
fun MacroScreen() {
    CenteredMessage(stringResource(R.string.macro_title))
}

package il.arik.nadlantracker.feature.favorites

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import il.arik.nadlantracker.R
import il.arik.nadlantracker.feature.results.CenteredMessage

@Composable
fun FavoritesScreen(onRunFavorite: (queryJson: String, favoriteId: Long) -> Unit) {
    CenteredMessage(stringResource(R.string.favorites_empty))
}

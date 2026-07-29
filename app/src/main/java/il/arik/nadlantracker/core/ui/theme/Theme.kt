package il.arik.nadlantracker.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.NoOpUpdate
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Terracotta600,
    onPrimary = Color.White,
    primaryContainer = Terracotta200,
    onPrimaryContainer = Terracotta800,
    inversePrimary = Terracotta300,

    secondary = Sage700,
    onSecondary = Color.White,
    secondaryContainer = Sage200,
    onSecondaryContainer = Sage800,

    // Reserved for the "serving cached data" banner.
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = GoldContainer,
    onTertiaryContainer = Terracotta900,

    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = Neutral700,

    surfaceContainerLowest = CreamRaised,
    surfaceContainerLow = Neutral100,
    surfaceContainer = Neutral200,
    surfaceContainerHigh = CreamSurface,
    surfaceContainerHighest = Neutral300,

    outline = Neutral400,
    outlineVariant = Neutral300,

    error = Clay,
    onError = Color.White,
    errorContainer = ClayContainer,
    onErrorContainer = ClayOnContainer,

    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
    scrim = Color.Black,
)

private val DarkColorScheme = darkColorScheme(
    primary = Terracotta400,
    onPrimary = Terracotta900,
    primaryContainer = Terracotta700,
    onPrimaryContainer = Terracotta200,
    inversePrimary = Terracotta600,

    secondary = Sage400,
    onSecondary = Sage900,
    secondaryContainer = Sage700,
    onSecondaryContainer = Sage200,

    tertiary = GoldLight,
    onTertiary = Terracotta900,
    tertiaryContainer = Gold,
    onTertiaryContainer = GoldContainer,

    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
    surfaceVariant = Neutral900,
    onSurfaceVariant = Neutral500,

    surfaceContainerLowest = Color(0xFF1A1817),
    surfaceContainerLow = Color(0xFF252221),
    surfaceContainer = Color(0xFF2B2826),
    surfaceContainerHigh = Neutral900,
    surfaceContainerHighest = Neutral800,

    outline = Neutral700,
    outlineVariant = Neutral800,

    error = ClayLight,
    onError = ClayOnContainer,
    errorContainer = ClayDarkContainer,
    onErrorContainer = ClayContainer,

    inverseSurface = Cream,
    inverseOnSurface = Ink,
    scrim = Color.Black,
)

/**
 * Dynamic color is off by design.
 *
 * With it on, Android 12+ replaced this entire palette with colors sampled from
 * the user's wallpaper — which meant the app had a theme that almost nobody saw,
 * and the ₪/m² map scale had to fight whatever hue it landed next to. Data
 * products need a stable palette: a reader has to learn that sage means a rise
 * and terracotta a fall, and that only works if it is the same every time.
 *
 * The parameter is kept so callers don't break, but leave it false.
 */
@Composable
fun NadlanTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                // Dark icons over the cream ground, light icons over ink.
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

package il.arik.nadlantracker.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import il.arik.nadlantracker.R

// Rubik carries the display voice, Assistant the body. Both ship full Hebrew
// coverage with matching x-heights, and both are open source.
//
// Drop these into app/src/main/res/font/ (Google Fonts → Download family):
//   rubik_medium.ttf, rubik_semibold.ttf, rubik_bold.ttf
//   assistant_light.ttf, assistant_regular.ttf, assistant_semibold.ttf, assistant_bold.ttf
//
// Filenames must be lowercase with underscores or aapt will reject them.

val Rubik = FontFamily(
    Font(R.font.rubik_medium, FontWeight.Medium),
    Font(R.font.rubik_semibold, FontWeight.SemiBold),
    Font(R.font.rubik_bold, FontWeight.Bold),
)

val Assistant = FontFamily(
    Font(R.font.assistant_light, FontWeight.Light),
    Font(R.font.assistant_regular, FontWeight.Normal),
    Font(R.font.assistant_semibold, FontWeight.SemiBold),
    Font(R.font.assistant_bold, FontWeight.Bold),
)

// Hebrew has no ascenders/descenders to speak of and no case contrast, so it
// reads smaller than Latin at the same point size and needs more leading.
// Every size below is a step up from the Material default for that role.
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 52.sp, lineHeight = 60.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 42.sp, lineHeight = 50.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp, lineHeight = 42.sp,
    ),

    headlineLarge = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp, lineHeight = 38.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp, lineHeight = 33.sp,
    ),
    // Screen titles land here — חיפוש עסקאות, המעקב שלי, מדדי הדיור.
    headlineSmall = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp, lineHeight = 30.sp,
    ),

    // Prices and index values. Rubik's figures are the reason to use it here.
    titleLarge = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp, lineHeight = 27.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Rubik, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 23.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 20.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp, lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp,
    ),

    // Chips, nav labels, stat captions. Bold, because Assistant at 12sp in
    // Hebrew disappears at Regular.
    labelLarge = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 19.sp, letterSpacing = 0.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp, lineHeight = 17.sp, letterSpacing = 0.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Assistant, fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
)

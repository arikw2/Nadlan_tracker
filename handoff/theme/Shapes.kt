package il.arik.nadlantracker.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Organic over-rounds. Material's defaults (4/8/12/16/28) are too tight for it,
// and the difference between a 12dp card and a 22dp card is most of why the
// redesign reads as warmer than the current build.
//
// Buttons, chips and inputs are not covered here — they take CircleShape at the
// call site (border-radius: 999px in the design).
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

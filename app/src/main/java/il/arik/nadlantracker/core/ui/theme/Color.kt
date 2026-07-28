package il.arik.nadlantracker.core.ui.theme

import androidx.compose.ui.graphics.Color

// Organic — warm cream ground, terracotta accent, sage second accent.
// Ramps are generated in OKLCH on one shared lightness scale, so the same
// step of any role carries the same visual weight.

// Neutral
val Neutral100 = Color(0xFFF9F4ED)
val Neutral200 = Color(0xFFEEE7DB)
val Neutral300 = Color(0xFFDCD3C4)
val Neutral400 = Color(0xFFC0B6A5)
val Neutral500 = Color(0xFFA19786)
val Neutral600 = Color(0xFF82796A)
val Neutral700 = Color(0xFF645C50)
val Neutral800 = Color(0xFF474238)
val Neutral900 = Color(0xFF2E2B25)

// Terracotta — the primary accent
val Terracotta100 = Color(0xFFFFF2EB)
val Terracotta200 = Color(0xFFFFE1D0)
val Terracotta300 = Color(0xFFFFC6A5)
val Terracotta400 = Color(0xFFF6A06B)
val Terracotta500 = Color(0xFFD67F48)
val Terracotta600 = Color(0xFFB2622D)
val Terracotta700 = Color(0xFF8C491A)
val Terracotta800 = Color(0xFF643312)
val Terracotta900 = Color(0xFF402310)

// Sage — a genuine second voice, not a highlight
val Sage100 = Color(0xFFF0FAE1)
val Sage200 = Color(0xFFE1EECC)
val Sage300 = Color(0xFFCCDBB2)
val Sage400 = Color(0xFFAEBF92)
val Sage500 = Color(0xFF8FA073)
val Sage600 = Color(0xFF728157)
val Sage700 = Color(0xFF56633F)
val Sage800 = Color(0xFF3D472B)
val Sage900 = Color(0xFF272E1B)

// Ground and ink
val Cream = Color(0xFFF5EAD8)
val CreamSurface = Color(0xFFEBDDC5)
val CreamRaised = Color(0xFFFDF7EA)
val Ink = Color(0xFF201E1D)

// Gold — carried over from the original theme, now used only for the
// "serving cached data" state so it stays distinct from both accents.
val Gold = Color(0xFF7A5F1F)
val GoldContainer = Color(0xFFF7E3C6)
val GoldLight = Color(0xFFE8C77D)

// Error — warmed so it belongs to this palette rather than Material's default red
val Clay = Color(0xFFA33327)
val ClayLight = Color(0xFFF0A08F)
val ClayContainer = Color(0xFFF7D9D2)
val ClayOnContainer = Color(0xFF5C1C14)
val ClayDarkContainer = Color(0xFF7A2A1E)

// ₪/m² quintile scale for the map. Unchanged from MapTab.kt — these are a
// data encoding, not brand colors, and they must stay legible over raster tiles.
val ScaleCheapest = Color(0xFF2E7D32)
val ScaleCheap = Color(0xFF9BC53D)
val ScaleMid = Color(0xFFF2C14E)
val ScaleDear = Color(0xFFEF8354)
val ScaleDearest = Color(0xFFC5283D)

// Trend direction. Sage for a rise, terracotta for a fall — deliberately not
// green/red, which reads as good/bad. A price rise is not good news to a buyer.
val TrendUp = Sage700
val TrendDown = Terracotta600
val TrendUpDark = Sage400
val TrendDownDark = Terracotta400

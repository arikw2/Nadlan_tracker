# Theme handoff — נדל״ן טרקר

Four files for `app/src/main/java/il/arik/nadlantracker/core/ui/theme/`. They replace `Color.kt`, `Type.kt` and `Theme.kt`; `Shapes.kt` is new.

This lands the palette and type without touching a single screen. Every existing composable picks it up through `MaterialTheme`.

## 1. Fonts

Download from Google Fonts and drop the `.ttf` files into `app/src/main/res/font/` — lowercase names with underscores or aapt rejects them:

```
rubik_medium.ttf        rubik_semibold.ttf      rubik_bold.ttf
assistant_light.ttf     assistant_regular.ttf
assistant_semibold.ttf  assistant_bold.ttf
```

Both families are SIL Open Font License and both carry full Hebrew coverage.

Bundled rather than downloadable fonts on purpose: downloadable fonts need Play Services, resolve asynchronously, and flash the fallback face on first paint. Seven files is about 400KB — worth it for a first frame that is never wrong.

## 2. Copy the files

```
Color.kt   → replaces the six Teal/Gold vals
Type.kt    → replaces the one-line default Typography
Theme.kt   → replaces the dynamicColor branch
Shapes.kt  → new
```

`MainActivity.kt` needs no change — `NadlanTrackerTheme { }` keeps its signature.

## 3. What changes on screen, with no other edits

- **The app finally has a palette.** `dynamicColor = true` meant Android 12+ sampled the user's wallpaper and discarded your theme entirely. Most users have never seen the teal.
- **Type stops being Roboto.** `Type.kt` was an empty `Typography()`.
- **Cards round from 12dp to 22dp** via `Shapes.large`.
- **The stale-data banner keeps its own color** — `tertiaryContainer` is now reserved for it, so it reads as a state rather than as decoration.

## Two decisions worth knowing about

**Trend direction is sage and terracotta, not green and red.** A price rise is not good news to a buyer and not bad news to a seller — your audience is mixed, so the palette shouldn't editorialise. It also survives red-green color blindness, which green/red does not.

**The map's ₪/מ״ר quintile scale is unchanged.** Those five colors in `MapTab.kt` are a data encoding, and they have to stay legible over osmdroid raster tiles. They're now named in `Color.kt` (`ScaleCheapest`…`ScaleDearest`) so they're at least declared in one place.

## Contrast

`primary` is Terracotta600 `#b2622d`, not the Organic base `#c67139`. The base is tuned to 3:1 against cream — fine for icons and large text, short of the 4.5:1 that white-on-primary button labels need. Terracotta600 clears it at 4.67:1. Same reasoning puts `secondary` at Sage700 rather than the `#7a8a5e` base.

## Not included

RTL is already correct in your build — `locales_config.xml` plus `Icons.AutoMirrored` is the right setup and this doesn't touch it.

One thing to fix separately, in `Formatters.kt`: `percent()` returns `"+3.5%"` with a bare leading sign, which bidi reorders to `3.5%+` inside an RTL layout. Wrap the numeric run in isolates:

```kotlin
fun percent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "\u2066$sign${"%.1f".format(value)}%\u2069"
}
```

`price()` already handles this with an RLM prefix; `percent()` was missed. It shows up on every trend readout in the app.

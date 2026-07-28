# Redesign brief — נדל״ן טרקר

Everything needed to implement the redesign. Read this first, then `theme/README.md`.

## What's here

```
handoff/
  BRIEF.md                  ← you are here
  theme/
    Color.kt                drop-in replacement for core/ui/theme/Color.kt
    Type.kt                 drop-in replacement for core/ui/theme/Type.kt
    Theme.kt                drop-in replacement for core/ui/theme/Theme.kt
    Shapes.kt               new file
    README.md               font setup + rationale
  Nadlan Tracker Redesign.dc.html   the visual reference — open in a browser
```

The `.dc.html` file is the design, not source to compile. Open it in a browser to see every screen. It is interactive: sort the desktop table, hover rows to light map parcels, tap the range pills.

## Order of work

**1. Land the theme.** Copy the four `.kt` files into `app/src/main/java/il/arik/nadlantracker/core/ui/theme/`, add the seven font files listed in `theme/README.md` to `app/src/main/res/font/`, build. No screen edits. The app will already look substantially different — the current build discards its own palette on Android 12+ because `dynamicColor = true`.

**2. Fix the bidi bug in `core/ui/Formatters.kt`.** `percent()` returns a bare `"+3.5%"`, which RTL reorders to `3.5%+`. Wrap the numeric run in isolates:

```kotlin
fun percent(value: Double): String {
    val sign = if (value > 0) "+" else ""
    return "\u2066$sign${"%.1f".format(value)}%\u2069"
}
```

`price()` already guards this with an RLM; `percent()` was missed. It's visible on every trend readout in the app.

**3. Restructure navigation.** See below.

**4. Rebuild screens** against the visual reference, one at a time.

## Navigation change

Current bottom nav: `חיפוש · מועדפים · מדד הדיור · הגדרות`

New bottom nav: `מעקב · חיפוש · מדד · השוואה`

- **מעקב becomes the start destination.** The app currently opens on an empty search field. A tracker should open on what you're already tracking. This is the saved-areas list, with the מדד hero card on top.
- **מועדפים disappears as a destination** — it *is* the home screen now. Keeping both showed the same list twice.
- **השוואה gets promoted** out of the compare-mode chip buried inside מועדפים. Most users will never find it where it currently lives.
- **הגדרות demotes** to a gear icon in the home header.

`NadlanNavHost.kt` and `NavRoutes.kt` both need editing. `CompareScreen.kt` already exists and mostly works — it needs a route of its own and an area-picker entry point, not a rewrite.

## Screens in the reference

Each maps to existing code except where marked new.

| Design | Existing file |
|---|---|
| מעקב (home) | new composition of FavoritesScreen.kt + MacroScreen.kt |
| חיפוש | feature/search/SearchScreen.kt |
| עסקאות | feature/results/ResultsScreen.kt |
| מגמות | feature/results/TrendsTab.kt |
| מפה | feature/results/MapTab.kt |
| עסקה (deal detail) | **new** — sheet over ResultsScreen |
| השוואה | feature/compare/CompareScreen.kt |
| מדד הדיור | feature/macro/MacroScreen.kt |
| פתיחה (first run) | **new** |
| Edge states | DealsRepository.kt, TrendCalculator.kt |

## The two new screens

**עסקה — deal detail sheet.** Today a deal card is terminal; the only exit is out to nadlan.gov.il. The sheet gives the deal context: its ₪/מ״ר against the neighbourhood median, גוש/חלקה, and the three nearest comparable sales. Opens as a `ModalBottomSheet` from a card tap.

**פתיחה — first run.** A new user currently lands on a blank search field with no idea what the app does. One line stating the premise, then six seed areas as chips — one tap starts tracking. Show once, gate on a DataStore flag.

## Edge states

Both are frequent, not exotic. govmap rate-limits aggressively, and radius searches routinely return single-digit deal counts.

- **WAF block / cached data.** Name the cause, show when the cache was taken, offer both "show cached" and "retry". Uses `tertiaryContainer`, which is reserved for this state.
- **Too few deals for a trend.** Say how many there are and offer the two fixes as buttons — wider radius, longer period. Don't render an empty chart frame.

## Things to preserve

- **The map's five ₪/מ״ר quintile colors are unchanged.** They're a data encoding, not brand color, and must stay legible over osmdroid raster tiles. Now named in `Color.kt` as `ScaleCheapest`…`ScaleDearest`.
- **RTL setup is already correct** — `locales_config.xml` plus `Icons.AutoMirrored`. Don't touch it.
- **The launcher icon's house glyph** is used as the brand mark in the redesign, path data unchanged from `ic_launcher_foreground.xml`. Only the tile color moves, teal → ink.

## Two decisions worth knowing

**Trend direction is sage/terracotta, not green/red.** The audience is mixed — a price rise is good news to a seller and bad to a buyer, so the palette shouldn't editorialise. It also survives red-green color blindness.

**Type is Rubik over Assistant.** Both carry full Hebrew and ship under the OFL. Sizes are stepped up from Material defaults throughout: Hebrew has no ascenders, descenders or case contrast, so it reads smaller than Latin at the same point size.

## Not covered

Desktop web exists in the reference as a separate design (a persistent map beside a sortable deal table). It is not a port of the Android UI and shouldn't be built from this codebase. iPhone via Compose Multiplatform hasn't been designed yet.

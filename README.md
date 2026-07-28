# נדל״ן טרקר — Nadlan Tracker

Android app (Hebrew, RTL) for tracking Israeli real-estate data straight from official government sources: actual transaction prices, trend graphs, and the CBS dwelling-price index — with saved searches and offline caching.

## Features

- **מעקב (home)** — the app opens on what you're already tracking: the CBS index as a hero card over your saved areas, each with its median price, ₪/m², 12-month sparkline and change. Tap an area to re-run it; toggle the bell for daily alerts.
- **חיפוש עסקאות** — search real transactions by street, neighborhood, whole settlement, or radius around an address, using the public govmap.gov.il API that backs [nadlan.gov.il](https://www.nadlan.gov.il) (Israel Tax Authority data). Filter by period and room count; deal cards show price, ₪/m², rooms, floor, property type, neighborhood, and גוש/חלקה.
- **עסקה** — tapping a deal opens a detail sheet: its ₪/m² against the surrounding median, the registry reference, the three nearest comparable sales, and a link to the official parcel page for construction year.
- **מגמות** — charts per search: median price by month, median ₪/m², and monthly deal counts, each printing its own value range instead of a dense axis.
- **מפה** — every search result also renders on an OpenStreetMap view (osmdroid, no API key), each deal a dot colored by ₪/m² quintile from green (cheap) to red (expensive), using the parcel centroid that govmap attaches to every deal.
- **מדדי הדיור** — the CBS dwelling-price index (40010) *and* rent index (120460) from [api.cbs.gov.il](https://api.cbs.gov.il), with 5y/10y/full-history views and latest/YoY/MoM stats — buy vs. rent trends side by side.
- **השוואת אזורים** — its own tab: tick two tracked areas, get their median-price lines overlaid plus a paired table of medians, ₪/m², area and deal counts.
- **התראות עסקאות** — toggle the bell on a tracked area and a daily background check (WorkManager) notifies you when new transactions are registered there.
- **פתיחה** — a first-run screen states the premise in one line and seeds a tracked area in one tap, instead of dropping a new user on an empty search field.
- **Offline-friendly** — results are cached in Room for 24h; when the government source is down or rate-limits, the app serves the cached data with a banner that names the cause and offers a retry.

## Design

The UI follows a design handoff kept in [`handoff/`](handoff/) — open `handoff/Nadlan Tracker Redesign.dc.html` in a browser for the visual reference, and read `handoff/BRIEF.md` for the rationale. The direction is "Organic": a warm cream ground with terracotta primary and sage secondary, Rubik for display and Assistant for body (both OFL, both full Hebrew, bundled as static instances — see [`docs/FONTS.md`](docs/FONTS.md)), 22dp cards and pill-shaped controls.

Two decisions worth knowing:

- **`dynamicColor` is off.** With it on, Android 12+ replaced the palette with wallpaper-sampled colors, so almost nobody saw the app's own theme and the ₪/m² map scale had to fight whatever hue it landed next to. A data product needs a stable palette.
- **Trend direction is sage/terracotta, not green/red.** A price rise is good news to a seller and bad to a buyer, so the palette doesn't editorialise — and it survives red-green color blindness. The five ₪/m² map quintile colors are unchanged: they're a data encoding, not brand color.

## Architecture

Single-module Kotlin app, package-by-feature, no backend server — the app talks to the public endpoints directly.

| Layer | What's in it |
|---|---|
| `core/network` | Retrofit APIs for govmap + CBS, browser-header/rate-limit/retry interceptors |
| `core/database` | Room: cached deals, favorites, index points, cache metadata |
| `data` | tolerant DTOs, mappers (incl. near-duplicate deal collapsing), repositories |
| `domain` | `SearchQuery` (serializable, persisted by favorites), `TrendCalculator` |
| `feature` | Compose screens: home, search, results (deals + trends + map + deal sheet), compare, macro, onboarding, settings |

Bottom navigation is `מעקב · חיפוש · מדד · השוואה` as a floating pill; settings is a gear in the home header.

Key libraries: Jetpack Compose (Material 3), Navigation-Compose type-safe routes, Room, Retrofit + kotlinx.serialization, Vico charts, coroutines.

### The undocumented APIs

The government endpoints are undocumented and were mapped by probing — the full discovered contract is documented in [`scripts/probe_api.sh`](scripts/probe_api.sh), which also regenerates the recorded JSON fixtures in `app/src/test/resources/fixtures/` used by the parsing contract tests. Notable quirks:

- govmap `street-deals` / `neighborhood-deals` / `settlement-deals` honor `limit`/`offset`, plus (discovered from the site's own frontend bundle) `startDate`/`endDate`, `roomNums`, and `propertyType` (Hebrew values). The app pushes the date window server-side; rooms/type filters stay client-side so tweaking them never refetches. `dealType` exists in the frontend but the server 500s on it.
- Construction year (שנת בנייה) is **not** in the govmap schema. It exists only in nadlan.gov.il's `deal-data` API (`yearBuilt`), which sits behind a signed JWT, reCAPTCHA, and per-user quotas — deliberately not integrated.
- The same transaction often appears twice (two source systems, dates ±1 day, amounts ±0.5%) — the mapper collapses these near-duplicates.
- The API rate-limits aggressively; the app throttles to one govmap request per 1.5s, backs off on 429/5xx, and treats a double-403 as a WAF block that falls back to cache.

## Building

```bash
./gradlew assembleDebug testDebugUnitTest
```

Requires JDK 17+ and the Android SDK (platform 35). All tests are JVM-only (JUnit + Robolectric + MockWebServer) — no emulator needed. CI (`.github/workflows/android.yml`) builds every push and uploads the debug APK as an artifact: open the run's **Artifacts** section and download `app-debug-apk` to install on a device.

## Disclaimer

Data is presented as-is from public government sources. This is not investment advice. האפליקציה אינה ייעוץ השקעות.

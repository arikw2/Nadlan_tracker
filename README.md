# נדל״ן טרקר — Nadlan Tracker

Android app (Hebrew, RTL) for tracking Israeli real-estate data straight from official government sources: actual transaction prices, trend graphs, and the CBS dwelling-price index — with saved searches and offline caching.

## Features

- **חיפוש עסקאות** — search real transactions by street, neighborhood, or radius around an address, using the public govmap.gov.il API that backs [nadlan.gov.il](https://www.nadlan.gov.il) (Israel Tax Authority data). Filter by period and room count.
- **מגמות** — charts per search: median price by month, median ₪/m², and monthly deal counts.
- **מדד הדיור** — the CBS dwelling-price index (code 40010) from [api.cbs.gov.il](https://api.cbs.gov.il), with 5y/10y/full-history views and latest/YoY/MoM stats.
- **מועדפים** — save any search and re-run it later with one tap to get fresh data.
- **Offline-friendly** — results are cached in Room for 24h; when the government source is down or rate-limits, the app serves the cached data with a clear banner.

## Architecture

Single-module Kotlin app, package-by-feature, no backend server — the app talks to the public endpoints directly.

| Layer | What's in it |
|---|---|
| `core/network` | Retrofit APIs for govmap + CBS, browser-header/rate-limit/retry interceptors |
| `core/database` | Room: cached deals, favorites, index points, cache metadata |
| `data` | tolerant DTOs, mappers (incl. near-duplicate deal collapsing), repositories |
| `domain` | `SearchQuery` (serializable, persisted by favorites), `TrendCalculator` |
| `feature` | Compose screens: search, results (deals + trends), favorites, macro, settings |

Key libraries: Jetpack Compose (Material 3), Navigation-Compose type-safe routes, Room, Retrofit + kotlinx.serialization, Vico charts, coroutines.

### The undocumented APIs

The government endpoints are undocumented and were mapped by probing — the full discovered contract is documented in [`scripts/probe_api.sh`](scripts/probe_api.sh), which also regenerates the recorded JSON fixtures in `app/src/test/resources/fixtures/` used by the parsing contract tests. Notable quirks:

- govmap `street-deals` / `neighborhood-deals` honor `limit`/`offset` but **ignore date-range params** — all filtering is client-side.
- The same transaction often appears twice (two source systems, dates ±1 day, amounts ±0.5%) — the mapper collapses these near-duplicates.
- The API rate-limits aggressively; the app throttles to one govmap request per 1.5s, backs off on 429/5xx, and treats a double-403 as a WAF block that falls back to cache.

## Building

```bash
./gradlew assembleDebug testDebugUnitTest
```

Requires JDK 17+ and the Android SDK (platform 35). All tests are JVM-only (JUnit + Robolectric + MockWebServer) — no emulator needed. CI (`.github/workflows/android.yml`) builds every push and uploads the debug APK as an artifact: open the run's **Artifacts** section and download `app-debug-apk` to install on a device.

## Disclaimer

Data is presented as-is from public government sources. This is not investment advice. האפליקציה אינה ייעוץ השקעות.

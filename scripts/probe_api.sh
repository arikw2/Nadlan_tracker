#!/usr/bin/env bash
# Dev-only probe script: records live responses from the govmap and CBS APIs
# into app/src/test/resources/fixtures/. These fixtures are the contract-test
# ground truth for the app's parsers — re-run this script if the APIs change.
#
# Discovered API contract (probed 2026-07):
#
# govmap.gov.il (backs nadlan.gov.il transaction data):
#   POST /api/search-service/autocomplete
#        body: {"searchText":"...","language":"he","isAccurate":false,"maxResults":10}
#        -> {resultsCount, results:[{id, text, type, score, shape:"POINT(x y)", originalText}], aggregations}
#        types: street | address | poi | institutes; coordinates are EPSG:3857 (Web Mercator)
#   GET  /api/real-estate/deals/{x},{y}/{radiusMeters}
#        -> [{dealscount:"4", settlementNameHeb, streetNameHeb, houseNum, polygon_id, objectid}]
#        (buildings/parcels near the point; use polygon_id for the deal endpoints below)
#   GET  /api/real-estate/street-deals/{polygonId}?limit=&offset=
#        -> {totalCount:"98", data:[deal...]} — all deals on that polygon's street
#   GET  /api/real-estate/neighborhood-deals/{polygonId}?limit=&offset=
#        -> same envelope — deals in the polygon's neighborhood (totalCount caps at 1500)
#   GET  /api/real-estate/settlement-deals/{polygonId}?limit=&offset=
#        -> same envelope — deals across the whole settlement (city/town)
#   Extra query params on all three deals endpoints (from the govmap frontend bundle):
#        startDate/endDate ("YYYY-MM-DD") — VERIFIED working server-side date filter
#        roomNums ("3,3.5,4") — VERIFIED working
#        propertyType (Hebrew description, e.g. "דירה") — VERIFIED; English enum values return 0
#        dealType ("first-hand-deal"/"second-hand-deal") — server returns 500, do not use
#   Deal fields: objectid, dealId, dealAmount(int ILS), dealDate(ISO), assetArea(int m2),
#        assetRoomNum, floorNo(Hebrew), propertyTypeDescription?, dealNatureDescription?,
#        settlementNameHeb/Eng, streetNameHeb/Eng, houseNum("12.0"), neighborhood,
#        gushNum, parcelNum, subParcelNum, polygonId, shape(MULTIPOLYGON), sourceorder
#   NOTES: limit/offset work; aggressive rate limiting — keep >=1.5s between requests.
#          Construction year (שנת בנייה) is NOT in the govmap deal schema; it exists only
#          in api.nadlan.gov.il/deal-data ("yearBuilt"), which is protected by a signed
#          JWT + reCAPTCHA + per-user limits — intentionally not integrated.
#
# api.cbs.gov.il (dwelling price index, code 40010):
#   GET /index/data/price?id=40010&format=json&lang=he&last=N
#   GET /index/data/price?id=40010&format=json&lang=he&startPeriod=MM-YYYY&endPeriod=MM-YYYY&page=N
#        -> {month:[{code, name, date:[{year, month, monthDesc, percent, percentYear,
#            currBase:{baseDesc, value}, prevBase}]}], quarter:[...], paging:{total_items,
#            page_size:100, current_page, last_page, next_url, ...}}
#        page size caps at 100 rows; use the paging block to fetch more.
#   Catalog: GET /index/catalog/catalog?format=json&lang=he (chapter "aa" = housing, mainCode 40010)

set -euo pipefail

FIXTURES="$(cd "$(dirname "$0")/.." && pwd)/app/src/test/resources/fixtures"
mkdir -p "$FIXTURES"

UA="Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"
GOVMAP=(-H "User-Agent: $UA" -H "Referer: https://www.govmap.gov.il/" -H "Origin: https://www.govmap.gov.il")
THROTTLE=2  # seconds between govmap calls — the API rate-limits aggressively

echo "1/5 autocomplete (דיזנגוף תל אביב)"
curl -sS --max-time 30 -X POST "https://www.govmap.gov.il/api/search-service/autocomplete" \
  -H "Content-Type: application/json" "${GOVMAP[@]}" \
  -d '{"searchText":"דיזנגוף תל אביב","language":"he","isAccurate":false,"maxResults":10}' \
  | jq . > "$FIXTURES/autocomplete.json"
sleep "$THROTTLE"

echo "2/5 radius deals (Dizengoff mid-point, 100m)"
curl -sS --max-time 30 "https://www.govmap.gov.il/api/real-estate/deals/3871101.97,3774606.05/100" \
  "${GOVMAP[@]}" | jq . > "$FIXTURES/radius_deals.json"
sleep "$THROTTLE"

echo "3/5 street deals (polygon 6902-274)"
curl -sS --max-time 30 "https://www.govmap.gov.il/api/real-estate/street-deals/6902-274" \
  "${GOVMAP[@]}" | jq . > "$FIXTURES/street_deals.json"
sleep "$THROTTLE"

echo "4/5 neighborhood deals (polygon 6902-274)"
curl -sS --max-time 30 "https://www.govmap.gov.il/api/real-estate/neighborhood-deals/6902-274" \
  "${GOVMAP[@]}" | jq . > "$FIXTURES/neighborhood_deals.json"

echo "5/5 CBS dwelling price index (last 24 months)"
curl -sS --max-time 30 "https://api.cbs.gov.il/index/data/price?id=40010&format=json&lang=he&last=24" \
  | jq . > "$FIXTURES/cbs_price_last24.json"

echo "Done. Fixtures written to $FIXTURES"

package il.arik.nadlantracker.data.mapper

import il.arik.nadlantracker.core.database.entity.DealEntity
import il.arik.nadlantracker.data.dto.govmap.AutocompleteResultDto
import il.arik.nadlantracker.data.dto.govmap.DealDto
import il.arik.nadlantracker.domain.model.Deal
import il.arik.nadlantracker.domain.model.LocationCandidate
import java.time.LocalDate
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * The single DTO→domain conversion point. Normalizes the messy upstream
 * formats (prices as numbers or formatted strings, "12.0" house numbers,
 * ISO timestamps) and drops rows that lack a usable date or price instead
 * of failing the whole response.
 */
object DealMapper {

    /** Deal plus its parcel coordinate, when the row carried a shape. */
    data class MappedDeal(val deal: Deal, val x: Double?, val y: Double?)

    fun fromDto(dto: DealDto): MappedDeal? {
        val date = parseDate(dto.dealDate) ?: return null
        val price = parseAmount(dto.dealAmount) ?: return null
        if (price <= 0) return null

        val coordinate = Wkt.firstCoordinate(dto.shape)
        val deal = Deal(
            date = date,
            priceIls = price,
            rooms = dto.assetRoomNum?.takeIf { it > 0 },
            areaSqm = dto.assetArea?.takeIf { it > 0 },
            propertyType = dto.dealNatureDescription ?: dto.propertyTypeDescription,
            address = buildAddress(dto.streetNameHeb, dto.houseNum),
            city = dto.settlementNameHeb?.trim(),
            neighborhood = dto.neighborhood?.trim()?.takeUnless { it.isEmpty() },
            floor = dto.floorNo?.trim()?.takeUnless { it.isEmpty() },
            gushHelka = buildGushHelka(dto.gushNum, dto.parcelNum, dto.subParcelNum),
        )
        return MappedDeal(deal, coordinate?.first, coordinate?.second)
    }

    /**
     * Content-derived identity: govmap emits duplicate source records for the
     * same transaction under different dealIds (one row often with null
     * descriptions), so identity comes from the deal's substance.
     */
    fun dealKey(deal: Deal): String =
        listOf(deal.date.toEpochDay(), deal.priceIls, deal.address, deal.city, deal.areaSqm, deal.rooms)
            .joinToString("|")

    /**
     * Collapses near-duplicate registrations of the same transaction. The two
     * upstream source systems disagree by a day or two on the date and by a
     * fraction of a percent on the amount (e.g. 16,140,000 vs 16,140,400 one
     * day apart at the same address), so exact hashing can't catch them.
     * Rows carrying a property type win over their sparser twins.
     */
    fun dedupeNearDuplicates(deals: List<Deal>): List<Deal> {
        val accepted = mutableListOf<Deal>()
        val byUnit = deals
            .sortedWith(compareByDescending<Deal> { it.propertyType != null }.thenBy { it.date })
            .groupBy { listOf(it.address, it.city, it.areaSqm, it.rooms) }
        for (group in byUnit.values) {
            val kept = mutableListOf<Deal>()
            for (deal in group) {
                val twin = kept.any { other ->
                    kotlin.math.abs(deal.date.toEpochDay() - other.date.toEpochDay()) <= MAX_TWIN_DAY_GAP &&
                        priceWithinTolerance(deal.priceIls, other.priceIls)
                }
                if (!twin) kept += deal
            }
            accepted += kept
        }
        return accepted
    }

    private fun priceWithinTolerance(a: Long, b: Long): Boolean {
        val larger = maxOf(a, b).toDouble()
        return kotlin.math.abs(a - b) <= larger * PRICE_TOLERANCE
    }

    private const val MAX_TWIN_DAY_GAP = 3L
    private const val PRICE_TOLERANCE = 0.005

    fun toEntity(queryHash: String, deal: Deal): DealEntity = DealEntity(
        queryHash = queryHash,
        dealKey = dealKey(deal),
        dealDateEpochDay = deal.date.toEpochDay(),
        priceIls = deal.priceIls,
        rooms = deal.rooms,
        areaSqm = deal.areaSqm,
        propertyType = deal.propertyType,
        address = deal.address,
        city = deal.city,
        neighborhood = deal.neighborhood,
        floor = deal.floor,
        gushHelka = deal.gushHelka,
    )

    fun fromEntity(entity: DealEntity): Deal = Deal(
        date = LocalDate.ofEpochDay(entity.dealDateEpochDay),
        priceIls = entity.priceIls,
        rooms = entity.rooms,
        areaSqm = entity.areaSqm,
        propertyType = entity.propertyType,
        address = entity.address,
        city = entity.city,
        neighborhood = entity.neighborhood,
        floor = entity.floor,
        gushHelka = entity.gushHelka,
    )

    fun candidateFromDto(dto: AutocompleteResultDto): LocationCandidate? {
        val (x, y) = Wkt.firstCoordinate(dto.shape) ?: return null
        val text = dto.text?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        return LocationCandidate(text, dto.type ?: "unknown", x, y)
    }

    private fun parseDate(raw: String?): LocalDate? {
        if (raw == null || raw.length < 10) return null
        return runCatching { LocalDate.parse(raw.substring(0, 10)) }.getOrNull()
    }

    private fun parseAmount(raw: JsonPrimitive?): Long? {
        val content = raw?.contentOrNull ?: return null
        val digits = content.replace(Regex("[^0-9.]"), "")
        return digits.toDoubleOrNull()?.toLong()
    }

    private fun buildGushHelka(gush: Long?, parcel: Long?, subParcel: Long?): String? {
        if (gush == null || gush <= 0) return null
        return buildString {
            append(gush)
            if (parcel != null && parcel > 0) append('/').append(parcel)
            if (parcel != null && subParcel != null && subParcel > 0) append('/').append(subParcel)
        }
    }

    private fun buildAddress(street: String?, houseNum: String?): String? {
        val cleanStreet = street?.trim().takeUnless { it.isNullOrEmpty() } ?: return null
        val number = houseNum?.trim()?.let { raw ->
            val asDouble = raw.toDoubleOrNull()
            when {
                asDouble == null -> raw.takeUnless { it.isEmpty() }
                asDouble == 0.0 -> null
                asDouble % 1.0 == 0.0 -> asDouble.toLong().toString()
                else -> raw
            }
        }
        return if (number != null) "$cleanStreet $number" else cleanStreet
    }
}

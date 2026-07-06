package il.arik.nadlantracker.data.mapper

import il.arik.nadlantracker.core.database.entity.IndexPointEntity
import il.arik.nadlantracker.data.dto.cbs.CbsPointDto
import il.arik.nadlantracker.domain.model.IndexPoint
import java.time.YearMonth

object IndexMapper {

    fun toEntity(seriesCode: Long, dto: CbsPointDto): IndexPointEntity? {
        val year = dto.year ?: return null
        val month = dto.month?.takeIf { it in 1..12 } ?: return null
        val value = dto.currBase?.value ?: return null
        return IndexPointEntity(
            seriesCode = seriesCode,
            period = "%04d-%02d".format(year, month),
            value = value,
            monthlyChangePercent = dto.percent,
            yearlyChangePercent = dto.percentYear,
        )
    }

    fun fromEntity(entity: IndexPointEntity): IndexPoint = IndexPoint(
        period = YearMonth.parse(entity.period),
        value = entity.value,
        monthlyChangePercent = entity.monthlyChangePercent,
        yearlyChangePercent = entity.yearlyChangePercent,
    )
}

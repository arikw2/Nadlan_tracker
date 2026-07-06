package il.arik.nadlantracker.core.ui

import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {

    private val hebrewLocale = Locale("he", "IL")
    private val intFormat = NumberFormat.getIntegerInstance(hebrewLocale)
    private val dateFormat = DateTimeFormatter.ofPattern("d.M.yyyy")

    fun price(ils: Long): String = "‏${intFormat.format(ils)} ₪"

    fun compactPrice(value: Double): String = when {
        value >= 1_000_000 -> trimZero(value / 1_000_000) + "M"
        value >= 1_000 -> trimZero(value / 1_000) + "K"
        else -> intFormat.format(value.toLong())
    }

    fun number(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    fun date(date: LocalDate): String = dateFormat.format(date)

    /** Short month axis label, e.g. "3/26". */
    fun shortPeriod(period: YearMonth): String = "${period.monthValue}/${period.year % 100}"

    fun percent(value: Double): String {
        val sign = if (value > 0) "+" else ""
        return "$sign${"%.1f".format(value)}%"
    }

    private fun trimZero(value: Double): String {
        val rounded = "%.1f".format(value)
        return rounded.removeSuffix(".0").removeSuffix(",0")
    }
}

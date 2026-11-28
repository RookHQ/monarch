package dev.monarchui.components.billing.invoiceview

import android.icu.text.DateFormat
import android.icu.text.NumberFormat
import android.icu.util.TimeZone
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.monarchui.theme.MonarchColors
import dev.monarchui.tokens.MonarchTokens
import java.math.BigDecimal
import java.util.Date
import java.util.Locale

/** Component tokens and the pure rules of MonarchInvoiceView. */
internal object MonarchInvoiceViewTokens {
    /** The gap between the header, the parties, the lines, the totals and the notes. */
    val SectionGap: Dp = MonarchTokens.Space.space6.dp

    /** The gap inside a block: between the parties, between the lines, between total rows. */
    val BlockGap: Dp = MonarchTokens.Space.space3.dp

    /** The gap between the lines of one party or one line item. */
    val LineGap: Dp = MonarchTokens.Space.space1.dp

    /** The gap between table cells and between header actions. */
    val CellGap: Dp = MonarchTokens.Space.space3.dp

    /** The padding around the printed copy. */
    val PrintPadding: Dp = MonarchTokens.Space.space8.dp

    /** The width of the quantity column of the line table. */
    val QuantityWidth: Dp = MonarchTokens.Space.space10.dp

    /** The width of the unit price and amount columns of the line table. */
    val FigureWidth: Dp = (MonarchTokens.Space.space20 + MonarchTokens.Space.space2).dp

    /** From this width the line items are a table with column headings, as on the web; below, stacked lines. */
    val TableFrom: Dp = (MonarchTokens.Space.space24 * 3 + MonarchTokens.Space.space12).dp

    /** From this width on, the parties and facts sit side by side. */
    val WideFrom: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The width the printed copy is laid out at before it is scaled to the page. */
    val PrintWidth: Dp = MonarchTokens.Breakpoint.sm.dp

    /** The weight of the description column of the table; the numeric columns have a weight of 1. */
    const val DESCRIPTION_WEIGHT: Float = 2f

    /** Most digits after the separator of a quantity. */
    const val QUANTITY_DIGITS: Int = 4

    /** Most digits after the separator of a tax rate. */
    const val RATE_DIGITS: Int = 3

    /** The colour of secondary text: details, notes headings, column headings. */
    fun mutedColor(colors: MonarchColors): Color = colors.textMuted


    /** The quantity written in [locale] with up to four decimals: "6.5". */
    fun quantityText(quantity: BigDecimal, locale: Locale): String =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = QUANTITY_DIGITS }
            .format(quantity)

    /** The tax rate written in [locale] with a percent sign: "18%". */
    fun rateText(rate: BigDecimal, locale: Locale): String =
        NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = RATE_DIGITS }.format(rate) + "%"

    /** A date in epoch milliseconds (UTC), as the Material 3 date picker gives it, written in [locale]. */
    fun dateText(millis: Long, locale: Locale): String =
        DateFormat.getDateInstance(DateFormat.LONG, locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    /** The title with the number after it: "Invoice INV-0042". */
    fun heading(title: String, number: String?): String =
        if (number.isNullOrBlank()) title else "$title $number"

    /** What TalkBack reads for one line: description, detail, then each labelled figure. */
    fun spokenLine(description: String, detail: String?, figures: List<Pair<String, String>>): String =
        (listOfNotNull(description, detail?.takeIf { it.isNotBlank() }) + figures.map { "${it.first} ${it.second}" })
            .joinToString(", ")
}

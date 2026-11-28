package com.rookhq.monarch.components.billing.lineitemseditor

import android.icu.text.NumberFormat
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * The colours of a line items editor.
 *
 * @property heading the heading.
 * @property required the required marker after the heading.
 * @property itemHeader the "Item 2" header of a row.
 * @property figures the "12 × $95.00" line of a read-only row and the empty message.
 * @property error the error under the rows.
 * @property divider the lines between rows and above the total.
 * @property swipe the background revealed when a row is swiped to remove it.
 * @property onSwipe the "Remove" text on that background.
 */
@Immutable
internal data class MonarchLineItemsEditorColors(
    val heading: Color,
    val required: Color,
    val itemHeader: Color,
    val figures: Color,
    val error: Color,
    val divider: Color,
    val swipe: Color,
    val onSwipe: Color,
)

/** Component tokens, the colour rules and the pure helpers of MonarchLineItemsEditor. */
internal object MonarchLineItemsEditorTokens {
    /** The space between the heading, the rows, the button, the error and the totals. */
    val SectionGap: Dp = MonarchTokens.Space.space4.dp

    /** The space between the parts of a row. */
    val FieldGap: Dp = MonarchTokens.Space.space2.dp

    /** The space above and below the content of a row. */
    val RowPadding: Dp = MonarchTokens.Space.space3.dp

    /** The space at the ends of the swipe background. */
    val SwipePadding: Dp = MonarchTokens.Space.space4.dp

    /** The space between the totals rows. */
    val TotalsGap: Dp = MonarchTokens.Space.space2.dp

    /** From this font scale on, the quantity and the price, and the discount and its type, stack. */
    const val STACK_FONT_SCALE: Float = 1.3f

    /** The most decimals a written quantity gets. */
    private const val QUANTITY_DIGITS = 6

    /** The most decimals a written rate gets: "7.125%". */
    private const val RATE_DIGITS = 3

    /**
     * The colours: the heading in the default text colour, the required marker and the error in the danger text
     * colour, row headers and figures muted, subtle dividers, and the danger surface behind a swiped row.
     */
    fun colors(colors: MonarchColors): MonarchLineItemsEditorColors = MonarchLineItemsEditorColors(
        heading = colors.textDefault,
        required = colors.dangerStrong,
        itemHeader = colors.textMuted,
        figures = colors.textMuted,
        error = colors.dangerStrong,
        divider = colors.borderSubtle,
        swipe = colors.dangerSubtle,
        onSwipe = colors.dangerStrong,
    )

    /** The narrowest window width that keeps the rows as a table; narrower ones stack each item. */
    val TableMinWidth: Dp = MonarchTokens.Space.space24.dp * 5

    /** Whether the side-by-side fields stack at [fontScale]. */
    fun stacks(fontScale: Float): Boolean = fontScale >= STACK_FONT_SCALE

    /** Whether a row can be removed: only above [minimumItems] rows. */
    fun removable(count: Int, minimumItems: Int): Boolean = count > minimumItems

    /** [major] in the currency's minor units, rounded half away from zero, as the currency field keeps it. */
    fun toMinor(major: BigDecimal?, digits: Int): Long? =
        major?.movePointRight(digits)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()

    /** [minor] units back in major units. */
    fun fromMinor(minor: Long?, digits: Int): BigDecimal? = minor?.let { BigDecimal.valueOf(it, digits) }

    /** A number from a field as an exact decimal: "1.5" stays 1.5. */
    fun decimal(value: Double?): BigDecimal? = value?.takeIf { it.isFinite() }?.let(BigDecimal::valueOf)

    /** A percentage (18 for 18%) written in [locale]: "18%", "7.125%". */
    fun percent(rate: BigDecimal, locale: Locale): String =
        NumberFormat.getPercentInstance(locale).apply { maximumFractionDigits = RATE_DIGITS }
            .format(rate.movePointLeft(2))

    /** A quantity written in [locale] without trailing zeros: "12", "1.5". */
    fun quantity(value: BigDecimal?, locale: Locale): String =
        NumberFormat.getInstance(locale).apply { maximumFractionDigits = QUANTITY_DIGITS }
            .format(value ?: BigDecimal.ZERO)
}

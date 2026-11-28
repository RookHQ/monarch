package com.rookhq.monarch.components.search.facetedfilterpanel

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/** Slider steps and number text of MonarchFacetedFilterPanel. Pure, so it is tested without a view. */
internal object FacetedFilterPanelFormat {
    /** Slider positions past this many are continuous, as a slider cannot show more ticks usefully. */
    private const val MAX_STEPS = 1_000

    /** The number of slider steps between the bounds for [kind]'s step: 0 when continuous. */
    fun steps(kind: MonarchFacetKind.Range): Int {
        val span = kind.bounds.endInclusive - kind.bounds.start
        if (kind.step <= 0f || span <= 0f) return 0
        val positions = (span / kind.step).roundToInt()
        return if (positions in 2..MAX_STEPS) positions - 1 else 0
    }

    /** A value with its unit: "25 MB". Whole numbers have no decimals. */
    fun valueText(value: Float, unit: String?, locale: Locale): String {
        val format = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
        val number = format.format(value.toDouble())
        return if (unit.isNullOrBlank()) number else "$number $unit"
    }

    /** A count in the locale's digits: "1,204". */
    fun countText(count: Int, locale: Locale): String = NumberFormat.getIntegerInstance(locale).format(count)
}

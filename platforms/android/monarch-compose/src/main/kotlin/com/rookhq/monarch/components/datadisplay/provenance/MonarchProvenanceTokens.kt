package com.rookhq.monarch.components.datadisplay.provenance

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.internal.MonarchRelativeTime
import com.rookhq.monarch.internal.MonarchSurfaceColors
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.tokens.MonarchTokens
import java.util.Locale

/** Component tokens and rules of MonarchProvenance, built from primitives and roles only. */
internal object MonarchProvenanceTokens {
    val Gap: Dp = MonarchTokens.Space.space1.dp


    /** The middle dot between the source and the time. Decorative. */
    const val SEPARATOR: String = " · "

    /** The padding of the chip; the inline form has none. */
    fun padding(variant: MonarchProvenanceVariant): PaddingValues = when (variant) {
        MonarchProvenanceVariant.Chip ->
            PaddingValues(horizontal = MonarchTokens.Space.space1_5.dp, vertical = MonarchTokens.Space.space0_5.dp)
        MonarchProvenanceVariant.Inline -> PaddingValues(MonarchTokens.Space.space0.dp)
    }

    /** The chip has the subtle background and border with muted text; the inline form only muted text. */
    fun colors(variant: MonarchProvenanceVariant, colors: MonarchColors): MonarchSurfaceColors = when (variant) {
        MonarchProvenanceVariant.Chip -> MonarchSurfaceColors(colors.bgSubtle, colors.textMuted, colors.borderSubtle)
        MonarchProvenanceVariant.Inline -> MonarchSurfaceColors(Color.Transparent, colors.textMuted, null)
    }

    /** The colour of the source: the link colour when it opens the source, else the text colour of the form. */
    fun sourceColor(clickable: Boolean, variant: MonarchProvenanceVariant, colors: MonarchColors): Color =
        if (clickable) colors.textLink else colors(variant, colors).content

    /** [time] relative to [now] by the shared Monarch rule (the web rule): "30 seconds ago", "5 minutes ago". */
    fun relativeTime(time: Long, now: Long, locale: Locale = Locale.getDefault()): String =
        MonarchRelativeTime.format(time, now, locale)
}

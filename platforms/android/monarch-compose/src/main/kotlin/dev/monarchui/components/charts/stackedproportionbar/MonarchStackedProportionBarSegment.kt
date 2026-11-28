package dev.monarchui.components.charts.stackedproportionbar

import androidx.compose.runtime.Immutable
import dev.monarchui.components.charts.chartlegend.MonarchChartColor
import dev.monarchui.foundation.MonarchTone

/**
 * One part of a `MonarchStackedProportionBar`: a palette part, or a status part with a tone.
 *
 * @property key the stable id of the part.
 * @property label the name in the legend and for TalkBack, such as "Replied". Required, so a part is never told
 * apart by colour alone. Already localised.
 * @property value the amount, in the same unit as the other parts. Negative and non-finite values count as 0.
 * @property color the palette slot of a palette part. `null` uses the position of the part: the first five take
 * `Data1` to `Data5`, later ones `Neutral`. Pin it when a filter can remove parts, so colours follow the entity.
 * @property tone the status of a status part. Used only when every part has a tone (status mode); then parts take
 * the solid colour of their tone, and `Neutral` the subtle text colour.
 */
@Immutable
public class MonarchStackedProportionBarSegment private constructor(
    public val key: String,
    public val label: String,
    public val value: Double,
    public val color: MonarchChartColor?,
    public val tone: MonarchTone?,
) {
    /**
     * A palette part.
     *
     * @param key the stable id of the part.
     * @param label the name in the legend and for TalkBack. Already localised.
     * @param value the amount. Negative and non-finite values count as 0.
     * @param color the palette slot. `null` uses the position of the part.
     */
    public constructor(key: String, label: String, value: Double, color: MonarchChartColor? = null) :
        this(key, label, value, color, null)

    /**
     * A status part, coloured by [tone] instead of the palette.
     *
     * @param key the stable id of the part.
     * @param label the name in the legend and for TalkBack. Already localised.
     * @param value the amount. Negative and non-finite values count as 0.
     * @param tone the status: `Success`, `Warning`, `Danger`, `Info` or `Neutral` (no data, paused).
     */
    public constructor(key: String, label: String, value: Double, tone: MonarchTone) :
        this(key, label, value, null, tone)
}

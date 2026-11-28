package dev.monarchui.components.scheduling.minimonth

import androidx.compose.runtime.Immutable
import dev.monarchui.foundation.MonarchTone

/**
 * Dots and a description for one day of a `MonarchMiniMonth`, for example
 * the events of that day or whether it is available.
 *
 * The dots are decoration: say what they mean in [description], which
 * TalkBack reads with the day.
 *
 * @property dots the colour of each dot: a tone, or `null` for the accent colour. At most three are drawn.
 * @property description added to what TalkBack reads for the day, for example "3 events" or "Available".
 */
@Immutable
public class MonarchMiniMonthDayInfo(
    public val dots: List<MonarchTone?>,
    public val description: String? = null,
) {
    /**
     * [dotCount] dots in the accent colour.
     *
     * @param dotCount the number of dots; at most three are drawn.
     * @param description added to what TalkBack reads for the day.
     */
    public constructor(dotCount: Int, description: String? = null) :
        this(List(dotCount.coerceIn(0, MAX_DOTS)) { null }, description)

    /** Holds the limit of dots. */
    public companion object {
        /** The most dots drawn under a day. */
        public const val MAX_DOTS: Int = 3
    }
}

package com.rookhq.monarch.components.datadisplay.metric

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorDirection
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorGoodDirection
import com.rookhq.monarch.components.charts.deltaindicator.MonarchDeltaIndicatorVariant
import com.rookhq.monarch.internal.MonarchDefaultIcons

/**
 * The change of a `MonarchMetric` against an earlier period, drawn by
 * `MonarchDeltaIndicator`. It takes the main options of the indicator.
 *
 * @property comparison what the change is measured against, for example "vs last week".
 * @property goodDirection which direction is good, which decides the tone.
 * @property variant plain text, or a tinted pill.
 * @property flatThreshold a change whose size is at most this is shown as flat.
 * @property formatValue writes the change; `null` writes a percentage.
 * @property icon draws the arrow for a direction; Defaults to the diagonal arrow of the web; `null` draws none.
 */
@Immutable
public class MonarchMetricChange private constructor(
    internal val source: MonarchMetricChangeSource,
    public val comparison: String?,
    public val goodDirection: MonarchDeltaIndicatorGoodDirection,
    public val variant: MonarchDeltaIndicatorVariant,
    public val flatThreshold: Double,
    public val formatValue: ((Double) -> String)?,
    public val icon: (@Composable (direction: MonarchDeltaIndicatorDirection) -> Unit)?,
) {
    /**
     * A change given as a ratio: 0.08 is up 8%.
     *
     * @param value the change as a ratio, or `null` when there is no earlier value.
     * @param comparison what the change is measured against, for example "vs last week".
     * @param goodDirection which direction is good.
     * @param variant plain text, or a tinted pill.
     * @param flatThreshold a change whose size is at most this is shown as flat.
     * @param formatValue writes the change; `null` writes a percentage.
     * @param icon draws the arrow for a direction.
     */
    public constructor(
        value: Double?,
        comparison: String? = null,
        goodDirection: MonarchDeltaIndicatorGoodDirection = MonarchDeltaIndicatorGoodDirection.Up,
        variant: MonarchDeltaIndicatorVariant = MonarchDeltaIndicatorVariant.Plain,
        flatThreshold: Double = 0.0,
        formatValue: ((Double) -> String)? = null,
        icon: (@Composable (direction: MonarchDeltaIndicatorDirection) -> Unit)? = MonarchDefaultIcons.Delta,
    ) : this(
        MonarchMetricChangeSource(value, null, null, fromValues = false),
        comparison,
        goodDirection,
        variant,
        flatThreshold,
        formatValue,
        icon,
    )

    /** Builds a change from two values. */
    public companion object {
        /**
         * A change worked out by `MonarchDeltaIndicator` from the current and the previous value.
         *
         * @param current the value of this period.
         * @param previous the value of the earlier period.
         * @param comparison what the change is measured against, for example "vs last week".
         * @param goodDirection which direction is good.
         * @param variant plain text, or a tinted pill.
         * @param flatThreshold a change whose size is at most this is shown as flat.
         * @param formatValue writes the change; `null` writes a percentage.
         * @param icon draws the arrow for a direction.
         * @return the change.
         */
        public fun between(
            current: Double?,
            previous: Double?,
            comparison: String? = null,
            goodDirection: MonarchDeltaIndicatorGoodDirection = MonarchDeltaIndicatorGoodDirection.Up,
            variant: MonarchDeltaIndicatorVariant = MonarchDeltaIndicatorVariant.Plain,
            flatThreshold: Double = 0.0,
            formatValue: ((Double) -> String)? = null,
            icon: (@Composable (direction: MonarchDeltaIndicatorDirection) -> Unit)? = MonarchDefaultIcons.Delta,
        ): MonarchMetricChange = MonarchMetricChange(
            MonarchMetricChangeSource(null, current, previous, fromValues = true),
            comparison,
            goodDirection,
            variant,
            flatThreshold,
            formatValue,
            icon,
        )
    }
}

/** Where the change of a metric comes from: a ratio, or two values for `MonarchDeltaIndicator` to compare. */
internal class MonarchMetricChangeSource(
    val value: Double?,
    val current: Double?,
    val previous: Double?,
    val fromValues: Boolean,
)

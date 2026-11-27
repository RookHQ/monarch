package com.rookhq.monarch.components.charts.gauge

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * A stretch of the scale of a `MonarchGauge` with a meaning, such as "Healthy" from 0 to 70.
 *
 * @property range the stretch. A value on the boundary of two bands belongs to the upper one.
 * @property tone the colour of the band, and of the value arc while the value is in it.
 * @property label the name of the band, written under the value and added to the spoken value. Give every
 * band a label; without one the spoken value falls back to the range.
 */
@Immutable
public class MonarchGaugeBand(
    public val range: ClosedFloatingPointRange<Double>,
    public val tone: MonarchTone,
    public val label: String? = null,
)

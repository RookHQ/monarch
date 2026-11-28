package com.rookhq.monarch.components.datadisplay.scorering

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/**
 * A band of the score scale for `MonarchScoreRingColor.Auto`: scores at or
 * above [minimum] take [tone].
 *
 * @property minimum the lowest score of the band, on the scale of the ring.
 * @property tone the tone of the band.
 */
@Immutable
public data class MonarchScoreRingThreshold(val minimum: Double, val tone: MonarchTone) {
    public companion object {
        /** 80 and up success, 60 info, 40 warning, below 40 danger, on a scale of 100. */
        public val Defaults: List<MonarchScoreRingThreshold> = listOf(
            MonarchScoreRingThreshold(minimum = 80.0, tone = MonarchTone.Success),
            MonarchScoreRingThreshold(minimum = 60.0, tone = MonarchTone.Info),
            MonarchScoreRingThreshold(minimum = 40.0, tone = MonarchTone.Warning),
            MonarchScoreRingThreshold(minimum = Double.NEGATIVE_INFINITY, tone = MonarchTone.Danger),
        )
    }
}

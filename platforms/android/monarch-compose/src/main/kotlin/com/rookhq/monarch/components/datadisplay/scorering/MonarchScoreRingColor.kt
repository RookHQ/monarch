package com.rookhq.monarch.components.datadisplay.scorering

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.foundation.MonarchTone

/** The colour of the arc of a `MonarchScoreRing`. The same cases exist on every platform. */
@Immutable
public sealed interface MonarchScoreRingColor {
    /** The tone of the band the score falls in, from the thresholds. The default. */
    public data object Auto : MonarchScoreRingColor

    /** The accent colour, for a score with no good or bad meaning. */
    public data object Accent : MonarchScoreRingColor

    /**
     * One fixed tone, whatever the score.
     *
     * @property tone the tone of the arc and the number.
     */
    public data class Tone(val tone: MonarchTone) : MonarchScoreRingColor
}

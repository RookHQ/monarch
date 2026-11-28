package com.rookhq.monarch.components.intelligence.aiscore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.rookhq.monarch.R
import com.rookhq.monarch.foundation.MonarchTone

/**
 * A named range of a `MonarchAiScore`, from [minimum] up to the next band's
 * minimum. The name is always written next to the score, so the tone is never
 * the only signal.
 *
 * @property minimum the lowest score of the band, from 0 to 100.
 * @property label the name of the band, for example "High".
 * @property tone the colour of the ring and the badge while the score is in this band.
 */
@Immutable
public class MonarchAiScoreBand(
    public val minimum: Double,
    public val label: String,
    public val tone: MonarchTone = MonarchTone.Neutral,
) {
    public companion object {
        private const val MEDIUM_MINIMUM = 40.0
        private const val HIGH_MINIMUM = 70.0

        /**
         * Low from 0 (danger), Medium from 40 (warning) and High from 70
         * (success), named with the built-in strings of `strings_aiscore.xml`.
         */
        @Composable
        @ReadOnlyComposable
        public fun defaults(): List<MonarchAiScoreBand> = listOf(
            MonarchAiScoreBand(0.0, stringResource(R.string.monarch_aiscore_band_low), MonarchTone.Danger),
            MonarchAiScoreBand(
                MEDIUM_MINIMUM,
                stringResource(R.string.monarch_aiscore_band_medium),
                MonarchTone.Warning,
            ),
            MonarchAiScoreBand(HIGH_MINIMUM, stringResource(R.string.monarch_aiscore_band_high), MonarchTone.Success),
        )
    }
}

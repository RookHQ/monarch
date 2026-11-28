package dev.monarchui.components.datadisplay.scorering

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import dev.monarchui.R
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A circular score: an arc filled to [value] out of [max], with the number
 * always written in the centre.
 *
 * A determinate Material 3 `CircularProgressIndicator` with a centred `Text`.
 * The ring, the number, the label and the description are one TalkBack
 * element: the label (or "Score") is its name, "78 out of 100" its state, and
 * the range is exposed as progress information. Without a score the ring is
 * empty, the centre shows a dash and the state is "No score yet". The ring
 * grows with the font size setting so the number always fits.
 *
 * @param value the score. `null`, or a value that is not a number, shows an empty ring. Limited to 0 through [max].
 * @param modifier the modifier of the ring and its texts.
 * @param color `Auto` takes the tone of the band from [thresholds]; `Accent` is for a score with no good or bad
 * meaning; `Tone` is fixed.
 * @param size the diameter at the default font size: 48, 80 or 120dp.
 * @param max the top of the scale. Zero, negative or infinite values fall back to 100.
 * @param thresholds the bands `Auto` uses, checked from the highest minimum.
 * @param showMax shows "/ 100" under the number. `null` shows it on `Md` and `Lg`.
 * @param label shown under the ring and read as its name.
 * @param description a second line under the label, for example "Updated today".
 * @param format formats the number in the centre and in the spoken value. `null` writes the whole number in
 * the current locale.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchScoreRing(
    value: Double?,
    modifier: Modifier = Modifier,
    color: MonarchScoreRingColor = MonarchScoreRingColor.Auto,
    size: MonarchScoreRingSize = MonarchScoreRingSize.Md,
    max: Double = MonarchScoreRingTokens.DEFAULT_MAX,
    thresholds: List<MonarchScoreRingThreshold> = MonarchScoreRingThreshold.Defaults,
    showMax: Boolean? = null,
    label: String? = null,
    description: String? = null,
    format: ((Double) -> String)? = null,
    labels: MonarchScoreRingLabels = MonarchScoreRingLabels(),
) {
    val safeMax = MonarchScoreRingTokens.safeMax(max)
    val score = MonarchScoreRingTokens.clamp(value, safeMax)
    val write = format ?: MonarchScoreRingTokens::formatWhole
    val number = score?.let(write)
    val maxText = MonarchScoreRingTokens.formatWhole(safeMax)
    val tone = MonarchScoreRingTokens.tone(color, score, thresholds)
    val colors = MonarchScoreRingTokens.colors(tone, MonarchTheme.colors)
    val name = label ?: monarchString(labels.name, R.string.monarch_scorering_name)
    val spokenName = listOfNotNull(name, description).joinToString(", ")
    val state = if (number != null) {
        labels.valueText?.invoke(number, maxText) ?: stringResource(R.string.monarch_scorering_value, number, maxText)
    } else {
        monarchString(labels.noValue, R.string.monarch_scorering_no_value)
    }
    val ofMax = labels.ofMax?.invoke(maxText) ?: stringResource(R.string.monarch_scorering_of_max, maxText)
    val fraction by animateFloatAsState(
        targetValue = ((score ?: 0.0) / safeMax).toFloat(),
        animationSpec = MonarchTheme.motion.spec(MonarchTheme.motion.slow),
        label = "score ring",
    )
    val diameter: Dp = MonarchScoreRingTokens.diameter(size) * LocalDensity.current.fontScale
    Column(
        modifier = modifier.clearAndSetSemantics {
            contentDescription = spokenName
            stateDescription = state
            if (score != null) progressBarRangeInfo = ProgressBarRangeInfo(score.toFloat(), 0f..safeMax.toFloat())
        },
        verticalArrangement = Arrangement.spacedBy(MonarchScoreRingTokens.Gap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { fraction },
                modifier = Modifier.size(diameter),
                color = colors.arc,
                strokeWidth = MonarchScoreRingTokens.stroke(diameter),
                trackColor = colors.track,
                strokeCap = StrokeCap.Round,
                gapSize = MonarchScoreRingTokens.ArcGap,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = number ?: MonarchScoreRingTokens.NO_VALUE_MARK,
                    style = MonarchScoreRingTokens.numberStyle(size, MonarchTheme.typography),
                    color = colors.text,
                    maxLines = 1,
                )
                if (MonarchScoreRingTokens.showsMax(showMax, size, score != null)) {
                    Text(text = ofMax, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
                }
            }
        }
        if (label != null) {
            Text(
                text = label,
                style = MonarchTheme.typography.label,
                color = MonarchTheme.colors.textDefault,
            )
        }
        if (description != null) {
            Text(
                text = description,
                style = MonarchTheme.typography.caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
    }
}

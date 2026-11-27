package com.rookhq.monarch.components.datadisplay.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import com.rookhq.monarch.R
import com.rookhq.monarch.components.typography.tabularnumberstyle.MonarchTabularNumberStyle
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchNumberText
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Shows that work is in progress, as a bar or a ring.
 *
 * Drawn by Monarch (a pill track with a pill fill, a ring with a round-capped arc), not the Material 3
 * indicators, which leave a gap between the fill and the track and draw a stop dot. With a [progress] value it
 * is determinate and TalkBack reads the percentage; without one it is
 * indeterminate. When the user has turned animations off, an indeterminate
 * indicator stands still and is still announced as indeterminate.
 *
 * @param modifier the modifier of the indicator.
 * @param progress the completed fraction from 0 to 1, or `null` when the duration is unknown.
 * Values outside the range are clamped.
 * @param variant a bar or a ring.
 * @param label what is in progress, for TalkBack, for example "Uploading report". `null` uses the built-in
 * string `monarch_progress_loading` when indeterminate and no label when determinate.
 * @param tone the colour of the indicator. The accent by default; a status tone for quotas and checks
 * ("over the limit" in danger). Keep the meaning in text too.
 * @param size the thickness of a bar or the diameter of a ring.
 * @param showValue writes the percentage after the bar or the small ring, or inside a large ring. Only for a
 * determinate indicator. TalkBack reads the percentage once, from the indicator.
 */
@Composable
public fun MonarchProgress(
    modifier: Modifier = Modifier,
    progress: Float? = null,
    variant: MonarchProgressVariant = MonarchProgressVariant.Linear,
    label: String? = null,
    tone: MonarchTone = MonarchTone.Accent,
    size: MonarchProgressSize = MonarchProgressSize.Md,
    showValue: Boolean = false,
) {
    val description = label ?: if (progress == null) stringResource(R.string.monarch_progress_loading) else null
    val colors = MonarchProgressTokens.colors(tone, MonarchTheme.colors)
    val fraction = progress?.coerceIn(0f, 1f)
    val locale = LocalConfiguration.current.locales[0]
    val percent = remember(locale) { MonarchNumberText.percent(locale, maxFractionDigits = 0) }
    val value = if (showValue && fraction != null) percent(fraction.toDouble()) else null
    when {
        value == null -> Indicator(modifier, variant, size, fraction, description, colors)
        MonarchProgressTokens.valueInside(variant, size) ->
            RingWithValue(modifier, size, fraction ?: 0f, value, description, colors)
        else -> Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(MonarchProgressTokens.ValueGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bar = if (variant == MonarchProgressVariant.Linear) Modifier.weight(1f) else Modifier
            Indicator(bar, variant, size, fraction, description, colors)
            ValueText(value, colors, Modifier)
        }
    }
}

/** One indicator node: the range or "indeterminate", and the description. */
@Composable
@Suppress("LongParameterList")
private fun Indicator(
    modifier: Modifier,
    variant: MonarchProgressVariant,
    size: MonarchProgressSize,
    fraction: Float?,
    description: String?,
    colors: MonarchProgressColors,
) {
    when (variant) {
        MonarchProgressVariant.Linear -> Bar(modifier, size, fraction, description, colors)
        MonarchProgressVariant.Circular -> ProgressRing(
            modifier = modifier,
            diameter = MonarchProgressTokens.ringDiameter(size),
            stroke = MonarchProgressTokens.ringStroke(size),
            fraction = fraction,
            description = description,
            colors = colors,
        )
    }
}

@Composable
private fun Bar(
    modifier: Modifier,
    size: MonarchProgressSize,
    fraction: Float?,
    description: String?,
    colors: MonarchProgressColors,
) {
    // With animations off an indeterminate bar is a still segment at the start, announced as indeterminate.
    ProgressBar(
        modifier = modifier,
        height = MonarchProgressTokens.barHeight(size),
        fraction = fraction,
        description = description,
        colors = colors,
    )
}

/** A large ring with the value inside. The ring grows with the text at large font sizes, so the value is never cut. */
@Composable
@Suppress("LongParameterList")
private fun RingWithValue(
    modifier: Modifier,
    size: MonarchProgressSize,
    fraction: Float,
    value: String,
    description: String?,
    colors: MonarchProgressColors,
) {
    val diameter = MonarchProgressTokens.ringDiameter(size)
    Layout(
        content = {
            // The layout node carries the range and the name; the ring is drawing only.
            ProgressRing(
                modifier = Modifier,
                diameter = diameter,
                stroke = MonarchProgressTokens.ringStroke(size),
                fraction = fraction,
                description = null,
                colors = colors,
                semantics = false,
            )
            ValueText(value, colors, Modifier)
        },
        modifier = modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            if (description != null) contentDescription = description
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val text = measurables[1].measure(loose)
        val inset = (MonarchProgressTokens.ringStroke(size) + MonarchProgressTokens.ValueGap).roundToPx() * 2
        val side = maxOf(diameter.roundToPx(), text.width + inset, text.height + inset)
        val ring = measurables[0].measure(Constraints.fixed(side, side))
        layout(side, side) {
            ring.place(0, 0)
            text.place((side - text.width) / 2, (side - text.height) / 2)
        }
    }
}

@Composable
private fun ValueText(value: String, colors: MonarchProgressColors, modifier: Modifier) {
    Text(
        text = value,
        // Shown only: TalkBack reads the percentage from the indicator.
        modifier = modifier.semantics { hideFromAccessibility() },
        // Tabular figures, so "64%" and "100%" line up and a changing value does not jitter.
        style = MonarchTabularNumberStyle.applyTo(MonarchTheme.typography.label),
        color = colors.value,
    )
}

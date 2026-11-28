package com.rookhq.monarch.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.theme.MonarchTheme
import com.rookhq.monarch.tokens.MonarchTokens

/** The filled part of a still ring, drawn while animations are off. */
internal const val MonarchStillProgressFraction: Float = 0.25f

/**
 * The Material 3 progress ring with the Monarch rule for reduced motion: an
 * indeterminate ring stands still (a quarter arc) when the user has turned
 * animations off, and is still announced as indeterminate. Components that
 * show a small busy ring use this instead of their own copy.
 *
 * @param progress the completed fraction from 0 to 1, or `null` for indeterminate.
 * @param diameter the size of the ring.
 * @param color the arc.
 * @param trackColor the full circle behind the arc. `Color.Transparent` for none.
 * @param strokeWidth the width of the arc.
 * @param modifier the modifier of the ring.
 * @param label what TalkBack reads for the ring, or `null` to leave the naming to a parent.
 * @param stillFraction the part of the ring drawn while animations are off.
 */
@Composable
@Suppress("LongParameterList")
internal fun MonarchProgressRing(
    progress: Float?,
    diameter: Dp,
    color: Color,
    trackColor: Color,
    strokeWidth: Dp,
    modifier: Modifier = Modifier,
    label: String? = null,
    stillFraction: Float = MonarchStillProgressFraction,
) {
    val still = progress == null && MonarchTheme.motion.reducedMotion
    when {
        still -> Box(
            modifier = modifier
                .size(diameter)
                .clearAndSetSemantics {
                    progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
                    if (label != null) contentDescription = label
                },
        ) {
            CircularProgressIndicator(
                progress = { stillFraction },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = strokeWidth,
                gapSize = MonarchTokens.Space.space0.dp,
                trackColor = trackColor,
            )
        }
        progress == null -> CircularProgressIndicator(
            modifier = modifier.size(diameter).labelled(label),
            color = color,
            strokeWidth = strokeWidth,
            gapSize = MonarchTokens.Space.space0.dp,
            trackColor = trackColor,
        )
        else -> CircularProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = modifier.size(diameter).labelled(label),
            color = color,
            strokeWidth = strokeWidth,
            gapSize = MonarchTokens.Space.space0.dp,
            trackColor = trackColor,
        )
    }
}

private fun Modifier.labelled(label: String?): Modifier =
    if (label == null) this else semantics { contentDescription = label }

package dev.monarchui.components.technical.lifecyclebadge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * The release stage of an API, SDK or feature: planned, preview, stable or
 * deprecated, as a quiet capsule with a coloured dot and a text label.
 *
 * Built by Monarch from the shared tone palette: `MonarchBadge` colours its
 * text with the tone and has no dot, so this component draws the web's
 * quieter look, with the tone only in the dot and the border. The label is
 * always shown; the dot is hidden from TalkBack.
 *
 * @param lifecycle the stage.
 * @param modifier the modifier of the badge.
 * @param tone replaces the tone of the stage: planned neutral, preview info, stable success,
 * deprecated warning.
 * @param showDot whether the dot before the label is drawn.
 * @param label replaces the built-in label of the stage (`monarch_lifecyclebadge_<stage>`).
 */
@Composable
public fun MonarchLifecycleBadge(
    lifecycle: MonarchLifecycle,
    modifier: Modifier = Modifier,
    tone: MonarchTone? = null,
    showDot: Boolean = true,
    label: String? = null,
) {
    LifecycleCapsule(
        text = label ?: stringResource(MonarchLifecycleBadgeTokens.labelRes(lifecycle)),
        tone = tone ?: MonarchLifecycleBadgeTokens.toneOf(lifecycle),
        showDot = showDot,
        modifier = modifier,
    )
}

/**
 * A lifecycle badge for a stage given by its key, for stages that come from
 * data, such as a customer's lifecycle stage.
 *
 * One of the four built-in keys (`planned`, `preview`, `stable`,
 * `deprecated`, matched exactly) is drawn as that stage. Any other key is
 * neutral and shown as the key with underscores and hyphens turned into
 * spaces and a capital first letter, unless [label] and [tone] are given.
 *
 * @param stage the key of the stage.
 * @param modifier the modifier of the badge.
 * @param tone replaces the tone of the stage. Unknown stages are neutral.
 * @param showDot whether the dot before the label is drawn.
 * @param label replaces the label. Already localised.
 */
@Composable
public fun MonarchLifecycleBadge(
    stage: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone? = null,
    showDot: Boolean = true,
    label: String? = null,
) {
    val builtIn = MonarchLifecycleBadgeTokens.lifecycleOf(stage)
    if (builtIn != null) {
        MonarchLifecycleBadge(builtIn, modifier, tone, showDot, label)
    } else {
        LifecycleCapsule(
            text = label ?: MonarchLifecycleBadgeTokens.readableStage(stage),
            tone = tone ?: MonarchTone.Neutral,
            showDot = showDot,
            modifier = modifier,
        )
    }
}

@Composable
private fun LifecycleCapsule(text: String, tone: MonarchTone, showDot: Boolean, modifier: Modifier) {
    val tokens = MonarchLifecycleBadgeTokens
    val colors = tokens.colors(tone, MonarchTheme.colors)
    val dotSize = tokens.DotSize * LocalDensity.current.fontScale
    Row(
        modifier = modifier
            .monarchSurface(colors.surface, MonarchShapeDefaults.Mark)
            .heightIn(min = tokens.MinHeight)
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            // Drawn only: a box without semantics, so TalkBack reads the label alone.
            Box(modifier = Modifier.size(dotSize).drawBehind { drawCircle(colors.dot) })
        }
        Text(
            text = text,
            style = MonarchTheme.typography.caption,
            fontWeight = FontWeight(tokens.FONT_WEIGHT),
            color = colors.surface.content,
        )
    }
}

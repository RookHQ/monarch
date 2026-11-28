package dev.monarchui.components.technical.methodbadge

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * An HTTP method label (GET, POST, PUT, PATCH, DELETE) for endpoint lists,
 * API references and request logs.
 *
 * Built by Monarch from the shared tone palette: `MonarchBadge` is a capsule
 * in the caption role and cannot take the code typeface or a minimum width.
 * The method is always the visible text, in upper case and in the code
 * typeface, so the tone is never the only signal. Not interactive; TalkBack
 * reads the method.
 *
 * @param method the HTTP method in any case. It is trimmed and shown in upper case.
 * @param modifier the modifier of the badge.
 * @param tone replaces the tone chosen from the method by [MonarchMethodBadgeDefaults.toneFor]:
 * GET success, POST info, PUT and PATCH warning, DELETE danger, anything else neutral.
 */
@Composable
public fun MonarchMethodBadge(
    method: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone? = null,
) {
    val tokens = MonarchMethodBadgeTokens
    val colors = tokens.colors(tone ?: MonarchMethodBadgeDefaults.toneFor(method), MonarchTheme.colors)
    val fontScale = LocalDensity.current.fontScale
    Box(
        modifier = modifier
            .monarchSurface(colors, MonarchTheme.shapes.sm)
            .widthIn(min = tokens.MinWidth * fontScale)
            .heightIn(min = tokens.MinHeight)
            .padding(horizontal = tokens.HorizontalPadding, vertical = tokens.VerticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tokens.label(method),
            style = MonarchTheme.typography.code,
            fontWeight = FontWeight(tokens.FONT_WEIGHT),
            color = colors.content,
        )
    }
}

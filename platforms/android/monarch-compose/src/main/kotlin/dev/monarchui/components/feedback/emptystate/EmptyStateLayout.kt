package dev.monarchui.components.feedback.emptystate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.internal.MonarchCentredText
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.theme.MonarchTheme

/**
 * The centred column of MonarchEmptyState and MonarchErrorState: an icon tile,
 * a heading, a message, a row of actions, extra content and a tip.
 *
 * @param title the heading.
 * @param modifier the modifier of the column.
 * @param size the spacing and the type sizes.
 * @param tileColors the fill of the icon tile and the colour of the icon.
 * @param message the text under the heading.
 * @param icon the icon in the tile; no tile without it.
 * @param tip a short hint at the end.
 * @param actions the buttons, laid out in a row that wraps.
 * @param extra content between the actions and the tip.
 */
@Composable
internal fun EmptyStateLayout(
    title: String,
    modifier: Modifier,
    size: MonarchEmptyStateSize,
    tileColors: MonarchSurfaceColors,
    message: String?,
    icon: (@Composable () -> Unit)?,
    tip: String?,
    actions: (@Composable () -> Unit)?,
    extra: (@Composable () -> Unit)? = null,
    pageTile: Boolean = false,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Column(
        // Like the web block: as wide as its container up to 448dp, centred in it.
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = MonarchEmptyStateTokens.MaxWidth)
            .padding(MonarchEmptyStateTokens.padding(size)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MonarchEmptyStateTokens.gap(size)),
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(
                        if (pageTile) MonarchEmptyStateTokens.PageTileSize else MonarchEmptyStateTokens.tileSize(size),
                    )
                    .background(
                        tileColors.container,
                        if (pageTile) {
                            MonarchTheme.shapes.xl
                        } else {
                            MonarchEmptyStateTokens.tileShape(size, MonarchTheme.shapes)
                        },
                    )
                    .clearAndSetSemantics { },
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides tileColors.content, content = icon)
            }
        }
        // Centred line by line only when the text wraps: a single centred line
        // can be cut by a pixel at the largest font size (the small title was).
        MonarchCentredText(
            text = title,
            modifier = Modifier.semantics { heading() },
            style = MonarchEmptyStateTokens.titleStyle(size, typography),
            color = colors.textDefault,
        )
        if (message != null) {
            MonarchCentredText(
                text = message,
                style = MonarchEmptyStateTokens.messageStyle(size, typography),
                color = colors.textMuted,
            )
        }
        if (actions != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(
                    MonarchEmptyStateTokens.ActionGap,
                    Alignment.CenterHorizontally,
                ),
                verticalArrangement = Arrangement.spacedBy(MonarchEmptyStateTokens.ActionGap),
            ) { actions() }
        }
        extra?.invoke()
        if (tip != null) {
            MonarchCentredText(text = tip, style = typography.caption, color = colors.textMuted)
        }
    }
}

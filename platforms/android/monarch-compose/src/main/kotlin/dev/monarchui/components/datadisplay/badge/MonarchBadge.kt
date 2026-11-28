package dev.monarchui.components.datadisplay.badge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A short status or metadata label, for example "Draft" or "3 new".
 *
 * Built by Monarch: Material 3 has no equivalent (its `Badge` is the
 * notification dot on a navigation item). A badge is not interactive and has
 * no touch target. TalkBack reads its text; the tone is never the only
 * carrier of meaning.
 *
 * @param text the label. Keep it to one or two words.
 * @param modifier the modifier of the badge.
 * @param tone the status the badge conveys.
 * @param variant the emphasis.
 * @param leadingIcon an icon before the text, for example a `MonarchIcon` of size `Sm` with a `null`
 * content description.
 * @param size the height and padding: `Md` (24dp) or the compact `Sm` (20dp, 11sp text).
 * @param dot a small status dot, in the text colour, before the text, as the web's dot badge.
 */
@Composable
public fun MonarchBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    variant: MonarchBadgeVariant = MonarchBadgeVariant.Subtle,
    leadingIcon: (@Composable () -> Unit)? = null,
    size: MonarchBadgeSize = MonarchBadgeSize.Md,
    dot: Boolean = false,
) {
    val colors = MonarchBadgeTokens.colors(tone, variant, MonarchTheme.colors)
    Row(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .heightIn(min = MonarchBadgeTokens.minHeight(size))
            .padding(
                horizontal = MonarchBadgeTokens.horizontalPadding(size),
                vertical = MonarchBadgeTokens.VerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchBadgeTokens.ContentGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides colors.content) {
            if (dot) {
                Box(
                    Modifier
                        .size(MonarchBadgeTokens.DotSize)
                        .background(colors.content, CircleShape)
                        .clearAndSetSemantics { },
                )
            }
            leadingIcon?.invoke()
            Text(
                text = text,
                style = MonarchBadgeTokens.textStyle(size, MonarchTheme.typography.caption),
                fontWeight = FontWeight(MonarchBadgeTokens.FONT_WEIGHT),
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

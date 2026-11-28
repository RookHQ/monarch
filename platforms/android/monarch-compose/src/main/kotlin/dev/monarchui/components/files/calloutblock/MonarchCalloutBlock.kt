package dev.monarchui.components.files.calloutblock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme

/**
 * A highlighted note inside a document with one paragraph of text.
 *
 * See the overload with a content slot for blocks with more than one paragraph.
 *
 * @param text the paragraph, already localised.
 * @param modifier the modifier of the block.
 * @param tone the meaning, which selects the tint and the border.
 * @param icon an icon at the start, in the tone colour, for example a `MonarchIcon` with a `null`
 * content description. Monarch does not bundle icons, so there is no default.
 * @param toneLabel replaces the built-in spoken name of the tone.
 */
@Composable
public fun MonarchCalloutBlock(
    text: String,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.callout(tone),
    toneLabel: String? = null,
) {
    MonarchCalloutBlock(modifier = modifier, tone = tone, icon = icon, toneLabel = toneLabel) {
        Text(text)
    }
}

/**
 * A highlighted note inside a document: a tinted, bordered block with an
 * optional icon that sets a paragraph or a few blocks apart from the text
 * around it. The display form of the callout block of a document.
 *
 * Built by Monarch: Material 3 has no inline note container. It is content,
 * not a live message, so it is never announced on its own; for a status
 * message with a title and an action use `MonarchBanner`.
 *
 * TalkBack reads the name of the tone ("Warning") on the block before the
 * content, so the tone does not depend on colour; the neutral tone has no
 * name. The content keeps its own elements, so links and buttons inside it
 * stay reachable.
 *
 * @param modifier the modifier of the block.
 * @param tone the meaning, which selects the tint and the border.
 * @param icon an icon at the start, in the tone colour. It is decoration: the tone is spoken by the block.
 * @param toneLabel replaces the built-in spoken name of the tone. Ignored for the neutral tone unless set.
 * @param content the content, in a column. Text takes the body style and the default text colour.
 */
@Composable
public fun MonarchCalloutBlock(
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Neutral,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.callout(tone),
    toneLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchCalloutBlockTokens.colors(tone, MonarchTheme.colors)
    val spoken = toneLabel ?: MonarchCalloutBlockTokens.toneLabel(tone)?.let { stringResource(it) }
    Row(
        modifier = modifier
            .semantics {
                isTraversalGroup = true
                if (spoken != null) contentDescription = spoken
            }
            .monarchSurface(
                MonarchSurfaceColors(colors.container, colors.content, colors.border),
                MonarchShapeDefaults.Container,
            )
            .padding(
                horizontal = MonarchCalloutBlockTokens.HorizontalPadding,
                vertical = MonarchCalloutBlockTokens.VerticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(MonarchCalloutBlockTokens.IconGap),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) {
            Box(modifier = Modifier.clearAndSetSemantics { }) {
                CompositionLocalProvider(LocalContentColor provides colors.icon, content = icon)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchCalloutBlockTokens.ContentGap),
        ) {
            CompositionLocalProvider(LocalContentColor provides colors.content) {
                ProvideTextStyle(MonarchTheme.typography.body) {
                    content()
                }
            }
        }
    }
}

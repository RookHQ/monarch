package com.rookhq.monarch.components.datadisplay.listrow

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A row of a list: an optional leading element, one to three lines of text, an optional trailing element.
 *
 * It replaces the Material 3 `ListItem` and has the same slot names, so a call changes by its name only:
 * `ListItem(headlineContent = …, supportingContent = …, leadingContent = …, trailingContent = …)` becomes
 * `MonarchListRow(headlineContent = …, …)`; the `colors`, `tonalElevation` and `shadowElevation` arguments go.
 * The headline is 14sp medium, the supporting line 14sp muted, the overline 12sp muted; the row is at least
 * 48dp high, with 16dp at the sides and 12dp between the leading element, the text and the trailing element.
 * The slots take plain content; text inside them uses the row's text styles and colours.
 *
 * With [onClick] the whole row is one tap target with the button role, TalkBack reads its texts as one
 * element, and it shows pressed and hovered fills and the Monarch focus ring. [selected] draws the accent
 * subtle fill and announces the row as selected. A row without [onClick] is plain content; put its own
 * controls in [trailingContent].
 *
 * @param headlineContent the main line of the row.
 * @param modifier the modifier of the row.
 * @param overlineContent a small line above the headline.
 * @param supportingContent a line under the headline.
 * @param leadingContent an icon, avatar or checkbox before the text, centred in a box of at least 24dp;
 * a larger avatar (32 or 40dp) keeps its size.
 * @param trailingContent an element after the text, for example a badge, a value or a button. Text in it is 14sp muted,
 * so a time or a value never outweighs the headline.
 * @param onClick called when the user taps the row. `null` for a row that does nothing.
 * @param onClickLabel what TalkBack says a double tap does, such as "Details". `null`
 * keeps the platform's generic wording. Only with [onClick].
 * @param enabled whether the row can be tapped. A disabled row is dimmed and announced as disabled.
 * @param selected whether the row is the current choice.
 * @param divider draws a subtle 1dp line under the row, for a list whose rows are not separated otherwise.
 * @param interactionSource the source of the press, focus and hover interactions of the row.
 */
@Composable
@Suppress("LongParameterList", "LongMethod")
public fun MonarchListRow(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClickLabel: String? = null,
    divider: Boolean = false,
    interactionSource: MutableInteractionSource? = null,
) {
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val hovered by source.collectIsHoveredAsState()
    val fill = MonarchListRowTokens.container(selected, (pressed || hovered) && onClick != null && enabled, colors)
    val textColor = if (enabled) colors.textDefault else colors.textDisabled
    val secondary = if (enabled) colors.textMuted else colors.textDisabled
    val interactive = if (onClick != null) {
        Modifier
            .monarchFocusRing(MonarchTheme.shapes.none)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier
    }
    // The modifier, the tap and the state sit on the outer node, so a test tag or a caller's semantics reach the
    // row's one TalkBack element.
    Column(
        modifier
            .fillMaxWidth()
            .then(interactive)
            .semantics(mergeDescendants = true) { if (selected) this.selected = true },
    ) {
        Row(
            modifier = Modifier
                .background(fill)
                .heightIn(min = MonarchListRowTokens.MinHeight)
                .padding(MonarchListRowTokens.ContentPadding),
            horizontalArrangement = Arrangement.spacedBy(MonarchListRowTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(LocalContentColor provides textColor) {
                if (leadingContent != null) {
                    Box(
                        modifier = Modifier.defaultMinSize(
                            minWidth = MonarchListRowTokens.LeadingSize,
                            minHeight = MonarchListRowTokens.LeadingSize,
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        leadingContent()
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MonarchListRowTokens.LineGap),
                ) {
                    if (overlineContent != null) {
                        Line(MonarchListRowTokens.overline(typography), secondary, overlineContent)
                    }
                    Line(MonarchListRowTokens.headline(typography), textColor, headlineContent)
                    if (supportingContent != null) {
                        Line(MonarchListRowTokens.supporting(typography), secondary, supportingContent)
                    }
                }
                // A value or a time after the text is meta: 14sp muted, never larger than the headline.
                if (trailingContent != null) {
                    Line(MonarchListRowTokens.supporting(typography), secondary, trailingContent)
                }
            }
        }
        if (divider) MonarchDivider(variant = MonarchDividerVariant.Subtle)
    }
}

@Composable
private fun Line(style: TextStyle, color: Color, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides style, LocalContentColor provides color) { content() }
}

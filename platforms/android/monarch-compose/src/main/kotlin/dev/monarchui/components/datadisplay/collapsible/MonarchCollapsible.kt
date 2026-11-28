package dev.monarchui.components.datadisplay.collapsible

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * One section that shows and hides its content: "Show advanced settings",
 * "Details", an expandable row.
 *
 * Built by Monarch from a clickable header and `AnimatedVisibility`: Material 3
 * has no disclosure component. The header is one button with the state
 * "Expanded" or "Collapsed" and the TalkBack expand and collapse actions. The
 * content grows and shrinks, or appears at once when the user has turned
 * animations off. Hidden content leaves the composition, so TalkBack does not
 * reach it; hoist any state inside it that must survive closing.
 *
 * For a set of sections where one opens at a time, use an accordion.
 *
 * @param expanded whether the content is shown.
 * @param onExpandedChange called with the new state when the user activates the header.
 * @param label the visible label of the header. Name the content: "Advanced settings".
 * @param modifier the modifier of the collapsible.
 * @param enabled whether the user can activate the header. The content keeps its current state.
 * @param heading marks the header as a heading, so TalkBack users can move between sections by heading.
 * Use it when the collapsible titles a section of the screen.
 * @param trailingIcon an icon after the label, for example a chevron that points down as a `MonarchIcon`
 * with a `null` content description. It turns half a turn while expanded. Monarch bundles no icons, so
 * there is no default.
 * @param expandedLabel the state TalkBack reads while expanded. `null` uses the built-in string
 * `monarch_collapsible_expanded`.
 * @param collapsedLabel the state TalkBack reads while collapsed. `null` uses the built-in string
 * `monarch_collapsible_collapsed`.
 * @param interactionSource the source of the press, focus and hover interactions of the header.
 * @param content the content that is shown and hidden, laid out in a column.
 */
@Composable
public fun MonarchCollapsible(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    expandedLabel: String? = null,
    collapsedLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    heading: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    MonarchCollapsible(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        header = { Text(text = label, modifier = Modifier.weight(1f, fill = false)) },
        modifier = modifier,
        enabled = enabled,
        heading = heading,
        trailingIcon = trailingIcon,
        expandedLabel = expandedLabel,
        collapsedLabel = collapsedLabel,
        interactionSource = interactionSource,
        content = content,
    )
}

/**
 * A collapsible whose header is custom content: a title with a count or a
 * badge, two lines, an avatar. The header is still one button with the
 * expanded or collapsed state; do not put other buttons in it.
 *
 * @param expanded whether the content is shown.
 * @param onExpandedChange called with the new state when the user activates the header.
 * @param header the content of the header, in a row before the [trailingIcon]. Text in it uses the label style
 * and the header colour. Give the main text `Modifier.weight(1f, fill = false)` so it wraps.
 * @param modifier the modifier of the collapsible.
 * @param enabled whether the user can activate the header.
 * @param heading marks the header as a heading for TalkBack navigation.
 * @param trailingIcon an icon after the header, for example a chevron. It turns half a turn while expanded.
 * @param expandedLabel the state TalkBack reads while expanded. `null` uses `monarch_collapsible_expanded`.
 * @param collapsedLabel the state TalkBack reads while collapsed. `null` uses `monarch_collapsible_collapsed`.
 * @param interactionSource the source of the press, focus and hover interactions of the header.
 * @param content the content that is shown and hidden, laid out in a column.
 */
@Composable
public fun MonarchCollapsible(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    header: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    heading: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    expandedLabel: String? = null,
    collapsedLabel: String? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val state = if (expanded) {
        monarchString(expandedLabel, R.string.monarch_collapsible_expanded)
    } else {
        monarchString(collapsedLabel, R.string.monarch_collapsible_collapsed)
    }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) MonarchCollapsibleTokens.EXPANDED_ROTATION else 0f,
        animationSpec = motion.spec(),
        label = "collapsible icon",
    )
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .monarchFocusRing(MonarchShapeDefaults.Row)
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = { onExpandedChange(!expanded) },
                )
                .semantics {
                    if (heading) this.heading()
                    stateDescription = state
                    if (enabled) {
                        if (expanded) {
                            collapse {
                                onExpandedChange(false)
                                true
                            }
                        } else {
                            expand {
                                onExpandedChange(true)
                                true
                            }
                        }
                    }
                }
                .heightIn(min = MonarchCollapsibleTokens.HeaderMinHeight),
            horizontalArrangement = Arrangement.spacedBy(MonarchCollapsibleTokens.HeaderGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MonarchCollapsibleTokens.labelColor(enabled, colors),
                LocalTextStyle provides MonarchTheme.typography.label,
            ) {
                header()
            }
            if (trailingIcon != null) {
                val iconColor = MonarchCollapsibleTokens.iconColor(enabled, colors)
                CompositionLocalProvider(LocalContentColor provides iconColor) {
                    Box(modifier = Modifier.graphicsLayer { rotationZ = rotation }) { trailingIcon() }
                }
            }
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(motion.spec()) + fadeIn(motion.spec()),
            exit = shrinkVertically(motion.spec()) + fadeOut(motion.spec()),
        ) {
            Column(modifier = Modifier.padding(top = MonarchCollapsibleTokens.ContentTopPadding), content = content)
        }
    }
}

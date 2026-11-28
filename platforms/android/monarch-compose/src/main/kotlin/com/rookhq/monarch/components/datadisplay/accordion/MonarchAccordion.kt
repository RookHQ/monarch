package com.rookhq.monarch.components.datadisplay.accordion

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * Stacked sections whose content opens under each header: FAQs, settings
 * groups, details of a record.
 *
 * Built by Monarch: Material 3 has no accordion. Each header is a full-width
 * button that is also a heading, so TalkBack's heading navigation reaches it,
 * with the state "Expanded" or "Collapsed" and the expand and collapse
 * actions. The open content grows into place, or appears at once when the
 * user has turned animations off. Closed content leaves the composition, so
 * TalkBack does not reach it; hoist state inside it that must survive closing.
 * For one section on its own, use `MonarchCollapsible`.
 *
 * @param items the sections, in order.
 * @param expanded the ids of the open sections.
 * @param onExpandedChange called with the new set of open ids when the user activates a header.
 * @param modifier the modifier of the accordion.
 * @param multiple lets any number of sections stay open. Without it, opening one closes the others.
 * @param collapsible with one section open at a time, lets its header close it again. Ignored with
 * [multiple].
 * @param enabled whether the user can open and close sections. Each item can also be disabled on its own.
 * @param trailingIcon an icon at the end of each header, for example a chevron that points down as a
 * `MonarchIcon` with a `null` content description. It turns half a turn while open. Monarch bundles no
 * icons, so there is no default.
 * @param expandedLabel replaces the built-in state "Expanded" (`monarch_accordion_expanded`).
 * @param collapsedLabel replaces the built-in state "Collapsed" (`monarch_accordion_collapsed`).
 * @param headerTrailing content at the end of a header, before the [trailingIcon], such as a count or a
 * status badge. It is part of the header button: no buttons in it.
 * @param content the content of a section, laid out in a column, in the muted body style.
 */
@Composable
public fun MonarchAccordion(
    items: List<MonarchAccordionItem>,
    expanded: Set<String>,
    onExpandedChange: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
    multiple: Boolean = false,
    collapsible: Boolean = true,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null,
    expandedLabel: String? = null,
    collapsedLabel: String? = null,
    headerTrailing: (@Composable (item: MonarchAccordionItem) -> Unit)? = null,
    content: @Composable ColumnScope.(item: MonarchAccordionItem) -> Unit,
) {
    val colors = MonarchTheme.colors
    val motion = MonarchTheme.motion
    val open = monarchString(expandedLabel, R.string.monarch_accordion_expanded)
    val closed = monarchString(collapsedLabel, R.string.monarch_accordion_collapsed)
    Column(modifier = modifier) {
        MonarchDivider(variant = MonarchAccordionTokens.DividerVariant)
        items.forEach { item ->
            val isOpen = item.id in expanded
            Header(
                item = item,
                open = isOpen,
                enabled = enabled && item.enabled,
                canCollapse = MonarchAccordionTokens.canCollapse(multiple, collapsible),
                state = if (isOpen) open else closed,
                trailingIcon = trailingIcon,
                headerTrailing = headerTrailing,
                onToggle = {
                    val next = MonarchAccordionTokens.toggle(expanded, item.id, multiple, collapsible)
                    if (next != expanded) onExpandedChange(next)
                },
            )
            AnimatedVisibility(
                visible = isOpen,
                enter = expandVertically(motion.spec()) + fadeIn(motion.spec()),
                exit = shrinkVertically(motion.spec()) + fadeOut(motion.spec()),
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides MonarchAccordionTokens.contentColor(colors),
                    LocalTextStyle provides MonarchAccordionTokens.contentStyle(MonarchTheme.typography),
                ) {
                    Column(modifier = Modifier.padding(bottom = MonarchAccordionTokens.ContentBottomPadding)) {
                        content(item)
                    }
                }
            }
            MonarchDivider(variant = MonarchAccordionTokens.DividerVariant)
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun Header(
    item: MonarchAccordionItem,
    open: Boolean,
    enabled: Boolean,
    canCollapse: Boolean,
    state: String,
    trailingIcon: (@Composable () -> Unit)?,
    headerTrailing: (@Composable (item: MonarchAccordionItem) -> Unit)?,
    onToggle: () -> Unit,
) {
    val colors = MonarchTheme.colors
    val rotation by animateFloatAsState(
        targetValue = if (open) MonarchAccordionTokens.EXPANDED_ROTATION else 0f,
        animationSpec = MonarchTheme.motion.spec(),
        label = "accordion icon",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(),
                enabled = enabled,
                role = Role.Button,
                onClick = onToggle,
            )
            .semantics {
                heading()
                stateDescription = state
                if (enabled) {
                    if (open && canCollapse) {
                        collapse {
                            onToggle()
                            true
                        }
                    } else if (!open) {
                        expand {
                            onToggle()
                            true
                        }
                    }
                }
            }
            .heightIn(min = MonarchAccordionTokens.HeaderMinHeight)
            .padding(vertical = MonarchAccordionTokens.HeaderPadding),
        horizontalArrangement = Arrangement.spacedBy(MonarchAccordionTokens.HeaderGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MonarchAccordionTokens.titleStyle(MonarchTheme.typography),
                color = MonarchAccordionTokens.titleColor(enabled, colors),
            )
            item.subtitle?.let {
                Text(
                    text = it,
                    style = MonarchTheme.typography.caption,
                    color = MonarchAccordionTokens.subtitleColor(enabled, colors),
                )
            }
        }
        if (headerTrailing != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchAccordionTokens.iconColor(enabled, colors)) {
                headerTrailing(item)
            }
        }
        if (trailingIcon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchAccordionTokens.iconColor(enabled, colors)) {
                Box(modifier = Modifier.graphicsLayer { rotationZ = rotation }) { trailingIcon() }
            }
        }
    }
}

package dev.monarchui.components.layout.sectioncard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.accessibility.monarchFocusRing
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingState
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingStateVariant
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.components.layout.sectionheader.MonarchSectionHeader
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A titled card for one section of a screen or dashboard: a header with
 * actions, the body, an optional footer, and built-in loading, empty and
 * error states. Optionally collapsible.
 *
 * Built from `MonarchCard` (with its tone border), a
 * `MonarchSectionHeader`, and `MonarchLoadingState`, `MonarchEmptyState` and
 * `MonarchErrorState` for the states. The header is one TalkBack element with
 * the heading; its actions stay separate. With [onExpandedChange] the header
 * is a button that shows and hides the body and the footer, with the state
 * "Expanded" or "Collapsed" and the TalkBack expand and collapse actions; the
 * body grows and shrinks, or appears at once when animations are off.
 *
 * A [tone] other than neutral colours the border and the icon and is spoken
 * with the title; name the zone in the title as well ("Danger zone").
 *
 * @param title the section title, already localised. A TalkBack heading.
 * @param modifier the modifier of the card.
 * @param description a sentence under the title that explains the section.
 * @param meta a short secondary line: a count, a date range, "Updated 5 min ago".
 * @param tone the tone of the border and the icon. `Danger` for a destructive zone.
 * @param state what the body shows: the content, skeleton rows, the empty state or the error state.
 * @param busy the content is refreshing in place: a progress ring after the title while the content stays.
 * @param retrying the retry of the error state is running.
 * @param flush the body has no padding, for a list or a table that runs to the edges.
 * @param expanded whether the body and the footer show. Only used with [onExpandedChange].
 * @param onExpandedChange makes the header a button that shows and hides the body, called with the new state.
 * Without it the card is not collapsible.
 * @param emptyMessage the message of the empty state.
 * @param errorMessage the message of the error state.
 * @param onRetry shows a retry button in the error state.
 * @param icon a decorative icon before the title, tinted in the tone.
 * @param toggleIcon an icon at the end of a collapsible header, for example a chevron that points down as a
 * `MonarchIcon` with a `null` content description. It turns half a turn while expanded. Monarch bundles no
 * icons, so there is no default.
 * @param actions buttons, a menu or a "View all" link at the end of the header.
 * @param footer the footer bar under a line: secondary actions, a summary, a pager.
 * @param labels replacements for the built-in strings.
 * @param content the body, shown in the ready state.
 */
@Composable
public fun MonarchSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    meta: String? = null,
    tone: MonarchTone = MonarchTone.Neutral,
    state: MonarchSectionCardState = MonarchSectionCardState.Ready,
    busy: Boolean = false,
    retrying: Boolean = false,
    flush: Boolean = false,
    expanded: Boolean = true,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    emptyMessage: String? = null,
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    toggleIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    labels: MonarchSectionCardLabels = MonarchSectionCardLabels(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = MonarchSectionCardTokens
    val motion = MonarchTheme.motion
    val collapsible = onExpandedChange != null
    val open = !collapsible || expanded
    val loadingLabel = monarchString(labels.loading, R.string.monarch_sectioncard_loading)
    MonarchCard(
        modifier = modifier,
        contentPadding = PaddingValues(),
        fillWidth = false,
        tone = tone,
    ) {
        Column {
            Header(
                title = title,
                description = description,
                meta = meta,
                tone = tone,
                busy = busy,
                loading = busy || state == MonarchSectionCardState.Loading,
                loadingLabel = loadingLabel,
                expanded = open,
                onExpandedChange = onExpandedChange,
                icon = icon,
                toggleIcon = toggleIcon,
                actions = actions,
                labels = labels,
            )
            AnimatedVisibility(
                visible = open,
                enter = expandVertically(motion.spec()) + fadeIn(motion.spec()),
                exit = shrinkVertically(motion.spec()) + fadeOut(motion.spec()),
            ) {
                Column {
                    Column(modifier = Modifier.fillMaxWidth().padding(tokens.bodyPadding(flush))) {
                        Body(state, loadingLabel, emptyMessage, errorMessage, retrying, onRetry, labels, content)
                    }
                    if (footer != null) Footer(footer)
                }
            }
        }
    }
}

@Composable
private fun Header(
    title: String,
    description: String?,
    meta: String?,
    tone: MonarchTone,
    busy: Boolean,
    loading: Boolean,
    loadingLabel: String,
    expanded: Boolean,
    onExpandedChange: ((Boolean) -> Unit)?,
    icon: (@Composable () -> Unit)?,
    toggleIcon: (@Composable () -> Unit)?,
    actions: (@Composable RowScope.() -> Unit)?,
    labels: MonarchSectionCardLabels,
) {
    val tokens = MonarchSectionCardTokens
    val toneName = tokens.toneName(tone)?.let { labels.tone ?: stringResource(it) }
    val openState = when {
        onExpandedChange == null -> null
        expanded -> monarchString(labels.expanded, R.string.monarch_sectioncard_expanded)
        else -> monarchString(labels.collapsed, R.string.monarch_sectioncard_collapsed)
    }
    val spoken = tokens.spokenState(
        listOf(openState, toneName, loadingLabel.takeIf { loading }),
        stringResource(R.string.monarch_sectioncard_state_separator),
    )
    val rotation by animateFloatAsState(
        targetValue = if (expanded) tokens.EXPANDED_ROTATION else 0f,
        animationSpec = MonarchTheme.motion.spec(),
        label = "section card toggle icon",
    )
    val interaction = if (onExpandedChange != null) {
        Modifier
            .monarchFocusRing(MonarchShapeDefaults.Row)
            .clickable(
                interactionSource = null,
                indication = ripple(),
                role = Role.Button,
                onClick = { onExpandedChange(!expanded) },
            )
            .semantics {
                spoken?.let { stateDescription = it }
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
            .heightIn(min = tokens.ToggleMinHeight)
    } else {
        Modifier.semantics(mergeDescendants = true) { spoken?.let { stateDescription = it } }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(interaction)
            .padding(start = tokens.Inset, end = tokens.Inset, top = tokens.Inset, bottom = tokens.HeaderGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.IconGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.iconColor(tone, MonarchTheme.colors)) {
                Box(Modifier.align(Alignment.Top).clearAndSetSemantics { }) { icon() }
            }
        }
        MonarchSectionHeader(
            title = title,
            modifier = Modifier.weight(1f),
            description = description,
            meta = meta,
            busy = busy,
            busyLabel = loadingLabel,
            tone = tone.takeIf { it == MonarchTone.Danger },
            actions = actions,
        )
        if (onExpandedChange != null && toggleIcon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                Box(Modifier.rotate(rotation).clearAndSetSemantics { }) { toggleIcon() }
            }
        }
    }
}

@Composable
private fun Body(
    state: MonarchSectionCardState,
    loadingLabel: String,
    emptyMessage: String?,
    errorMessage: String?,
    retrying: Boolean,
    onRetry: (() -> Unit)?,
    labels: MonarchSectionCardLabels,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (state) {
        MonarchSectionCardState.Ready -> Column(content = content)
        MonarchSectionCardState.Loading -> MonarchLoadingState(
            label = loadingLabel,
            variant = MonarchLoadingStateVariant.Rows,
            rows = MonarchSectionCardTokens.LOADING_ROWS,
        )
        MonarchSectionCardState.Empty -> MonarchEmptyState(
            title = monarchString(labels.emptyTitle, R.string.monarch_sectioncard_empty_title),
            size = MonarchEmptyStateSize.Sm,
            message = emptyMessage,
        )
        MonarchSectionCardState.Error -> MonarchErrorState(
            title = monarchString(labels.errorTitle, R.string.monarch_sectioncard_error_title),
            size = MonarchEmptyStateSize.Sm,
            retrying = retrying,
            message = errorMessage,
            onRetry = onRetry,
        )
    }
}

@Composable
private fun Footer(footer: @Composable RowScope.() -> Unit) {
    val tokens = MonarchSectionCardTokens
    MonarchDivider(variant = MonarchDividerVariant.Subtle)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.Inset, vertical = tokens.FooterGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.IconGap),
        verticalAlignment = Alignment.CenterVertically,
        content = footer,
    )
}

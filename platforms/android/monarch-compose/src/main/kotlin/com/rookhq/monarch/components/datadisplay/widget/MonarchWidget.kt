package com.rookhq.monarch.components.datadisplay.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeleton
import com.rookhq.monarch.components.feedback.skeleton.MonarchSkeletonShape
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme

/**
 * The frame of a dashboard widget: a title with a period, header actions, an
 * overflow menu, View all, and its own loading, empty and failed states.
 *
 * Built from `MonarchCard`, `MonarchButton`, a `MonarchMenu` and
 * `MonarchIconButton`, `MonarchSkeleton`, `MonarchEmptyState`,
 * `MonarchInlineErrorRetry` and `MonarchInlineNotice`. The title is a TalkBack
 * heading. At large font sizes the header controls move under the title. The
 * updated line is relative to the current time and refreshes every minute.
 *
 * @param title the widget's title.
 * @param modifier the modifier of the widget.
 * @param description a caption line under the title.
 * @param period the period the figures cover, for example "Last 30 days", muted beside the title.
 * @param variant a card, or no surface for a widget inside another surface.
 * @param state ready, loading, empty or failed.
 * @param retrying with a failed state and [onRetry]: the Try again button is busy.
 * @param updatedAt when the data was last updated, in epoch milliseconds, shown as "Updated 5 minutes ago".
 * Hidden while loading.
 * @param now the current time in epoch milliseconds for the updated line. `null` uses the clock and refreshes
 * every minute.
 * @param footer a note under the body. Hidden while loading.
 * @param icon a decorative icon before the title, for example a `MonarchIcon` with a `null` content description.
 * @param menuIcon the icon of the menu button. Without it the button shows a horizontal ellipsis.
 * @param onRetry adds Try again to the failed state.
 * @param onViewAll adds a View all button to the header; the app navigates.
 * @param labels replacements for the built-in strings.
 * @param actions controls in the header before View all, such as a small segmented button row.
 * @param menu the overflow menu's items, usually `MonarchMenuItem`s; call `close` from an item to close it.
 * @param content the body, shown in the ready state.
 */
@Composable
public fun MonarchWidget(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    period: String? = null,
    variant: MonarchWidgetVariant = MonarchWidgetVariant.Card,
    state: MonarchWidgetState = MonarchWidgetState.Ready,
    retrying: Boolean = false,
    updatedAt: Long? = null,
    now: Long? = null,
    footer: String? = null,
    icon: (@Composable () -> Unit)? = null,
    menuIcon: (@Composable () -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onViewAll: (() -> Unit)? = null,
    labels: MonarchWidgetLabels = MonarchWidgetLabels(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val body: @Composable ColumnScope.() -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchWidgetTokens.SectionGap)) {
            Header(title, description, period, icon, menuIcon, onViewAll, labels, actions, menu)
            Body(title, state, retrying, onRetry, labels, content)
            if (MonarchWidgetTokens.showsFooter(state, updatedAt, footer)) {
                Footer(updatedAt, now, footer, labels)
            }
        }
    }
    when (variant) {
        MonarchWidgetVariant.Card ->
            MonarchCard(modifier = modifier, contentPadding = MonarchWidgetTokens.ContentPadding, content = body)
        MonarchWidgetVariant.Plain -> Column(modifier = modifier, content = body)
    }
}

@Composable
private fun Header(
    title: String,
    description: String?,
    period: String?,
    icon: (@Composable () -> Unit)?,
    menuIcon: (@Composable () -> Unit)?,
    onViewAll: (() -> Unit)?,
    labels: MonarchWidgetLabels,
    actions: (@Composable RowScope.() -> Unit)?,
    menu: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
) {
    val tokens = MonarchWidgetTokens
    val hasControls = actions != null || onViewAll != null || menu != null
    val controls: @Composable () -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions?.invoke(this)
            if (onViewAll != null) ViewAll(title, onViewAll, labels)
            if (menu != null) Menu(title, menuIcon, labels, menu)
        }
    }
    if (tokens.stackedHeader(LocalDensity.current.fontScale)) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
            TitleBlock(title, description, period, icon, Modifier.fillMaxWidth())
            if (hasControls) controls()
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TitleBlock(title, description, period, icon, Modifier.weight(1f))
            if (hasControls) controls()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TitleBlock(
    title: String,
    description: String?,
    period: String?,
    icon: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val tokens = MonarchWidgetTokens
    val colors = MonarchTheme.colors
    val typography = MonarchTheme.typography
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.mutedColor(colors)) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(tokens.TitleGap)) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = typography.subheading,
                    color = tokens.titleColor(colors),
                )
                if (!period.isNullOrEmpty()) {
                    Text(text = period, style = typography.caption, color = tokens.mutedColor(colors))
                }
            }
            if (!description.isNullOrEmpty()) {
                Text(text = description, style = typography.caption, color = tokens.mutedColor(colors))
            }
        }
    }
}

@Composable
private fun ViewAll(title: String, onViewAll: () -> Unit, labels: MonarchWidgetLabels) {
    val name = labels.viewAllLabel?.invoke(title) ?: stringResource(R.string.monarch_widget_view_all_label, title)
    MonarchButton(
        text = monarchString(labels.viewAll, R.string.monarch_widget_view_all),
        onClick = onViewAll,
        modifier = Modifier.semantics { contentDescription = name },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        trailingIcon = MonarchDefaultIcons.ChevronRight,
    )
}

@Composable
private fun Menu(
    title: String,
    menuIcon: (@Composable () -> Unit)?,
    labels: MonarchWidgetLabels,
    items: @Composable ColumnScope.(close: () -> Unit) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val name = labels.menu?.invoke(title) ?: stringResource(R.string.monarch_widget_menu, title)
    Box {
        MonarchIconButton(
            label = name,
            onClick = { expanded = true },
            size = MonarchButtonSize.Sm,
            icon = menuIcon ?: MonarchDefaultIcons.More,
        )
        MonarchMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items { expanded = false }
        }
    }
}

@Composable
private fun Body(
    title: String,
    state: MonarchWidgetState,
    retrying: Boolean,
    onRetry: (() -> Unit)?,
    labels: MonarchWidgetLabels,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (state) {
        MonarchWidgetState.Ready -> Column(content = content)
        MonarchWidgetState.Loading -> Loading(title, labels)
        is MonarchWidgetState.Empty -> MonarchEmptyState(
            title = state.message ?: monarchString(labels.empty, R.string.monarch_widget_empty),
            modifier = Modifier.fillMaxWidth(),
            size = MonarchEmptyStateSize.Sm,
        )
        is MonarchWidgetState.Failed -> {
            val message = state.message ?: monarchString(labels.error, R.string.monarch_widget_error)
            if (onRetry != null) {
                MonarchInlineErrorRetry(message = message, onRetry = onRetry, retrying = retrying)
            } else {
                MonarchInlineNotice(
                    message = message,
                    tone = MonarchTone.Danger,
                    announcement = MonarchBannerAnnouncement.Polite,
                )
            }
        }
    }
}

@Composable
private fun Loading(title: String, labels: MonarchWidgetLabels) {
    val name = labels.loading?.invoke(title) ?: stringResource(R.string.monarch_widget_loading, title)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = name },
        verticalArrangement = Arrangement.spacedBy(MonarchWidgetTokens.SkeletonGap),
    ) {
        MonarchWidgetTokens.SkeletonWidths.forEach { width ->
            MonarchSkeleton(modifier = Modifier.fillMaxWidth(width), shape = MonarchSkeletonShape.Text)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Footer(updatedAt: Long?, now: Long?, footer: String?, labels: MonarchWidgetLabels) {
    val tokens = MonarchWidgetTokens
    val colors = MonarchTheme.colors
    val style = MonarchTheme.typography.caption
    val clock = widgetMinuteClock(running = updatedAt != null && now == null, times = listOf(updatedAt))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap)) {
        if (!footer.isNullOrEmpty()) Text(text = footer, style = style, color = tokens.mutedColor(colors))
        if (updatedAt != null) {
            val relative = tokens.relativeTime(updatedAt, now ?: clock)
            Text(
                text = labels.updated?.invoke(relative) ?: stringResource(R.string.monarch_widget_updated, relative),
                style = style,
                color = tokens.mutedColor(colors),
            )
        }
    }
}

/** The current time, redrawn as often as the freshest of [times] needs while [running], so relative times stay true. */
@Composable
private fun widgetMinuteClock(running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, running = running)

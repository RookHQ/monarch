package dev.monarchui.components.identity.securityalertcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme

/**
 * Recent security alerts for an account, each with a severity badge, a
 * relative time, an unread mark and optional follow-up, mark-as-read and
 * dismiss actions, plus loading, empty and error states.
 *
 * Built on `MonarchCard`, `MonarchBadge`, `MonarchButton`, `MonarchProgress`,
 * `MonarchEmptyState` and `MonarchErrorState`. The title is a heading. Each
 * alert's text is one TalkBack element read as its title and description,
 * with the state "Unread", the severity and the time, so nothing depends on
 * colour. The actions are separate buttons. A dismissal is announced through
 * a polite live region. Times are relative ("5 minutes ago") and follow the
 * clock every minute unless [now] is given.
 *
 * @param alerts the alerts, newest first.
 * @param modifier the modifier of the card.
 * @param state ready, loading or failed. A ready, empty list shows the empty state.
 * @param retrying whether a retry is in progress, in the failed state.
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock.
 * @param onRetry retries loading; shows "Try again" in the failed state.
 * @param onAction runs the follow-up of an alert that has an `actionLabel`.
 * @param onMarkRead marks one alert read. Shows "Mark as read" on unread alerts.
 * @param onMarkAllRead marks every alert read. Shows "Mark all as read" while any are unread.
 * @param onDismiss dismisses an alert; remove it from [alerts]. Shows a dismiss button on every alert.
 * @param severityIcon an icon before each alert, tinted with the severity's strong colour and hidden from
 * TalkBack. Monarch bundles no icons; the severity is always written in the badge.
 * @param dismissIcon the close mark of the dismiss buttons. Without it they show "Dismiss" as text.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchSecurityAlertCard(
    alerts: List<MonarchSecurityAlert>,
    modifier: Modifier = Modifier,
    state: MonarchSecurityAlertCardState = MonarchSecurityAlertCardState.Ready,
    retrying: Boolean = false,
    now: Long? = null,
    onRetry: (() -> Unit)? = null,
    onAction: ((MonarchSecurityAlert) -> Unit)? = null,
    onMarkRead: ((String) -> Unit)? = null,
    onMarkAllRead: (() -> Unit)? = null,
    onDismiss: ((String) -> Unit)? = null,
    severityIcon: (@Composable (MonarchSecurityAlertSeverity) -> Unit)? = MonarchDefaultIcons.securitySeverity,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchSecurityAlertCardLabels = MonarchSecurityAlertCardLabels(),
) {
    val tokens = MonarchSecurityAlertCardTokens
    val ready = state == MonarchSecurityAlertCardState.Ready
    val unread = if (ready) tokens.unreadCount(alerts) else 0
    var dismissedText by remember { mutableStateOf("") }
    val dismissedFormat = stringResource(R.string.monarch_securityalertcard_dismissed)
    val clock = alertClock(now, ready && alerts.any { it.dateMillis != null }, alerts.map { it.dateMillis })
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            Header(unread, onMarkAllRead, labels)
            when {
                state == MonarchSecurityAlertCardState.Loading -> Loading(labels)
                state == MonarchSecurityAlertCardState.Failed -> MonarchErrorState(
                    title = monarchString(labels.errorTitle, R.string.monarch_securityalertcard_error_title),
                    size = MonarchEmptyStateSize.Sm,
                    retrying = retrying,
                    onRetry = onRetry,
                )
                alerts.isEmpty() -> MonarchEmptyState(
                    title = monarchString(labels.emptyTitle, R.string.monarch_securityalertcard_empty_title),
                    size = MonarchEmptyStateSize.Sm,
                    message = monarchString(
                        labels.emptyDescription,
                        R.string.monarch_securityalertcard_empty_description,
                    ),
                )
                else -> AlertList(
                    alerts = alerts,
                    now = clock,
                    onAction = onAction,
                    onMarkRead = onMarkRead,
                    onDismiss = onDismiss?.let { dismiss ->
                        { alert: MonarchSecurityAlert ->
                            dismissedText = labels.dismissed?.invoke(alert.title) ?: dismissedFormat.format(alert.title)
                            dismiss(alert.id)
                        }
                    },
                    severityIcon = severityIcon,
                    dismissIcon = dismissIcon,
                    labels = labels,
                )
            }
        }
        // Always present, so a dismissal is announced once as a change of this live region.
        MonarchLiveStatus(text = dismissedText.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** A progress indicator and "Loading alerts", read as one element. */
@Composable
private fun Loading(labels: MonarchSecurityAlertCardLabels) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchProgress(modifier = Modifier.clearAndSetSemantics { }, variant = MonarchProgressVariant.Circular)
        Text(
            text = monarchString(labels.loading, R.string.monarch_securityalertcard_loading),
            style = MonarchTheme.typography.body,
            color = MonarchTheme.colors.textMuted,
        )
    }
}

@Composable
private fun Header(unread: Int, onMarkAllRead: (() -> Unit)?, labels: MonarchSecurityAlertCardLabels) {
    val colors = MonarchTheme.colors
    // As the web: Mark all as read sits at the end of the title row.
    Row(horizontalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.RowGap)) {
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.LineGap),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.RowGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clearAndSetSemantics { }
                        .size(MonarchSecurityAlertCardTokens.BellTile)
                        .background(colors.bgMuted, MonarchShapeDefaults.Control),
                    contentAlignment = Alignment.Center,
                ) { MonarchDefaultIcons.Bell() }
                Text(
                    text = monarchString(labels.title, R.string.monarch_securityalertcard_title),
                    modifier = Modifier.weight(1f, fill = false).semantics { heading() },
                    style = MonarchTheme.typography.subheading,
                    color = colors.textDefault,
                )
            }
            Text(
                text = monarchString(labels.description, R.string.monarch_securityalertcard_description),
                style = MonarchTheme.typography.caption,
                color = colors.textMuted,
            )
            // As the web: the count is a plain line under the description.
            if (unread > 0) {
                Text(
                    text = labels.unreadCount?.invoke(unread)
                        ?: pluralStringResource(R.plurals.monarch_securityalertcard_unread_count, unread, unread),
                    style = MonarchTheme.typography.caption,
                    color = colors.textDefault,
                )
            }
        }
        if (unread > 0 && onMarkAllRead != null) {
            MonarchButton(
                text = monarchString(labels.markAllRead, R.string.monarch_securityalertcard_mark_all_read),
                onClick = onMarkAllRead,
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun AlertList(
    alerts: List<MonarchSecurityAlert>,
    now: Long,
    onAction: ((MonarchSecurityAlert) -> Unit)?,
    onMarkRead: ((String) -> Unit)?,
    onDismiss: ((MonarchSecurityAlert) -> Unit)?,
    severityIcon: (@Composable (MonarchSecurityAlertSeverity) -> Unit)?,
    dismissIcon: (@Composable () -> Unit)?,
    labels: MonarchSecurityAlertCardLabels,
) {
    val listLabel = monarchString(labels.listLabel, R.string.monarch_securityalertcard_list_label)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listLabel
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.SectionGap),
    ) {
        alerts.forEach { alert ->
            AlertRow(alert, now, onAction, onMarkRead, onDismiss, severityIcon, dismissIcon, labels)
        }
    }
}

@Composable
private fun AlertRow(
    alert: MonarchSecurityAlert,
    now: Long,
    onAction: ((MonarchSecurityAlert) -> Unit)?,
    onMarkRead: ((String) -> Unit)?,
    onDismiss: ((MonarchSecurityAlert) -> Unit)?,
    severityIcon: (@Composable (MonarchSecurityAlertSeverity) -> Unit)?,
    dismissIcon: (@Composable () -> Unit)?,
    labels: MonarchSecurityAlertCardLabels,
) {
    val tokens = MonarchSecurityAlertCardTokens
    val colors = MonarchTheme.colors
    // As the web: a tinted tile per alert, the dismiss cross at the top end of it.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSubtle, MonarchTheme.shapes.md)
            .padding(tokens.RowGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
    ) {
        if (severityIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.iconColor(alert.severity, colors)) {
                Box(
                    Modifier
                        .clearAndSetSemantics { }
                        .size(tokens.IconTile)
                        .background(tokens.iconTile(alert.severity, colors), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { severityIcon(alert.severity) }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            AlertText(alert, now, labels)
            AlertActions(alert, onAction, onMarkRead, labels)
        }
        if (onDismiss != null) DismissButton(alert, { onDismiss(alert) }, dismissIcon, labels)
    }
}

@Composable
private fun AlertText(alert: MonarchSecurityAlert, now: Long, labels: MonarchSecurityAlertCardLabels) {
    val tokens = MonarchSecurityAlertCardTokens
    val colors = MonarchTheme.colors
    val severity = labels.severity?.invoke(alert.severity) ?: stringResource(tokens.severityName(alert.severity))
    val unread = if (alert.read) null else monarchString(labels.unread, R.string.monarch_securityalertcard_unread)
    val time = alert.dateMillis?.let { tokens.relativeTime(it, now) }
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = tokens.spokenName(alert.title, alert.description)
            stateDescription = tokens.spokenState(unread, severity, time)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.RowGap), verticalAlignment = Alignment.Top) {
            if (!alert.read) {
                Box(
                    Modifier
                        .padding(top = tokens.UnreadDotTop)
                        .size(tokens.UnreadDotSize)
                        .background(tokens.unreadColor(alert.read, colors), CircleShape),
                )
            }
            Text(
                text = alert.title,
                modifier = Modifier.weight(1f),
                style = if (alert.read) MonarchTheme.typography.body else MonarchTheme.typography.bodyStrong,
                color = colors.textDefault,
            )
        }
        alert.description?.let {
            Text(text = it, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchBadge(
                text = severity,
                tone = tokens.tone(alert.severity),
                variant = tokens.badgeVariant(alert.severity),
            )
            if (time != null) Text(text = time, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun AlertActions(
    alert: MonarchSecurityAlert,
    onAction: ((MonarchSecurityAlert) -> Unit)?,
    onMarkRead: ((String) -> Unit)?,
    labels: MonarchSecurityAlertCardLabels,
) {
    val action = alert.actionLabel?.takeIf { onAction != null }
    val markRead = onMarkRead?.takeIf { !alert.read }
    if (action == null && markRead == null) return
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchSecurityAlertCardTokens.RowGap),
    ) {
        if (action != null && onAction != null) {
            MonarchButton(text = action, onClick = { onAction(alert) }, size = MonarchButtonSize.Sm)
        }
        if (markRead != null) {
            MonarchButton(
                text = monarchString(labels.markRead, R.string.monarch_securityalertcard_mark_read),
                onClick = { markRead(alert.id) },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun DismissButton(
    alert: MonarchSecurityAlert,
    onClick: () -> Unit,
    icon: (@Composable () -> Unit)?,
    labels: MonarchSecurityAlertCardLabels,
) {
    val name = labels.dismiss?.invoke(alert.title)
        ?: stringResource(R.string.monarch_securityalertcard_dismiss, alert.title)
    if (icon != null) {
        MonarchIconButton(label = name, onClick = onClick, size = MonarchButtonSize.Sm, icon = icon)
    } else {
        MonarchButton(
            text = monarchString(labels.dismissTitle, R.string.monarch_securityalertcard_dismiss_title),
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = name },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun alertClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)

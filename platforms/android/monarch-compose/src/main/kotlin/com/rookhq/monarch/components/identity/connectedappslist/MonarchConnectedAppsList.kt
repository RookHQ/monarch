package com.rookhq.monarch.components.identity.connectedappslist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFavicon
import com.rookhq.monarch.components.datadisplay.favicon.MonarchFaviconSize
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.layout.divider.MonarchDividerVariant
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeader
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * The apps a person signed in to with their account: logo, name, domain,
 * Verified and Suspended badges, when it was connected and last used, the
 * workspace and what it can access, with Remove access behind a
 * confirmation.
 *
 * Built by Monarch in a `MonarchCard`: a `MonarchSectionHeader`, one row per
 * app with a `MonarchFavicon`, `MonarchBadge`s and a danger `MonarchButton`,
 * and the Material 3 `AlertDialog` (the confirm dialog) before removing.
 * Loading, empty and failed states use `MonarchLoadingState`,
 * `MonarchEmptyState` and `MonarchErrorState`. Each app's text is one
 * TalkBack element read as its name with the badges, domain, times and
 * workspace as its state; removal and failures are announced through a
 * polite live region. The rows are not lazy: the list is short and usually
 * sits inside a scrolling settings screen. Times are relative ("3 days ago")
 * and follow the clock every minute unless [now] is given.
 *
 * @param apps the connected apps, in the order shown.
 * @param modifier the modifier of the card.
 * @param state ready, loading or error. A ready, empty list shows the empty state.
 * @param title replaces the built-in "Connected apps". An empty string hides the header.
 * @param description replaces the line under the title.
 * @param retrying whether a retry is in progress, in the error state.
 * @param confirmRevoke asks before removing access. Turn it off when [onRevoke] opens the app's own confirmation.
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock.
 * @param onRetry retries loading; shows "Try again" in the error state.
 * @param onRevoke removes an app's access; remove the app from [apps] when it returns. Shows Remove access on every
 * row; the row's button is busy while it runs, and a thrown exception's message (or a built-in sentence) is shown
 * under the row and announced.
 * @param labels replacements for the other built-in strings.
 */
@Composable
public fun MonarchConnectedAppsList(
    apps: List<MonarchConnectedApp>,
    modifier: Modifier = Modifier,
    state: MonarchConnectedAppsListState = MonarchConnectedAppsListState.Ready,
    title: String? = null,
    description: String? = null,
    retrying: Boolean = false,
    confirmRevoke: Boolean = true,
    now: Long? = null,
    onRetry: (() -> Unit)? = null,
    onRevoke: (suspend (MonarchConnectedApp) -> Unit)? = null,
    labels: MonarchConnectedAppsListLabels = MonarchConnectedAppsListLabels(),
) {
    val tokens = MonarchConnectedAppsListTokens
    val ready = state == MonarchConnectedAppsListState.Ready
    val revocation = rememberRevocation(onRevoke, labels)
    var pending by remember { mutableStateOf<MonarchConnectedApp?>(null) }
    val clock = connectedAppsClock(
        now,
        ready && apps.isNotEmpty(),
        apps.flatMap { listOf(it.connectedAtMillis, it.lastUsedAtMillis) },
    )
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            val heading = title ?: stringResource(R.string.monarch_connectedappslist_title)
            if (heading.isNotEmpty()) {
                MonarchSectionHeader(
                    title = heading,
                    description = description ?: stringResource(R.string.monarch_connectedappslist_description),
                )
            }
            when {
                state == MonarchConnectedAppsListState.Loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_connectedappslist_loading),
                    variant = MonarchLoadingStateVariant.Rows,
                    rows = 2,
                )
                state == MonarchConnectedAppsListState.Error -> MonarchErrorState(
                    title = monarchString(labels.errorTitle, R.string.monarch_connectedappslist_error_title),
                    size = MonarchEmptyStateSize.Sm,
                    retrying = retrying,
                    onRetry = onRetry,
                )
                apps.isEmpty() -> MonarchEmptyState(
                    title = monarchString(labels.emptyTitle, R.string.monarch_connectedappslist_empty_title),
                    size = MonarchEmptyStateSize.Sm,
                    message = monarchString(labels.emptyMessage, R.string.monarch_connectedappslist_empty_message),
                )
                else -> AppList(
                    apps = apps,
                    now = clock,
                    revocation = revocation,
                    onRevoke = onRevoke?.let {
                        { app: MonarchConnectedApp -> if (confirmRevoke) pending = app else revocation.run(app) }
                    },
                    labels = labels,
                )
            }
        }
        // Always present, so a removal or a failure is announced once as a change of this live region.
        MonarchLiveStatus(text = revocation.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    pending?.let { app ->
        ConfirmDialog(app, onConfirm = {
            pending = null
            revocation.run(app)
        }, onDismiss = { pending = null }, labels = labels)
    }
}

@Composable
private fun AppList(
    apps: List<MonarchConnectedApp>,
    now: Long,
    revocation: ConnectedAppsRevocation,
    onRevoke: ((MonarchConnectedApp) -> Unit)?,
    labels: MonarchConnectedAppsListLabels,
) {
    val listLabel = monarchString(labels.listLabel, R.string.monarch_connectedappslist_list_label)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listLabel
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchConnectedAppsListTokens.SectionGap),
    ) {
        apps.forEachIndexed { index, app ->
            if (index > 0) MonarchDivider(variant = MonarchDividerVariant.Subtle)
            AppRow(
                app = app,
                now = now,
                running = app.id in revocation.running,
                failure = revocation.failures[app.id],
                onRevoke = onRevoke,
                labels = labels,
            )
        }
    }
}

@Composable
private fun AppRow(
    app: MonarchConnectedApp,
    now: Long,
    running: Boolean,
    failure: String?,
    onRevoke: ((MonarchConnectedApp) -> Unit)?,
    labels: MonarchConnectedAppsListLabels,
) {
    val tokens = MonarchConnectedAppsListTokens
    val colors = MonarchTheme.colors
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.RowGap), verticalAlignment = Alignment.Top) {
        MonarchFavicon(image = app.logo, name = app.name, size = MonarchFaviconSize.Md)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.LineGap)) {
            AppText(app, now, labels)
            if (app.scopes.isNotEmpty()) {
                val scopesName = labels.scopesLabel?.invoke(app.name)
                    ?: stringResource(R.string.monarch_connectedappslist_scopes_label, app.name)
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = scopesName
                        isTraversalGroup = true
                    },
                ) {
                    app.scopes.forEach { scope ->
                        Text(text = "• $scope", style = MonarchTheme.typography.caption, color = colors.textDefault)
                    }
                }
            }
            if (onRevoke != null) {
                val revokeName = labels.revokeLabel?.invoke(app.name)
                    ?: stringResource(R.string.monarch_connectedappslist_revoke_label, app.name)
                MonarchButton(
                    text = monarchString(labels.revoke, R.string.monarch_connectedappslist_revoke),
                    onClick = { onRevoke(app) },
                    modifier = Modifier.semantics { contentDescription = revokeName },
                    variant = MonarchButtonVariant.DangerSubtle,
                    size = MonarchButtonSize.Sm,
                    loading = running,
                )
            }
            if (failure != null) {
                Text(text = failure, style = MonarchTheme.typography.caption, color = tokens.failureColor(colors))
            }
        }
    }
}

/** The name, badges, domain, times and workspace of an app, read as one element. */
@Composable
private fun AppText(app: MonarchConnectedApp, now: Long, labels: MonarchConnectedAppsListLabels) {
    val tokens = MonarchConnectedAppsListTokens
    val colors = MonarchTheme.colors
    val verified = monarchString(labels.verified, R.string.monarch_connectedappslist_verified)
    val suspended = monarchString(labels.suspended, R.string.monarch_connectedappslist_suspended)
    val badges = listOfNotNull(verified.takeIf { app.verified }, suspended.takeIf { app.suspended })
    val times = tokens.timesLine(
        tokens.labelled(
            monarchString(labels.connected, R.string.monarch_connectedappslist_connected),
            app.connectedAtMillis?.let { tokens.relativeTime(it, now) },
        ),
        tokens.labelled(
            monarchString(labels.lastUsed, R.string.monarch_connectedappslist_last_used),
            app.lastUsedAtMillis?.let { tokens.relativeTime(it, now) },
        ),
    )
    val workspace = tokens.labelled(
        monarchString(labels.workspace, R.string.monarch_connectedappslist_workspace),
        app.workspace,
    )
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = app.name
            stateDescription = tokens.spokenDetails(badges, app.domain, times, workspace)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = app.name,
                style = MonarchTheme.typography.bodyStrong,
                color = tokens.nameColor(app.suspended, colors),
            )
            if (app.verified) {
                MonarchBadge(text = verified, tone = MonarchTone.Success, leadingIcon = MonarchDefaultIcons.Check)
            }
            if (app.suspended) MonarchBadge(text = suspended, tone = MonarchTone.Warning)
        }
        listOfNotNull(app.domain, times, workspace).forEach { line ->
            Text(text = line, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun ConfirmDialog(
    app: MonarchConnectedApp,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchConnectedAppsListLabels,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = labels.confirmTitle?.invoke(app.name)
            ?: stringResource(R.string.monarch_connectedappslist_confirm_title, app.name),
        description = monarchString(labels.confirmMessage, R.string.monarch_connectedappslist_confirm_message),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_connectedappslist_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.revoke, R.string.monarch_connectedappslist_revoke),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
    )
}

/** The removals in progress, the failures by app id, the last announcement and a way to start a removal. */
private class ConnectedAppsRevocation(
    val running: Set<String>,
    val failures: Map<String, String>,
    val announcement: String,
    val run: (MonarchConnectedApp) -> Unit,
)

@Composable
private fun rememberRevocation(
    onRevoke: (suspend (MonarchConnectedApp) -> Unit)?,
    labels: MonarchConnectedAppsListLabels,
): ConnectedAppsRevocation {
    val scope = rememberCoroutineScope()
    val current by rememberUpdatedState(onRevoke)
    val running = remember { mutableStateMapOf<String, Boolean>() }
    val failures = remember { mutableStateMapOf<String, String>() }
    var announcement by remember { mutableStateOf("") }
    val revoked = stringResource(R.string.monarch_connectedappslist_revoked)
    val failed = stringResource(R.string.monarch_connectedappslist_revoke_failed)
    return ConnectedAppsRevocation(running.keys.toSet(), failures.toMap(), announcement) { app ->
        val revoke = current
        if (revoke != null && running[app.id] != true) {
            running[app.id] = true
            failures.remove(app.id)
            scope.launch {
                val outcome = runCatching { revoke(app) }
                running.remove(app.id)
                val error = outcome.exceptionOrNull()
                if (error is CancellationException) throw error
                announcement = if (error == null) {
                    labels.revoked?.invoke(app.name) ?: revoked.format(app.name)
                } else {
                    val message = error.message?.takeIf { it.isNotBlank() }
                        ?: labels.revokeFailed?.invoke(app.name) ?: failed.format(app.name)
                    failures[app.id] = message
                    message
                }
            }
        }
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun connectedAppsClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)

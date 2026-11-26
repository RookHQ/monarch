package com.rookhq.monarch.components.identity.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.identity.devicelist.MonarchDeviceKind
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCard
import com.rookhq.monarch.components.layout.sectioncard.MonarchSectionCardLabels
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The id used while every other session is being signed out. */
private const val OthersId = "\u0000others"

/**
 * Where the person is signed in: one row per session with the device, when it
 * signed in and was last active, a This device badge, and Sign out for one
 * session or for every other session, each behind a confirmation.
 *
 * Built by Monarch on `MonarchSectionCard`, with one row per session
 * (`MonarchBadge`, a ghost `MonarchButton`), the Material 3 `AlertDialog` to
 * confirm (the confirm dialog), and the card's loading, empty and failed
 * states. Each session's text is one TalkBack element read as its label with
 * the badge, detail and times as its state. Only one sign-out runs at a time;
 * the button that started it shows its progress, and a failure is a danger
 * `MonarchInlineNotice` above the list. Results are announced through a
 * polite live region. The rows are not lazy: the list is short and sits in
 * the app's scrolling settings screen. Times are relative ("5 minutes ago")
 * and follow the clock every minute unless [now] is given.
 *
 * @param sessions the person's sessions, the current one included, in the order shown.
 * @param modifier the modifier of the card.
 * @param title replaces the built-in title "Where you're signed in".
 * @param description replaces the line under the title.
 * @param state ready, loading or failed. A ready, empty list shows the empty state.
 * @param retrying whether a retry is in progress, in the failed state.
 * @param confirm asks before signing out. Turn it off when the callbacks open the app's own confirmation.
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock.
 * @param footnote a note under the list, such as how long open apps stay signed in.
 * @param onRetry retries loading; shows "Try again" in the failed state.
 * @param onSignOut signs one session out, given its id; remove it from [sessions] when it returns. Shows Sign out
 * on every row. Throwing shows the failure.
 * @param onSignOutOthers signs out every session except the current one, given their ids. Shows Sign out other
 * devices in the header when there are other sessions. Throwing shows the failure.
 * @param sessionIcon an icon for each session's device kind, hidden from TalkBack. Monarch bundles no icons;
 * the kind is never the only information.
 * @param labels replacements for the other built-in strings.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchSessionList(
    sessions: List<MonarchSession>,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    state: MonarchSessionListState = MonarchSessionListState.Ready,
    retrying: Boolean = false,
    confirm: Boolean = true,
    now: Long? = null,
    footnote: String? = null,
    onRetry: (() -> Unit)? = null,
    onSignOut: (suspend (String) -> Unit)? = null,
    onSignOutOthers: (suspend (List<String>) -> Unit)? = null,
    sessionIcon: (@Composable (MonarchDeviceKind) -> Unit)? = null,
    labels: MonarchSessionListLabels = MonarchSessionListLabels(),
) {
    val tokens = MonarchSessionListTokens
    val ready = state == MonarchSessionListState.Ready
    val run = rememberSessionSignOut(onSignOut, onSignOutOthers, labels)
    var pending by remember { mutableStateOf<SessionSignOutTarget?>(null) }
    val clock = sessionListClock(
        now,
        ready && sessions.isNotEmpty(),
        sessions.flatMap { listOf(it.signedInMillis, it.lastActiveMillis) },
    )
    val start: (SessionSignOutTarget) -> Unit = { target -> if (confirm) pending = target else run.start(target) }
    val others = tokens.otherIds(sessions)
    Column(modifier = modifier) {
        MonarchSectionCard(
            title = title ?: stringResource(R.string.monarch_sessionlist_title),
            description = description ?: stringResource(R.string.monarch_sessionlist_description),
            state = MonarchSessionListTokens.cardState(state, sessions.isEmpty()),
            retrying = retrying,
            onRetry = onRetry,
            actions = signOutOthersAction(
                shown = tokens.showsSignOutOthers(onSignOutOthers != null, ready, sessions),
                runningId = run.runningId,
                labels = labels,
                onClick = { start(SessionSignOutTarget.Others(others)) },
            ),
            footer = footnote?.let(::footnoteSlot),
            labels = MonarchSectionCardLabels(
                loading = monarchString(labels.loading, R.string.monarch_sessionlist_loading),
                emptyTitle = monarchString(labels.emptyTitle, R.string.monarch_sessionlist_empty_title),
                errorTitle = monarchString(labels.errorTitle, R.string.monarch_sessionlist_error_title),
            ),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
                run.failure?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
                val listLabel = monarchString(labels.listLabel, R.string.monarch_sessionlist_list_label)
                Column(
                    modifier = Modifier.semantics {
                        contentDescription = listLabel
                        isTraversalGroup = true
                    },
                    verticalArrangement = Arrangement.spacedBy(tokens.SectionGap),
                ) {
                    sessions.forEachIndexed { index, session ->
                        SessionRow(
                            session = session,
                            now = clock,
                            signingOut = run.runningId == session.id,
                            signOutEnabled = run.runningId == null,
                            onSignOut = onSignOut?.let { { start(SessionSignOutTarget.One(session)) } },
                            sessionIcon = sessionIcon,
                            labels = labels,
                        )
                    }
                }
            }
        }
        // Always present, so a result is announced once as a change of this live region.
        MonarchLiveStatus(text = run.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    pending?.let { target ->
        SessionConfirmDialog(
            target = target,
            onConfirm = {
                pending = null
                run.start(target)
            },
            onDismiss = { pending = null },
            labels = labels,
        )
    }
}

/** The header action that signs out every other session, or `null` when it is not shown. */
private fun signOutOthersAction(
    shown: Boolean,
    runningId: String?,
    labels: MonarchSessionListLabels,
    onClick: () -> Unit,
): (@Composable RowScope.() -> Unit)? = if (shown) {
    {
        MonarchButton(
            text = monarchString(labels.signOutOthers, R.string.monarch_sessionlist_sign_out_others),
            onClick = onClick,
            size = MonarchButtonSize.Sm,
            enabled = runningId == null || runningId == OthersId,
            loading = runningId == OthersId,
        )
    }
} else {
    null
}

/** The footer slot that shows [text]. */
private fun footnoteSlot(text: String): @Composable RowScope.() -> Unit = { Footnote(text) }

@Composable
private fun Footnote(text: String) {
    Text(text = text, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
}

@Composable
private fun SessionRow(
    session: MonarchSession,
    now: Long,
    signingOut: Boolean,
    signOutEnabled: Boolean,
    onSignOut: (() -> Unit)?,
    sessionIcon: (@Composable (MonarchDeviceKind) -> Unit)?,
    labels: MonarchSessionListLabels,
) {
    val tokens = MonarchSessionListTokens
    val shape = MonarchTheme.shapes.md
    // As the web: a tinted tile per session, the icon at the start and Sign out at the end of the row.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MonarchTheme.colors.bgSubtle, shape)
            .padding(tokens.RowGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (sessionIcon != null) {
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                Box(Modifier.clearAndSetSemantics { }) { sessionIcon(session.kind) }
            }
        }
        Column(Modifier.weight(1f)) { SessionText(session, now, labels) }
        if (onSignOut != null) {
            val name = labels.signOutLabel?.invoke(session.label)
                ?: stringResource(R.string.monarch_sessionlist_sign_out_label, session.label)
            MonarchButton(
                text = monarchString(labels.signOut, R.string.monarch_sessionlist_sign_out),
                onClick = onSignOut,
                modifier = Modifier.semantics { contentDescription = name },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = signOutEnabled || signingOut,
                loading = signingOut,
            )
        }
    }
}

/** The label, badge, detail and times of a session, read as one element. */
@Composable
private fun SessionText(session: MonarchSession, now: Long, labels: MonarchSessionListLabels) {
    val tokens = MonarchSessionListTokens
    val colors = MonarchTheme.colors
    val current = monarchString(labels.current, R.string.monarch_sessionlist_current)
    val signedIn = monarchString(labels.signedIn, R.string.monarch_sessionlist_signed_in)
    val lastActive = monarchString(labels.lastActive, R.string.monarch_sessionlist_last_active)
    val times = tokens.line(
        session.signedInMillis?.let { "$signedIn ${tokens.relativeTime(it, now)}" },
        session.lastActiveMillis?.takeIf { !session.current }?.let { "$lastActive ${tokens.relativeTime(it, now)}" },
    )
    val detail = session.detail?.takeIf { it.isNotBlank() }
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = session.label
            stateDescription = tokens.spokenDetails(current.takeIf { session.current }, detail, times)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = session.label, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            if (session.current) MonarchBadge(text = current, tone = tokens.CurrentTone)
        }
        listOfNotNull(detail, times).forEach { line ->
            Text(text = line, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun SessionConfirmDialog(
    target: SessionSignOutTarget,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchSessionListLabels,
) {
    val title: String
    val message: String
    val confirmText: String
    when (target) {
        is SessionSignOutTarget.One -> {
            val current = target.session.current
            title = labels.confirmTitle?.invoke(target.session.label, current) ?: if (current) {
                stringResource(R.string.monarch_sessionlist_confirm_title_current)
            } else {
                stringResource(R.string.monarch_sessionlist_confirm_title, target.session.label)
            }
            message = labels.confirmMessage?.invoke(current) ?: stringResource(
                if (current) {
                    R.string.monarch_sessionlist_confirm_message_current
                } else {
                    R.string.monarch_sessionlist_confirm_message
                },
            )
            confirmText = monarchString(labels.signOut, R.string.monarch_sessionlist_sign_out)
        }
        is SessionSignOutTarget.Others -> {
            val count = target.ids.size
            title = monarchString(labels.othersTitle, R.string.monarch_sessionlist_others_title)
            message = labels.othersMessage?.invoke(count)
                ?: pluralStringResource(R.plurals.monarch_sessionlist_others_message, count, count)
            confirmText = monarchString(labels.signOutOthers, R.string.monarch_sessionlist_sign_out_others)
        }
    }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = title,
        description = message,
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_sessionlist_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(text = confirmText, onClick = onConfirm, variant = MonarchButtonVariant.Danger)
        },
    )
}

/** What a confirmation or a sign-out is for: one session, or every other session. */
private sealed interface SessionSignOutTarget {
    class One(val session: MonarchSession) : SessionSignOutTarget
    class Others(val ids: List<String>) : SessionSignOutTarget
}

/** The sign-out in progress, the last failure and announcement, and a way to start one. */
private class SessionSignOut(
    val runningId: String?,
    val failure: String?,
    val announcement: String,
    val start: (SessionSignOutTarget) -> Unit,
)

@Composable
private fun rememberSessionSignOut(
    onSignOut: (suspend (String) -> Unit)?,
    onSignOutOthers: (suspend (List<String>) -> Unit)?,
    labels: MonarchSessionListLabels,
): SessionSignOut {
    val scope = rememberCoroutineScope()
    val one by rememberUpdatedState(onSignOut)
    val others by rememberUpdatedState(onSignOutOthers)
    var runningId by remember { mutableStateOf<String?>(null) }
    var failure by remember { mutableStateOf<String?>(null) }
    var announcement by remember { mutableStateOf("") }
    val signedOut = stringResource(R.string.monarch_sessionlist_signed_out)
    val failed = stringResource(R.string.monarch_sessionlist_sign_out_failed)
    val othersFailed = monarchString(labels.signOutOthersFailed, R.string.monarch_sessionlist_sign_out_others_failed)
    val resources = LocalResources.current
    return SessionSignOut(runningId, failure, announcement) { target ->
        if (runningId == null) {
            val work: (suspend () -> Unit)?
            val done: String
            val fail: String
            when (target) {
                is SessionSignOutTarget.One -> {
                    val action = one
                    work = action?.let { { it(target.session.id) } }
                    runningId = target.session.id
                    done = labels.signedOut?.invoke(target.session.label) ?: signedOut.format(target.session.label)
                    fail = labels.signOutFailed?.invoke(target.session.label) ?: failed.format(target.session.label)
                }
                is SessionSignOutTarget.Others -> {
                    val action = others
                    work = action?.let { { it(target.ids) } }
                    runningId = OthersId
                    val count = target.ids.size
                    done = labels.signedOutOthers?.invoke(count) ?: resources.getQuantityString(
                        R.plurals.monarch_sessionlist_signed_out_others,
                        count,
                        count,
                    )
                    fail = othersFailed
                }
            }
            failure = null
            scope.launch {
                val outcome = runCatching { work?.invoke() }
                runningId = null
                val error = outcome.exceptionOrNull()
                if (error is CancellationException) throw error
                if (error == null) {
                    announcement = done
                } else {
                    failure = fail
                    announcement = fail
                }
            }
        }
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun sessionListClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)

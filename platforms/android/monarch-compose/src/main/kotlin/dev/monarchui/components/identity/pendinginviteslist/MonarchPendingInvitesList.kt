package dev.monarchui.components.identity.pendinginviteslist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.copybutton.MonarchCopyButton
import dev.monarchui.components.identity.memberrow.MonarchMemberRow
import dev.monarchui.components.identity.memberrow.MonarchMemberRowLabels
import dev.monarchui.components.identity.memberrow.MonarchMemberRowStatus
import dev.monarchui.components.layout.sectioncard.MonarchSectionCard
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardLabels
import dev.monarchui.components.layout.sectioncard.MonarchSectionCardState
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * Invitations that have not been accepted yet: the email, the role, who sent
 * it, when it was sent and when it expires, with Resend, Copy link and Revoke
 * behind a confirmation.
 *
 * A product pattern composed from Monarch parts: a `MonarchSectionCard` (its
 * title is a heading; it shows the loading, empty and error states) holding
 * one `MonarchMemberRow` per invitation, in its invited status, separated by
 * dividers. The rows are a `Column`, not a lazy list, so the card can sit
 * under the member list on a screen that scrolls. The member row brings Resend
 * invite (with "Invite sent again." after it), Revoke with its Material 3
 * confirmation dialog, progress while an action runs and its failure; this
 * list adds the detail line, the Expired badge and Copy link
 * (`MonarchCopyButton`), and announces a revoke through a polite live region.
 *
 * @param invites the invitations, shown in the order given.
 * @param modifier the modifier of the card.
 * @param title replaces the built-in title, "Pending invitations"; it wins over `labels.title`.
 * @param description a line under the title.
 * @param state ready, loading, empty or error, as in `MonarchSectionCard`. A ready empty list shows the empty state.
 * @param retrying shows the retry button busy in the error state.
 * @param now the reference time for relative times and expiry, in epoch milliseconds. `null` follows the clock,
 * refreshed every minute.
 * @param locale the locale of the relative times. `null` uses the first locale of the configuration.
 * @param onRetry retries loading; shows a retry button in the error state.
 * @param onResend sends the invitation again, given its id. Without it rows have no Resend invite. Throw to show
 * the error in the row.
 * @param onRevoke revokes the invitation, given its id, after a confirmation. Without it rows have no Revoke. The
 * app removes the invitation from [invites] on success; throw to show the error in the row.
 * @param labels replacements for every built-in string, including the member row strings in `row`.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchPendingInvitesList(
    invites: List<MonarchPendingInvite>,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    state: MonarchSectionCardState = MonarchSectionCardState.Ready,
    retrying: Boolean = false,
    now: Long? = null,
    locale: Locale? = null,
    onRetry: (() -> Unit)? = null,
    onResend: (suspend (id: String) -> Unit)? = null,
    onRevoke: (suspend (id: String) -> Unit)? = null,
    labels: MonarchPendingInvitesListLabels = MonarchPendingInvitesListLabels(),
) {
    val tokens = MonarchPendingInvitesListTokens
    val reference = rememberMonarchRelativeClock(invites.flatMap { listOf(it.sentAt, it.expiresAt) }, fixed = now)
    val writeIn = locale ?: LocalConfiguration.current.locales[0]
    var announcement by remember { mutableStateOf("") }
    val listName = monarchString(labels.listLabel, R.string.monarch_pendinginviteslist_list_label)
    val resendText = monarchString(labels.row.resend, R.string.monarch_pendinginviteslist_resend)
    val revokeText = monarchString(labels.row.revokeConfirm, R.string.monarch_pendinginviteslist_revoke)
    val baseRow = pendingInvitesListRowLabels(labels.row, "", resendText, revokeText)
    val expiredRow = pendingInvitesListRowLabels(
        labels.row,
        monarchString(labels.expired, R.string.monarch_pendinginviteslist_expired),
        resendText,
        revokeText,
    )
    Column(modifier = modifier) {
        PendingInvitesListCard(title, description, tokens.state(state, invites.size), retrying, onRetry, labels) {
            // As the web: each invitation is a tinted tile inside the card.
            Column(
                modifier = Modifier
                    .padding(horizontal = MonarchTheme.spacing.space4)
                    .padding(bottom = MonarchTheme.spacing.space4)
                    .semantics {
                        isTraversalGroup = true
                        contentDescription = listName
                    },
                verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            ) {
                invites.forEachIndexed { _, invite ->
                    val expired = tokens.expired(invite, reference)
                    val revoked = labels.revoked?.invoke(invite.email)
                        ?: stringResource(R.string.monarch_pendinginviteslist_revoked, invite.email)
                    PendingInvitesListRow(
                        invite = invite,
                        expired = expired,
                        detail = pendingInvitesListDetail(invite, expired, reference, writeIn, labels),
                        onResend = onResend,
                        onRevoke = onRevoke?.let { revoke ->
                            suspend {
                                revoke(invite.id)
                                announcement = revoked
                            }
                        },
                        rowLabels = if (expired) expiredRow else baseRow,
                        labels = labels,
                    )
                }
            }
        }
        // Always present, so a revoke is announced even though its row goes away.
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun PendingInvitesListCard(
    title: String?,
    description: String?,
    state: MonarchSectionCardState,
    retrying: Boolean,
    onRetry: (() -> Unit)?,
    labels: MonarchPendingInvitesListLabels,
    content: @Composable () -> Unit,
) {
    MonarchSectionCard(
        title = title ?: monarchString(labels.title, R.string.monarch_pendinginviteslist_title),
        description = description,
        state = state,
        retrying = retrying,
        flush = true,
        onRetry = onRetry,
        labels = MonarchSectionCardLabels(
            loading = monarchString(labels.loading, R.string.monarch_pendinginviteslist_loading),
            emptyTitle = monarchString(labels.emptyTitle, R.string.monarch_pendinginviteslist_empty_title),
            errorTitle = monarchString(labels.errorTitle, R.string.monarch_pendinginviteslist_error_title),
        ),
    ) { content() }
}

/** One invitation: a `MonarchMemberRow` in its invited status, with Copy link while it has not expired. */
@Composable
private fun PendingInvitesListRow(
    invite: MonarchPendingInvite,
    expired: Boolean,
    detail: String?,
    onResend: (suspend (id: String) -> Unit)?,
    onRevoke: (suspend () -> Unit)?,
    rowLabels: MonarchMemberRowLabels,
    labels: MonarchPendingInvitesListLabels,
) {
    val link = invite.link?.takeIf { !expired }
    MonarchMemberRow(
        modifier = Modifier.background(MonarchTheme.colors.bgSubtle, MonarchTheme.shapes.md),
        name = invite.email,
        meta = detail,
        status = MonarchMemberRowStatus.Invited,
        statusTone = MonarchTone.Warning.takeIf { expired },
        badges = listOfNotNull(invite.role),
        canChangeRole = false,
        canRemove = onRevoke != null,
        onResend = onResend?.let { resend -> suspend { resend(invite.id) } },
        onRemove = onRevoke,
        labels = rowLabels,
        actionsFirst = true,
        removeIcon = null,
        showAvatar = false,
        actions = if (link != null) {
            {
                MonarchCopyButton(
                    value = link,
                    size = MonarchButtonSize.Sm,
                    target = labels.copyTarget?.invoke(invite.email)
                        ?: stringResource(R.string.monarch_pendinginviteslist_copy_target, invite.email),
                    label = monarchString(labels.copyLink, R.string.monarch_pendinginviteslist_copy_link),
                )
            }
        } else {
            null
        },
    )
}

/** "Invited by Sam Rivera · Sent 2 days ago · Expires in 5 days"; an expired invitation leaves out its expiry. */
@Composable
private fun pendingInvitesListDetail(
    invite: MonarchPendingInvite,
    expired: Boolean,
    now: Long,
    locale: Locale,
    labels: MonarchPendingInvitesListLabels,
): String? {
    val tokens = MonarchPendingInvitesListTokens
    val by = invite.invitedBy?.takeIf { it.isNotBlank() }?.let {
        labels.invitedBy?.invoke(it) ?: stringResource(R.string.monarch_pendinginviteslist_invited_by, it)
    }
    val sent = invite.sentAt?.let { tokens.relativeTime(it, now, locale) }?.let {
        labels.sent?.invoke(it) ?: stringResource(R.string.monarch_pendinginviteslist_sent, it)
    }
    val expires = invite.expiresAt?.takeIf { !expired }?.let { tokens.relativeTime(it, now, locale) }?.let {
        labels.expires?.invoke(it) ?: stringResource(R.string.monarch_pendinginviteslist_expires, it)
    }
    return tokens.detail(listOf(by, sent, expires))
}

/** The member row strings with the list's short "Resend" and "Revoke"; "Expired" replaces "Invited" when given. */
private fun pendingInvitesListRowLabels(
    row: MonarchMemberRowLabels,
    invited: String?,
    resend: String,
    revoke: String,
): MonarchMemberRowLabels =
    MonarchMemberRowLabels(
        you = row.you,
        invited = invited,
        suspended = row.suspended,
        presence = row.presence,
        roleLabel = row.roleLabel,
        chooseRole = row.chooseRole,
        resend = resend,
        resendLabel = row.resendLabel,
        resent = row.resent,
        remove = row.remove,
        revoke = row.revoke,
        removeTitle = row.removeTitle,
        revokeTitle = row.revokeTitle,
        removeDescription = row.removeDescription,
        revokeDescription = row.revokeDescription,
        removeConfirm = row.removeConfirm,
        revokeConfirm = revoke,
        cancel = row.cancel,
        failed = row.failed,
    )

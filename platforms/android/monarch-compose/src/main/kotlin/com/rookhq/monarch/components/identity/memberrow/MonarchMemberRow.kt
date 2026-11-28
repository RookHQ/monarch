package com.rookhq.monarch.components.identity.memberrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import com.rookhq.monarch.R
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.components.datadisplay.listrow.MonarchListRow
import com.rookhq.monarch.components.identity.avatar.MonarchAvatar
import com.rookhq.monarch.components.identity.presencedot.MonarchPresenceStatus
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchPresenceName
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One member of a workspace, space or channel: avatar, name, email, role,
 * status (active, invited, suspended) and actions that are confirmed and may
 * run for a while.
 *
 * Built by Monarch on a `MonarchListRow` for the identity, with
 * `MonarchAvatar` and `MonarchBadge`; the controls sit under it: a compact
 * `MonarchRolePicker` (an exposed dropdown menu) for the role, `MonarchButton`
 * for Resend invite and Remove, and a Material 3 `AlertDialog` that confirms a
 * removal. Actions are suspending functions: the control shows progress while
 * one runs, a thrown error is shown in the row and announced, and the role
 * picker shows the new role until [role] changes or the change fails, when it
 * returns to the previous role.
 *
 * The identity is one TalkBack element: the name ("Priya Raman, You") and, as
 * its state, the status, presence, badges, a read-only role, the email and the
 * meta line. Each control is named after the member, such as "Remove Priya
 * Raman".
 *
 * @param name the display name; also seeds the avatar's initials and colour. For an invitation, the email.
 * @param modifier the modifier of the row.
 * @param image the avatar picture, loaded by the app; `null` shows the initials.
 * @param email the email or handle under the name.
 * @param meta extra detail: joined date, invite expiry, availability.
 * @param presence a presence dot on the avatar, also read in words.
 * @param status active, invited or suspended.
 * @param role the value of the member's role, one of [roles].
 * @param roles the roles allowed here. Their titles also show for a read-only role; disabled roles cannot be
 * picked.
 * @param badges extra neutral badges after the name, such as "Guest".
 * @param self the signed-in person's own row: "You" after the name; role picker and Remove hidden by default.
 * @param enabled disables every control in the row.
 * @param canChangeRole shows the role picker; by default when [onRoleChange] is given and it is not your own row.
 * @param canRemove shows Remove; by default when [onRemove] is given and it is not your own row.
 * @param removeConsequences lines added under the text of the remove confirmation.
 * @param onRoleChange makes the role a picker. Throw to restore the previous role and show the error.
 * @param onResend sends the invitation again; shown for invited rows. "Invite sent again." follows success.
 * @param onRemove removes the member or revokes the invitation after a confirmation. The app removes the row;
 * a thrown error is shown in the row.
 * @param removeIcon an icon shown before the word of the remove button. Monarch bundles no icons.
 * @param statusTone the tone of the status badge. `null` keeps the default: info for Invited, warning for
 * Suspended.
 * @param swipeActions also offers the actions as swipes, as iOS does in a list: swipe toward the start for Remove
 * (or Revoke invite), which still asks first, and toward the end for Resend invite. Each swipe is also a TalkBack
 * action on the member, named like its button.
 * @param labels replacements for the built-in strings.
 * @param actionsFirst puts [actions] before Resend invite and Remove, as the invitation list does with Copy link.
 * @param actions more trailing controls after Remove, such as a menu button. Name them after the member.
 * @param showAvatar off, the row has no avatar and the text starts at the edge, as the invitation list does.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchMemberRow(
    name: String,
    modifier: Modifier = Modifier,
    image: Painter? = null,
    email: String? = null,
    meta: String? = null,
    presence: MonarchPresenceStatus? = null,
    status: MonarchMemberRowStatus = MonarchMemberRowStatus.Active,
    role: String? = null,
    roles: List<MonarchRoleOption<String>> = emptyList(),
    badges: List<String> = emptyList(),
    self: Boolean = false,
    enabled: Boolean = true,
    canChangeRole: Boolean? = null,
    canRemove: Boolean? = null,
    removeConsequences: List<String> = emptyList(),
    onRoleChange: (suspend (String) -> Unit)? = null,
    onResend: (suspend () -> Unit)? = null,
    onRemove: (suspend () -> Unit)? = null,
    removeIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Trash,
    statusTone: MonarchTone? = null,
    swipeActions: Boolean = false,
    labels: MonarchMemberRowLabels = MonarchMemberRowLabels(),
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
    actionsFirst: Boolean = false,
    showAvatar: Boolean = true,
) {
    val tokens = MonarchMemberRowTokens
    val scope = rememberCoroutineScope()
    val failed = monarchString(labels.failed, R.string.monarch_memberrow_failed)
    var notice by remember { mutableStateOf<MonarchMemberRowNotice?>(null) }
    val actionState = MemberRowActionState(scope, failed) { notice = it }
    val editable = tokens.editable(canChangeRole, onRoleChange != null, self, roles.size)
    val removable = tokens.removable(canRemove, onRemove != null, self)
    val invited = status == MonarchMemberRowStatus.Invited
    val resend = onResend?.takeIf { invited }
    val controls = rememberMemberRowControls(
        actions = actionState,
        texts = memberRowTexts(name, invited, labels),
        onRemove = onRemove?.takeIf { removable },
        onResend = resend,
    )
    val swipe = memberRowSwipe(swipeActions && enabled, controls)
    BoxWithConstraints(modifier = modifier) {
        // A wide row, as the web: the controls sit beside the identity; a narrow one puts them under it.
        val wide = maxWidth >= tokens.WideRowWidth
        Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
            val identity = IdentityContent(
                name = name,
                image = image,
                email = email,
                meta = meta,
                presence = presence,
                status = status,
                statusTone = statusTone,
                readOnlyRole = if (editable) null else role,
                roles = roles,
                badges = badges,
                self = self,
                showAvatar = showAvatar,
            )
            val padded = Modifier.padding(horizontal = tokens.Inset)
            val showPicker = editable && onRoleChange != null
            val identityContent = @Composable {
                MemberRowSwipeBox(swipe, controls) { Identity(identity, labels, swipe, controls) }
            }
            if (wide) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = tokens.Inset)) {
                    Box(Modifier.weight(1f)) { identityContent() }
                    if (showPicker) {
                        Box(Modifier.width(tokens.WideRolePickerWidth)) {
                            MemberRowRolePicker(
                                name, role, roles, enabled, onRoleChange, actionState, labels, Modifier,
                                showLabel = false,
                            )
                        }
                    }
                    MemberRowButtons(
                        enabled, controls, removeIcon, labels, Modifier, actions, actionsFirst,
                    )
                }
            } else {
                identityContent()
            }
            // Always present, so a failure or a resent invitation is announced as a change of this region.
            Box(
                modifier = padded.semantics {
                    liveRegion = LiveRegionMode.Polite
                    notice?.let { contentDescription = it.text }
                },
            ) {
                notice?.let { current ->
                    Text(
                        text = current.text,
                        modifier = Modifier.clearAndSetSemantics { },
                        style = MonarchTheme.typography.caption,
                        color = tokens.noticeColor(current, MonarchTheme.colors),
                    )
                }
            }
            if (!wide) {
                if (showPicker) {
                    MemberRowRolePicker(name, role, roles, enabled, onRoleChange, actionState, labels, padded)
                }
                MemberRowButtons(
                    enabled, controls, removeIcon, labels, padded, actions, actionsFirst,
                )
            }
        }
    }
    if (controls.onRemove != null) MemberRowRemoveDialog(removeConsequences, controls, labels)
}

/** Which swipe actions the row offers now: those whose action exists and does not run. */
private fun memberRowSwipe(on: Boolean, controls: MemberRowControls): MemberRowSwipe = MemberRowSwipe(
    remove = on && controls.onRemove != null && !controls.removing,
    resend = on && controls.onResend != null && !controls.resending,
)

/** Resend invite, Remove and the app's own controls, in one wrapping row. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberRowButtons(
    enabled: Boolean,
    controls: MemberRowControls,
    removeIcon: (@Composable () -> Unit)?,
    labels: MonarchMemberRowLabels,
    modifier: Modifier,
    actions: (@Composable FlowRowScope.() -> Unit)?,
    actionsFirst: Boolean,
) {
    if (controls.onResend == null && controls.onRemove == null && actions == null) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchMemberRowTokens.ItemGap),
        verticalArrangement = Arrangement.spacedBy(MonarchMemberRowTokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (actionsFirst) actions?.invoke(this)
        if (controls.onResend != null) MemberRowResend(enabled, controls, labels)
        if (controls.onRemove != null) MemberRowRemoveButton(enabled, removeIcon, controls)
        if (!actionsFirst) actions?.invoke(this)
    }
}

/** The words of the row's actions, built in or replaced. */
@Composable
private fun memberRowTexts(name: String, invited: Boolean, labels: MonarchMemberRowLabels): MemberRowTexts =
    MemberRowTexts(
        remove = removeTexts(name, invited, labels),
        resendName = labels.resendLabel?.invoke(name) ?: stringResource(R.string.monarch_memberrow_resend_label, name),
        resendText = monarchString(labels.resend, R.string.monarch_memberrow_resend),
        resent = monarchString(labels.resent, R.string.monarch_memberrow_resent),
    )

/** What the identity shows. */
@Suppress("LongParameterList") // One holder instead of passing each value through the identity.
private class IdentityContent(
    val name: String,
    val image: Painter?,
    val email: String?,
    val meta: String?,
    val presence: MonarchPresenceStatus?,
    val status: MonarchMemberRowStatus,
    val statusTone: MonarchTone?,
    val readOnlyRole: String?,
    val roles: List<MonarchRoleOption<String>>,
    val badges: List<String>,
    val self: Boolean,
    val showAvatar: Boolean,
)

/** Runs an action with the row's scope, showing its failure, or [run]'s done notice, under the identity. */
internal class MemberRowActionState(
    private val scope: CoroutineScope,
    private val failed: String,
    private val show: (MonarchMemberRowNotice?) -> Unit,
) {
    fun run(
        block: suspend () -> Unit,
        busy: (Boolean) -> Unit,
        onFailure: () -> Unit = {},
        onDone: () -> MonarchMemberRowNotice? = { null },
    ) {
        show(null)
        busy(true)
        scope.launch {
            val outcome = runCatching { block() }
            busy(false)
            val error = outcome.exceptionOrNull()
            if (error == null) {
                show(onDone())
            } else {
                val text = MonarchMemberRowTokens.failureText(error, failed)
                onFailure()
                show(MonarchMemberRowNotice(text, error = true))
            }
        }
    }
}

/** The avatar, the name with its badges, and the lines under it, as one TalkBack element. */
@Composable
private fun Identity(
    content: IdentityContent,
    labels: MonarchMemberRowLabels,
    swipe: MemberRowSwipe,
    controls: MemberRowControls,
) {
    val name = content.name
    val image = content.image
    val email = content.email
    val meta = content.meta
    val presence = content.presence
    val status = content.status
    val self = content.self
    val badges = content.badges
    val readOnlyRole = content.readOnlyRole
    val roles = content.roles
    val tokens = MonarchMemberRowTokens
    val you = monarchString(labels.you, R.string.monarch_memberrow_you)
    val statusText = statusText(status, labels)
    val presenceText = presence?.let { monarchPresenceName(it, labels.presence?.invoke(it)) }
    val roleTitle = tokens.roleTitle(readOnlyRole, roles)
    val spokenName = if (self) "$name, $you" else name
    val value = tokens.spokenValue(listOf(statusText, presenceText) + badges + listOf(roleTitle, email, meta))
    val hasSupporting = email != null || meta != null || roleTitle != null
    MonarchListRow(
        headlineContent = {
            val tone = tokens.statusTone(status, content.statusTone)
            Headline(name, status, statusText, tone, badges, you.takeIf { self })
        },
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spokenName
            if (value.isNotEmpty()) stateDescription = value
            val swipeNames = memberRowSwipeActions(swipe, controls)
            if (swipeNames.isNotEmpty()) customActions = swipeNames
        },
        supportingContent = if (hasSupporting) {
            { Supporting(email, meta, roleTitle) }
        } else {
            null
        },
        leadingContent = if (content.showAvatar) {
            {
                MonarchAvatar(
                    image = image,
                    name = name,
                    status = presence,
                    decorative = true,
                    statusLabel = presenceText,
                )
            }
        } else {
            null
        },
    )
}

@Composable
private fun statusText(status: MonarchMemberRowStatus, labels: MonarchMemberRowLabels): String? = when (status) {
    MonarchMemberRowStatus.Active -> null
    MonarchMemberRowStatus.Invited ->
        monarchString(labels.invited, R.string.monarch_memberrow_invited).takeIf { it.isNotBlank() }
    MonarchMemberRowStatus.Suspended -> monarchString(labels.suspended, R.string.monarch_memberrow_suspended)
}

/** The name, then the You, status and extra badges. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Headline(
    name: String,
    status: MonarchMemberRowStatus,
    statusText: String?,
    statusTone: MonarchTone,
    badges: List<String>,
    you: String?,
) {
    val tokens = MonarchMemberRowTokens
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MonarchTheme.typography.bodyStrong,
            color = tokens.nameColor(status, MonarchTheme.colors),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (you != null) {
            // As the web: "(You)" follows the name in plain muted text, not in a badge.
            Text(text = "($you)", style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
        }
        badges.forEach { MonarchBadge(text = it, variant = MonarchBadgeVariant.Outline) }
        if (statusText != null) MonarchBadge(text = statusText, tone = statusTone)
    }
}

@Composable
private fun Supporting(email: String?, meta: String?, roleTitle: String?) {
    val muted = MonarchTheme.colors.textMuted
    val caption = MonarchTheme.typography.caption
    Column {
        if (email != null) {
            Text(text = email, style = caption, color = muted, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
        }
        if (meta != null) Text(text = meta, style = caption, color = muted)
        if (roleTitle != null) Text(text = roleTitle, style = caption, color = muted)
    }
}

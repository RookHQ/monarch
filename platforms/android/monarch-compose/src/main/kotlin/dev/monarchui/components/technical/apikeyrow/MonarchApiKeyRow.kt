package dev.monarchui.components.technical.apikeyrow

import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.iconbutton.MonarchIconButton
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.components.datadisplay.listrow.MonarchListRow
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadge
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import dev.monarchui.components.datadisplay.statusbadge.MonarchStatusDefinition
import dev.monarchui.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.rememberMonarchRelativeClock
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * One API key in a list: its name and status, the visible prefix with the
 * rest masked, when it was created, last used and expires, its scopes, and
 * Rotate and Revoke actions that ask for confirmation before calling the app.
 *
 * Built on `MonarchListRow`, with a `MonarchStatusBadge`, outline
 * `MonarchBadge`s for the scopes and a `MonarchMenu` for the
 * actions. Rotate and Revoke open a `MonarchDialog` (ConfirmDialog);
 * Revoke can ask for a phrase to be typed first. After confirmation the row
 * runs the action, shows "Rotating…" or "Revoking…" with the menu disabled,
 * and shows a `MonarchInlineErrorRetry` when the action throws. The secret
 * itself is never shown. Times are relative ("3 days ago") and follow the
 * clock every minute unless [now] is given.
 *
 * Monarch bundles no icons: without [menuIcon] the actions button shows
 * "Actions".
 *
 * @param name the key's name, also used in the confirmations and the menu's spoken name.
 * @param prefix the visible, non-secret start of the key; the rest is drawn as a mask.
 * @param createdAt when the key was created, in epoch milliseconds.
 * @param modifier the modifier of the row.
 * @param lastUsedAt when the key was last used, in epoch milliseconds; `null` reads "Never used".
 * @param expiresAt when the key expires, in epoch milliseconds, if it does.
 * @param scopes the scope names, shown as outline badges.
 * @param maxScopes the scopes shown before a +N badge.
 * @param status active, expiring, expired or revoked; expired and revoked keys have no actions.
 * @param now the reference time for the relative times, in epoch milliseconds. Without it they follow the clock.
 * @param revokeConfirmPhrase a phrase, usually the name, that must be typed before Revoke key works.
 * @param onRotate rotates the key after confirmation. A thrown exception is shown in the row with a retry.
 * Show the new secret with `MonarchKeyCreatedDialog`.
 * @param onRevoke revokes the key after confirmation, like [onRotate].
 * @param menuIcon makes the actions button an icon button with this icon, such as three dots. It keeps its
 * spoken name.
 * @param labels replacements for the built-in strings.
 * @param inlineActions shows Rotate and Revoke as buttons under the details, and writes the dates as "Created: last
 * month", instead of a menu and one dotted line. The extra [actions] are then not shown.
 * @param actions more menu items after Rotate and Revoke, such as Edit. Call `close` to close the menu.
 */
@Composable
public fun MonarchApiKeyRow(
    name: String,
    prefix: String,
    createdAt: Long,
    modifier: Modifier = Modifier,
    lastUsedAt: Long? = null,
    expiresAt: Long? = null,
    scopes: List<String> = emptyList(),
    maxScopes: Int = 3,
    status: MonarchApiKeyStatus = MonarchApiKeyStatus.Active,
    now: Long? = null,
    revokeConfirmPhrase: String? = null,
    onRotate: (suspend () -> Unit)? = null,
    onRevoke: (suspend () -> Unit)? = null,
    menuIcon: (@Composable () -> Unit)? = null,
    labels: MonarchApiKeyRowLabels = MonarchApiKeyRowLabels(),
    inlineActions: Boolean = false,
    actions: (@Composable ColumnScope.(close: () -> Unit) -> Unit)? = null,
) {
    val tokens = MonarchApiKeyRowTokens
    val texts = apiKeyRowTexts(labels)
    val scope = rememberCoroutineScope()
    var confirming by remember { mutableStateOf<MonarchApiKeyRowAction?>(null) }
    val runner = remember(scope) { ApiKeyRowRunner(scope) }
    runner.onRotate = onRotate
    runner.onRevoke = onRevoke
    val running = runner.running
    val hasActions = tokens.hasActions(status, onRotate != null || onRevoke != null || actions != null)
    val clock = apiKeyRowClock(now, listOf(createdAt, lastUsedAt, expiresAt))
    MonarchListRow(
        headlineContent = { NameAndStatus(name, status, texts) },
        modifier = modifier,
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                Details(
                    prefix,
                    createdAt,
                    lastUsedAt,
                    expiresAt,
                    scopes,
                    maxScopes,
                    clock,
                    texts,
                    labels,
                    inlineActions,
                )
                Progress(running, runner.failure, texts, onRetry = runner::run)
                if (inlineActions && hasActions) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(tokens.BadgeGap, Alignment.End),
                    ) {
                        onRotate?.let {
                            MonarchButton(
                                text = texts.rotate,
                                onClick = { confirming = MonarchApiKeyRowAction.Rotate },
                                enabled = running == null,
                                variant = MonarchButtonVariant.Secondary,
                                size = MonarchButtonSize.Sm,
                            )
                        }
                        onRevoke?.let {
                            MonarchButton(
                                text = texts.revoke,
                                onClick = { confirming = MonarchApiKeyRowAction.Revoke },
                                enabled = running == null,
                                variant = MonarchButtonVariant.DangerSubtle,
                                size = MonarchButtonSize.Sm,
                            )
                        }
                    }
                }
            }
        },
        trailingContent = if (hasActions && !inlineActions) {
            {
                ActionsMenu(
                    name = name,
                    enabled = running == null,
                    onRotate = onRotate?.let { { confirming = MonarchApiKeyRowAction.Rotate } },
                    onRevoke = onRevoke?.let { { confirming = MonarchApiKeyRowAction.Revoke } },
                    icon = menuIcon,
                    texts = texts,
                    labels = labels,
                    actions = actions,
                )
            }
        } else {
            null
        },
    )
    confirming?.let { action ->
        ApiKeyRowConfirmDialog(
            action = action,
            name = name,
            phrase = revokeConfirmPhrase.takeIf { action == MonarchApiKeyRowAction.Revoke },
            texts = texts,
            labels = labels,
            onDismiss = { confirming = null },
            onConfirm = {
                confirming = null
                runner.run(action)
            },
        )
    }
}

/** A failed action and the exception's message. */
private class ApiKeyRowFailure(val action: MonarchApiKeyRowAction, val message: String?)

/** Runs one action at a time in the row's scope and keeps what is running and what failed. */
@Stable
private class ApiKeyRowRunner(private val scope: CoroutineScope) {
    var onRotate: (suspend () -> Unit)? = null
    var onRevoke: (suspend () -> Unit)? = null
    var running: MonarchApiKeyRowAction? by mutableStateOf(null)
        private set
    var failure: ApiKeyRowFailure? by mutableStateOf(null)
        private set

    fun run(action: MonarchApiKeyRowAction) {
        val block = (if (action == MonarchApiKeyRowAction.Rotate) onRotate else onRevoke) ?: return
        if (running != null) return
        failure = null
        running = action
        scope.launch {
            val error = runCatching { block() }.exceptionOrNull()
            if (error is CancellationException) throw error
            running = null
            failure = error?.let { ApiKeyRowFailure(action, it.message) }
        }
    }
}

@Composable
private fun NameAndStatus(name: String, status: MonarchApiKeyStatus, texts: ApiKeyRowTexts) {
    FlowRow(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(MonarchApiKeyRowTokens.BadgeGap),
        verticalArrangement = Arrangement.spacedBy(MonarchApiKeyRowTokens.Gap),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        MonarchText(text = name, style = MonarchTextStyle.BodyStrong)
        val label = texts.status(status)
        // The badge merges itself; it is read here, as a word after the name, so name and status are one element.
        Box(Modifier.clearAndSetSemantics { text = AnnotatedString(label) }) {
            MonarchStatusBadge(
                definition = MonarchStatusDefinition(label, MonarchApiKeyRowTokens.tone(status)),
                size = MonarchStatusBadgeSize.Sm,
            )
        }
    }
}

@Composable
private fun Details(
    prefix: String,
    createdAt: Long,
    lastUsedAt: Long?,
    expiresAt: Long?,
    scopes: List<String>,
    maxScopes: Int,
    now: Long,
    texts: ApiKeyRowTexts,
    labels: MonarchApiKeyRowLabels,
    inline: Boolean,
) {
    val tokens = MonarchApiKeyRowTokens
    val muted = tokens.mutedColor(MonarchTheme.colors)
    val prefixName = labels.prefix?.invoke(prefix) ?: stringResource(R.string.monarch_apikeyrow_prefix, prefix)
    val locale = LocalConfiguration.current.locales[0]
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        MonarchText(
            text = prefix + tokens.MASK,
            modifier = Modifier.clearAndSetSemantics { contentDescription = prefixName },
            style = MonarchTextStyle.Code,
            color = muted,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
        )
        if (inline) {
            val times = listOfNotNull(
                "${texts.created}: ${tokens.relativeTime(createdAt, now, locale)}",
                "${texts.lastUsed}: " + (lastUsedAt?.let { tokens.relativeTime(it, now, locale) } ?: texts.neverUsed),
            )
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                MonarchText(text = times.joinToString("   "), style = MonarchTextStyle.Caption, color = muted)
                expiresAt?.let {
                    MonarchText(
                        text = "${texts.expires}: ${tokens.relativeTime(it, now, locale)}",
                        style = MonarchTextStyle.Caption,
                        color = muted,
                    )
                }
            }
        } else {
            val times = listOfNotNull(
                "${texts.created} ${tokens.relativeTime(createdAt, now, locale)}",
                lastUsedAt?.let { "${texts.lastUsed} ${tokens.relativeTime(it, now, locale)}" } ?: texts.neverUsed,
                expiresAt?.let { "${texts.expires} ${tokens.relativeTime(it, now, locale)}" },
            )
            MonarchText(text = tokens.timesLine(times), style = MonarchTextStyle.Caption, color = muted)
        }
        Scopes(scopes, maxScopes, texts, labels)
    }
}

@Composable
private fun Scopes(scopes: List<String>, maxScopes: Int, texts: ApiKeyRowTexts, labels: MonarchApiKeyRowLabels) {
    val tokens = MonarchApiKeyRowTokens
    if (scopes.isEmpty()) {
        MonarchText(
            text = texts.noScopes,
            style = MonarchTextStyle.Caption,
            color = tokens.mutedColor(MonarchTheme.colors),
        )
        return
    }
    val hidden = tokens.hiddenScopes(scopes, maxScopes)
    val more = labels.moreScopes?.invoke(hidden.size)
        ?: stringResource(R.string.monarch_apikeyrow_more_scopes, hidden.size.toString())
    val spoken = (listOf(texts.scopes) + scopes).joinToString(", ")
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(tokens.BadgeGap),
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        tokens.shownScopes(scopes, maxScopes).forEach {
            MonarchBadge(text = it, variant = MonarchBadgeVariant.Outline)
        }
        if (hidden.isNotEmpty()) MonarchBadge(text = more, variant = MonarchBadgeVariant.Outline)
    }
}

@Composable
private fun Progress(
    running: MonarchApiKeyRowAction?,
    failure: ApiKeyRowFailure?,
    texts: ApiKeyRowTexts,
    onRetry: (MonarchApiKeyRowAction) -> Unit,
) {
    val tokens = MonarchApiKeyRowTokens
    // One live region that stays in the tree, so starting an action is announced.
    Box(modifier = Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }) {
        if (running != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(tokens.BadgeGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(tokens.SpinnerSize)
                        .clearAndSetSemantics { },
                    color = MonarchTheme.colors.accentSolid,
                )
                MonarchText(
                    text = if (running == MonarchApiKeyRowAction.Rotate) texts.rotating else texts.revoking,
                    style = MonarchTextStyle.Caption,
                )
            }
        }
    }
    if (failure != null) {
        val fallback = if (failure.action == MonarchApiKeyRowAction.Rotate) texts.rotateFailed else texts.revokeFailed
        MonarchInlineErrorRetry(
            message = failure.message?.takeIf { it.isNotBlank() } ?: fallback,
            onRetry = { onRetry(failure.action) },
        )
    }
}

@Composable
private fun ActionsMenu(
    name: String,
    enabled: Boolean,
    onRotate: (() -> Unit)?,
    onRevoke: (() -> Unit)?,
    icon: (@Composable () -> Unit)?,
    texts: ApiKeyRowTexts,
    labels: MonarchApiKeyRowLabels,
    actions: (@Composable ColumnScope.(close: () -> Unit) -> Unit)?,
) {
    var expanded by remember { mutableStateOf(false) }
    val spoken = labels.actions?.invoke(name) ?: stringResource(R.string.monarch_apikeyrow_actions, name)
    val describe = Modifier.semantics { contentDescription = spoken }
    Box {
        if (icon != null) {
            MonarchIconButton(label = spoken, onClick = { expanded = true }, enabled = enabled) {
                Box(Modifier.clearAndSetSemantics { }) { icon() }
            }
        } else {
            MonarchButton(
                text = texts.menu,
                onClick = { expanded = true },
                modifier = describe,
                variant = MonarchButtonVariant.Secondary,
                size = MonarchButtonSize.Sm,
                enabled = enabled,
            )
        }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            onRotate?.let { rotate ->
                MonarchMenuItem(
                    text = texts.rotate,
                    onClick = {
                        expanded = false
                        rotate()
                    },
                )
            }
            onRevoke?.let { revoke ->
                MonarchMenuItem(
                    text = texts.revoke,
                    onClick = {
                        expanded = false
                        revoke()
                    },
                    destructive = true,
                )
            }
            actions?.invoke(this) { expanded = false }
        }
    }
}

/** The built-in strings that do not name anything: the app's replacement, else the resource. */
private class ApiKeyRowTexts(private val labels: MonarchApiKeyRowLabels, private val resources: Resources) {
    private fun pick(override: String?, @StringRes id: Int): String = override ?: resources.getString(id)

    val created: String get() = pick(labels.created, R.string.monarch_apikeyrow_created)
    val lastUsed: String get() = pick(labels.lastUsed, R.string.monarch_apikeyrow_last_used)
    val neverUsed: String get() = pick(labels.neverUsed, R.string.monarch_apikeyrow_never_used)
    val expires: String get() = pick(labels.expires, R.string.monarch_apikeyrow_expires)
    val scopes: String get() = pick(labels.scopes, R.string.monarch_apikeyrow_scopes)
    val noScopes: String get() = pick(labels.noScopes, R.string.monarch_apikeyrow_no_scopes)
    val menu: String get() = pick(labels.menu, R.string.monarch_apikeyrow_menu)
    val rotate: String get() = pick(labels.rotate, R.string.monarch_apikeyrow_rotate)
    val revoke: String get() = pick(labels.revoke, R.string.monarch_apikeyrow_revoke)
    val cancel: String get() = pick(labels.cancel, R.string.monarch_apikeyrow_cancel)
    val rotating: String get() = pick(labels.rotating, R.string.monarch_apikeyrow_rotating)
    val revoking: String get() = pick(labels.revoking, R.string.monarch_apikeyrow_revoking)
    val rotateFailed: String get() = pick(labels.rotateFailed, R.string.monarch_apikeyrow_rotate_failed)
    val revokeFailed: String get() = pick(labels.revokeFailed, R.string.monarch_apikeyrow_revoke_failed)

    fun status(status: MonarchApiKeyStatus): String = labels.status?.invoke(status) ?: resources.getString(
        when (status) {
            MonarchApiKeyStatus.Active -> R.string.monarch_apikeyrow_status_active
            MonarchApiKeyStatus.Expiring -> R.string.monarch_apikeyrow_status_expiring
            MonarchApiKeyStatus.Expired -> R.string.monarch_apikeyrow_status_expired
            MonarchApiKeyStatus.Revoked -> R.string.monarch_apikeyrow_status_revoked
        },
    )
}

@Composable
private fun apiKeyRowTexts(labels: MonarchApiKeyRowLabels): ApiKeyRowTexts {
    val resources = LocalResources.current
    return remember(labels, resources) { ApiKeyRowTexts(labels, resources) }
}

@Composable
private fun ApiKeyRowConfirmDialog(
    action: MonarchApiKeyRowAction,
    name: String,
    phrase: String?,
    texts: ApiKeyRowTexts,
    labels: MonarchApiKeyRowLabels,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    val rotate = action == MonarchApiKeyRowAction.Rotate
    val matches = MonarchApiKeyRowTokens.phraseMatches(phrase, typed)
    val title = if (rotate) {
        labels.rotateTitle?.invoke(name) ?: stringResource(R.string.monarch_apikeyrow_rotate_title, name)
    } else {
        labels.revokeTitle?.invoke(name) ?: stringResource(R.string.monarch_apikeyrow_revoke_title, name)
    }
    val message = if (rotate) {
        monarchString(labels.rotateMessage, R.string.monarch_apikeyrow_rotate_message)
    } else {
        monarchString(labels.revokeMessage, R.string.monarch_apikeyrow_revoke_message)
    }
    val confirm = if (rotate) {
        monarchString(labels.rotateConfirm, R.string.monarch_apikeyrow_rotate_confirm)
    } else {
        monarchString(labels.revokeConfirm, R.string.monarch_apikeyrow_revoke_confirm)
    }
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = title,
        description = message,
        actions = {
            MonarchButton(text = texts.cancel, onClick = onDismiss, variant = MonarchButtonVariant.Ghost)
            MonarchButton(
                text = confirm,
                onClick = { if (matches) onConfirm() },
                variant = if (rotate) MonarchButtonVariant.Primary else MonarchButtonVariant.Danger,
                enabled = matches,
            )
        },
        content = if (phrase != null) {
            {
                MonarchTypeToConfirmInput(
                    phrase = phrase,
                    value = typed,
                    onValueChange = { text -> typed = text },
                    label = labels.revokePhrasePrompt?.invoke(phrase)
                        ?: stringResource(R.string.monarch_apikeyrow_revoke_phrase_prompt, phrase),
                )
            }
        } else {
            null
        },
    )
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs. */
@Composable
private fun apiKeyRowClock(fixed: Long?, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed)

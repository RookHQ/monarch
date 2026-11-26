package com.rookhq.monarch.components.identity.passkeymanager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyStateSize
import com.rookhq.monarch.components.feedback.errorstate.MonarchErrorState
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.layout.sectionheader.MonarchSectionHeader
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchLiveStatus
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.rememberMonarchRelativeClock
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A person's passkeys, with when each was added and last used and a Synced
 * badge, and Add a passkey, Rename and Remove behind a confirmation.
 *
 * Monarch manages the list only. Creating a passkey stays in the app: in
 * [onAdd] it runs `androidx.credentials.CredentialManager.createCredential`
 * with the server's challenge, because the relying party and the server are
 * the app's.
 *
 * Built by Monarch in a `MonarchCard`: a `MonarchSectionHeader` with the add
 * button, one row per passkey with a `MonarchBadge` and `MonarchButton`s, the
 * Material 3 `AlertDialog` with a `MonarchTextField` to rename (the prompt
 * dialog) and with a danger button to confirm removal (the confirm dialog),
 * and `MonarchInlineNotice`s for the unsupported, cancelled and failed
 * states. Each passkey's text is one TalkBack element read as its name with
 * Synced, the detail and the times as its state; every outcome is announced
 * through a polite live region. The rows are not lazy: the list is short and
 * usually sits inside a scrolling settings screen. Times are relative and
 * follow the clock every minute unless [now] is given.
 *
 * @param passkeys the person's passkeys, in the order shown.
 * @param modifier the modifier of the card.
 * @param title replaces the built-in title "Passkeys".
 * @param description replaces the line under the title.
 * @param state ready, loading or failed. A ready, empty list shows the empty state.
 * @param retrying whether a retry is in progress, in the failed state.
 * @param supported whether this device can create passkeys. When false, Add is disabled and a warning says why.
 * @param lastSignInMethod passkeys are the person's only way to sign in, so removing the last one warns in the
 * confirmation.
 * @param now the reference time for relative times, in epoch milliseconds. Without it they follow the clock.
 * @param onRetry retries loading; shows "Try again" in the failed state.
 * @param onAdd runs the app's passkey registration and adds it to [passkeys]. Shows Add a passkey. Return `false`
 * when the person cancelled the system sheet (`CreateCredentialCancellationException`), which shows a neutral note;
 * throwing shows the failure.
 * @param onRename saves a new name, given the id and the trimmed name. Shows Rename. Throwing shows the failure.
 * @param onRemove removes a passkey after the confirmation, given its id. Shows Remove. Throwing shows the failure.
 * @param passkeyIcon an icon at the start of each row, such as a key, hidden from TalkBack. Monarch bundles no icons.
 * @param labels replacements for the other built-in strings.
 */
@Composable
public fun MonarchPasskeyManager(
    passkeys: List<MonarchPasskey>,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    state: MonarchPasskeyManagerState = MonarchPasskeyManagerState.Ready,
    retrying: Boolean = false,
    supported: Boolean = true,
    lastSignInMethod: Boolean = false,
    now: Long? = null,
    onRetry: (() -> Unit)? = null,
    onAdd: (suspend () -> Boolean)? = null,
    onRename: (suspend (String, String) -> Unit)? = null,
    onRemove: (suspend (String) -> Unit)? = null,
    passkeyIcon: (@Composable () -> Unit)? = null,
    labels: MonarchPasskeyManagerLabels = MonarchPasskeyManagerLabels(),
) {
    val tokens = MonarchPasskeyManagerTokens
    val tasks = remember { PasskeyManagerTasks() }
    val texts = passkeyManagerTexts(labels)
    val scope = rememberCoroutineScope()
    var renaming by remember { mutableStateOf<MonarchPasskey?>(null) }
    var removing by remember { mutableStateOf<MonarchPasskey?>(null) }
    val clock = passkeyManagerClock(
        now,
        state == MonarchPasskeyManagerState.Ready && passkeys.isNotEmpty(),
        passkeys.flatMap { listOf(it.createdMillis, it.lastUsedMillis) },
    )
    MonarchCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            PasskeyHeader(
                title = title,
                description = description,
                supported = supported,
                adding = tasks.adding,
                onAdd = onAdd?.let { add -> { tasks.add(scope, add, texts) } },
                labels = labels,
            )
            tasks.notice?.let { MonarchInlineNotice(message = it.text, tone = it.tone) }
            when {
                state == MonarchPasskeyManagerState.Loading -> MonarchLoadingState(
                    label = monarchString(labels.loading, R.string.monarch_passkeymanager_loading),
                    variant = MonarchLoadingStateVariant.Rows,
                    rows = 2,
                )
                state == MonarchPasskeyManagerState.Failed -> MonarchErrorState(
                    title = monarchString(labels.errorTitle, R.string.monarch_passkeymanager_error_title),
                    size = MonarchEmptyStateSize.Sm,
                    retrying = retrying,
                    onRetry = onRetry,
                )
                passkeys.isEmpty() -> MonarchEmptyState(
                    title = monarchString(labels.emptyTitle, R.string.monarch_passkeymanager_empty_title),
                    size = MonarchEmptyStateSize.Sm,
                    message = monarchString(labels.emptyMessage, R.string.monarch_passkeymanager_empty_message),
                )
                else -> PasskeyList(
                    passkeys = passkeys,
                    now = clock,
                    busyId = tasks.busyId,
                    onRename = onRename?.let { { passkey: MonarchPasskey -> renaming = passkey } },
                    onRemove = onRemove?.let { { passkey: MonarchPasskey -> removing = passkey } },
                    passkeyIcon = passkeyIcon,
                    labels = labels,
                )
            }
        }
        // Always present, so every outcome is announced once as a change of this live region.
        MonarchLiveStatus(text = tasks.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
    val renamed = renaming
    if (renamed != null && onRename != null) {
        RenameDialog(
            passkey = renamed,
            onSave = { name ->
                renaming = null
                tasks.rename(scope, renamed, name, onRename, texts)
            },
            onDismiss = { renaming = null },
            labels = labels,
        )
    }
    RemoveConfirmation(
        passkey = removing,
        warnsLastMethod = tokens.warnsLastMethod(lastSignInMethod, passkeys.size),
        onConfirm = { passkey ->
            removing = null
            onRemove?.let { tasks.remove(scope, passkey, it, texts) }
        },
        onDismiss = { removing = null },
        labels = labels,
    )
}

/** The title, the description, Add a passkey, and the warning when the device cannot create passkeys. */
@Composable
private fun PasskeyHeader(
    title: String?,
    description: String?,
    supported: Boolean,
    adding: Boolean,
    onAdd: (() -> Unit)?,
    labels: MonarchPasskeyManagerLabels,
) {
    MonarchSectionHeader(
        title = title ?: stringResource(R.string.monarch_passkeymanager_title),
        description = description ?: stringResource(R.string.monarch_passkeymanager_description),
    )
    // Below the text, not beside it: the description keeps the full width instead of wrapping in a narrow column.
    if (onAdd != null) {
        MonarchButton(
            text = monarchString(labels.add, R.string.monarch_passkeymanager_add),
            onClick = onAdd,
            variant = MonarchButtonVariant.Primary,
            size = MonarchButtonSize.Sm,
            enabled = supported,
            loading = adding,
        )
    }
    if (!supported && onAdd != null) {
        MonarchInlineNotice(
            message = monarchString(labels.unsupported, R.string.monarch_passkeymanager_unsupported),
            tone = MonarchTone.Warning,
        )
    }
}

@Composable
private fun PasskeyList(
    passkeys: List<MonarchPasskey>,
    now: Long,
    busyId: String?,
    onRename: ((MonarchPasskey) -> Unit)?,
    onRemove: ((MonarchPasskey) -> Unit)?,
    passkeyIcon: (@Composable () -> Unit)?,
    labels: MonarchPasskeyManagerLabels,
) {
    val listLabel = monarchString(labels.listLabel, R.string.monarch_passkeymanager_list_label)
    Column(
        modifier = Modifier.semantics {
            contentDescription = listLabel
            isTraversalGroup = true
        },
        verticalArrangement = Arrangement.spacedBy(MonarchPasskeyManagerTokens.SectionGap),
    ) {
        passkeys.forEachIndexed { index, passkey ->
            PasskeyRow(
                passkey = passkey,
                now = now,
                busy = busyId == passkey.id,
                actionsEnabled = busyId == null,
                onRename = onRename?.let { { it(passkey) } },
                onRemove = onRemove?.let { { it(passkey) } },
                passkeyIcon = passkeyIcon,
                labels = labels,
            )
        }
    }
}

@Composable
private fun PasskeyRow(
    passkey: MonarchPasskey,
    now: Long,
    busy: Boolean,
    actionsEnabled: Boolean,
    onRename: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    passkeyIcon: (@Composable () -> Unit)?,
    labels: MonarchPasskeyManagerLabels,
) {
    val tokens = MonarchPasskeyManagerTokens
    val colors = MonarchTheme.colors
    // As the web: a tinted tile per passkey, the key in a small tile at the start, the actions at the end.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bgSubtle, MonarchTheme.shapes.md)
            .padding(tokens.RowGap),
        horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (passkeyIcon != null) {
            CompositionLocalProvider(LocalContentColor provides tokens.iconColor(colors)) {
                Box(
                    modifier = Modifier
                        .size(tokens.IconTile)
                        .background(colors.bgMuted, MonarchTheme.shapes.md)
                        .clearAndSetSemantics { },
                    contentAlignment = Alignment.Center,
                ) { passkeyIcon() }
            }
        }
        Column(Modifier.weight(1f)) { PasskeyText(passkey, now, labels) }
        if (onRename != null) {
            val renameName = labels.renameLabel?.invoke(passkey.name)
                ?: stringResource(R.string.monarch_passkeymanager_rename_label, passkey.name)
            MonarchButton(
                text = monarchString(labels.rename, R.string.monarch_passkeymanager_rename),
                onClick = onRename,
                modifier = Modifier.semantics { contentDescription = renameName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = actionsEnabled,
            )
        }
        if (onRemove != null) {
            val removeName = labels.removeLabel?.invoke(passkey.name)
                ?: stringResource(R.string.monarch_passkeymanager_remove_label, passkey.name)
            MonarchButton(
                text = monarchString(labels.remove, R.string.monarch_passkeymanager_remove),
                onClick = onRemove,
                modifier = Modifier.semantics { contentDescription = removeName },
                variant = MonarchButtonVariant.Ghost,
                size = MonarchButtonSize.Sm,
                enabled = actionsEnabled || busy,
                loading = busy,
            )
        }
    }
}

/** The name, Synced, the detail and the times of a passkey, read as one element. */
@Composable
private fun PasskeyText(passkey: MonarchPasskey, now: Long, labels: MonarchPasskeyManagerLabels) {
    val tokens = MonarchPasskeyManagerTokens
    val colors = MonarchTheme.colors
    val synced = monarchString(labels.synced, R.string.monarch_passkeymanager_synced)
    val created = monarchString(labels.created, R.string.monarch_passkeymanager_created)
    val lastUsed = monarchString(labels.lastUsed, R.string.monarch_passkeymanager_last_used)
    val times = tokens.line(
        passkey.createdMillis?.let { "$created ${tokens.relativeTime(it, now)}" },
        passkey.lastUsedMillis?.let { "$lastUsed ${tokens.relativeTime(it, now)}" }
            ?: monarchString(labels.neverUsed, R.string.monarch_passkeymanager_never_used),
    )
    val badges = listOfNotNull(synced.takeIf { passkey.synced })
    Column(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = passkey.name
            stateDescription = tokens.spokenDetails(badges, passkey.detail, times)
        },
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.RowGap),
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = passkey.name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            if (passkey.synced) MonarchBadge(text = synced, tone = MonarchTone.Info)
        }
        listOfNotNull(passkey.detail, times).forEach { line ->
            Text(text = line, style = MonarchTheme.typography.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun RenameDialog(
    passkey: MonarchPasskey,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchPasskeyManagerLabels,
) {
    var draft by remember(passkey.id) { mutableStateOf(passkey.name) }
    val canSave = MonarchPasskeyManagerTokens.canSave(draft, passkey.name)
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = monarchString(labels.renameTitle, R.string.monarch_passkeymanager_rename_title),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_passkeymanager_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.save, R.string.monarch_passkeymanager_save),
                onClick = { onSave(draft.trim()) },
                variant = MonarchButtonVariant.Primary,
                enabled = canSave,
            )
        },
        content = {
            MonarchTextField(
                value = draft,
                onValueChange = { draft = it },
                label = monarchString(labels.nameField, R.string.monarch_passkeymanager_name_field),
            )
        },
    )
}

/** The removal confirmation of [passkey], when there is one. */
@Composable
private fun RemoveConfirmation(
    passkey: MonarchPasskey?,
    warnsLastMethod: Boolean,
    onConfirm: (MonarchPasskey) -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchPasskeyManagerLabels,
) {
    if (passkey != null) RemoveDialog(passkey, warnsLastMethod, { onConfirm(passkey) }, onDismiss, labels)
}

@Composable
private fun RemoveDialog(
    passkey: MonarchPasskey,
    warnsLastMethod: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    labels: MonarchPasskeyManagerLabels,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = labels.confirmTitle?.invoke(passkey.name)
            ?: stringResource(R.string.monarch_passkeymanager_confirm_title, passkey.name),
        description = monarchString(labels.confirmMessage, R.string.monarch_passkeymanager_confirm_message),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_passkeymanager_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.confirmRemove, R.string.monarch_passkeymanager_confirm_remove),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
        content = if (warnsLastMethod) {
            {
                Text(
                    text = monarchString(labels.lastMethodWarning, R.string.monarch_passkeymanager_last_method_warning),
                    style = MonarchTheme.typography.body,
                    color = MonarchPasskeyManagerTokens.warningColor(MonarchTheme.colors),
                )
            }
        } else {
            null
        },
    )
}

/** A notice under the header: its text and tone. */
private class PasskeyManagerNotice(val text: String, val tone: MonarchTone)

/** The resolved built-in strings the tasks report. */
private class PasskeyManagerTexts(
    val cancelled: String,
    val addFailed: String,
    val added: String,
    val renamed: (String) -> String,
    val removed: (String) -> String,
    val renameFailed: String,
    val removeFailed: (String) -> String,
)

@Composable
private fun passkeyManagerTexts(labels: MonarchPasskeyManagerLabels): PasskeyManagerTexts {
    val renamed = stringResource(R.string.monarch_passkeymanager_renamed)
    val removed = stringResource(R.string.monarch_passkeymanager_removed)
    val removeFailed = stringResource(R.string.monarch_passkeymanager_remove_failed)
    return PasskeyManagerTexts(
        cancelled = monarchString(labels.cancelled, R.string.monarch_passkeymanager_cancelled),
        addFailed = monarchString(labels.addFailed, R.string.monarch_passkeymanager_add_failed),
        added = monarchString(labels.added, R.string.monarch_passkeymanager_added),
        renamed = labels.renamed ?: { renamed.format(it) },
        removed = labels.removed ?: { removed.format(it) },
        renameFailed = monarchString(labels.renameFailed, R.string.monarch_passkeymanager_rename_failed),
        removeFailed = labels.removeFailed ?: { removeFailed.format(it) },
    )
}

/** What runs: adding, the passkey being renamed or removed, the notice and the last announcement. */
private class PasskeyManagerTasks {
    var adding by mutableStateOf(false)
    var busyId by mutableStateOf<String?>(null)
    var notice by mutableStateOf<PasskeyManagerNotice?>(null)
    var announcement by mutableStateOf("")

    fun add(scope: CoroutineScope, onAdd: suspend () -> Boolean, texts: PasskeyManagerTexts) {
        if (adding) return
        adding = true
        notice = null
        scope.launch {
            val outcome = runCatching { onAdd() }
            adding = false
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            when {
                error != null -> report(texts.addFailed, MonarchTone.Danger)
                outcome.getOrNull() == false -> report(texts.cancelled, MonarchTone.Neutral)
                else -> announcement = texts.added
            }
        }
    }

    fun rename(
        scope: CoroutineScope,
        passkey: MonarchPasskey,
        name: String,
        onRename: suspend (String, String) -> Unit,
        texts: PasskeyManagerTexts,
    ) = runForRow(scope, passkey, { onRename(passkey.id, name) }, texts.renamed(name), texts.renameFailed)

    fun remove(
        scope: CoroutineScope,
        passkey: MonarchPasskey,
        onRemove: suspend (String) -> Unit,
        texts: PasskeyManagerTexts,
    ) = runForRow(
        scope = scope,
        passkey = passkey,
        task = { onRemove(passkey.id) },
        done = texts.removed(passkey.name),
        failed = texts.removeFailed(passkey.name),
    )

    private fun runForRow(
        scope: CoroutineScope,
        passkey: MonarchPasskey,
        task: suspend () -> Unit,
        done: String,
        failed: String,
    ) {
        if (busyId != null) return
        busyId = passkey.id
        notice = null
        scope.launch {
            val outcome = runCatching { task() }
            busyId = null
            val error = outcome.exceptionOrNull()
            if (error is CancellationException) throw error
            if (error == null) announcement = done else report(failed, MonarchTone.Danger)
        }
    }

    private fun report(text: String, tone: MonarchTone) {
        notice = PasskeyManagerNotice(text, tone)
        announcement = text
    }
}

/** The reference time: [fixed], or the clock, redrawn as often as the freshest of [times] needs while [running]. */
@Composable
private fun passkeyManagerClock(fixed: Long?, running: Boolean, times: List<Long?>): Long =
    rememberMonarchRelativeClock(times, fixed = fixed, running = running)

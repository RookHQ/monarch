package dev.monarchui.components.technical.keycreateddialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.actions.copyfield.MonarchCopyField
import dev.monarchui.components.feedback.banner.MonarchBanner
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.components.overlays.dialog.MonarchDialog
import dev.monarchui.components.technical.secretfield.MonarchSecretField
import dev.monarchui.components.technical.secretfield.MonarchSecretFieldLabels
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * A dialog that shows a newly created secret exactly once: the value with a
 * copy button, a "store it now" warning, and an acknowledgement the person
 * must tick before the dialog can close.
 *
 * Built on `MonarchDialog`. The value is a read-only
 * `MonarchCopyField`, or a `MonarchSecretField` with a reveal toggle when
 * [masked]; the copied clip is marked sensitive for a secret. The notice is a
 * `MonarchBanner` and the acknowledgement a `MonarchCheckboxRow`. While a
 * secret is not acknowledged, Back and taps outside do not close the dialog,
 * and Done shows and announces why instead of closing. The dialog is shown
 * while the app composes it: show it right after the server returns the new
 * value and stop composing it in [onDone].
 *
 * Monarch does not set `FLAG_SECURE`; a screen that shows secrets sets it on
 * its window.
 *
 * @param secret the value to show once.
 * @param onDone called when Done or Back closes the dialog after the value was acknowledged (or at once for a
 * public value). Stop showing the dialog here.
 * @param modifier the modifier of the dialog.
 * @param kind a secret seen once, or a public value that can be viewed again.
 * @param label what the value is, such as "Signing secret": the field's label (unless [name] is given), part
 * of the copy button's name and of the acknowledgement. "API key" when `null`.
 * @param name the name the person gave it, shown as the field's label instead of [label].
 * @param title replaces the title "Copy your new key".
 * @param description text under the title.
 * @param requiresAcknowledgement whether the checkbox must be ticked before closing. By default only for a
 * secret.
 * @param masked shows the value as dots with a reveal toggle; copy works without revealing.
 * @param multiline wraps the whole value over several lines, for certificates and private keys. Ignored when
 * masked.
 * @param note extra text under the value, such as when an old key stops working.
 * @param onCopy called with the copied text after each copy.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchKeyCreatedDialog(
    secret: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    kind: MonarchKeyCreatedDialogKind = MonarchKeyCreatedDialogKind.Secret,
    label: String? = null,
    name: String? = null,
    title: String? = null,
    description: String? = null,
    requiresAcknowledgement: Boolean? = null,
    masked: Boolean = false,
    multiline: Boolean = false,
    note: String? = null,
    onCopy: ((String) -> Unit)? = null,
    labels: MonarchKeyCreatedDialogLabels = MonarchKeyCreatedDialogLabels(),
) {
    val tokens = MonarchKeyCreatedDialogTokens
    val needsAcknowledgement = tokens.needsAcknowledgement(kind, requiresAcknowledgement)
    // A new secret starts unacknowledged.
    var acknowledged by rememberSaveable(secret) { mutableStateOf(false) }
    var nudged by rememberSaveable(secret) { mutableStateOf(false) }
    val canClose = tokens.canClose(needsAcknowledgement, acknowledged)
    val valueLabel = label ?: monarchString(labels.label, R.string.monarch_keycreateddialog_label)
    val nudge = monarchString(labels.acknowledgeRequired, R.string.monarch_keycreateddialog_acknowledge_required)
    MonarchDialog(
        onDismissRequest = { if (canClose) onDone() },
        title = title ?: monarchString(labels.title, R.string.monarch_keycreateddialog_title),
        modifier = modifier,
        description = description,
        dismissible = canClose,
        // A stray tap must not close the only view of the key; back closes it once acknowledged.
        dismissOnClickOutside = false,
        actions = {
            MonarchButton(
                text = monarchString(labels.done, R.string.monarch_keycreateddialog_done),
                onClick = { if (canClose) onDone() else nudged = true },
                variant = MonarchButtonVariant.Primary,
            )
        },
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
                Notice(kind, labels)
                Value(secret, name ?: valueLabel, kind, masked, multiline, onCopy, labels)
                note?.let { MonarchText(text = it, style = MonarchTextStyle.Caption) }
                if (needsAcknowledgement) {
                    Column(verticalArrangement = Arrangement.spacedBy(tokens.NudgeGap)) {
                        MonarchCheckboxRow(
                            checked = acknowledged,
                            onCheckedChange = {
                                acknowledged = it
                                if (it) nudged = false
                            },
                            label = labels.acknowledge?.invoke(valueLabel)
                                ?: stringResource(R.string.monarch_keycreateddialog_acknowledge, valueLabel),
                            modifier = Modifier.semantics { if (nudged) error(nudge) },
                        )
                        if (nudged) {
                            MonarchText(
                                text = nudge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { liveRegion = LiveRegionMode.Assertive },
                                style = MonarchTextStyle.Caption,
                                color = tokens.nudgeColor(MonarchTheme.colors),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun Notice(kind: MonarchKeyCreatedDialogKind, labels: MonarchKeyCreatedDialogLabels) {
    val secret = kind == MonarchKeyCreatedDialogKind.Secret
    MonarchBanner(
        message = if (secret) {
            monarchString(labels.secretNotice, R.string.monarch_keycreateddialog_secret_notice)
        } else {
            monarchString(labels.publicNotice, R.string.monarch_keycreateddialog_public_notice)
        },
        tone = MonarchKeyCreatedDialogTokens.noticeTone(kind),
        title = if (secret) {
            monarchString(labels.secretNoticeTitle, R.string.monarch_keycreateddialog_secret_notice_title)
        } else {
            monarchString(labels.publicNoticeTitle, R.string.monarch_keycreateddialog_public_notice_title)
        },
    )
}

@Composable
private fun Value(
    secret: String,
    fieldLabel: String,
    kind: MonarchKeyCreatedDialogKind,
    masked: Boolean,
    multiline: Boolean,
    onCopy: ((String) -> Unit)?,
    labels: MonarchKeyCreatedDialogLabels,
) {
    if (masked) {
        MonarchSecretField(
            value = secret,
            label = fieldLabel,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            copyable = true,
            onCopy = onCopy,
            labels = MonarchSecretFieldLabels(copy = labels.copy, copied = labels.copied),
        )
    } else {
        MonarchCopyField(
            value = secret,
            label = fieldLabel,
            modifier = Modifier.fillMaxWidth(),
            // A long key wraps in its field on a phone instead of being cut off at the Copy button.
            multiline = multiline || secret.length > 20,
            sensitive = MonarchKeyCreatedDialogTokens.sensitive(kind),
            copyVariant = MonarchButtonVariant.Primary,
            onCopy = onCopy,
            copyLabel = labels.copy,
            copiedLabel = labels.copied,
        )
    }
}

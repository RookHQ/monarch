package com.rookhq.monarch.components.identity.memberrow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.iconbutton.MonarchIconButton
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.components.identity.rolepicker.MonarchRoleOption
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePicker
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePickerLayout
import com.rookhq.monarch.components.identity.rolepicker.MonarchRolePickerTokens
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/** The role picker; the chosen role shows while the change runs and until the app's role changes. */
@Composable
internal fun MemberRowRolePicker(
    name: String,
    role: String?,
    roles: List<MonarchRoleOption<String>>,
    enabled: Boolean,
    onRoleChange: suspend (String) -> Unit,
    actions: MemberRowActionState,
    labels: MonarchMemberRowLabels,
    modifier: Modifier,
    showLabel: Boolean = true,
) {
    var pending by remember(role) { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val label = labels.roleLabel?.invoke(name) ?: stringResource(R.string.monarch_memberrow_role_label, name)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(MonarchMemberRowTokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val onSelect = { next: String ->
            if (!busy && next != (pending ?: role)) {
                pending = next
                actions.run(
                    block = { onRoleChange(next) },
                    busy = { busy = it },
                    onFailure = { pending = null },
                )
            }
        }
        val placeholder = monarchString(labels.chooseRole, R.string.monarch_memberrow_choose_role)
        if (showLabel) {
            MonarchRolePicker(
                roles = roles,
                selected = pending ?: role,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
                layout = MonarchRolePickerLayout.Compact,
                enabled = enabled && !busy,
                label = label,
                placeholder = placeholder,
            )
        } else {
            // A wide row: the select alone, as on the web, named for TalkBack by the label.
            MonarchSelect(
                options = roles.map {
                    MonarchSelectOption(
                        it.value,
                        it.title,
                        description = MonarchRolePickerTokens.details(it),
                        enabled = it.enabled,
                    )
                },
                selected = pending ?: role,
                onSelect = onSelect,
                modifier = Modifier.weight(1f),
                placeholder = placeholder,
                enabled = enabled && !busy,
                contentDescription = label,
            )
        }
        if (busy) MonarchProgress(variant = MonarchProgressVariant.Circular)
    }
}

/** Resend invite, busy while it runs; [MemberRowControls.resend] shows "Invite sent again." after success. */
@Composable
internal fun MemberRowResend(enabled: Boolean, controls: MemberRowControls, labels: MonarchMemberRowLabels) {
    MonarchButton(
        text = monarchString(labels.resend, R.string.monarch_memberrow_resend),
        onClick = controls::resend,
        modifier = Modifier.semantics { contentDescription = controls.texts.resendName },
        size = MonarchButtonSize.Sm,
        enabled = enabled,
        loading = controls.resending,
    )
}

/** Remove or Revoke invite, behind a confirmation, busy while it runs. */
@Composable
internal fun MemberRowRemoveButton(
    enabled: Boolean,
    icon: (@Composable () -> Unit)?,
    controls: MemberRowControls,
) {
    val texts = controls.texts.remove
    if (icon != null) {
        // As the web: a quiet trash mark, named for TalkBack by the spoken action.
        MonarchIconButton(
            label = texts.spoken,
            onClick = controls::askRemove,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            loading = controls.removing,
        ) { icon() }
        return
    }
    MonarchButton(
        text = texts.confirm,
        onClick = controls::askRemove,
        modifier = Modifier.semantics { contentDescription = texts.spoken },
        variant = if (texts.revoking) MonarchButtonVariant.DangerSubtle else MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
        loading = controls.removing,
    )
}

/** The confirmation of Remove or Revoke invite, from the button, a swipe or a TalkBack action. */
@Composable
internal fun MemberRowRemoveDialog(
    consequences: List<String>,
    controls: MemberRowControls,
    labels: MonarchMemberRowLabels,
) {
    if (controls.confirming) {
        val texts = controls.texts.remove
        MonarchDialog(
            onDismissRequest = { controls.confirming = false },
            title = texts.title,
            description = texts.description,
            actions = {
                MonarchButton(
                    text = monarchString(labels.cancel, R.string.monarch_memberrow_cancel),
                    onClick = { controls.confirming = false },
                    variant = MonarchButtonVariant.Ghost,
                )
                MonarchButton(text = texts.confirm, onClick = controls::remove, variant = MonarchButtonVariant.Danger)
            },
            content = if (consequences.isEmpty()) {
                null
            } else {
                {
                    Column(verticalArrangement = Arrangement.spacedBy(MonarchMemberRowTokens.ItemGap)) {
                        consequences.forEach {
                            Text(text = it, style = MonarchTheme.typography.body, color = MonarchTheme.colors.textMuted)
                        }
                    }
                }
            },
        )
    }
}

/** The words of the remove button and its confirmation, for a member or an invitation. */
internal class RemoveTexts(
    val spoken: String,
    val confirm: String,
    val title: String,
    val description: String,
    /** True for an invitation: the button is Revoke, in the danger tone as on the web. */
    val revoking: Boolean = false,
)

@Composable
internal fun removeTexts(name: String, invited: Boolean, labels: MonarchMemberRowLabels): RemoveTexts = if (invited) {
    RemoveTexts(
        spoken = labels.revoke?.invoke(name) ?: stringResource(R.string.monarch_memberrow_revoke, name),
        confirm = monarchString(labels.revokeConfirm, R.string.monarch_memberrow_revoke_confirm),
        title = labels.revokeTitle?.invoke(name) ?: stringResource(R.string.monarch_memberrow_revoke_title, name),
        description = monarchString(labels.revokeDescription, R.string.monarch_memberrow_revoke_description),
        revoking = true,
    )
} else {
    RemoveTexts(
        spoken = labels.remove?.invoke(name) ?: stringResource(R.string.monarch_memberrow_remove, name),
        confirm = monarchString(labels.removeConfirm, R.string.monarch_memberrow_remove_confirm),
        title = labels.removeTitle?.invoke(name) ?: stringResource(R.string.monarch_memberrow_remove_title, name),
        description = monarchString(labels.removeDescription, R.string.monarch_memberrow_remove_description),
    )
}

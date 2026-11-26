package com.rookhq.monarch.components.technical.secretfield

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.rookhq.monarch.R
import com.rookhq.monarch.accessibility.monarchFocusRing
import com.rookhq.monarch.accessibility.monarchMinimumTouchTarget
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.actions.copybutton.MonarchCopyButton
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A masked secret, such as an API key, a token or a signing secret, with a
 * toggle that shows it as text and an optional copy button.
 *
 * Built on `MonarchTextField` with
 * `PasswordVisualTransformation`, the password keyboard without suggestions,
 * so the keyboard does not learn the secret, and a `MonarchCopyButton` that
 * marks the clip as sensitive, so Android 13 and later hide it in the
 * clipboard preview. Copy works while the secret stays masked.
 *
 * For a secret the server stores but never sends back, set [saved]: the field
 * stays empty, says that a value is saved, and anything typed replaces it.
 * Reveal and copy are off while the field is empty.
 *
 * Monarch does not set `FLAG_SECURE`. A screen that shows secrets should set
 * `WindowManager.LayoutParams.FLAG_SECURE` on its window, so the secret does
 * not appear in screenshots, screen recordings or the recent apps screen.
 *
 * Monarch bundles no icons, so the toggle shows "Show" or "Hide" and the copy
 * button "Copy" unless [revealIcon] and [copyIcon] give them icons.
 *
 * @param value the secret.
 * @param label what the secret is, such as "API key". The field's visible label and part of the names of the
 * reveal and copy buttons.
 * @param onValueChange called with the new text when the user edits the field.
 * @param modifier the modifier of the field.
 * @param placeholder shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText the error message. When not `null` the field is in the error state and the message replaces
 * [helperText] and the saved description.
 * @param enabled whether the user can edit the field and use its buttons.
 * @param readOnly whether the secret can be revealed and copied but not edited.
 * @param saved a value is saved but not sent to the app. While the field is empty it shows the saved
 * placeholder and description; the app decides what an empty field means when it saves.
 * @param revealable shows the reveal toggle.
 * @param copyable shows the copy button.
 * @param revealed whether the secret is shown as text. `null` lets the field keep this itself, starting masked.
 * @param onRevealedChange called with the new state when the user uses the toggle.
 * @param onCopy called with the copied text after each copy.
 * @param revealIcon replaces the text of the toggle, for example an eye icon. It receives whether the secret is
 * shown. It is decoration: the toggle keeps its spoken name.
 * @param copyIcon makes the copy button an icon button with this icon, for example a copy `MonarchIcon` with a
 * `null` content description.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
public fun MonarchSecretField(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    saved: Boolean = false,
    revealable: Boolean = true,
    copyable: Boolean = false,
    revealed: Boolean? = null,
    onRevealedChange: ((Boolean) -> Unit)? = null,
    onCopy: ((String) -> Unit)? = null,
    revealIcon: (@Composable (revealed: Boolean) -> Unit)? = null,
    copyIcon: (@Composable () -> Unit)? = null,
    labels: MonarchSecretFieldLabels = MonarchSecretFieldLabels(),
    interactionSource: MutableInteractionSource? = null,
) {
    val tokens = MonarchSecretFieldTokens
    var ownRevealed by rememberSaveable { mutableStateOf(false) }
    val shown = revealed ?: ownRevealed
    val showsSaved = tokens.showsSaved(saved, value)
    val actionsEnabled = tokens.actionsEnabled(enabled, value)
    val savedPlaceholder = monarchString(labels.savedPlaceholder, R.string.monarch_secretfield_saved_placeholder)
    val savedDescription = monarchString(labels.savedDescription, R.string.monarch_secretfield_saved_description)
    MonarchTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label,
        placeholder = if (showsSaved) savedPlaceholder else placeholder,
        helperText = if (showsSaved) savedDescription else helperText,
        errorText = errorText?.takeIf { it.isNotEmpty() },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = true,
        trailingIcon = if (revealable || copyable) {
            {
                Row(
                    modifier = Modifier.padding(end = tokens.EndPadding),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (revealable) {
                        RevealToggle(
                            revealed = shown,
                            onRevealedChange = { next ->
                                if (revealed == null) ownRevealed = next
                                onRevealedChange?.invoke(next)
                            },
                            enabled = actionsEnabled,
                            name = labels.show?.invoke(label)
                                ?: stringResource(R.string.monarch_secretfield_show_name, label),
                            icon = revealIcon,
                            labels = labels,
                        )
                    }
                    if (copyable) CopyAction(value, label, actionsEnabled, onCopy, copyIcon, labels)
                }
            }
        } else {
            null
        },
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
        ),
        interactionSource = interactionSource,
    )
}

@Composable
private fun CopyAction(
    value: String,
    label: String,
    enabled: Boolean,
    onCopy: ((String) -> Unit)?,
    icon: (@Composable () -> Unit)?,
    labels: MonarchSecretFieldLabels,
) {
    if (icon != null) {
        MonarchCopyButton(
            value = value,
            icon = icon,
            enabled = enabled,
            sensitive = true,
            target = label,
            onCopy = onCopy,
            label = labels.copy,
            copiedLabel = labels.copied,
            copiedAnnouncement = labels.copiedAnnouncement,
        )
    } else {
        MonarchCopyButton(
            value = value,
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = enabled,
            sensitive = true,
            target = label,
            onCopy = onCopy,
            label = labels.copy,
            copiedLabel = labels.copied,
            copiedAnnouncement = labels.copiedAnnouncement,
        )
    }
}

@Composable
private fun RevealToggle(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    enabled: Boolean,
    name: String,
    icon: (@Composable (Boolean) -> Unit)?,
    labels: MonarchSecretFieldLabels,
) {
    val text = if (revealed) {
        monarchString(labels.hideText, R.string.monarch_secretfield_hide)
    } else {
        monarchString(labels.showText, R.string.monarch_secretfield_show)
    }
    val shape = MonarchTheme.shapes.md
    Box(
        modifier = Modifier
            .monarchMinimumTouchTarget()
            .monarchFocusRing(shape)
            .clip(shape)
            .semantics { contentDescription = name }
            .toggleable(value = revealed, enabled = enabled, role = Role.Checkbox, onValueChange = onRevealedChange)
            .padding(horizontal = MonarchSecretFieldTokens.TogglePadding),
        contentAlignment = Alignment.Center,
    ) {
        // The visible text changes with the state; the spoken name and the on or off state do not.
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            if (icon != null) {
                icon(revealed)
            } else {
                Text(
                    text = text,
                    style = MonarchTheme.typography.label,
                    color = MonarchSecretFieldTokens.toggleColor(enabled, MonarchTheme.colors),
                )
            }
        }
    }
}

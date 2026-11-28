package com.rookhq.monarch.components.forms.passwordinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.rookhq.monarch.R
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyph
import com.rookhq.monarch.components.forms.textfield.MonarchFieldGlyphIcon
import com.rookhq.monarch.components.forms.textfield.MonarchFieldIconToggle
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme

/**
 * A password field with a toggle that shows the password as text.
 *
 * Built on `MonarchTextField` (the Material 3 `OutlinedTextField`) with
 * `PasswordVisualTransformation`, the password keyboard without suggestions,
 * and the autofill content type of [purpose], so password managers fill or
 * generate the right password. The toggle is at the end of the field; its
 * spoken name stays "Show password" and its on or off state says whether the
 * password is shown.
 *
 * TalkBack treats the field as a password field in both states: Compose 1.12
 * keeps the password flag of a text field from its first composition.
 *
 * Monarch bundles no icons, so the toggle shows "Show" or "Hide" unless
 * [revealIcon] gives it an eye icon.
 *
 * @param value the password.
 * @param onValueChange called with the new text when the user edits the field.
 * @param modifier the modifier of the field.
 * @param label the visible label.
 * @param placeholder shown while the field is empty and focused.
 * @param helperText guidance below the field.
 * @param errorText the error message. When not `null` the field is in the error state and the message replaces
 * [helperText].
 * @param purpose what the password is for, which sets the autofill content type.
 * @param enabled whether the user can edit the field and use the toggle.
 * @param revealed whether the password is shown as text. `null` lets the field keep this itself, starting hidden.
 * @param onRevealedChange called with the new state when the user uses the toggle.
 * @param revealIcon replaces the eye icon of the toggle. It receives whether the password is
 * shown. It is decoration: the toggle keeps its spoken name.
 * @param keyboardActions what happens when the user triggers the IME action.
 * @param labels replacements for the built-in strings.
 * @param interactionSource the source of the focus and press interactions of the field.
 * @param required shows the required mark after the label and adds "required" to the spoken name.
 * @param labelAction an action at the end of the label row, such as a "Forgot password?" link.
 */
@Composable
public fun MonarchPasswordInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    helperText: String? = null,
    errorText: String? = null,
    purpose: MonarchPasswordInputPurpose = MonarchPasswordInputPurpose.Current,
    enabled: Boolean = true,
    revealed: Boolean? = null,
    onRevealedChange: ((Boolean) -> Unit)? = null,
    revealIcon: (@Composable (revealed: Boolean) -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    labels: MonarchPasswordInputLabels = MonarchPasswordInputLabels(),
    interactionSource: MutableInteractionSource? = null,
    required: Boolean = false,
    labelAction: (@Composable () -> Unit)? = null,
) {
    var ownRevealed by rememberSaveable { mutableStateOf(false) }
    val shown = revealed ?: ownRevealed
    val autofill = MonarchPasswordInputTokens.contentType(purpose)
    MonarchTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.semantics { contentType = autofill },
        label = label,
        placeholder = placeholder,
        helperText = helperText,
        errorText = errorText,
        enabled = enabled,
        singleLine = true,
        trailingIcon = {
            RevealToggle(
                revealed = shown,
                onRevealedChange = { next ->
                    if (revealed == null) ownRevealed = next
                    onRevealedChange?.invoke(next)
                },
                enabled = enabled,
                icon = revealIcon,
                labels = labels,
            )
        },
        visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
        keyboardActions = keyboardActions,
        interactionSource = interactionSource,
        required = required,
        labelAction = labelAction,
    )
}

@Composable
private fun RevealToggle(
    revealed: Boolean,
    onRevealedChange: (Boolean) -> Unit,
    enabled: Boolean,
    icon: (@Composable (Boolean) -> Unit)?,
    labels: MonarchPasswordInputLabels,
) {
    val name = monarchString(labels.reveal, R.string.monarch_passwordinput_reveal)
    val text = if (revealed) {
        monarchString(labels.hide, R.string.monarch_passwordinput_hide)
    } else {
        monarchString(labels.show, R.string.monarch_passwordinput_show)
    }
    MonarchFieldIconToggle(
        name = name,
        checked = revealed,
        onCheckedChange = onRevealedChange,
        enabled = enabled,
        // What a double tap does, read after the name: "Show" or "Hide".
        actionLabel = text,
    ) {
        // The visible text changes with the state; the spoken name and the on or off state do not.
        Box(modifier = Modifier.clearAndSetSemantics { }) {
            if (icon != null) {
                icon(revealed)
            } else {
                MonarchFieldGlyphIcon(
                    glyph = if (revealed) MonarchFieldGlyph.EyeOff else MonarchFieldGlyph.Eye,
                    tint = MonarchPasswordInputTokens.toggleColor(enabled, MonarchTheme.colors),
                )
            }
        }
    }
}

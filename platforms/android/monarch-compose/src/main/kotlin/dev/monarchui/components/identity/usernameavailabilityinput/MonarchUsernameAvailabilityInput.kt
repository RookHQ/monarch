package dev.monarchui.components.identity.usernameavailabilityinput

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.forms.textfield.MonarchTextField
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/**
 * A text field whose value is checked while the person types: a handle, a workspace address, a slug.
 *
 * Built on the Material 3 `OutlinedTextField` with the colours of
 * `MonarchTextField`, plus a fixed prefix, a status indicator and a status
 * message. The app supplies the check in [validate]; the field runs it
 * [debounceMillis] after typing pauses, in a coroutine that is cancelled when
 * the text changes again, so a late result never describes old text. Monarch
 * does no networking of its own. The settled result ("Available", "Not
 * available", a failure) is announced through a polite live region;
 * "Checking" is not. A failed check offers a "Try again" button that runs the
 * check at once.
 *
 * @param value the text.
 * @param label the visible label, already localised.
 * @param onValueChange called with the new text, after [transform], on every change.
 * @param validate the check. Return `null` when there is nothing to check (for example the person's
 * current handle). Throwing sets the error status. Runs in a coroutine that is cancelled when the text
 * changes, so suspend calls in it are cancelled too.
 * @param modifier the modifier of the component.
 * @param placeholder an example shown while the field is empty and focused.
 * @param helperText guidance under the field, shown while there is no status message.
 * @param prefix fixed text before the value, such as a domain ("rook.com/").
 * @param required marks the field as required: an asterisk after the label, and "required" in the spoken name.
 * @param debounceMillis how long typing must pause before the check runs.
 * @param enabled whether the person can edit the field.
 * @param transform rewrites typed text before it is passed to [onValueChange], for example lower-casing.
 * @param onStatusChange called when the status or its message changes, so a form can block submit until
 * the status is `Valid`. Not called for the first status.
 * @param statusIcon an icon after the text for the settled statuses, for example a check mark for `Valid`.
 * Monarch bundles no icons; the message always says the same in words. While checking a progress
 * indicator is shown instead.
 * @param labels replacements for the built-in strings.
 * @param keyboardActions what happens when the person triggers the IME action.
 * @param interactionSource the source of the focus and press interactions of the field.
 */
@Composable
public fun MonarchUsernameAvailabilityInput(
    value: String,
    label: String,
    onValueChange: (String) -> Unit,
    validate: suspend (String) -> MonarchUsernameAvailabilityResult?,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    helperText: String? = null,
    prefix: String? = null,
    required: Boolean = false,
    debounceMillis: Long = MonarchUsernameAvailabilityInputTokens.DEFAULT_DEBOUNCE_MILLIS,
    enabled: Boolean = true,
    transform: ((String) -> String)? = null,
    onStatusChange: ((MonarchUsernameAvailabilityStatus, String?) -> Unit)? = null,
    statusIcon: (@Composable (MonarchUsernameAvailabilityStatus) -> Unit)? = null,
    labels: MonarchUsernameAvailabilityInputLabels = MonarchUsernameAvailabilityInputLabels(),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    interactionSource: MutableInteractionSource? = null,
) {
    val check = rememberAvailabilityCheck(value, debounceMillis, validate, onStatusChange)
    val status = check.state.status
    val message = statusMessage(check.state, labels)
    val colors = MonarchTheme.colors
    Column(modifier = modifier) {
        // The field shows the result like any field: the error, the success line and border, or the progress as
        // help. A failed check keeps its own row below, with Try again.
        val invalid = status == MonarchUsernameAvailabilityStatus.Invalid
        val valid = status == MonarchUsernameAvailabilityStatus.Valid
        val checking = status == MonarchUsernameAvailabilityStatus.Checking
        MonarchTextField(
            value = value,
            onValueChange = { onValueChange(transform?.invoke(it) ?: it) },
            modifier = Modifier.fillMaxWidth(),
            label = label,
            required = required,
            placeholder = placeholder,
            helperText = if (checking) message else helperText.takeIf { message == null },
            errorText = message.takeIf { invalid },
            enabled = enabled,
            trailingIcon = trailingFor(status, statusIcon),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            keyboardActions = keyboardActions,
            successText = message.takeIf { valid },
            prefix = prefix?.let { { Text(it, color = colors.textMuted) } },
            interactionSource = interactionSource,
        )
        if (message != null && status == MonarchUsernameAvailabilityStatus.Error) {
            FlowRow(
                modifier = Modifier.padding(top = MonarchUsernameAvailabilityInputTokens.MessageTopPadding),
                horizontalArrangement = Arrangement.spacedBy(MonarchUsernameAvailabilityInputTokens.MessageGap),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = message,
                    style = MonarchTheme.typography.caption,
                    color = MonarchUsernameAvailabilityInputTokens.messageColor(status, colors),
                )
                MonarchButton(
                    text = monarchString(labels.retry, R.string.monarch_usernameavailabilityinput_retry),
                    onClick = check.retry,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = enabled,
                )
            }
        }
        // Always present, so each settled result is announced once as a change of this live region.
        val announcement = if (MonarchUsernameAvailabilityInputTokens.announces(status)) message.orEmpty() else ""
        MonarchLiveStatus(text = announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

/** The check of the current text and a way to run it again at once. */
private class AvailabilityCheck(val state: MonarchUsernameAvailabilityCheck, val retry: () -> Unit)

@Composable
private fun rememberAvailabilityCheck(
    value: String,
    debounceMillis: Long,
    validate: suspend (String) -> MonarchUsernameAvailabilityResult?,
    onStatusChange: ((MonarchUsernameAvailabilityStatus, String?) -> Unit)?,
): AvailabilityCheck {
    var state by remember { mutableStateOf(MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Idle)) }
    var attempt by remember { mutableIntStateOf(0) }
    var immediate by remember { mutableStateOf(false) }
    val currentValidate by rememberUpdatedState(validate)
    val currentOnStatusChange by rememberUpdatedState(onStatusChange)
    var reported by remember { mutableStateOf(state) }
    LaunchedEffect(value, attempt) {
        if (value.isEmpty()) {
            state = MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Idle)
            return@LaunchedEffect
        }
        state = MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Checking)
        val wait = if (immediate) 0L else debounceMillis
        immediate = false
        delay(wait)
        val outcome = runCatching { currentValidate(value) }
        // A check cancelled by new typing never writes its result.
        (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
        state = MonarchUsernameAvailabilityInputTokens.checkOf(outcome)
    }
    LaunchedEffect(state) {
        if (state != reported) {
            reported = state
            currentOnStatusChange?.invoke(state.status, state.message)
        }
    }
    return AvailabilityCheck(state) {
        immediate = true
        attempt++
    }
}

@Composable
private fun statusMessage(
    check: MonarchUsernameAvailabilityCheck,
    labels: MonarchUsernameAvailabilityInputLabels,
): String? =
    when (check.status) {
        MonarchUsernameAvailabilityStatus.Idle -> null
        MonarchUsernameAvailabilityStatus.Checking ->
            monarchString(labels.checking, R.string.monarch_usernameavailabilityinput_checking)
        MonarchUsernameAvailabilityStatus.Valid ->
            check.message ?: monarchString(labels.valid, R.string.monarch_usernameavailabilityinput_valid)
        MonarchUsernameAvailabilityStatus.Invalid ->
            check.message ?: monarchString(labels.invalid, R.string.monarch_usernameavailabilityinput_invalid)
        MonarchUsernameAvailabilityStatus.Error ->
            check.message ?: monarchString(labels.error, R.string.monarch_usernameavailabilityinput_error)
    }

private fun trailingFor(
    status: MonarchUsernameAvailabilityStatus,
    statusIcon: (@Composable (MonarchUsernameAvailabilityStatus) -> Unit)?,
): (@Composable () -> Unit)? = when {
    status == MonarchUsernameAvailabilityStatus.Checking -> {
        {
            CircularProgressIndicator(
                // The message says "Checking"; the indicator is decoration.
                modifier = Modifier
                    .size(MonarchUsernameAvailabilityInputTokens.SpinnerSize)
                    .clearAndSetSemantics { },
                strokeWidth = MonarchUsernameAvailabilityInputTokens.SpinnerStrokeWidth,
            )
        }
    }
    status == MonarchUsernameAvailabilityStatus.Idle || statusIcon == null -> null
    else -> {
        { statusIcon(status) }
    }
}

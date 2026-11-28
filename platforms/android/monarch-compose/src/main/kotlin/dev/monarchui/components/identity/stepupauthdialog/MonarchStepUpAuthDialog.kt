package dev.monarchui.components.identity.stepupauthdialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import dev.monarchui.internal.MonarchDefaultIcons
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.forms.otppininput.MonarchOtpPinInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInput
import dev.monarchui.components.forms.passwordinput.MonarchPasswordInputPurpose
import dev.monarchui.components.navigation.tabs.MonarchTabs
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Asks a signed-in person to confirm it is still them, with their password, an authenticator code, a passkey or
 * the device's biometrics or screen lock, before a high-risk action.
 *
 * The dialog is the content of a sheet: present it in a Material 3 `ModalBottomSheet`, whose state and dismissal
 * belong to the app. Pass [onVerifyingChange] to the sheet's `confirmValueChange` so it cannot be swiped away while
 * a check runs. The password is a `MonarchPasswordInput` with the current-password purpose, the code a
 * `MonarchOtpPinInput` of 6 digits in two groups that submits itself when complete. With more than one method a
 * Material 3 `SecondaryScrollableTabRow` switches between them.
 *
 * The system's biometric and passkey sheets are the app's: Monarch adds no `androidx.biometric` or
 * `androidx.credentials` dependency. For [MonarchStepUpMethod.DeviceOwner] the dialog calls [onRequestBiometric],
 * which shows `BiometricPrompt` and returns whether it passed, then hands [MonarchStepUpCredential.DeviceOwner] to
 * [onVerify]. For [MonarchStepUpMethod.Passkey] it hands [MonarchStepUpCredential.Passkey] to [onVerify], which
 * runs the Credential Manager assertion.
 *
 * A missing entry or a failure is written on the field, or in a danger `MonarchInlineNotice` for the passkey and
 * the device, and announced through a polite live region. While [onVerify] runs, the primary button shows its
 * progress indicator and the fields, tabs and Cancel are disabled.
 *
 * @param onVerify checks the credential with the server. Return when it is accepted; throw to show the failure. A
 * cancellation shows nothing.
 * @param modifier the modifier of the dialog content.
 * @param title replaces the title "Confirm it’s you".
 * @param description why the person is asked, such as "Deleting a workspace needs your password.".
 * @param methods the methods offered, most preferred first; the first is shown first. Repeats are dropped, an empty
 * list is the password, and [MonarchStepUpMethod.DeviceOwner] is offered only with [onRequestBiometric].
 * @param errorMessage turns a thrown error into a message. Without it, or when it returns `null`, "That did not
 * work. Try again." is shown.
 * @param onRequestBiometric runs the device check with `BiometricPrompt` and returns `true` when it passed, `false`
 * when the person cancelled it. Cancelling leaves the dialog as it was.
 * @param onForgot starts password recovery; shows "Forgot your password?" under the password field.
 * @param onCancel called by Cancel. Close the sheet in it.
 * @param onVerified called after [onVerify] succeeds. Close the sheet and run the protected action in it.
 * @param onVerifyingChange called with `true` when a check starts and `false` when it ends.
 * @param labels replaces any of the built-in strings.
 */
@Composable
public fun MonarchStepUpAuthDialog(
    onVerify: suspend (MonarchStepUpCredential) -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    methods: List<MonarchStepUpMethod> = listOf(MonarchStepUpMethod.Password),
    errorMessage: ((Throwable) -> String?)? = null,
    onRequestBiometric: (suspend () -> Boolean)? = null,
    onForgot: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onVerified: (() -> Unit)? = null,
    onVerifyingChange: ((Boolean) -> Unit)? = null,
    labels: MonarchStepUpAuthDialogLabels = MonarchStepUpAuthDialogLabels(),
) {
    val offered = MonarchStepUpAuthDialogTokens.methods(methods, onRequestBiometric != null)
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val method = offered[selected.coerceIn(0, offered.lastIndex)]
    val scope = rememberCoroutineScope()
    val callbacks = rememberUpdatedState(
        StepUpCallbacks(onVerify, onRequestBiometric, onVerified, onVerifyingChange, errorMessage),
    )
    val messages = StepUpMessages(
        generic = monarchString(labels.genericError, R.string.monarch_stepupauthdialog_generic_error),
        passwordRequired = monarchString(labels.passwordRequired, R.string.monarch_stepupauthdialog_password_required),
        codeIncomplete = monarchString(labels.codeIncomplete, R.string.monarch_stepupauthdialog_code_incomplete),
    )
    val flow = remember(scope) { StepUpFlow(scope) { callbacks.value } }
    val submit = { flow.submit(method, messages) }
    val verifying = flow.verifying

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MonarchStepUpAuthDialogTokens.Gap),
    ) {
        // The web draws a lock in a round tile above the title.
        Box(
            modifier = Modifier
                .size(MonarchStepUpAuthDialogTokens.IconTile)
                .clip(CircleShape)
                .background(MonarchTheme.colors.bgMuted)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides MonarchTheme.colors.textMuted) {
                MonarchDefaultIcons.Lock()
            }
        }
        MonarchText(
            text = title ?: monarchString(labels.title, R.string.monarch_stepupauthdialog_title),
            style = MonarchTextStyle.Heading,
            heading = true,
        )
        MonarchText(
            text = description ?: monarchString(labels.description, R.string.monarch_stepupauthdialog_description),
            color = MonarchStepUpAuthDialogTokens.description(MonarchTheme.colors),
        )
        if (offered.size > 1) {
            MethodTabs(offered, method, enabled = !verifying, labels) { index ->
                selected = index
                flow.clearErrors()
            }
        }
        when (method) {
            MonarchStepUpMethod.Password -> {
                MonarchPasswordInput(
                    value = flow.password,
                    onValueChange = { flow.password = it },
                    label = monarchString(labels.passwordLabel, R.string.monarch_stepupauthdialog_password_label),
                    errorText = flow.fieldError,
                    required = true,
                    purpose = MonarchPasswordInputPurpose.Current,
                    enabled = !verifying,
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                )
                if (onForgot != null) {
                    MonarchButton(
                        text = monarchString(labels.forgot, R.string.monarch_stepupauthdialog_forgot),
                        onClick = onForgot,
                        variant = MonarchButtonVariant.Ghost,
                        size = MonarchButtonSize.Sm,
                        enabled = !verifying,
                    )
                }
            }
            MonarchStepUpMethod.Code -> MonarchOtpPinInput(
                value = flow.code,
                onValueChange = { flow.code = MonarchStepUpAuthDialogTokens.code(it) },
                label = monarchString(labels.codeLabel, R.string.monarch_stepupauthdialog_code_label),
                length = MonarchStepUpAuthDialogTokens.CodeLength,
                helperText = monarchString(labels.codeHelper, R.string.monarch_stepupauthdialog_code_helper),
                errorText = flow.fieldError,
                enabled = !verifying,
                groupSize = MonarchStepUpAuthDialogTokens.CodeGroup,
                onComplete = { complete ->
                    flow.code = complete
                    submit()
                },
            )
            MonarchStepUpMethod.Passkey -> MonarchText(
                text = monarchString(labels.passkeyDescription, R.string.monarch_stepupauthdialog_passkey_description),
            )
            MonarchStepUpMethod.DeviceOwner -> MonarchText(
                text = monarchString(
                    labels.deviceOwnerDescription,
                    R.string.monarch_stepupauthdialog_device_owner_description,
                ),
            )
        }
        flow.noticeError?.let { MonarchInlineNotice(message = it, tone = MonarchTone.Danger) }
        Buttons(method, verifying, labels, onSubmit = { submit() }, onCancel = onCancel)
        // Always present, so a missing entry or a failure is announced as a change of this live region. The
        // password field announces its own error through its field frame, so it is not announced twice.
        val announcement = flow.fieldError?.takeIf { method != MonarchStepUpMethod.Password } ?: flow.noticeError
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (announcement != null) contentDescription = announcement
            },
        )
    }
}

@Composable
private fun MethodTabs(
    methods: List<MonarchStepUpMethod>,
    selected: MonarchStepUpMethod,
    enabled: Boolean,
    labels: MonarchStepUpAuthDialogLabels,
    onSelect: (Int) -> Unit,
) {
    val name = monarchString(labels.methodPicker, R.string.monarch_stepupauthdialog_method_picker)
    val names = methods.map { methodName(it, labels) }
    MonarchTabs(
        selectedIndex = methods.indexOf(selected),
        onSelectedIndexChange = onSelect,
        modifier = Modifier.semantics { contentDescription = name },
    ) {
        names.forEach { tab(text = it, enabled = enabled) }
    }
}

@Composable
private fun Buttons(
    method: MonarchStepUpMethod,
    verifying: Boolean,
    labels: MonarchStepUpAuthDialogLabels,
    onSubmit: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    val primary = when (method) {
        MonarchStepUpMethod.Password, MonarchStepUpMethod.Code ->
            monarchString(labels.submit, R.string.monarch_stepupauthdialog_submit)
        MonarchStepUpMethod.Passkey ->
            monarchString(labels.passkeyButton, R.string.monarch_stepupauthdialog_passkey_button)
        MonarchStepUpMethod.DeviceOwner ->
            monarchString(labels.deviceOwnerButton, R.string.monarch_stepupauthdialog_device_owner_button)
    }
    Column(verticalArrangement = Arrangement.spacedBy(MonarchStepUpAuthDialogTokens.ButtonGap)) {
        MonarchButton(
            text = primary,
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            variant = MonarchButtonVariant.Primary,
            loading = verifying,
        )
        if (onCancel != null) {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_stepupauthdialog_cancel),
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                variant = MonarchButtonVariant.Ghost,
                enabled = !verifying,
            )
        }
    }
}

@Composable
private fun methodName(method: MonarchStepUpMethod, labels: MonarchStepUpAuthDialogLabels): String =
    labels.methodName?.invoke(method) ?: stringResource(
        when (method) {
            MonarchStepUpMethod.Password -> R.string.monarch_stepupauthdialog_method_password
            MonarchStepUpMethod.Code -> R.string.monarch_stepupauthdialog_method_code
            MonarchStepUpMethod.Passkey -> R.string.monarch_stepupauthdialog_method_passkey
            MonarchStepUpMethod.DeviceOwner -> R.string.monarch_stepupauthdialog_method_device_owner
        },
    )

/** The app's callbacks, read when a check runs so the latest ones are used. */
private class StepUpCallbacks(
    val onVerify: suspend (MonarchStepUpCredential) -> Unit,
    val onRequestBiometric: (suspend () -> Boolean)?,
    val onVerified: (() -> Unit)?,
    val onVerifyingChange: ((Boolean) -> Unit)?,
    val errorMessage: ((Throwable) -> String?)?,
)

/** The resolved messages of a missing entry and a failure. */
private class StepUpMessages(val generic: String, val passwordRequired: String, val codeIncomplete: String)

/** The entries, errors and progress of the dialog, and the rules of submitting. */
private class StepUpFlow(private val scope: CoroutineScope, private val callbacks: () -> StepUpCallbacks) {
    // Secrets are never saved in the instance state.
    var password by mutableStateOf("")
    var code by mutableStateOf("")
    var fieldError by mutableStateOf<String?>(null)
        private set
    var noticeError by mutableStateOf<String?>(null)
        private set
    var verifying by mutableStateOf(false)
        private set

    fun clearErrors() {
        fieldError = null
        noticeError = null
    }

    fun submit(method: MonarchStepUpMethod, messages: StepUpMessages) {
        if (verifying) return
        clearErrors()
        when (method) {
            MonarchStepUpMethod.Password ->
                if (password.isEmpty()) {
                    fieldError = messages.passwordRequired
                } else {
                    check(MonarchStepUpCredential.Password(password), messages)
                }
            MonarchStepUpMethod.Code ->
                if (code.length < MonarchStepUpAuthDialogTokens.CodeLength) {
                    fieldError = messages.codeIncomplete
                } else {
                    check(MonarchStepUpCredential.Code(code), messages)
                }
            MonarchStepUpMethod.Passkey -> check(MonarchStepUpCredential.Passkey, messages)
            MonarchStepUpMethod.DeviceOwner -> requestDevice(messages)
        }
    }

    private fun requestDevice(messages: StepUpMessages) {
        val request = callbacks().onRequestBiometric ?: return
        markVerifying(true)
        scope.launch {
            val outcome = runCatching { request() }
            markVerifying(false)
            (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
            // A cancelled or failed system check leaves the dialog as it was.
            if (outcome.getOrDefault(false)) check(MonarchStepUpCredential.DeviceOwner, messages)
        }
    }

    private fun check(credential: MonarchStepUpCredential, messages: StepUpMessages) {
        markVerifying(true)
        scope.launch {
            val outcome = runCatching { callbacks().onVerify(credential) }
            markVerifying(false)
            val error = outcome.exceptionOrNull()
            when (error) {
                null -> callbacks().onVerified?.invoke()
                is CancellationException -> Unit
                else -> fail(credential.method, callbacks().errorMessage?.invoke(error) ?: messages.generic)
            }
        }
    }

    private fun fail(method: MonarchStepUpMethod, message: String) {
        if (MonarchStepUpAuthDialogTokens.hasField(method)) {
            fieldError = message
            if (method == MonarchStepUpMethod.Code) code = ""
        } else {
            noticeError = message
        }
    }

    private fun markVerifying(value: Boolean) {
        verifying = value
        callbacks().onVerifyingChange?.invoke(value)
    }
}

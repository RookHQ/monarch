package com.rookhq.monarch.components.identity.emailverificationbanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBanner
import com.rookhq.monarch.components.feedback.calloutbanner.MonarchCalloutBannerPlacement
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchDefaultIcons
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A notice that the person's email address is not verified yet, with the
 * address, a Resend button that waits out a cooldown, and an optional Change
 * email button.
 *
 * Built on `MonarchCalloutBanner`: the same surface, tone name and dismiss
 * button, with the buttons in its action slot. Resend runs [onResend] in a
 * coroutine of the banner and is busy meanwhile. After a successful send
 * "Verification email sent" is shown and announced and the button reads
 * "Resend in 42s", counting down every second, disabled until 0. A failed
 * send is shown and announced and Resend is available at once. The banner
 * does not hide itself: remove it in [onDismiss] or when the address is
 * verified.
 *
 * @param email the address waiting for verification, shown in the message.
 * @param onResend sends the email again. Throwing shows the error from [labels] and starts no cooldown.
 * @param modifier the modifier of the banner.
 * @param tone `Warning` when features are locked until the address is verified, `Info` when nothing is blocked
 * yet. Other tones draw as `Warning`.
 * @param placement a rounded callout in the content, or a full-width strip at the top of a screen.
 * @param cooldownSeconds seconds before another send after a successful one.
 * @param initialCooldownSeconds seconds already remaining when the banner appears, for example right after
 * sign-up sent the first email.
 * @param onChangeEmail shows a Change email button.
 * @param onDismiss shows a dismiss button. The app removes the banner here.
 * @param icon an icon at the start in the tone colour. Monarch bundles no icons.
 * @param dismissIcon the close mark of the dismiss button. Without it the button shows its label as text.
 * @param labels replacements for the built-in strings.
 */
@Composable
public fun MonarchEmailVerificationBanner(
    email: String,
    onResend: suspend () -> Unit,
    modifier: Modifier = Modifier,
    tone: MonarchTone = MonarchTone.Warning,
    placement: MonarchCalloutBannerPlacement = MonarchCalloutBannerPlacement.Section,
    cooldownSeconds: Int = MonarchEmailVerificationBannerTokens.DEFAULT_COOLDOWN_SECONDS,
    initialCooldownSeconds: Int = 0,
    onChangeEmail: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    icon: (@Composable () -> Unit)? = MonarchDefaultIcons.Warning,
    dismissIcon: (@Composable () -> Unit)? = MonarchDefaultIcons.Close,
    labels: MonarchEmailVerificationBannerLabels = MonarchEmailVerificationBannerLabels(),
) {
    var state by remember { mutableStateOf(MonarchResendCooldown.start(initialCooldownSeconds)) }
    val scope = rememberCoroutineScope()
    val currentOnResend by rememberUpdatedState(onResend)
    val currentCooldown by rememberUpdatedState(cooldownSeconds)
    LaunchedEffect(state.remaining) {
        if (state.remaining > 0) {
            delay(MonarchEmailVerificationBannerTokens.TICK_MILLIS)
            state = state.tick()
        }
    }
    val resend: () -> Unit = {
        state.begin()?.let { started ->
            state = started
            scope.launch {
                val outcome = runCatching { currentOnResend() }
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                state = if (outcome.isSuccess) state.succeed(currentCooldown) else state.fail()
            }
        }
    }
    val message = labels.message?.invoke(email)
        ?: stringResource(R.string.monarch_emailverificationbanner_message, email)
    MonarchCalloutBanner(
        message = message,
        modifier = modifier,
        tone = MonarchEmailVerificationBannerTokens.tone(tone),
        placement = placement,
        title = monarchString(labels.title, R.string.monarch_emailverificationbanner_title),
        icon = icon,
        action = { ResendActions(state, resend, onChangeEmail, labels) },
        onDismiss = onDismiss,
        dismissIcon = dismissIcon,
        dismissLabel = labels.dismiss,
    )
}

@Composable
private fun ResendActions(
    state: MonarchResendCooldown,
    onResend: () -> Unit,
    onChangeEmail: (() -> Unit)?,
    labels: MonarchEmailVerificationBannerLabels,
) {
    val tokens = MonarchEmailVerificationBannerTokens
    val resendText = if (state.remaining > 0) {
        labels.resendIn?.invoke(state.remaining)
            ?: stringResource(R.string.monarch_emailverificationbanner_resend_in, state.remaining)
    } else {
        monarchString(labels.resend, R.string.monarch_emailverificationbanner_resend)
    }
    val status = when {
        state.failed -> monarchString(labels.error, R.string.monarch_emailverificationbanner_error)
        state.sent -> monarchString(labels.sent, R.string.monarch_emailverificationbanner_sent)
        else -> null
    }
    Column(verticalArrangement = Arrangement.spacedBy(tokens.StatusGap)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
            verticalArrangement = Arrangement.spacedBy(tokens.ButtonGap),
        ) {
            MonarchButton(
                text = resendText,
                onClick = onResend,
                variant = MonarchButtonVariant.Secondary,
                size = MonarchButtonSize.Sm,
                enabled = state.remaining == 0,
                loading = state.sending,
            )
            if (onChangeEmail != null) {
                MonarchButton(
                    text = monarchString(labels.changeEmail, R.string.monarch_emailverificationbanner_change_email),
                    onClick = onChangeEmail,
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        }
        if (status != null) {
            Text(
                text = status,
                style = MonarchTheme.typography.caption,
                color = tokens.statusColor(state.failed, MonarchTheme.colors),
            )
        }
        // Always present, so each result is announced once as a change of this live region.
        Box(
            Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                if (status != null) contentDescription = status
            },
        )
    }
}

package com.rookhq.monarch.components.billing.paymentmethodcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeTokens
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.feedback.inlineerrorretry.MonarchInlineErrorRetry
import com.rookhq.monarch.components.overlays.dialog.MonarchDialog
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchImage
import com.rookhq.monarch.internal.MonarchShapeDefaults
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.internal.monarchSurface
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * A saved payment card: network, last four digits, expiry and holder, a
 * Default badge, and actions to make it the default or remove it after a
 * confirmation.
 *
 * Built from `MonarchCard`, `MonarchBadge`, `MonarchButton`,
 * `MonarchInlineErrorRetry` and a `MonarchDialog` for the removal
 * confirmation. It only displays a card the payment provider has saved; card
 * entry stays with the provider's SDK. The actions are `suspend` functions:
 * while one runs its button shows progress and the other does not start; when
 * it throws, a message with Try again appears under the card and is announced.
 *
 * @param brand a network id (visa, mastercard, amex, rupay, discover, diners, jcb, unionpay, maestro) shown by
 * its display name, or any display name, shown as given.
 * @param last4 the last four digits, exactly as the payment provider returns them.
 * @param modifier the modifier of the card.
 * @param expiryMonth the expiry month, 1 to 12.
 * @param expiryYear the expiry year, two or four digits.
 * @param holderName the cardholder's name, shown before the expiry.
 * @param expiry replaces the status worked out from the date, when the payment provider already reports it.
 * @param now the current time in epoch milliseconds, for working out the status.
 * @param default shows the Default badge and hides Make default.
 * @param removeEnabled whether Remove can be used, for example `false` on the only method of an active
 * subscription.
 * @param brandImage the network's logo, always decorative. While `null` a muted tile stands in.
 * @param labels replacements for the built-in strings.
 * @param onMakeDefault shows Make default on a card that is not the default. Progress shows while it runs; a
 * thrown exception shows the failure with Try again.
 * @param onRemove shows Remove, which asks for confirmation first. Progress shows while it runs; a thrown
 * exception shows the failure with Try again, which does not ask again.
 * @param actions further buttons after Make default and Remove.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchPaymentMethodCard(
    brand: String,
    last4: String,
    modifier: Modifier = Modifier,
    expiryMonth: Int? = null,
    expiryYear: Int? = null,
    holderName: String? = null,
    expiry: MonarchPaymentMethodCardExpiry? = null,
    now: Long = System.currentTimeMillis(),
    default: Boolean = false,
    removeEnabled: Boolean = true,
    brandImage: Painter? = null,
    labels: MonarchPaymentMethodCardLabels = MonarchPaymentMethodCardLabels(),
    onMakeDefault: (suspend () -> Unit)? = null,
    onRemove: (suspend () -> Unit)? = null,
    actions: (@Composable () -> Unit)? = null,
) {
    val tokens = MonarchPaymentMethodCardTokens
    val brandName = tokens.brandName(brand)?.let { stringResource(it) } ?: brand
    val name = labels.name?.invoke(brandName, last4)
        ?: stringResource(R.string.monarch_paymentmethodcard_name, brandName, last4)
    val status = tokens.status(expiry, expiryMonth, expiryYear, now)
    val runner = rememberPaymentMethodCardRunner(onMakeDefault, onRemove)
    var confirming by rememberSaveable { mutableStateOf(false) }
    // As the web: the default card has a red edge, and the actions sit at the end of the row.
    MonarchCard(modifier = modifier, tone = MonarchTone.Danger.takeIf { default }) {
        Column(verticalArrangement = Arrangement.spacedBy(tokens.SectionGap)) {
            val showsMakeDefault = tokens.showsMakeDefault(onMakeDefault != null, default)
            val hasActions = showsMakeDefault || onRemove != null || actions != null
            // One action fits beside the text of a phone; more go in a row under it, at the end.
            val beside = hasActions && !showsMakeDefault && actions == null
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    Header(name, status, default, holderName, expiryMonth, expiryYear, brandImage, labels)
                }
                if (beside) RemoveAction(name, labels, runner, removeEnabled) { confirming = true }
            }
            if (hasActions && !beside) {
                ActionsRow(name, labels, runner, status, showsMakeDefault, onRemove != null, removeEnabled, actions) {
                    confirming = true
                }
            }
            runner.failed?.let { action ->
                MonarchInlineErrorRetry(
                    message = monarchString(labels.failed, R.string.monarch_paymentmethodcard_failed),
                    onRetry = { runner.run(action) },
                )
            }
        }
    }
    if (confirming) {
        RemoveConfirmation(
            name = name,
            labels = labels,
            onConfirm = {
                confirming = false
                runner.run(MonarchPaymentMethodCardAction.Remove)
            },
            onDismiss = { confirming = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(
    name: String,
    status: MonarchPaymentMethodCardExpiry,
    default: Boolean,
    holderName: String?,
    expiryMonth: Int?,
    expiryYear: Int?,
    brandImage: Painter?,
    labels: MonarchPaymentMethodCardLabels,
) {
    val tokens = MonarchPaymentMethodCardTokens
    val colors = MonarchTheme.colors
    val date = tokens.expiryDate(expiryMonth, expiryYear)
    val expiryLine = date?.let {
        if (status == MonarchPaymentMethodCardExpiry.Expired) {
            labels.expiredOn?.invoke(it) ?: stringResource(R.string.monarch_paymentmethodcard_expired_on, it)
        } else {
            labels.expires?.invoke(it) ?: stringResource(R.string.monarch_paymentmethodcard_expires, it)
        }
    }
    val line = listOfNotNull(holderName?.takeIf { it.isNotBlank() }, expiryLine).joinToString(" · ")
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.HeaderGap), verticalAlignment = Alignment.Top) {
        MonarchImage(
            painter = brandImage,
            contentDescription = null,
            modifier = Modifier.size(tokens.ImageWidth, tokens.ImageHeight),
            shape = MonarchShapeDefaults.Mark,
            contentScale = ContentScale.Fit,
        ) {
            Box(Modifier.fillMaxSize().monarchSurface(tokens.imagePlaceholder(colors), MonarchShapeDefaults.Mark))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .semantics(mergeDescendants = true) { heading() },
            verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        ) {
            Text(text = name, style = MonarchTheme.typography.bodyStrong, color = colors.textDefault)
            if (default || status != MonarchPaymentMethodCardExpiry.Valid) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(tokens.LineGap),
                    verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
                ) {
                    Badges(status, default, labels)
                }
            }
            if (line.isNotEmpty()) {
                Text(text = line, style = MonarchTheme.typography.caption, color = colors.textMuted)
            }
        }
    }
}

@Composable
private fun Badges(status: MonarchPaymentMethodCardExpiry, default: Boolean, labels: MonarchPaymentMethodCardLabels) {
    if (default) {
        DefaultBadge(monarchString(labels.defaultBadge, R.string.monarch_paymentmethodcard_default_badge))
    }
    when (status) {
        MonarchPaymentMethodCardExpiry.Expiring -> MonarchBadge(
            text = monarchString(labels.expiringBadge, R.string.monarch_paymentmethodcard_expiring_badge),
            tone = MonarchTone.Warning,
        )
        MonarchPaymentMethodCardExpiry.Expired -> MonarchBadge(
            text = monarchString(labels.expiredBadge, R.string.monarch_paymentmethodcard_expired_badge),
            tone = MonarchTone.Danger,
        )
        MonarchPaymentMethodCardExpiry.Valid -> Unit
    }
}

/** The Default badge in the accent roles, drawn like a subtle `MonarchBadge`, which has no accent tone. */
@Composable
private fun DefaultBadge(text: String) {
    val colors = MonarchPaymentMethodCardTokens.defaultBadgeColors(MonarchTheme.colors)
    Text(
        text = text,
        modifier = Modifier
            .monarchSurface(colors, MonarchShapeDefaults.Mark)
            .heightIn(min = MonarchBadgeTokens.MinHeight)
            .padding(
                horizontal = MonarchBadgeTokens.HorizontalPadding,
                vertical = MonarchBadgeTokens.VerticalPadding,
            ),
        style = MonarchTheme.typography.caption,
        fontWeight = FontWeight(MonarchBadgeTokens.FONT_WEIGHT),
        color = colors.content,
    )
}

/** Make default, Remove and the app's own buttons, in a row that ends at the end edge of the card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
@Suppress("LongParameterList")
private fun ActionsRow(
    name: String,
    labels: MonarchPaymentMethodCardLabels,
    runner: PaymentMethodCardRunner,
    status: MonarchPaymentMethodCardExpiry,
    showsMakeDefault: Boolean,
    hasRemove: Boolean,
    removeEnabled: Boolean,
    actions: (@Composable () -> Unit)?,
    onRemoveClick: () -> Unit,
) {
    val tokens = MonarchPaymentMethodCardTokens
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.ActionGap, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(tokens.ActionGap),
    ) {
        if (showsMakeDefault) {
            ActionButton(
                label = monarchString(labels.makeDefault, R.string.monarch_paymentmethodcard_make_default),
                name = name,
                labels = labels,
                variant = MonarchButtonVariant.Secondary,
                enabled = tokens.makeDefaultEnabled(status, runner.running),
                loading = runner.running == MonarchPaymentMethodCardAction.MakeDefault,
                onClick = { runner.run(MonarchPaymentMethodCardAction.MakeDefault) },
            )
        }
        if (hasRemove) RemoveAction(name, labels, runner, removeEnabled, onRemoveClick)
        actions?.invoke()
    }
}

/** The Remove button: it asks for confirmation through [onClick]. */
@Composable
private fun RemoveAction(
    name: String,
    labels: MonarchPaymentMethodCardLabels,
    runner: PaymentMethodCardRunner,
    removeEnabled: Boolean,
    onClick: () -> Unit,
) {
    val tokens = MonarchPaymentMethodCardTokens
    ActionButton(
        label = monarchString(labels.remove, R.string.monarch_paymentmethodcard_remove),
        name = name,
        labels = labels,
        variant = MonarchButtonVariant.Ghost,
        enabled = tokens.removeEnabled(removeEnabled, runner.running),
        loading = runner.running == MonarchPaymentMethodCardAction.Remove,
        onClick = { if (tokens.canStart(runner.running)) onClick() },
    )
}

@Composable
private fun ActionButton(
    label: String,
    name: String,
    labels: MonarchPaymentMethodCardLabels,
    variant: MonarchButtonVariant,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
) {
    val spoken = labels.actionLabel?.invoke(label, name)
        ?: stringResource(R.string.monarch_paymentmethodcard_action_label, label, name)
    MonarchButton(
        text = label,
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = spoken },
        variant = variant,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
        loading = loading,
    )
}

@Composable
private fun RemoveConfirmation(
    name: String,
    labels: MonarchPaymentMethodCardLabels,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    MonarchDialog(
        onDismissRequest = onDismiss,
        title = labels.removeTitle?.invoke(name)
            ?: stringResource(R.string.monarch_paymentmethodcard_remove_title, name),
        description = monarchString(labels.removeMessage, R.string.monarch_paymentmethodcard_remove_message),
        actions = {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_paymentmethodcard_cancel),
                onClick = onDismiss,
                variant = MonarchButtonVariant.Ghost,
            )
            MonarchButton(
                text = monarchString(labels.removeConfirm, R.string.monarch_paymentmethodcard_remove_confirm),
                onClick = onConfirm,
                variant = MonarchButtonVariant.Danger,
            )
        },
    )
}

/** The running and the failed action of a payment method card, and a way to start one. */
private class PaymentMethodCardRunner(
    val running: MonarchPaymentMethodCardAction?,
    val failed: MonarchPaymentMethodCardAction?,
    val run: (MonarchPaymentMethodCardAction) -> Unit,
)

@Composable
private fun rememberPaymentMethodCardRunner(
    onMakeDefault: (suspend () -> Unit)?,
    onRemove: (suspend () -> Unit)?,
): PaymentMethodCardRunner {
    val scope = rememberCoroutineScope()
    val makeDefault by rememberUpdatedState(onMakeDefault)
    val remove by rememberUpdatedState(onRemove)
    var running by remember { mutableStateOf<MonarchPaymentMethodCardAction?>(null) }
    var failed by remember { mutableStateOf<MonarchPaymentMethodCardAction?>(null) }
    return PaymentMethodCardRunner(running, failed) { action ->
        val block = when (action) {
            MonarchPaymentMethodCardAction.MakeDefault -> makeDefault
            MonarchPaymentMethodCardAction.Remove -> remove
        }
        if (MonarchPaymentMethodCardTokens.canStart(running) && block != null) {
            running = action
            failed = null
            scope.launch {
                val outcome = runCatching { block() }
                running = null
                (outcome.exceptionOrNull() as? CancellationException)?.let { throw it }
                if (outcome.isFailure) failed = action
            }
        }
    }
}

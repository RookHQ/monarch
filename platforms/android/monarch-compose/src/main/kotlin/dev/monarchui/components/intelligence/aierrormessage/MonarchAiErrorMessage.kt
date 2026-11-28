package dev.monarchui.components.intelligence.aierrormessage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.MonarchSurfaceColors
import dev.monarchui.internal.monarchString
import dev.monarchui.internal.monarchSurface
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/**
 * The failure card for an assistant turn: what went wrong in plain words, the actions that can fix it, an
 * optional retry countdown and a technical details disclosure.
 *
 * Built by Monarch with the surface of `MonarchBanner` in the warning or
 * danger tone of the kind, `MonarchButton` actions and a `MonarchCollapsible`
 * for the details. Each action appears when its callback is given, in the
 * order that fits the kind; the first is the primary button. The title and
 * message are announced once, assertively, when the card appears, unless
 * [announce] is `false`. While [retryAfterMillis] counts down, "Try again" is
 * disabled and reads "Try again in 12s"; "You can try again now." is
 * announced when it ends.
 *
 * @param modifier the modifier of the card.
 * @param kind why the turn failed. `MonarchAiErrorKind.fromCode` maps a raw error code.
 * @param title replaces the built-in title of the kind.
 * @param message replaces the built-in description of the kind.
 * @param details technical information, such as a request id, shown in code type behind a disclosure.
 * @param retryAfterMillis time until a retry is allowed, counted from when the card appears.
 * @param retrying shows a progress indicator on the retry action, which then does not run.
 * @param announce announces the title and message when the card appears. Pass `false` when the same error
 * was already announced, for example when the message scrolls back into a list.
 * @param icon an icon at the start of the card for the kind, such as a clock for a rate limit. Monarch
 * bundles no icons; the tone is also read as a word.
 * @param labels replacements for the built-in strings.
 * @param onRetry shows "Try again".
 * @param onEditPrompt shows "Edit message": put the failed prompt back in the input.
 * @param onSwitchModel shows "Switch model".
 * @param onNewConversation shows "New conversation"; the primary action for `ContextOverflow`.
 * @param onSummarize shows "Summarize and continue", for `ContextOverflow` only.
 * @param actions extra actions after the built-in ones, for example a ghost `MonarchButton`.
 */
@Composable
public fun MonarchAiErrorMessage(
    modifier: Modifier = Modifier,
    kind: MonarchAiErrorKind = MonarchAiErrorKind.Unknown,
    title: String? = null,
    message: String? = null,
    details: String? = null,
    retryAfterMillis: Long? = null,
    retrying: Boolean = false,
    announce: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    labels: MonarchAiErrorMessageLabels = MonarchAiErrorMessageLabels(),
    onRetry: (() -> Unit)? = null,
    onEditPrompt: (() -> Unit)? = null,
    onSwitchModel: (() -> Unit)? = null,
    onNewConversation: (() -> Unit)? = null,
    onSummarize: (() -> Unit)? = null,
    actions: (@Composable FlowRowScope.() -> Unit)? = null,
) {
    val themeColors = MonarchTheme.colors
    val colors = MonarchAiErrorMessageTokens.colors(kind, themeColors)
    val shownTitle = title ?: stringResource(titleResource(kind))
    val shownMessage = message ?: stringResource(descriptionResource(kind))
    val toneName = labels.tone ?: stringResource(
        if (MonarchAiErrorMessageTokens.tone(kind) == MonarchTone.Warning) {
            R.string.monarch_aierrormessage_tone_warning
        } else {
            R.string.monarch_aierrormessage_tone_danger
        },
    )
    val secondsLeft = rememberCountdown(retryAfterMillis)
    val handlers = mapOf(
        MonarchAiErrorAction.Retry to onRetry,
        MonarchAiErrorAction.EditPrompt to onEditPrompt,
        MonarchAiErrorAction.SwitchModel to onSwitchModel,
        MonarchAiErrorAction.NewConversation to onNewConversation,
        MonarchAiErrorAction.Summarize to onSummarize.takeIf { kind == MonarchAiErrorKind.ContextOverflow },
    )
    val available = MonarchAiErrorMessageTokens.order(kind).mapNotNull { action ->
        handlers[action]?.let { action to it }
    }
    Column(
        modifier = modifier
            .monarchSurface(colors, MonarchShapeDefaults.Container)
            .padding(
                horizontal = MonarchAiErrorMessageTokens.HorizontalPadding,
                vertical = MonarchAiErrorMessageTokens.VerticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(MonarchAiErrorMessageTokens.ContentGap),
    ) {
        Header(icon, colors, toneName, shownTitle, shownMessage)
        if (available.isNotEmpty() || actions != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MonarchAiErrorMessageTokens.ActionGap),
                verticalArrangement = Arrangement.spacedBy(MonarchAiErrorMessageTokens.ActionGap),
            ) {
                available.forEachIndexed { index, (action, onClick) ->
                    ErrorAction(action, primary = index == 0, onClick, secondsLeft, retrying, labels)
                }
                actions?.invoke(this)
            }
        }
        if (onRetry == null && secondsLeft > 0) {
            Text(
                text = retryInText(secondsLeft, labels),
                style = MonarchTheme.typography.caption,
                color = themeColors.textMuted,
            )
        }
        if (!details.isNullOrEmpty()) Details(details, labels)
        Announcements(announce, toneName, shownTitle, shownMessage, retryAfterMillis, secondsLeft, labels)
    }
}

@Composable
private fun Header(
    icon: (@Composable () -> Unit)?,
    colors: MonarchSurfaceColors,
    toneName: String,
    title: String,
    message: String,
) {
    val spoken = stringResource(R.string.monarch_aierrormessage_spoken, toneName, title, message)
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalArrangement = Arrangement.spacedBy(MonarchAiErrorMessageTokens.ContentGap),
        verticalAlignment = Alignment.Top,
    ) {
        if (icon != null) CompositionLocalProvider(LocalContentColor provides colors.content, content = icon)
        Column(verticalArrangement = Arrangement.spacedBy(MonarchAiErrorMessageTokens.TitleGap)) {
            Text(text = title, style = MonarchTheme.typography.bodyStrong, color = MonarchTheme.colors.textDefault)
            Text(
                text = message,
                style = MonarchTheme.typography.label.copy(fontWeight = FontWeight.Normal),
                color = MonarchTheme.colors.textMuted,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ErrorAction(
    action: MonarchAiErrorAction,
    primary: Boolean,
    onClick: () -> Unit,
    secondsLeft: Int,
    retrying: Boolean,
    labels: MonarchAiErrorMessageLabels,
) {
    val retry = action == MonarchAiErrorAction.Retry
    val waiting = retry && secondsLeft > 0
    val text = when (action) {
        MonarchAiErrorAction.Retry -> if (waiting) {
            retryInText(secondsLeft, labels)
        } else {
            monarchString(labels.retry, R.string.monarch_aierrormessage_retry)
        }
        MonarchAiErrorAction.EditPrompt -> monarchString(labels.editPrompt, R.string.monarch_aierrormessage_edit_prompt)
        MonarchAiErrorAction.SwitchModel ->
            monarchString(labels.switchModel, R.string.monarch_aierrormessage_switch_model)
        MonarchAiErrorAction.NewConversation ->
            monarchString(labels.newConversation, R.string.monarch_aierrormessage_new_conversation)
        MonarchAiErrorAction.Summarize -> monarchString(labels.summarize, R.string.monarch_aierrormessage_summarize)
    }
    MonarchButton(
        text = text,
        onClick = onClick,
        variant = if (primary) MonarchButtonVariant.Primary else MonarchButtonVariant.Secondary,
        size = MonarchButtonSize.Sm,
        enabled = !waiting,
        loading = retry && retrying,
        leadingIcon = when (action) {
            MonarchAiErrorAction.Retry -> MonarchDefaultIcons.Retry
            MonarchAiErrorAction.NewConversation -> MonarchDefaultIcons.Plus
            else -> null
        },
    )
}

@Composable
private fun retryInText(seconds: Int, labels: MonarchAiErrorMessageLabels): String =
    labels.retryIn?.invoke(seconds) ?: stringResource(R.string.monarch_aierrormessage_retry_in, seconds)

@Composable
private fun Details(details: String, labels: MonarchAiErrorMessageLabels) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    MonarchCollapsible(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        label = monarchString(labels.details, R.string.monarch_aierrormessage_details),
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = MonarchDefaultIcons.ChevronDown,
    ) {
        Text(
            text = details,
            modifier = Modifier
                .fillMaxWidth()
                .monarchSurface(
                    colors = MonarchAiErrorMessageTokens.detailsColors(MonarchTheme.colors),
                    shape = MonarchShapeDefaults.Row,
                )
                .padding(MonarchAiErrorMessageTokens.DetailsPadding),
            style = MonarchTheme.typography.code,
            color = MonarchTheme.colors.textDefault,
        )
    }
}

/** The whole seconds left until a retry is allowed, counted from when the card appears. */
@Composable
private fun rememberCountdown(retryAfterMillis: Long?): Int {
    var left by remember(retryAfterMillis) {
        mutableIntStateOf(retryAfterMillis?.let { MonarchAiErrorMessageTokens.secondsLeft(it) } ?: 0)
    }
    LaunchedEffect(retryAfterMillis) {
        if (!MonarchAiErrorMessageTokens.countsDown(retryAfterMillis)) return@LaunchedEffect
        var remaining = requireNotNull(retryAfterMillis)
        while (remaining > 0) {
            left = MonarchAiErrorMessageTokens.secondsLeft(remaining)
            val step = minOf(remaining, MonarchAiErrorMessageTokens.TICK_MILLIS)
            delay(step)
            remaining -= step
        }
        left = 0
    }
    return left
}

/** The assertive announcement of the failure and the polite one when a countdown ends. */
@Suppress("LongParameterList")
@Composable
private fun Announcements(
    announce: Boolean,
    toneName: String,
    title: String,
    message: String,
    retryAfterMillis: Long?,
    secondsLeft: Int,
    labels: MonarchAiErrorMessageLabels,
) {
    // Filled after the first frame: a region whose text is there from the start is not read reliably.
    var alerting by remember { mutableStateOf(false) }
    LaunchedEffect(announce) { alerting = announce }
    val alert = if (alerting) stringResource(R.string.monarch_aierrormessage_spoken, toneName, title, message) else ""
    val ready = MonarchAiErrorMessageTokens.countsDown(retryAfterMillis) && secondsLeft == 0
    val readyText = if (ready) monarchString(labels.retryReady, R.string.monarch_aierrormessage_retry_ready) else ""
    Box(
        modifier = Modifier.semantics {
            liveRegion = LiveRegionMode.Assertive
            if (alert.isNotEmpty()) contentDescription = alert
        },
    )
    MonarchLiveStatus(text = readyText.takeIf { it.isNotEmpty() }, visible = false)
}

private fun titleResource(kind: MonarchAiErrorKind): Int = when (kind) {
    MonarchAiErrorKind.RateLimit -> R.string.monarch_aierrormessage_title_rate_limit
    MonarchAiErrorKind.ContextOverflow -> R.string.monarch_aierrormessage_title_context_overflow
    MonarchAiErrorKind.SafetyBlock -> R.string.monarch_aierrormessage_title_safety_block
    MonarchAiErrorKind.Network -> R.string.monarch_aierrormessage_title_network
    MonarchAiErrorKind.ToolFailure -> R.string.monarch_aierrormessage_title_tool_failure
    MonarchAiErrorKind.Unknown -> R.string.monarch_aierrormessage_title_unknown
}

private fun descriptionResource(kind: MonarchAiErrorKind): Int = when (kind) {
    MonarchAiErrorKind.RateLimit -> R.string.monarch_aierrormessage_description_rate_limit
    MonarchAiErrorKind.ContextOverflow -> R.string.monarch_aierrormessage_description_context_overflow
    MonarchAiErrorKind.SafetyBlock -> R.string.monarch_aierrormessage_description_safety_block
    MonarchAiErrorKind.Network -> R.string.monarch_aierrormessage_description_network
    MonarchAiErrorKind.ToolFailure -> R.string.monarch_aierrormessage_description_tool_failure
    MonarchAiErrorKind.Unknown -> R.string.monarch_aierrormessage_description_unknown
}

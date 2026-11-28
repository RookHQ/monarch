package dev.monarchui.components.intelligence.aicomputerview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.calloutbanner.MonarchCalloutBanner
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.internal.monarchTimeZone
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A live view of an AI agent operating a browser or a computer for the
 * person: a plain-words status, a banner while the agent is in control, the
 * buttons to pause, take over, hand back and stop, the latest screenshot with
 * the agent's pointer and last action marked, and a log of every action. It
 * is a viewer: the app supplies the screenshots and the actions and performs
 * every control.
 *
 * A product pattern composed from Monarch parts: `MonarchCalloutBanner` for
 * the control banner and the waiting notice, `MonarchButton` for the
 * controls, and `MonarchLightbox` to zoom the screenshot (a tap on it, or the
 * TalkBack action View larger, opens it full screen with pinch and button
 * zoom). The controls follow the screen, as on the web. Stop asks for an inline
 * confirmation. A private screenshot, the one after a private action, and a private live picture are never shown; a
 * private action's label and detail are never shown or announced, and typed
 * text shows only with [showTypedText]. Each status change is announced
 * once (a failure or a wait for the person assertively); new actions are
 * announced politely, at most once per [announceIntervalMillis].
 *
 * @param status where the session is.
 * @param modifier the modifier of the view.
 * @param frame the latest screenshot; `null` until the first one arrives.
 * @param actions what the agent did, oldest first.
 * @param currentUrl the address of the page the agent is on, shown as text without credentials.
 * @param windowTitle the title of the window or page.
 * @param pointer where the agent's pointer is now, in screenshot pixels.
 * @param surface what the agent operates; it only changes the wording.
 * @param waitingReason why the agent waits for the person, such as "Sign in to continue".
 * @param selectedActionId the action marked on the screenshot and highlighted in the log.
 * @param onSelectAction makes each logged action selectable; called with its id.
 * @param onPauseChange shows Pause while the agent acts and Resume while paused; called with the wanted state.
 * @param onTakeOver shows Take over while the agent holds control.
 * @param onHandBack shows Hand back while the person has taken over.
 * @param onStop shows Stop until the session finishes; called after the inline confirmation.
 * @param showTypedText shows the detail of typing actions. Private actions stay hidden.
 * @param compact hides the log behind an "Actions (n)" button.
 * @param logExpanded whether the compact log shows; `null` lets the view keep it, starting hidden.
 * @param onLogExpandedChange called when the person shows or hides the compact log.
 * @param announceIntervalMillis the shortest time between two action announcements.
 * @param visibleActions how many of the newest actions the log shows before "Show earlier".
 * @param locale the locale of the times; `null` uses the configuration's.
 * @param timeZone the zone of the times. An IANA id such as `"Europe/Berlin"`. `null`, or an id the device does not
 * know, is the device's zone.
 * @param live a live picture of the screen from the app, such as a video renderer; it replaces the screenshot.
 * @param liveSensitive the live picture may show private information: it is never shown.
 * @param labels replacements for the built-in strings.
 */
@ExperimentalMonarchApi
@OptIn(ExperimentalLayoutApi::class)
@Composable
public fun MonarchAiComputerView(
    status: MonarchAiComputerViewStatus,
    modifier: Modifier = Modifier,
    frame: MonarchAiComputerFrame? = null,
    actions: List<MonarchAiComputerAction> = emptyList(),
    currentUrl: String? = null,
    windowTitle: String? = null,
    pointer: MonarchAiComputerPoint? = null,
    surface: MonarchAiComputerViewSurface = MonarchAiComputerViewSurface.Browser,
    waitingReason: String? = null,
    selectedActionId: String? = null,
    onSelectAction: ((String) -> Unit)? = null,
    onPauseChange: ((Boolean) -> Unit)? = null,
    onTakeOver: (() -> Unit)? = null,
    onHandBack: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    showTypedText: Boolean = false,
    compact: Boolean = false,
    logExpanded: Boolean? = null,
    onLogExpandedChange: ((Boolean) -> Unit)? = null,
    announceIntervalMillis: Long = MonarchAiComputerViewTokens.DEFAULT_ANNOUNCE_INTERVAL_MILLIS,
    visibleActions: Int = MonarchAiComputerViewTokens.DEFAULT_VISIBLE_ACTIONS,
    locale: Locale? = null,
    timeZone: String? = null,
    live: (@Composable () -> Unit)? = null,
    liveSensitive: Boolean = false,
    labels: MonarchAiComputerViewLabels = MonarchAiComputerViewLabels(),
) {
    val tokens = MonarchAiComputerViewTokens
    val resources = LocalResources.current
    val texts = remember(labels, resources, surface) { AiComputerViewTexts(labels, resources, surface) }
    val clock = AiComputerViewClock(
        locale = locale ?: LocalConfiguration.current.locales[0],
        timeZone = monarchTimeZone(timeZone),
    )
    Column(
        modifier = modifier.semantics {
            isTraversalGroup = true
            contentDescription = texts.label
        },
        verticalArrangement = Arrangement.spacedBy(tokens.Gap),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.clearAndSetSemantics { }) {
                MonarchDefaultIcons.Toned(MonarchTone.Ai, MonarchDefaultIcons.Bot)
            }
            Text(
                text = texts.status(status),
                style = MonarchTheme.typography.bodyStrong,
                color = tokens.statusColor(status, MonarchTheme.colors),
            )
        }
        when {
            AiComputerViewRules.agentActing(status) -> AiComputerViewControlBanner(texts.agentInControl)
            status == MonarchAiComputerViewStatus.TakenOver ->
                MonarchCalloutBanner(message = texts.youInControl, tone = MonarchTone.Neutral)
            status == MonarchAiComputerViewStatus.WaitingForYou -> MonarchCalloutBanner(
                message = waitingReason?.takeIf { it.isNotBlank() } ?: texts.waitingFallback,
                tone = MonarchTone.Warning,
                title = texts.waitingTitle,
            )
            else -> Unit
        }
        AiComputerViewScreen(
            frame = frame,
            actions = actions,
            currentUrl = currentUrl,
            windowTitle = windowTitle,
            pointer = pointer,
            selectedActionId = selectedActionId,
            live = live,
            liveSensitive = liveSensitive,
            texts = texts,
            clock = clock,
            agentActing = AiComputerViewRules.agentActing(status),
        )
        AiComputerViewControls(
            status = status,
            texts = texts,
            onPauseChange = onPauseChange,
            onTakeOver = onTakeOver,
            onHandBack = onHandBack,
            onStop = onStop,
        )
        AiComputerViewLog(
            actions = actions,
            selectedActionId = selectedActionId,
            onSelectAction = onSelectAction,
            showTypedText = showTypedText,
            compact = compact,
            logExpanded = logExpanded,
            onLogExpandedChange = onLogExpandedChange,
            visibleActions = visibleActions,
            texts = texts,
            clock = clock,
        )
        AiComputerViewAnnouncer(status, actions, announceIntervalMillis, texts)
    }
}

/** The solid AI bar shown while the agent holds control, as the web: the assistant mark and the sentence. */
@Composable
private fun AiComputerViewControlBanner(message: String) {
    val tokens = MonarchAiComputerViewTokens
    val bar = tokens.banner(MonarchTheme.colors)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bar.container, MonarchShapeDefaults.Row)
            .padding(horizontal = tokens.BannerPaddingHorizontal, vertical = tokens.BannerPaddingVertical)
            .semantics(mergeDescendants = true) { },
        horizontalArrangement = Arrangement.spacedBy(tokens.ItemGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides bar.content) {
            Box(modifier = Modifier.clearAndSetSemantics { }) { MonarchDefaultIcons.Bot() }
        }
        Text(text = message, style = MonarchTheme.typography.label, color = bar.content)
    }
}

/** Pause or Resume, Take over or Hand back, and Stop with its inline confirmation. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiComputerViewControls(
    status: MonarchAiComputerViewStatus,
    texts: AiComputerViewTexts,
    onPauseChange: ((Boolean) -> Unit)?,
    onTakeOver: (() -> Unit)?,
    onHandBack: (() -> Unit)?,
    onStop: (() -> Unit)?,
) {
    val tokens = MonarchAiComputerViewTokens
    var confirming by rememberSaveable { mutableStateOf(false) }
    val canStop = AiComputerViewRules.canStop(status, onStop != null)
    LaunchedEffect(canStop) { if (!canStop) confirming = false }
    val gap = Arrangement.spacedBy(tokens.ItemGap)
    if (confirming && canStop) {
        Column(modifier = Modifier.semantics { isTraversalGroup = true }, verticalArrangement = gap) {
            Text(
                text = texts.stopQuestion,
                style = MonarchTheme.typography.body,
                color = tokens.textColor(MonarchTheme.colors),
            )
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                MonarchButton(
                    text = texts.stopConfirm,
                    onClick = {
                        confirming = false
                        onStop?.invoke()
                    },
                    variant = MonarchButtonVariant.Danger,
                    size = MonarchButtonSize.Sm,
                )
                MonarchButton(text = texts.stopCancel, onClick = { confirming = false }, size = MonarchButtonSize.Sm)
            }
        }
        return
    }
    val pause = onPauseChange
    FlowRow(
        horizontalArrangement = gap,
        verticalArrangement = gap,
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (onTakeOver != null && AiComputerViewRules.canTakeOver(status, handled = true)) {
            MonarchButton(
                text = texts.takeOver,
                onClick = onTakeOver,
                variant = AiComputerViewRules.takeOverVariant(status),
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Person,
            )
        }
        if (onHandBack != null && AiComputerViewRules.canHandBack(status, handled = true)) {
            MonarchButton(
                text = texts.handBack,
                onClick = onHandBack,
                variant = MonarchButtonVariant.Primary,
                size = MonarchButtonSize.Sm,
            )
        }
        if (pause != null && AiComputerViewRules.canPause(status, handled = true)) {
            MonarchButton(
                text = texts.pause,
                onClick = { pause(true) },
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Pause,
            )
        }
        if (pause != null && AiComputerViewRules.canResume(status, handled = true)) {
            MonarchButton(
                text = texts.resume,
                onClick = { pause(false) },
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Play,
            )
        }
        if (canStop) {
            // Stop sits at the end of the row, in the pale danger look, as on the web.
            Spacer(modifier = Modifier.weight(1f))
            MonarchButton(
                text = texts.stop,
                onClick = { confirming = true },
                variant = MonarchButtonVariant.DangerSubtle,
                size = MonarchButtonSize.Sm,
                leadingIcon = MonarchDefaultIcons.Stop,
            )
        }
    }
}

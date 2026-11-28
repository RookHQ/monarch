package dev.monarchui.components.intelligence.aivoicemode

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.intelligence.aiconversation.MonarchAiAgentRef
import dev.monarchui.components.intelligence.aiconversation.MonarchAiConversationMessage
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchDefaultIcons
import dev.monarchui.internal.MonarchImage
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme

/**
 * The surface for a live, two-way spoken conversation with an AI assistant: a
 * calm central visual, always-visible state text, live captions, and explicit
 * Mute, Interrupt, Captions and End controls.
 *
 * Built by Monarch from a `Column` with a Compose `Canvas` visual, a
 * scrolling captions log and `MonarchButton` controls: Material 3 has no
 * equivalent. Show the `Md` size in a full-screen `Dialog` (with
 * `DialogProperties(usePlatformDefaultWidth = false)`) or a screen of its own,
 * and the `Compact` size inside an assistant panel.
 *
 * It is UI only. The app owns the microphone, the audio, the connection and
 * audio focus, and reports what happens through [state], [level], [muted] and
 * [captions]; nothing is recorded, sent or ended without a button press. The
 * audio contract:
 * - Request audio focus (`AudioManager.requestAudioFocus` with
 *   `AUDIOFOCUS_GAIN` and `USAGE_VOICE_COMMUNICATION`) and set the audio mode
 *   before reporting `Connecting`; abandon focus after `Ended`.
 * - On a loss of audio focus, such as a phone call, pause and report `Muted`
 *   or `Reconnecting`; when focus returns, resume and report the live state
 *   again, or report `Ended`.
 * - On a route change (`AudioManager.ACTION_AUDIO_BECOMING_NOISY`, a headset
 *   or Bluetooth device coming or going), keep the call and keep reporting the
 *   state.
 * - Report `Error` with [error] when the call cannot continue.
 *
 * Captions are the conversation's own [MonarchAiConversationMessage] values:
 * user and assistant messages, oldest first. A message whose text is still
 * streaming is an interim caption: shown and updated in place, but hidden from
 * TalkBack. When its text is done it is announced once.
 *
 * The surface is one TalkBack group named "Voice conversation with" the
 * assistant's name. The state is always written and each change is announced
 * through a polite live region, except the first state and changes into
 * listening or speaking. Mute and Captions read as on or off. Space on a
 * keyboard toggles mute. The visual is decorative and holds still when
 * animations are off. When Interrupt goes away while it has focus, focus
 * moves to Mute.
 *
 * @param state where the call is, reported by the app.
 * @param modifier the modifier of the surface.
 * @param level the live audio level, 0 to 1, of whoever is talking. Drives the halo and the bars while
 * listening or speaking, and not when animations are off. Values outside are limited; not a number is 0.
 * @param agent the assistant. Its name labels the surface and its captions.
 * @param agentAvatar the assistant's picture, loaded by the app, shown beside its name and hidden from
 * TalkBack. `null` shows none.
 * @param captions the conversation's messages, oldest first.
 * @param captionsVisible whether captions show. `null` lets the surface keep its own value, on at first,
 * because captions are an accessibility need.
 * @param muted whether the microphone is muted. `null` means muted only in the `Muted` state; pass it when the
 * microphone stays muted while the assistant thinks or speaks.
 * @param elapsedSeconds how long the call has run. Shown as a clock while live and as the duration once ended.
 * @param error what went wrong, shown on the `Error` state. A general sentence by default.
 * @param size full size, or compact for a panel.
 * @param labels replacements for the built-in strings.
 * @param onCaptionsVisibleChange called with the wanted value when the person turns captions on or off.
 * @param onMuteChange called with the wanted value when the person presses Mute or Space. Shows Mute while the
 * call is live. Mute the microphone, then report it through [state] or [muted].
 * @param onInterrupt shows Interrupt while the assistant thinks or speaks; stop its reply in the handler.
 * @param onEnd shows End while the call is live. Only this button ends the call.
 * @param onRestart shows Start again on the `Ended` state.
 * @param onRetry shows Retry on the `Error` state.
 */
@Composable
public fun MonarchAiVoiceMode(
    state: MonarchAiVoiceModeState,
    modifier: Modifier = Modifier,
    level: Float = 0f,
    agent: MonarchAiAgentRef? = null,
    agentAvatar: Painter? = null,
    captions: List<MonarchAiConversationMessage> = emptyList(),
    captionsVisible: Boolean? = null,
    muted: Boolean? = null,
    elapsedSeconds: Int? = null,
    error: String? = null,
    size: MonarchAiVoiceModeSize = MonarchAiVoiceModeSize.Md,
    labels: MonarchAiVoiceModeLabels = MonarchAiVoiceModeLabels(),
    onCaptionsVisibleChange: ((Boolean) -> Unit)? = null,
    onMuteChange: ((Boolean) -> Unit)? = null,
    onInterrupt: (() -> Unit)? = null,
    onEnd: (() -> Unit)? = null,
    onRestart: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
) {
    val tokens = MonarchAiVoiceModeTokens
    val strings = monarchAiVoiceModeStrings(labels)
    val agentName = agent?.name?.takeIf { it.isNotBlank() } ?: strings.assistant
    val surfaceName = labels.label?.invoke(agentName) ?: stringResource(R.string.monarch_aivoicemode_label, agentName)
    val errorText = error ?: strings.errorFallback
    val captionsState = rememberCaptionsVisibility(captionsVisible, onCaptionsVisibleChange)
    val showCaptions = captionsState.first
    val lines = MonarchAiVoiceModeRules.captions(captions, agentName, strings.you)
    val live = MonarchAiVoiceModeRules.live(state)
    val isMuted = MonarchAiVoiceModeRules.muted(state, muted)
    val stateAnnouncement = stateAnnouncement(state, strings.state(state), errorText)
    val captionAnnouncement = captionAnnouncement(lines, showCaptions, strings)
    val muteOnSpace = if (live && onMuteChange != null) ({ onMuteChange(!isMuted) }) else null

    Column(
        modifier = modifier
            .surfaceBackground(size)
            .padding(tokens.padding(size))
            .semantics {
                isTraversalGroup = true
                contentDescription = surfaceName
            }
            .onSpace(muteOnSpace),
        verticalArrangement = Arrangement.spacedBy(tokens.padding(size), Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MonarchAiVoiceModeVisual(state = state, level = level, size = size)
        Summary(state, size, agentName, agentAvatar, isMuted, elapsedSeconds, errorText, strings, labels)
        if (showCaptions && (live || lines.isNotEmpty())) CaptionsLog(lines, size, strings)
        Controls(
            state = state,
            size = size,
            muted = isMuted,
            showCaptions = showCaptions,
            strings = strings,
            onCaptionsChange = captionsState.second,
            onMuteChange = onMuteChange,
            onInterrupt = onInterrupt,
            onEnd = onEnd,
            onRestart = onRestart,
            onRetry = onRetry,
        )
        // Always present, so each change is announced once as a change of these live regions.
        LiveRegion(stateAnnouncement)
        LiveRegion(captionAnnouncement)
    }
}

/** Whether captions show, and how to change it: the app's value when given, otherwise the surface's own. */
@Composable
private fun rememberCaptionsVisibility(
    captionsVisible: Boolean?,
    onCaptionsVisibleChange: ((Boolean) -> Unit)?,
): Pair<Boolean, (Boolean) -> Unit> {
    var own by rememberSaveable { mutableStateOf(true) }
    return (captionsVisible ?: own) to { visible ->
        if (captionsVisible == null) own = visible
        onCaptionsVisibleChange?.invoke(visible)
    }
}

/** Runs [action] when Space is pressed without a modifier key and no control inside used it. */
private fun Modifier.onSpace(action: (() -> Unit)?): Modifier = onKeyEvent { event ->
    val space = event.key == Key.Spacebar && event.type == KeyEventType.KeyDown &&
        !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed && !event.isShiftPressed
    if (space && action != null) {
        action()
        true
    } else {
        false
    }
}

@Composable
private fun Modifier.surfaceBackground(size: MonarchAiVoiceModeSize): Modifier {
    val colors = MonarchTheme.colors
    return if (size == MonarchAiVoiceModeSize.Md) {
        fillMaxWidth().background(colors.bgCanvas)
    } else {
        val (fill, border) = MonarchAiVoiceModeTokens.compactColors(colors)
        fillMaxWidth()
            .background(fill, MonarchShapeDefaults.Container)
            .border(MonarchShapeDefaults.BorderWidth, border, MonarchShapeDefaults.Container)
    }
}

@Composable
private fun ColumnScope.Summary(
    state: MonarchAiVoiceModeState,
    size: MonarchAiVoiceModeSize,
    agentName: String,
    agentAvatar: Painter?,
    muted: Boolean,
    elapsedSeconds: Int?,
    errorText: String,
    strings: MonarchAiVoiceModeStrings,
    labels: MonarchAiVoiceModeLabels,
) {
    val tokens = MonarchAiVoiceModeTokens
    val colors = MonarchTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val duration = elapsedSeconds?.let { MonarchAiVoiceModeRules.duration(it, locale) }
    Column(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.LineGap, Alignment.CenterHorizontally),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            if (agentAvatar != null) {
                MonarchImage(
                    painter = agentAvatar,
                    contentDescription = null,
                    modifier = Modifier.size(tokens.AvatarSize).clearAndSetSemantics { },
                    shape = MonarchShapeDefaults.Mark,
                ) { }
            }
            MonarchText(text = agentName, style = MonarchTextStyle.Label)
            if (MonarchAiVoiceModeRules.live(state) && duration != null) {
                val spoken = labels.elapsed?.let { "$it: $duration" }
                    ?: stringResource(R.string.monarch_aivoicemode_elapsed, duration)
                MonarchText(
                    text = duration,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
                    style = MonarchTextStyle.Label,
                    color = colors.textMuted,
                )
            }
        }
        MonarchText(
            text = strings.state(state),
            style = tokens.stateStyle(size),
            textAlign = TextAlign.Center,
            heading = false,
        )
        if (muted && state != MonarchAiVoiceModeState.Muted) {
            MonarchText(
                text = strings.microphoneMuted,
                style = MonarchTextStyle.Caption,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
        if (state == MonarchAiVoiceModeState.Error) {
            MonarchText(text = errorText, color = colors.dangerStrong, textAlign = TextAlign.Center)
        }
        if (state == MonarchAiVoiceModeState.Ended && duration != null) {
            val lasted = labels.lasted?.invoke(duration)
                ?: stringResource(R.string.monarch_aivoicemode_lasted, duration)
            MonarchText(text = lasted, color = colors.textMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CaptionsLog(
    lines: List<MonarchAiVoiceModeCaption>,
    size: MonarchAiVoiceModeSize,
    strings: MonarchAiVoiceModeStrings,
) {
    val tokens = MonarchAiVoiceModeTokens
    val scroll = rememberScrollState()
    StickToNewest(scroll, lines.lastOrNull()?.let { "${lines.size}:${it.id}:${it.text.length}:${it.final}" })
    val maxHeight = tokens.captionsHeight(size) * LocalDensity.current.fontScale
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            .semantics {
                isTraversalGroup = true
                contentDescription = strings.captionsLabel
            }
            .verticalScroll(scroll),
        verticalArrangement = Arrangement.spacedBy(tokens.LineGap),
    ) {
        if (lines.isEmpty()) {
            MonarchText(text = strings.captionsEmpty, color = MonarchTheme.colors.textMuted)
        }
        lines.forEach { line -> Caption(line, strings) }
    }
}

/** Keeps the newest caption in view, unless the person scrolled up to read. */
@Composable
private fun StickToNewest(scroll: ScrollState, tail: String?) {
    val threshold = with(LocalDensity.current) { MonarchAiVoiceModeTokens.StickThreshold.roundToPx() }
    var stuck by remember { mutableStateOf(true) }
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) stuck = scroll.value >= scroll.maxValue - threshold
        }
    }
    LaunchedEffect(scroll, tail) {
        snapshotFlow { scroll.maxValue }.collect { if (stuck) scroll.scrollTo(it) }
    }
}

@Composable
private fun Caption(line: MonarchAiVoiceModeCaption, strings: MonarchAiVoiceModeStrings) {
    val colors = MonarchTheme.colors
    val text: AnnotatedString = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = MonarchAiVoiceModeTokens.speakerColor(line.user, colors),
                fontWeight = FontWeight.SemiBold,
            ),
        ) { append(line.speaker) }
        append("\n")
        append(line.text)
    }
    val spoken = strings.caption(line.speaker, line.text)
    MonarchText(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                // An interim caption is visible but hidden from TalkBack until it is final.
                if (line.final) {
                    Modifier.clearAndSetSemantics { contentDescription = spoken }
                } else {
                    Modifier.semantics { hideFromAccessibility() }
                },
            ),
        color = colors.textDefault,
    )
}

@Composable
private fun Controls(
    state: MonarchAiVoiceModeState,
    size: MonarchAiVoiceModeSize,
    muted: Boolean,
    showCaptions: Boolean,
    strings: MonarchAiVoiceModeStrings,
    onCaptionsChange: (Boolean) -> Unit,
    onMuteChange: ((Boolean) -> Unit)?,
    onInterrupt: (() -> Unit)?,
    onEnd: (() -> Unit)?,
    onRestart: (() -> Unit)?,
    onRetry: (() -> Unit)?,
) {
    val tokens = MonarchAiVoiceModeTokens
    val buttonSize = tokens.buttonSize(size)
    when {
        MonarchAiVoiceModeRules.live(state) -> LiveControls(
            canInterrupt = MonarchAiVoiceModeRules.canInterrupt(state, onInterrupt != null),
            buttonSize = buttonSize,
            muted = muted,
            showCaptions = showCaptions,
            strings = strings,
            onCaptionsChange = onCaptionsChange,
            onMuteChange = onMuteChange,
            onInterrupt = onInterrupt,
            onEnd = onEnd,
        )
        state == MonarchAiVoiceModeState.Ended && onRestart != null ->
            MonarchButton(text = strings.restart, onClick = onRestart, size = buttonSize)
        state == MonarchAiVoiceModeState.Error && onRetry != null ->
            MonarchButton(text = strings.retry, onClick = onRetry, size = buttonSize)
    }
}

@Composable
private fun LiveControls(
    canInterrupt: Boolean,
    buttonSize: MonarchButtonSize,
    muted: Boolean,
    showCaptions: Boolean,
    strings: MonarchAiVoiceModeStrings,
    onCaptionsChange: (Boolean) -> Unit,
    onMuteChange: ((Boolean) -> Unit)?,
    onInterrupt: (() -> Unit)?,
    onEnd: (() -> Unit)?,
) {
    val muteFocus = remember { FocusRequester() }
    var interruptFocused by remember { mutableStateOf(false) }
    // Interrupt loses focus as it leaves; that loss must not hide that it had focus.
    val offered by rememberUpdatedState(canInterrupt)
    LaunchedEffect(canInterrupt) {
        if (!canInterrupt && interruptFocused) {
            interruptFocused = false
            if (onMuteChange != null) muteFocus.requestFocus()
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(MonarchAiVoiceModeTokens.ControlGap, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(MonarchAiVoiceModeTokens.ControlGap),
    ) {
        if (onMuteChange != null) {
            MonarchButton(
                text = strings.mute,
                leadingIcon = MonarchDefaultIcons.Mic,
                onClick = { onMuteChange(!muted) },
                modifier = Modifier
                    .focusRequester(muteFocus)
                    .semantics { toggleableState = ToggleableState(muted) },
                size = buttonSize,
            )
        }
        if (canInterrupt && onInterrupt != null) {
            MonarchButton(
                text = strings.interrupt,
                onClick = onInterrupt,
                modifier = Modifier.onFocusChanged { if (offered) interruptFocused = it.hasFocus },
                size = buttonSize,
            )
        }
        MonarchButton(
            text = strings.captions,
            leadingIcon = MonarchDefaultIcons.Captions,
            onClick = { onCaptionsChange(!showCaptions) },
            modifier = Modifier.semantics { toggleableState = ToggleableState(showCaptions) },
            variant = MonarchButtonVariant.Ghost,
            size = buttonSize,
        )
        if (onEnd != null) {
            MonarchButton(
                text = strings.end,
                onClick = onEnd,
                variant = MonarchButtonVariant.Danger,
                size = buttonSize,
                leadingIcon = MonarchDefaultIcons.Close,
            )
        }
    }
}

@Composable
private fun LiveRegion(text: String) {
    MonarchLiveStatus(text = text.takeIf { it.isNotEmpty() }, visible = false)
}

/**
 * The announcement of the last change of state: the new state text, with the error for `Error`. Empty at first
 * and after a change into listening or speaking.
 */
@Composable
private fun stateAnnouncement(state: MonarchAiVoiceModeState, stateText: String, errorText: String): String {
    val previous = remember { arrayOfNulls<MonarchAiVoiceModeState>(1) }
    var text by remember { mutableStateOf("") }
    val announceError = stringResource(R.string.monarch_aivoicemode_announce_error, stateText, errorText)
    LaunchedEffect(state) {
        val before = previous[0]
        previous[0] = state
        if (before == null || before == state) return@LaunchedEffect
        text = when {
            !MonarchAiVoiceModeRules.announces(before, state) -> ""
            state == MonarchAiVoiceModeState.Error -> announceError
            else -> stateText
        }
    }
    return text
}

/**
 * The announcement of the captions that became final since the last change, each once. Captions that are final
 * when the surface appears, or that become final while captions are hidden, are never announced.
 */
@Composable
private fun captionAnnouncement(
    lines: List<MonarchAiVoiceModeCaption>,
    visible: Boolean,
    strings: MonarchAiVoiceModeStrings,
): String {
    val announced = remember { lines.filter { it.final }.map { it.id }.toMutableSet() }
    var text by remember { mutableStateOf("") }
    LaunchedEffect(lines, visible) {
        val fresh = MonarchAiVoiceModeRules.unannounced(lines, announced)
        if (fresh.isEmpty()) return@LaunchedEffect
        announced += fresh.map { it.id }
        if (visible) text = fresh.joinToString(" ") { strings.caption(it.speaker, it.text) }
    }
    return text
}

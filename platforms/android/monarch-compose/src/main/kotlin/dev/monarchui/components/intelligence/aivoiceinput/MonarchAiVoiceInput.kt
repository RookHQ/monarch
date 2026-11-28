package dev.monarchui.components.intelligence.aivoiceinput

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeter
import dev.monarchui.components.collaboration.audiolevelmeter.MonarchAudioLevelMeterSize
import dev.monarchui.components.feedback.banner.MonarchBannerAnnouncement
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchLiveStatus
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme
import java.util.Locale

/**
 * A microphone button that dictates into a prompt: press to start and again
 * to stop, or hold to talk.
 *
 * Built from `MonarchButton`, `MonarchAudioLevelMeter` and
 * `MonarchInlineNotice` around a [MonarchAiSpeechRecognizer]; by default
 * [MonarchAiSystemSpeechRecognizer], the device's `android.speech.SpeechRecognizer`.
 * While listening it shows a level meter, the status ("Listening…") and the
 * words as they are recognised. Before the first session it asks for the
 * recogniser's missing permissions with the Activity Result API; when access is
 * refused it shows a notice with an "Open settings" action. It only hands text
 * back through [onTranscript] and never sends anything. It draws nothing when
 * the recogniser is not available.
 *
 * In toggle mode the button reads as on while listening. In push-to-talk mode,
 * which TalkBack users cannot hold, a double-tap starts and a second one stops.
 * Escape on a keyboard stops listening. "Listening…" and "Stopped listening."
 * are announced through a polite live region and a failure through the notice.
 * The level meter is decorative. The session is cancelled when the voice input
 * leaves the composition, and stopped when [enabled] becomes `false`.
 *
 * @param onTranscript called with recognised text. Guesses come with `final` false and may change; each settled
 * phrase comes once with `final` true. Put final text into the prompt; never send it automatically.
 * @param modifier the modifier of the voice input.
 * @param recognizer the speech engine. `null` uses [MonarchAiSystemSpeechRecognizer], which is experimental.
 * @param locale the language of the speech. `null` uses the device language.
 * @param mode press to start and stop, or hold to talk.
 * @param size the size of the button.
 * @param enabled whether the button can start a session.
 * @param showLevel shows a five-segment level meter while listening.
 * @param showTranscript shows the words under the button as they are recognised.
 * @param icon a microphone icon before the label, for example a `MonarchIcon` with a `null` content description.
 * Monarch bundles no icons.
 * @param labels replacements for the built-in strings.
 * @param onStateChange called with every change of state.
 */
@ExperimentalMonarchApi
@Composable
public fun MonarchAiVoiceInput(
    onTranscript: (text: String, final: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    recognizer: MonarchAiSpeechRecognizer? = null,
    locale: Locale? = null,
    mode: MonarchAiVoiceInputMode = MonarchAiVoiceInputMode.Toggle,
    size: MonarchButtonSize = MonarchButtonSize.Md,
    enabled: Boolean = true,
    showLevel: Boolean = true,
    showTranscript: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    labels: MonarchAiVoiceInputLabels = MonarchAiVoiceInputLabels(),
    onStateChange: ((MonarchAiVoiceInputState) -> Unit)? = null,
) {
    val context = LocalContext.current
    val engine = recognizer ?: remember(context) { MonarchAiSystemSpeechRecognizer(context.applicationContext) }
    val available = remember(engine, locale) { engine.isAvailable(locale) }
    if (!available) return
    val session = remember { MonarchAiVoiceInputSession() }
    session.onTranscript = onTranscript
    session.onStateChange = onStateChange
    session.announcements = MonarchAiVoiceInputAnnouncements(
        listening = monarchString(labels.listening, R.string.monarch_aivoiceinput_listening),
        stopped = monarchString(labels.stopped, R.string.monarch_aivoiceinput_stopped),
    )
    val start = rememberVoiceInputStart(session, engine, locale, mode == MonarchAiVoiceInputMode.Toggle, enabled)
    LaunchedEffect(enabled) { if (!enabled) session.stop() }
    DisposableEffect(session) { onDispose { session.cancel() } }
    VoiceInputBody(
        session = session,
        modifier = modifier,
        mode = mode,
        size = size,
        enabled = enabled,
        showLevel = showLevel,
        showTranscript = showTranscript,
        icon = icon,
        labels = labels,
        start = start,
    )
}

/**
 * What starts a session: it asks for the recogniser's missing permissions
 * with the Activity Result API first, and counts a refusal, or a missing
 * result registry, as access refused.
 */
@Composable
private fun rememberVoiceInputStart(
    session: MonarchAiVoiceInputSession,
    engine: MonarchAiSpeechRecognizer,
    locale: Locale?,
    continuous: Boolean,
    enabled: Boolean,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val begin = { session.start(scope, engine, locale, continuous) }
    val launcher = if (LocalActivityResultRegistryOwner.current != null) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) begin() else session.deny()
        }
    } else {
        null
    }
    return start@{
        if (!enabled || session.active) return@start
        val missing = engine.requiredPermissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            begin()
            return@start
        }
        session.awaitPermission()
        if (launcher != null) launcher.launch(missing.toTypedArray()) else session.deny()
    }
}

@Composable
private fun VoiceInputBody(
    session: MonarchAiVoiceInputSession,
    modifier: Modifier,
    mode: MonarchAiVoiceInputMode,
    size: MonarchButtonSize,
    enabled: Boolean,
    showLevel: Boolean,
    showTranscript: Boolean,
    icon: (@Composable () -> Unit)?,
    labels: MonarchAiVoiceInputLabels,
    start: () -> Unit,
) {
    Column(
        modifier = modifier.onPreviewKeyEvent { event ->
            val escape = event.key == Key.Escape && event.type == KeyEventType.KeyDown && session.active
            if (escape) session.stop()
            escape
        },
        verticalArrangement = Arrangement.spacedBy(MonarchAiVoiceInputTokens.StackGap),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchAiVoiceInputTokens.ControlGap),
            verticalArrangement = Arrangement.spacedBy(MonarchAiVoiceInputTokens.ControlGap),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceButton(mode, size, enabled, session.active, icon, labels, start = start, stop = session::stop)
            if (session.active && showLevel) {
                MonarchAudioLevelMeter(
                    level = session.level,
                    size = MonarchAudioLevelMeterSize.Sm,
                    segments = MonarchAiVoiceInputTokens.METER_SEGMENTS,
                    decorative = true,
                )
            }
            statusText(session.state, labels)?.let { status ->
                MonarchText(
                    text = status,
                    // Visible only: the live region announces it, so TalkBack does not read it twice.
                    modifier = Modifier.semantics { hideFromAccessibility() },
                    style = MonarchTextStyle.Caption,
                    color = MonarchTheme.colors.textMuted,
                )
            }
        }
        if (showTranscript && session.preview.isNotEmpty()) Transcript(session.preview, labels)
        val failure = session.failure
        if (failure != null && !session.active) FailureNotice(failure, labels)
        // Always present, so starting and stopping are each announced once as a change of this live region.
        MonarchLiveStatus(text = session.announcement.takeIf { it.isNotEmpty() }, visible = false)
    }
}

@Composable
private fun Transcript(text: String, labels: MonarchAiVoiceInputLabels) {
    val spoken = labels.transcript?.let { "$it: $text" }
        ?: stringResource(R.string.monarch_aivoiceinput_transcript, text)
    MonarchText(text = text, modifier = Modifier.clearAndSetSemantics { contentDescription = spoken })
}

@Composable
private fun VoiceButton(
    mode: MonarchAiVoiceInputMode,
    size: MonarchButtonSize,
    enabled: Boolean,
    active: Boolean,
    icon: (@Composable () -> Unit)?,
    labels: MonarchAiVoiceInputLabels,
    start: () -> Unit,
    stop: () -> Unit,
) {
    when (mode) {
        MonarchAiVoiceInputMode.Toggle -> MonarchButton(
            text = monarchString(labels.dictate, R.string.monarch_aivoiceinput_dictate),
            onClick = { if (active) stop() else start() },
            modifier = Modifier.semantics { toggleableState = ToggleableState(active) },
            variant = MonarchAiVoiceInputTokens.buttonVariant(active),
            size = size,
            enabled = enabled,
            leadingIcon = icon,
        )
        MonarchAiVoiceInputMode.PushToTalk -> MonarchAiVoiceInputHoldButton(
            text = monarchString(labels.holdToTalk, R.string.monarch_aivoiceinput_hold_to_talk),
            hint = monarchString(labels.holdHint, R.string.monarch_aivoiceinput_hold_hint),
            active = active,
            size = size,
            enabled = enabled,
            icon = icon,
            onPress = start,
            onRelease = stop,
            onToggle = { if (active) stop() else start() },
        )
    }
}

@Composable
private fun FailureNotice(failure: MonarchAiSpeechError, labels: MonarchAiVoiceInputLabels) {
    val context = LocalContext.current
    val message = when (failure) {
        MonarchAiSpeechError.Denied -> monarchString(labels.denied, R.string.monarch_aivoiceinput_denied)
        MonarchAiSpeechError.NoSpeech -> monarchString(labels.noSpeech, R.string.monarch_aivoiceinput_no_speech)
        MonarchAiSpeechError.NoMicrophone ->
            monarchString(labels.noMicrophone, R.string.monarch_aivoiceinput_no_microphone)
        MonarchAiSpeechError.Unavailable ->
            monarchString(labels.unavailable, R.string.monarch_aivoiceinput_unavailable)
        MonarchAiSpeechError.Failed -> monarchString(labels.error, R.string.monarch_aivoiceinput_error)
    }
    val openSettings = monarchString(labels.openSettings, R.string.monarch_aivoiceinput_open_settings)
    MonarchInlineNotice(
        message = message,
        tone = MonarchAiVoiceInputTokens.tone(failure),
        action = if (MonarchAiVoiceInputTokens.offersSettings(failure)) {
            {
                MonarchButton(
                    text = openSettings,
                    onClick = {
                        val intent = Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
            }
        } else {
            null
        },
        announcement = MonarchBannerAnnouncement.Polite,
    )
}

@Composable
private fun statusText(state: MonarchAiVoiceInputState, labels: MonarchAiVoiceInputLabels): String? = when (state) {
    MonarchAiVoiceInputState.Starting -> monarchString(labels.starting, R.string.monarch_aivoiceinput_starting)
    MonarchAiVoiceInputState.Listening -> monarchString(labels.listening, R.string.monarch_aivoiceinput_listening)
    else -> null
}

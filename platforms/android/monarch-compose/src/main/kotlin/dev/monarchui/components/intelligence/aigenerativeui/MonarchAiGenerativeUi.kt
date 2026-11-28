package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.monarchui.R
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.components.feedback.inlinenotice.MonarchInlineNotice
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUiPart
import dev.monarchui.components.intelligence.aigenerativeui.MonarchAiGenerativeUiTokens.Outcome
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.internal.monarchString

/**
 * Generative UI: renders a component the model asked for in a `ui` part of
 * the conversation, but only when its name is in the app's allow-listed
 * [MonarchAiUiRegistry].
 *
 * Props are untrusted data: they are sanitised (unsafe links dropped, sizes
 * limited), decoded by the component's own decoder into its own typed model
 * and checked by its validator. Nothing is evaluated and model output is
 * never rendered as markup. An unknown name or invalid props show a short
 * neutral `MonarchInlineNotice` instead, so one bad part never breaks the
 * thread. While props stream, the component renders as soon as they decode,
 * or shows a circular `MonarchProgress` named "Loading". Each refusal is
 * reported once through [onReject], for logging.
 *
 * @param part the `ui` part of the message: a component name and its props.
 * @param registry the allow-list. Only these names render.
 * @param modifier the modifier of the result.
 * @param labels replacements for the built-in strings.
 * @param onReject called once when a part is refused, with the reason and the part.
 * @param onAction receives the actions the component sends through its [MonarchAiUiEmit], with the payload
 * sanitised and the part named. Without it the channel is not connected.
 */
@Composable
public fun MonarchAiGenerativeUi(
    part: MonarchAiUiPart,
    registry: MonarchAiUiRegistry,
    modifier: Modifier = Modifier,
    labels: MonarchAiGenerativeUiLabels = MonarchAiGenerativeUiLabels(),
    onReject: ((MonarchAiUiRejection, MonarchAiUiPart) -> Unit)? = null,
    onAction: ((MonarchAiUiAction) -> Unit)? = null,
) {
    val outcome = MonarchAiGenerativeUiTokens.outcome(part, registry)
    val currentAction by rememberUpdatedState(onAction)
    val connected = onAction != null
    val emit = remember(part, connected) {
        if (connected) {
            MonarchAiUiEmit { type, payload ->
                currentAction?.invoke(MonarchAiGenerativeUiTokens.action(type, payload, part))
            }
        } else {
            MonarchAiUiEmit()
        }
    }
    val rejection = (outcome as? Outcome.Rejected)?.reason
    val currentReject by rememberUpdatedState(onReject)
    LaunchedEffect(rejection, part) {
        if (rejection != null) currentReject?.invoke(rejection, part)
    }
    Box(modifier = modifier) {
        when (outcome) {
            is Outcome.Rendered -> outcome.content(emit)
            Outcome.Pending -> Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = MonarchAiGenerativeUiTokens.PendingMinHeight),
                contentAlignment = Alignment.Center,
            ) {
                MonarchProgress(
                    variant = MonarchProgressVariant.Circular,
                    label = monarchString(labels.loading, R.string.monarch_aigenerativeui_loading),
                )
            }
            is Outcome.Rejected -> MonarchInlineNotice(
                message = when (outcome.reason) {
                    MonarchAiUiRejection.UnknownComponent ->
                        monarchString(labels.unknown, R.string.monarch_aigenerativeui_unknown)
                    MonarchAiUiRejection.InvalidProps ->
                        monarchString(labels.invalid, R.string.monarch_aigenerativeui_invalid)
                },
                tone = MonarchTone.Neutral,
            )
        }
    }
}

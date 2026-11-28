package dev.monarchui.components.intelligence.aigenerativeui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import dev.monarchui.accessibility.MonarchTouchTarget
import dev.monarchui.components.intelligence.aiconversation.MonarchAiJSON
import dev.monarchui.components.intelligence.aiconversation.MonarchAiPartState
import dev.monarchui.components.intelligence.aiconversation.MonarchAiUiPart

/** Component tokens and pure logic of MonarchAiGenerativeUi. */
internal object MonarchAiGenerativeUiTokens {
    /** The smallest height of the progress indicator while props stream. */
    val PendingMinHeight: Dp = MonarchTouchTarget.Minimum

    /** What the component shows for a part. */
    sealed interface Outcome {
        /** The component. */
        class Rendered(val content: @Composable (MonarchAiUiEmit) -> Unit) : Outcome

        /** A progress indicator: the props are still arriving. */
        data object Pending : Outcome

        /** A notice: the part was refused. */
        data class Rejected(val reason: MonarchAiUiRejection) : Outcome
    }

    /** The props to decode: streaming JSON text is read as far as it goes; complete props must already be data. */
    fun rawProps(part: MonarchAiUiPart): MonarchAiJSON {
        val props = part.props
        if (part.state != MonarchAiPartState.Streaming || props !is MonarchAiJSON.Text) return props
        return MonarchAiPartialJson.parse(props.value) ?: MonarchAiJSON.Object(emptyMap())
    }

    /**
     * Resolves a part against the registry. A part that is still streaming
     * is never refused for its props, because they may still become valid.
     */
    fun outcome(part: MonarchAiUiPart, registry: MonarchAiUiRegistry): Outcome {
        val entry = registry.entry(part.component) ?: return Outcome.Rejected(MonarchAiUiRejection.UnknownComponent)
        val streaming = part.state == MonarchAiPartState.Streaming
        if (streaming && entry.streaming == MonarchAiUiStreaming.OnComplete) return Outcome.Pending
        val prepared = MonarchAiUiSanitizer.sanitize(rawProps(part))?.let(entry.prepare)
            ?: MonarchAiUiPrepared.Refused(MonarchAiUiRejection.InvalidProps)
        return when (prepared) {
            is MonarchAiUiPrepared.Ready -> Outcome.Rendered(prepared.content)
            is MonarchAiUiPrepared.Refused -> if (streaming) Outcome.Pending else Outcome.Rejected(prepared.reason)
        }
    }

    /** The action a component sent, with its payload sanitised and the part named. */
    fun action(type: String, payload: MonarchAiJSON?, part: MonarchAiUiPart): MonarchAiUiAction = MonarchAiUiAction(
        type = type,
        payload = payload?.let { MonarchAiUiSanitizer.clean(it, key = "payload", depth = 0) },
        component = part.component,
        partId = part.id,
        toolCallId = part.toolCallId,
    )

    /** The manifest lines: `- Name: description`, or `- Name` without one. */
    fun manifest(entries: List<Pair<String, String?>>): String = entries.joinToString("\n") { (name, description) ->
        if (description.isNullOrEmpty()) "- $name" else "- $name: $description"
    }
}

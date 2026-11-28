package com.rookhq.monarch.components.intelligence.aipromptinput

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import androidx.compose.ui.input.key.Key

/** What a hardware key does in a `MonarchAiPromptInput`. */
internal enum class AiPromptInputKeyAction { Submit, Stop, None }

/** A key press, reduced to what the prompt input needs. */
internal data class AiPromptInputKey(
    val key: Key,
    val shift: Boolean,
    val command: Boolean,
    val hardware: Boolean,
)

/** The hardware keyboard and received-content rules of MonarchAiPromptInput. */
internal object MonarchAiPromptInputRules {
    /**
     * What a key press does. Only a hardware keyboard acts: Enter (without
     * Shift) sends with [MonarchAiPromptInputSubmitOn.Enter], Ctrl or Meta
     * with Enter sends with either setting, and Escape stops a running
     * response when the app can stop it.
     */
    fun keyAction(
        press: AiPromptInputKey,
        submitOn: MonarchAiPromptInputSubmitOn,
        busy: Boolean,
        canStop: Boolean,
    ): AiPromptInputKeyAction {
        if (!press.hardware) return AiPromptInputKeyAction.None
        return when (press.key) {
            Key.Enter, Key.NumPadEnter -> {
                val sends = press.command || (submitOn == MonarchAiPromptInputSubmitOn.Enter && !press.shift)
                if (sends) AiPromptInputKeyAction.Submit else AiPromptInputKeyAction.None
            }
            Key.Escape -> if (busy && canStop) AiPromptInputKeyAction.Stop else AiPromptInputKeyAction.None
            else -> AiPromptInputKeyAction.None
        }
    }

    /** Whether any of [mimeTypes] matches one of the [accept] patterns, such as `image/` and a star. */
    fun accepts(mimeTypes: List<String>, accept: List<String>): Boolean =
        mimeTypes.any { type -> accept.any { ClipDescription.compareMimeTypes(type, it) } }

    /** The content URIs of [clip] when its types match [accept]; empty otherwise. */
    fun acceptedUris(clip: ClipData, accept: List<String>): List<Uri> {
        val description = clip.description
        val types = (0 until description.mimeTypeCount).map { description.getMimeType(it) }
        if (!accepts(types, accept)) return emptyList()
        return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
    }
}

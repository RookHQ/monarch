package com.rookhq.monarch.components.intelligence.aitoolapproval

import com.rookhq.monarch.components.intelligence.aiconversation.MonarchAiJSON
import com.rookhq.monarch.components.intelligence.aitoolcall.MonarchAiToolCallFormat
import com.rookhq.monarch.components.intelligence.aitoolcall.MonarchAiToolCallTokens

/** How MonarchAiToolApproval writes, edits and reads a tool call's arguments. */
internal object MonarchAiToolApprovalFormat {
    /** The arguments as JSON text for the editor: an object or array indented by two spaces; no input is "{}". */
    fun editableText(input: MonarchAiJSON): String {
        val value = if (input == MonarchAiJSON.Null) MonarchAiJSON.Object(emptyMap()) else input
        return MonarchAiToolCallFormat.format(value).text
    }

    /** What approving sends, from the editor's state. */
    fun edit(editing: Boolean, text: String, original: String): AiToolApprovalEdit {
        if (!editing || text.trim() == original.trim()) return AiToolApprovalEdit.Unchanged
        return try {
            AiToolApprovalEdit.Edited(MonarchAiJSON.parse(text))
        } catch (@Suppress("SwallowedException") invalid: IllegalArgumentException) {
            // Shown to the person as "Fix the JSON before approving.".
            AiToolApprovalEdit.Invalid
        }
    }

    /** The arguments as labelled rows, or `null` when the input is not an object. */
    fun rows(input: MonarchAiJSON, argumentLabels: Map<String, String>): List<Pair<String, String>>? =
        (input as? MonarchAiJSON.Object)?.members?.map { (key, value) ->
            (argumentLabels[key] ?: MonarchAiToolCallTokens.humanize(key)) to readable(value)
        }

    /** A value in words: text as it is, numbers and booleans as JSON, a list of plain values joined by commas. */
    fun readable(value: MonarchAiJSON): String = when (value) {
        MonarchAiJSON.Null -> "—"
        is MonarchAiJSON.Text -> value.value
        is MonarchAiJSON.Number, is MonarchAiJSON.Bool -> value.toJsonString()
        is MonarchAiJSON.Array ->
            if (value.values.all { it !is MonarchAiJSON.Array && it !is MonarchAiJSON.Object }) {
                value.values.joinToString(", ") { if (it is MonarchAiJSON.Text) it.value else it.toJsonString() }
            } else {
                MonarchAiToolCallFormat.format(value).text
            }
        is MonarchAiJSON.Object -> MonarchAiToolCallFormat.format(value).text
    }

    /** The reason sent with a denial: the trimmed text, or `null` when empty. */
    fun reason(text: String): String? = text.trim().ifEmpty { null }
}

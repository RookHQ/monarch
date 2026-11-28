package dev.monarchui.components.intelligence.aimentioncontext

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.painter.Painter
import dev.monarchui.components.intelligence.aicontextchips.MonarchAiContextItem

/**
 * One search result in a `MonarchAiMentionContext`.
 *
 * @property id the stable id; it becomes the context chip's id.
 * @property kind what the result points at.
 * @property label the display name, already localised.
 * @property description the secondary line: a path, a role, a record type.
 * @property tokens the estimated tokens it would add to the context.
 * @property image a person's picture, loaded by the app. `null` shows initials.
 * @property enabled `false` shows the result but it cannot be chosen, such as a document the assistant may not read.
 */
@Immutable
public data class MonarchAiMentionItem(
    public val id: String,
    public val kind: MonarchAiMentionKind,
    public val label: String,
    public val description: String? = null,
    public val tokens: Int? = null,
    public val image: Painter? = null,
    public val enabled: Boolean = true,
) {
    /** The chip to add to `MonarchAiContextChips` once the result is chosen. */
    public val contextItem: MonarchAiContextItem
        get() = MonarchAiContextItem(
            id = id,
            kind = kind.contextKind,
            label = label,
            tokens = tokens,
            description = description,
        )
}

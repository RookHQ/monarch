package dev.monarchui.components.intelligence.aicontextchips

import androidx.compose.runtime.Immutable

/**
 * One thing the AI will see with the next prompt.
 *
 * @property id a stable id, passed back to `onRemove`.
 * @property kind what kind of thing it is.
 * @property label its name, such as a file name. Already localised.
 * @property tokens how many tokens it takes, or `null` when unknown. Negative counts are shown as 0.
 * @property description a longer description, read by TalkBack after the chip.
 * @property removable whether the person can remove it. Even when `true`, it is only removable when the
 * chips have an `onRemove`.
 * @property group a short word shown before the label, such as "Page" or "Selected". `null` shows the label alone.
 */
@Immutable
public class MonarchAiContextItem(
    public val id: String,
    public val kind: MonarchAiContextKind,
    public val label: String,
    public val tokens: Int? = null,
    public val description: String? = null,
    public val removable: Boolean = true,
    public val group: String? = null,
)

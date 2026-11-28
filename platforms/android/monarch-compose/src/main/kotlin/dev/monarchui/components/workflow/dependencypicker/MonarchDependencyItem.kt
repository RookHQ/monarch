package dev.monarchui.components.workflow.dependencypicker

import androidx.compose.runtime.Immutable
import dev.monarchui.components.workflow.statuspicker.MonarchStatusOption

/**
 * An item that can be linked, such as an issue.
 *
 * @property id a stable id.
 * @property title the title, already localised.
 * @property key a short key shown before the title, such as `STR-12`.
 * @property status the current status, shown with its glyph and read by name.
 */
@Immutable
public class MonarchDependencyItem(
    public val id: String,
    public val title: String,
    public val key: String? = null,
    public val status: MonarchStatusOption? = null,
)

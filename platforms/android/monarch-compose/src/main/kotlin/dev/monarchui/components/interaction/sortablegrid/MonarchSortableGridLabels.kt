package dev.monarchui.components.interaction.sortablegrid

import androidx.compose.runtime.Immutable
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection

/**
 * Replacements for the built-in strings of a `MonarchSortableGrid`. Every
 * `null` uses the built-in string named after it in `strings_sortablegrid.xml`.
 * The functions receive the item's label and, for the announcements, its
 * position from one and the number of items.
 *
 * @property handle the spoken name of a tile's drag handle: "Reorder Revenue".
 * @property pickedUp announced when a drag starts: "Picked up Revenue. Position 2 of 6.".
 * @property moved announced after a move by an action or an arrow key: "Revenue moved to position 4 of 6.".
 * @property dropped announced when a drag ends: "Revenue dropped at position 4 of 6.".
 * @property cancelled announced when a drag is cancelled: "Reorder cancelled. Revenue returned to position 2
 * of 6.".
 * @property actions replacements for the names of the handle's TalkBack actions: "Move up", "Move down",
 * "Move backward" and "Move forward".
 */
@Immutable
public class MonarchSortableGridLabels(
    public val handle: ((label: String) -> String)? = null,
    public val pickedUp: ((label: String, position: Int, total: Int) -> String)? = null,
    public val moved: ((label: String, position: Int, total: Int) -> String)? = null,
    public val dropped: ((label: String, position: Int, total: Int) -> String)? = null,
    public val cancelled: ((label: String, position: Int, total: Int) -> String)? = null,
    public val actions: Map<MonarchDragHandleDirection, String> = emptyMap(),
)

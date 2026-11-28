package com.rookhq.monarch.components.interaction.reorderablelist

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.interaction.draghandle.MonarchDragHandleDirection

/**
 * Replacements for the built-in strings of a `MonarchReorderableList`. Every
 * `null` uses the built-in string named after it in
 * `strings_reorderablelist.xml`. The functions receive the item's label and,
 * for the announcements, its position from one and the number of items.
 *
 * @property handle the spoken name of a row's drag handle: "Reorder Design review".
 * @property moveUp the spoken name of a row's move up button: "Move Design review up".
 * @property moveDown the spoken name of a row's move down button: "Move Design review down".
 * @property up the visible text of the move up button: "Up".
 * @property down the visible text of the move down button: "Down".
 * @property pickedUp announced when a drag starts: "Picked up Design review. Position 2 of 5.".
 * @property moved announced after a move by an action or a button: "Design review moved to position 3 of 5.".
 * @property dropped announced when a drag ends: "Design review dropped at position 3 of 5.".
 * @property cancelled announced when a drag is cancelled: "Reorder cancelled. Design review returned to
 * position 2 of 5.".
 * @property actions replacements for the names of the handle's TalkBack actions, "Move up" and "Move down".
 */
@Immutable
public class MonarchReorderableListLabels(
    public val handle: ((label: String) -> String)? = null,
    public val moveUp: ((label: String) -> String)? = null,
    public val moveDown: ((label: String) -> String)? = null,
    public val up: String? = null,
    public val down: String? = null,
    public val pickedUp: ((label: String, position: Int, total: Int) -> String)? = null,
    public val moved: ((label: String, position: Int, total: Int) -> String)? = null,
    public val dropped: ((label: String, position: Int, total: Int) -> String)? = null,
    public val cancelled: ((label: String, position: Int, total: Int) -> String)? = null,
    public val actions: Map<MonarchDragHandleDirection, String> = emptyMap(),
)

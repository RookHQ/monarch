package dev.monarchui.components.workflow.kanbanboard

import androidx.compose.runtime.Immutable
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection

/**
 * Replacements for the built-in strings of a `MonarchKanbanBoard`. Every
 * `null` keeps the built-in string `monarch_kanbanboard_<key>`. Cards are named
 * by their plain-text labels; positions count from one.
 *
 * @property handle the TalkBack name of a card's drag handle: "Move Fix login".
 * @property moveMenu the TalkBack name of a card's Move to button: "Move Fix login to another column".
 * @property move the visible text of the Move to button, "Move".
 * @property moveTo a choice of the Move to menu: "Move to Done".
 * @property countText the visible count of a column, from the count and the limit (`null` without one): "3 / 5".
 * @property count what TalkBack reads for a column's count, from the count and the limit (`null` without one):
 * "3 cards, limit 5, at the limit".
 * @property empty the text of an empty column, "No cards".
 * @property blocked shown on a column that refuses the card being dragged, "Not allowed here".
 * @property pickedUp announced when a drag starts: "Picked up Fix login. To do, position 2 of 5."
 * @property moved announced after a move by an action: "Fix login: Doing, position 1 of 3."
 * @property dropped announced when a drag ends or the menu moves a card: "Fix login dropped in Doing, position 1
 * of 3."
 * @property cancelled announced when a drag is cancelled: "Move cancelled. Fix login returned to To do."
 * @property notAllowed announced when a card is dropped on a column that refuses it: "Fix login cannot move to Done."
 * @property failed announced when `onMove` throws: "Could not move Fix login. It is back in To do."
 * @property actions replacements for the names of the handle's TalkBack actions: "Move up", "Move down", "Move
 * backward" and "Move forward".
 */
@Immutable
public class MonarchKanbanBoardLabels(
    public val handle: ((card: String) -> String)? = null,
    public val moveMenu: ((card: String) -> String)? = null,
    public val move: String? = null,
    public val moveTo: ((column: String) -> String)? = null,
    public val countText: ((count: Int, limit: Int?) -> String)? = null,
    public val count: ((count: Int, limit: Int?) -> String)? = null,
    public val empty: String? = null,
    public val blocked: String? = null,
    public val pickedUp: ((card: String, column: String, position: Int, total: Int) -> String)? = null,
    public val moved: ((card: String, column: String, position: Int, total: Int) -> String)? = null,
    public val dropped: ((card: String, column: String, position: Int, total: Int) -> String)? = null,
    public val cancelled: ((card: String, column: String) -> String)? = null,
    public val notAllowed: ((card: String, column: String) -> String)? = null,
    public val failed: ((card: String, column: String) -> String)? = null,
    public val actions: Map<MonarchDragHandleDirection, String> = emptyMap(),
)

package dev.monarchui.components.workflow.actioneditor

import androidx.compose.runtime.Immutable

/**
 * Replacements for the built-in strings of a `MonarchActionEditor`. Every `null` keeps the built-in string
 * `monarch_actioneditor_<key>`.
 *
 * @property label the TalkBack name of the list, "Actions".
 * @property action the heading of one action, from its position: "Action 1".
 * @property type the label of the type menu, "Action".
 * @property remove the visible text of the remove button without an icon, "Remove".
 * @property removeItem the TalkBack name of the remove button, from the position.
 * @property moveUp the visible text of the move up button.
 * @property moveDown the visible text of the move down button.
 * @property moveUpItem the TalkBack name of the move up button, from the position.
 * @property moveDownItem the TalkBack name of the move down button, from the position.
 * @property moved announced after a move, from the type's name, the new position and the count.
 * @property add the add button, "Add action".
 * @property empty shown when there are no actions.
 * @property choose the empty choice of an optional menu and the placeholder of menus, "Choose…".
 * @property required the error of an empty required parameter, from its label.
 * @property outOfRange the error of a number out of range, from its label.
 * @property unknownType the name of a stored type the list no longer has, from its key.
 * @property unavailable the warning on an action whose type is disabled; the type's reason follows it.
 * @property maxReached shown instead of the add button at the limit, from the limit.
 */
@Immutable
public class MonarchActionEditorLabels(
    public val label: String? = null,
    public val action: ((position: Int) -> String)? = null,
    public val type: String? = null,
    public val remove: String? = null,
    public val removeItem: ((position: Int) -> String)? = null,
    public val moveUp: String? = null,
    public val moveDown: String? = null,
    public val moveUpItem: ((position: Int) -> String)? = null,
    public val moveDownItem: ((position: Int) -> String)? = null,
    public val moved: ((type: String, position: Int, count: Int) -> String)? = null,
    public val add: String? = null,
    public val empty: String? = null,
    public val choose: String? = null,
    public val required: ((label: String) -> String)? = null,
    public val outOfRange: ((label: String) -> String)? = null,
    public val unknownType: ((key: String) -> String)? = null,
    public val unavailable: String? = null,
    public val maxReached: ((max: Int) -> String)? = null,
)

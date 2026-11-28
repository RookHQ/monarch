package com.rookhq.monarch.components.files.pagetree

import androidx.compose.runtime.Immutable
import com.rookhq.monarch.components.interaction.reorderablelist.MonarchReorderableListLabels

/**
 * Replacements for the built-in strings of a `MonarchPageTree`. Every `null`
 * uses the built-in string named after it in `strings_pagetree.xml`.
 *
 * @property label the TalkBack name of the tree: "Pages".
 * @property untitled the title of a page whose title is blank: "Untitled page".
 * @property number writes a page number: "01". The default pads to two digits in the locale's digits.
 * @property noPages shown under an empty group without an add action: "No pages yet.".
 * @property addPage writes the TalkBack name of a group's add button: "Add page to Basics".
 * @property addPageText the visible text of a group's add button when no icon is given: "Add page".
 * @property addFirstPage the button under an empty group: "Add first page".
 * @property addGroup the button after the groups: "Add group".
 * @property groupActions writes the TalkBack name of a group's menu button: "Actions for Basics".
 * @property pageActions writes the TalkBack name of a page's menu button: "Actions for Welcome".
 * @property more the visible text of a menu button when no icon is given: "More".
 * @property renameGroup the menu item that renames a group: "Rename group".
 * @property moveGroupUp the menu item that moves a group up: "Move group up".
 * @property moveGroupDown the menu item that moves a group down: "Move group down".
 * @property deleteGroup the menu item and the confirm button that delete a group: "Delete group".
 * @property moveTo writes the menu item that moves a page to another group: "Move to Guides".
 * @property deletePage the menu item and the confirm button that delete a page: "Delete page".
 * @property addGroupTitle the title of the dialog that adds a group: "Add a group".
 * @property groupName the label of the name field: "Group name".
 * @property newGroupName the name the add dialog starts with: "New group".
 * @property renameSubmit the confirm button of the rename dialog: "Rename".
 * @property cancel the button that closes a dialog: "Cancel".
 * @property deleteGroupTitle writes the title of the delete group dialog: "Delete the “Basics” group?".
 * @property deleteGroupConsequence writes what else is deleted from the number of pages: "Its 3 pages are
 * deleted too".
 * @property typeToConfirm writes the label of the field that asks for the group's name: "Type Basics to
 * confirm".
 * @property deletePageTitle writes the title of the delete page dialog: "Delete “Welcome”?".
 * @property cannotUndo the warning in the delete dialogs: "This cannot be undone.".
 * @property groupMoved writes the announcement after a group moves, from its name, new position from 1 and the
 * number of groups: "Basics moved to position 2 of 3.".
 * @property pageMovedTo writes the announcement after a page moves to another group: "Welcome moved to
 * Guides.".
 * @property reorderableList the strings of the drag reorder of the pages of a group.
 */
@Immutable
public class MonarchPageTreeLabels(
    public val label: String? = null,
    public val untitled: String? = null,
    public val number: ((Int) -> String)? = null,
    public val noPages: String? = null,
    public val addPage: ((group: String) -> String)? = null,
    public val addPageText: String? = null,
    public val addFirstPage: String? = null,
    public val addGroup: String? = null,
    public val groupActions: ((group: String) -> String)? = null,
    public val pageActions: ((page: String) -> String)? = null,
    public val more: String? = null,
    public val renameGroup: String? = null,
    public val moveGroupUp: String? = null,
    public val moveGroupDown: String? = null,
    public val deleteGroup: String? = null,
    public val moveTo: ((group: String) -> String)? = null,
    public val deletePage: String? = null,
    public val addGroupTitle: String? = null,
    public val groupName: String? = null,
    public val newGroupName: String? = null,
    public val renameSubmit: String? = null,
    public val cancel: String? = null,
    public val deleteGroupTitle: ((group: String) -> String)? = null,
    public val deleteGroupConsequence: ((pages: Int) -> String)? = null,
    public val typeToConfirm: ((group: String) -> String)? = null,
    public val deletePageTitle: ((page: String) -> String)? = null,
    public val cannotUndo: String? = null,
    public val groupMoved: ((group: String, position: Int, total: Int) -> String)? = null,
    public val pageMovedTo: ((page: String, group: String) -> String)? = null,
    public val reorderableList: MonarchReorderableListLabels = MonarchReorderableListLabels(),
)

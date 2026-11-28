package dev.monarchui.components.files.pagetree

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import dev.monarchui.R
import dev.monarchui.internal.monarchString

/** The strings of one tree, with the caller's replacements applied. */
@Suppress("LongParameterList")
internal class PageTreeTexts(
    val label: String,
    val untitled: String,
    val number: (Int) -> String,
    val noPages: String,
    val addPage: (String) -> String,
    val addPageText: String,
    val addFirstPage: String,
    val addGroup: String,
    val groupActions: (String) -> String,
    val pageActions: (String) -> String,
    val more: String,
    val renameGroup: String,
    val moveGroupUp: String,
    val moveGroupDown: String,
    val deleteGroup: String,
    val moveTo: (String) -> String,
    val deletePage: String,
    val addGroupTitle: String,
    val groupName: String,
    val newGroupName: String,
    val renameSubmit: String,
    val cancel: String,
    val deleteGroupTitle: (String) -> String,
    val deleteGroupConsequence: (Int) -> String,
    val typeToConfirm: (String) -> String,
    val deletePageTitle: (String) -> String,
    val cannotUndo: String,
    val groupMoved: (String, Int, Int) -> String,
    val pageMovedTo: (String, String) -> String,
)

/** Reads every built-in string once, so the callbacks can format them outside composition. */
@Composable
internal fun pageTreeTexts(labels: MonarchPageTreeLabels, groups: List<MonarchPageTreeGroup>): PageTreeTexts {
    val locale = LocalConfiguration.current.locales[0]
    val addPage = stringResource(R.string.monarch_pagetree_add_page)
    val groupActions = stringResource(R.string.monarch_pagetree_group_actions)
    val pageActions = stringResource(R.string.monarch_pagetree_page_actions)
    val moveTo = stringResource(R.string.monarch_pagetree_move_to)
    val deleteGroupTitle = stringResource(R.string.monarch_pagetree_delete_group_title)
    val typeToConfirm = stringResource(R.string.monarch_pagetree_type_to_confirm)
    val deletePageTitle = stringResource(R.string.monarch_pagetree_delete_page_title)
    val groupMoved = stringResource(R.string.monarch_pagetree_group_moved)
    val pageMovedTo = stringResource(R.string.monarch_pagetree_page_moved_to)
    // Plurals need the count while composing: resolve one per group size in use.
    val consequences = groups.map { it.pages.size }.distinct().associateWith { count ->
        pluralStringResource(R.plurals.monarch_pagetree_delete_group_consequence, count, count)
    }
    return PageTreeTexts(
        label = monarchString(labels.label, R.string.monarch_pagetree_label),
        untitled = monarchString(labels.untitled, R.string.monarch_pagetree_untitled),
        number = labels.number ?: { pageTreeNumber(it, locale) },
        noPages = monarchString(labels.noPages, R.string.monarch_pagetree_no_pages),
        addPage = labels.addPage ?: { addPage.format(it) },
        addPageText = monarchString(labels.addPageText, R.string.monarch_pagetree_add_page_text),
        addFirstPage = monarchString(labels.addFirstPage, R.string.monarch_pagetree_add_first_page),
        addGroup = monarchString(labels.addGroup, R.string.monarch_pagetree_add_group),
        groupActions = labels.groupActions ?: { groupActions.format(it) },
        pageActions = labels.pageActions ?: { pageActions.format(it) },
        more = monarchString(labels.more, R.string.monarch_pagetree_more),
        renameGroup = monarchString(labels.renameGroup, R.string.monarch_pagetree_rename_group),
        moveGroupUp = monarchString(labels.moveGroupUp, R.string.monarch_pagetree_move_group_up),
        moveGroupDown = monarchString(labels.moveGroupDown, R.string.monarch_pagetree_move_group_down),
        deleteGroup = monarchString(labels.deleteGroup, R.string.monarch_pagetree_delete_group),
        moveTo = labels.moveTo ?: { moveTo.format(it) },
        deletePage = monarchString(labels.deletePage, R.string.monarch_pagetree_delete_page),
        addGroupTitle = monarchString(labels.addGroupTitle, R.string.monarch_pagetree_add_group_title),
        groupName = monarchString(labels.groupName, R.string.monarch_pagetree_group_name),
        newGroupName = monarchString(labels.newGroupName, R.string.monarch_pagetree_new_group_name),
        renameSubmit = monarchString(labels.renameSubmit, R.string.monarch_pagetree_rename_submit),
        cancel = monarchString(labels.cancel, R.string.monarch_pagetree_cancel),
        deleteGroupTitle = labels.deleteGroupTitle ?: { deleteGroupTitle.format(it) },
        deleteGroupConsequence = labels.deleteGroupConsequence ?: { consequences[it].orEmpty() },
        typeToConfirm = labels.typeToConfirm ?: { typeToConfirm.format(it) },
        deletePageTitle = labels.deletePageTitle ?: { deletePageTitle.format(it) },
        cannotUndo = monarchString(labels.cannotUndo, R.string.monarch_pagetree_cannot_undo),
        groupMoved = labels.groupMoved ?: { group, position, total -> groupMoved.format(group, position, total) },
        pageMovedTo = labels.pageMovedTo ?: { page, group -> pageMovedTo.format(page, group) },
    )
}

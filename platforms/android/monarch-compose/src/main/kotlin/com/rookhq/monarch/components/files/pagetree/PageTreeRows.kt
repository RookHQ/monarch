package com.rookhq.monarch.components.files.pagetree

import com.rookhq.monarch.internal.MonarchSmallCapsLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuDivider
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.theme.MonarchTheme

/** A dialog the tree shows: a name to ask for or a delete to confirm. */
internal sealed interface PageTreeDialog {
    data object AddGroup : PageTreeDialog

    data class RenameGroup(val group: MonarchPageTreeGroup) : PageTreeDialog

    data class DeleteGroup(val group: MonarchPageTreeGroup) : PageTreeDialog

    data class DeletePage(val page: MonarchPageTreePage, val title: String) : PageTreeDialog
}

/** What the rows can do, with the reorders turned into new groups, a callback and an announcement. */
@Suppress("LongParameterList")
internal class PageTreeActions(
    val groups: List<MonarchPageTreeGroup>,
    val enabled: Boolean,
    val texts: PageTreeTexts,
    val onReorder: ((List<MonarchPageTreeGroup>, MonarchPageTreeMove) -> Unit)?,
    val onAddPage: ((String) -> Unit)?,
    val canRename: Boolean,
    val canDeleteGroup: Boolean,
    val canDeletePage: Boolean,
    val openDialog: (PageTreeDialog) -> Unit,
    val announce: (String) -> Unit,
) {
    /** A drag or a handle action inside a group. `MonarchReorderableList` announces it. */
    fun movePageWithin(groupId: String, from: Int, to: Int) {
        val change = pageTreeMovePageWithin(groups, groupId, from, to) ?: return
        onReorder?.invoke(change.groups, change.move)
    }

    /** "Move to" another group: the page joins the end of that group. */
    fun movePageTo(page: MonarchPageTreePage, group: MonarchPageTreeGroup) {
        val change = pageTreeMovePageTo(groups, page.id, group.id) ?: return
        onReorder?.invoke(change.groups, change.move)
        announce(texts.pageMovedTo(pageTreeTitle(page, texts.untitled), group.label))
    }

    /** Move group up or down. */
    fun moveGroup(group: MonarchPageTreeGroup, delta: Int) {
        val change = pageTreeMoveGroup(groups, group.id, delta) ?: return
        onReorder?.invoke(change.groups, change.move)
        announce(texts.groupMoved(group.label, change.move.toIndex + 1, groups.size))
    }
}

/** A group's heading: its label, its page count when editable, the add button and the group menu. */
@Composable
internal fun PageTreeHeading(
    group: MonarchPageTreeGroup,
    index: Int,
    actions: PageTreeActions,
    addIcon: (@Composable () -> Unit)?,
    menuIcon: (@Composable () -> Unit)?,
) {
    val texts = actions.texts
    val colors = MonarchTheme.colors
    val editable = actions.onReorder != null || actions.onAddPage != null || actions.canRename ||
        actions.canDeleteGroup
    val items = pageTreeGroupMenu(group, index, actions)
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = MonarchPageTreeTokens.RowMinHeight),
        horizontalArrangement = Arrangement.spacedBy(MonarchPageTreeTokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // As on the web: a small upper-case label.
        MonarchSmallCapsLabel(
            text = group.label,
            modifier = Modifier.weight(1f),
            color = MonarchPageTreeTokens.headingColor(actions.enabled, colors),
        )
        if (editable) {
            MonarchText(
                text = group.pages.size.toString(),
                modifier = Modifier.semantics { hideFromAccessibility() },
                style = MonarchTextStyle.Caption,
                color = MonarchPageTreeTokens.numberColor(actions.enabled, colors),
            )
        }
        actions.onAddPage?.let { add ->
            PageTreeIconButton(texts.addPage(group.label), texts.addPageText, addIcon, actions.enabled) {
                add(group.id)
            }
        }
        if (items.isNotEmpty()) {
            PageTreeMenu(texts.groupActions(group.label), texts.more, menuIcon, actions.enabled, items)
        }
    }
}

/** One page: its number and title as one selectable element, and its menu. */
@Suppress("LongParameterList")
@Composable
internal fun PageTreePageRow(
    page: MonarchPageTreePage,
    group: MonarchPageTreeGroup,
    current: Boolean,
    number: String?,
    onSelect: (String) -> Unit,
    actions: PageTreeActions,
    menuIcon: (@Composable () -> Unit)?,
    modifier: Modifier,
) {
    val texts = actions.texts
    val colors = MonarchTheme.colors
    val title = pageTreeTitle(page, texts.untitled)
    val items = pageTreePageMenu(page, title, group, actions)
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(MonarchPageTreeTokens.rowFill(current, colors), MonarchTheme.shapes.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = MonarchPageTreeTokens.RowMinHeight)
                .selectable(selected = current, enabled = actions.enabled, onClick = { onSelect(page.id) })
                .padding(horizontal = MonarchPageTreeTokens.RowPadding),
            horizontalArrangement = Arrangement.spacedBy(MonarchPageTreeTokens.Gap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (number != null) {
                MonarchText(
                    text = number,
                    modifier = Modifier.semantics { hideFromAccessibility() },
                    style = MonarchTextStyle.CodeSmall,
                    color = MonarchPageTreeTokens.numberColor(actions.enabled, colors),
                )
            }
            MonarchText(
                text = title,
                modifier = Modifier.weight(1f),
                style = if (current) MonarchTextStyle.BodyStrong else MonarchTextStyle.Body,
                color = MonarchPageTreeTokens.titleColor(current, actions.enabled, colors),
            )
        }
        if (items.isNotEmpty()) {
            PageTreeMenu(texts.pageActions(title), texts.more, menuIcon, actions.enabled, items)
        }
    }
}

/** One item of a page or group menu. */
internal class PageTreeMenuItem(
    val text: String,
    val danger: Boolean = false,
    val dividerBefore: Boolean = false,
    val onClick: () -> Unit,
)

private fun pageTreePageMenu(
    page: MonarchPageTreePage,
    title: String,
    group: MonarchPageTreeGroup,
    actions: PageTreeActions,
): List<PageTreeMenuItem> {
    val items = mutableListOf<PageTreeMenuItem>()
    if (actions.onReorder != null) {
        actions.groups.filter { it.id != group.id }.forEach { other ->
            items += PageTreeMenuItem(actions.texts.moveTo(other.label)) { actions.movePageTo(page, other) }
        }
    }
    if (actions.canDeletePage && pageTreeCanDeletePage(actions.groups)) {
        items += PageTreeMenuItem(actions.texts.deletePage, danger = true, dividerBefore = items.isNotEmpty()) {
            actions.openDialog(PageTreeDialog.DeletePage(page, title))
        }
    }
    return items
}

private fun pageTreeGroupMenu(
    group: MonarchPageTreeGroup,
    index: Int,
    actions: PageTreeActions,
): List<PageTreeMenuItem> {
    val texts = actions.texts
    val items = mutableListOf<PageTreeMenuItem>()
    if (actions.canRename) {
        items += PageTreeMenuItem(texts.renameGroup) { actions.openDialog(PageTreeDialog.RenameGroup(group)) }
    }
    if (actions.onReorder != null && index > 0) {
        items += PageTreeMenuItem(texts.moveGroupUp) { actions.moveGroup(group, -1) }
    }
    if (actions.onReorder != null && index < actions.groups.lastIndex) {
        items += PageTreeMenuItem(texts.moveGroupDown) { actions.moveGroup(group, 1) }
    }
    if (actions.canDeleteGroup && pageTreeCanDeleteGroup(actions.groups)) {
        items += PageTreeMenuItem(texts.deleteGroup, danger = true, dividerBefore = items.isNotEmpty()) {
            actions.openDialog(PageTreeDialog.DeleteGroup(group))
        }
    }
    return items
}

/** A small ghost button showing [icon], or [text] when there is none, named [name] for TalkBack. */
@Composable
private fun PageTreeIconButton(
    name: String,
    text: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MonarchButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = name },
        variant = MonarchButtonVariant.Ghost,
        size = MonarchButtonSize.Sm,
        enabled = enabled,
    ) {
        if (icon != null) icon() else Text(text, modifier = Modifier.clearAndSetSemantics { })
    }
}

/** A menu button and its `MonarchMenu`. */
@Composable
private fun PageTreeMenu(
    name: String,
    text: String,
    icon: (@Composable () -> Unit)?,
    enabled: Boolean,
    items: List<PageTreeMenuItem>,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        PageTreeIconButton(name, text, icon, enabled) { expanded = true }
        MonarchMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                if (item.dividerBefore) MonarchMenuDivider()
                MonarchMenuItem(
                    text = item.text,
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                    destructive = item.danger,
                )
            }
        }
    }
}

package dev.monarchui.components.files.pagetree

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableList
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.theme.MonarchTheme

/**
 * The pages of a multi-page document in named groups, with running page
 * numbers. Read only it is navigation; with editing handlers it organises
 * pages and groups.
 *
 * Built by Monarch from Compose layouts: Material 3 has no page tree. The
 * groups are headings and the pages selectable rows; the open page has the
 * subtle accent fill, an accent bar at its start and the selected state.
 * With [onReorder], each group's pages are a `MonarchReorderableList` (long
 * press and drag, the handle's Move up and Move down actions, arrow keys), a
 * page's menu moves it to another group, and a group's menu moves the group
 * up and down. Each handler adds its control: an add button on each heading,
 * "Add group" at the end, rename and delete in the menus. Renaming and adding
 * ask for a name in a Material 3 `AlertDialog`; deleting asks for
 * confirmation, and a group with pages also asks for its name through
 * `MonarchTypeToConfirmInput`. Moves are announced through a polite live
 * region. The tree scrolls itself, so give it a bounded height and do not
 * put it inside another vertical scroll.
 *
 * @param groups the groups and their pages, in order.
 * @param selected the id of the open page, or `null`.
 * @param onSelect called with a page's id when it is chosen. Navigate to the page.
 * @param modifier the modifier of the tree.
 * @param numbered shows page numbers (01, 02, …) running across groups, in the locale's digits.
 * @param enabled whether pages can be chosen and edited.
 * @param scrollState the scroll state of the tree.
 * @param labels replacements for the built-in strings.
 * @param onReorder makes the order editable. Receives the new groups and what moved; store them.
 * @param onAddPage shows an add button on each group heading. Receives the group's id; the app adds the page.
 * @param onAddGroup shows "Add group" at the end, which asks for a name. Receives the trimmed name.
 * @param onRenameGroup shows "Rename group" in the group menu. Called only when the name changed.
 * @param onDeleteGroup shows "Delete group" while there is more than one group, behind a confirmation.
 * @param onDeletePage shows "Delete page" in the page menu while there is more than one page, behind a
 * confirmation.
 * @param addIcon the icon of the heading add buttons; without it they show "Add page".
 * @param menuIcon the icon of the menu buttons; without it they show "More".
 */
@Composable
public fun MonarchPageTree(
    groups: List<MonarchPageTreeGroup>,
    selected: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    numbered: Boolean = true,
    enabled: Boolean = true,
    scrollState: ScrollState = rememberScrollState(),
    labels: MonarchPageTreeLabels = MonarchPageTreeLabels(),
    onReorder: ((groups: List<MonarchPageTreeGroup>, move: MonarchPageTreeMove) -> Unit)? = null,
    onAddPage: ((groupId: String) -> Unit)? = null,
    onAddGroup: ((label: String) -> Unit)? = null,
    onRenameGroup: ((groupId: String, label: String) -> Unit)? = null,
    onDeleteGroup: ((groupId: String) -> Unit)? = null,
    onDeletePage: ((pageId: String) -> Unit)? = null,
    addIcon: (@Composable () -> Unit)? = null,
    menuIcon: (@Composable () -> Unit)? = null,
) {
    val texts = pageTreeTexts(labels, groups)
    val numbers = remember(groups) { pageTreeNumbers(groups) }
    var dialog by remember { mutableStateOf<PageTreeDialog?>(null) }
    val announcer = rememberMonarchAnnouncer()
    val announce: (String) -> Unit = announcer::announce
    val actions = PageTreeActions(
        groups = groups,
        enabled = enabled,
        texts = texts,
        onReorder = onReorder,
        onAddPage = onAddPage,
        canRename = onRenameGroup != null,
        canDeleteGroup = onDeleteGroup != null,
        canDeletePage = onDeletePage != null,
        openDialog = { dialog = it },
        announce = announce,
    )
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .semantics {
                    isTraversalGroup = true
                    contentDescription = texts.label
                },
        ) {
            groups.forEachIndexed { index, group ->
                if (index > 0) Spacer(Modifier.height(MonarchPageTreeTokens.GroupGap))
                PageTreeHeading(group, index, actions, addIcon, menuIcon)
                PageTreeGroupPages(group, selected, onSelect, numbered, numbers, actions, menuIcon, labels)
            }
            if (onAddGroup != null) {
                MonarchButton(
                    text = texts.addGroup,
                    onClick = { dialog = PageTreeDialog.AddGroup },
                    modifier = Modifier.padding(top = MonarchPageTreeTokens.Gap),
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                    enabled = enabled,
                )
            }
        }
        // Always present, so TalkBack reads each move without focus moving.
        MonarchAnnouncerNode(announcer)
    }
    PageTreeDialogs(
        dialog = dialog,
        texts = texts,
        onDismiss = { dialog = null },
        onAddGroup = onAddGroup,
        onRenameGroup = onRenameGroup,
        onDeleteGroup = onDeleteGroup,
        onDeletePage = onDeletePage,
    )
}

/** The pages of one group: a reorderable list when editable, rows otherwise, or the empty group's text. */
@Suppress("LongParameterList")
@Composable
private fun PageTreeGroupPages(
    group: MonarchPageTreeGroup,
    selected: String?,
    onSelect: (String) -> Unit,
    numbered: Boolean,
    numbers: Map<String, Int>,
    actions: PageTreeActions,
    menuIcon: (@Composable () -> Unit)?,
    labels: MonarchPageTreeLabels,
) {
    val texts = actions.texts
    val row: @Composable (MonarchPageTreePage, Modifier) -> Unit = { page, rowModifier ->
        PageTreePageRow(
            page = page,
            group = group,
            current = page.id == selected,
            number = if (numbered) numbers[page.id]?.let(texts.number) else null,
            onSelect = onSelect,
            actions = actions,
            menuIcon = menuIcon,
            modifier = rowModifier,
        )
    }
    when {
        group.pages.isEmpty() && actions.onAddPage != null -> MonarchButton(
            text = texts.addFirstPage,
            onClick = { actions.onAddPage.invoke(group.id) },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
            enabled = actions.enabled,
        )
        group.pages.isEmpty() -> MonarchText(
            text = texts.noPages,
            modifier = Modifier.padding(vertical = MonarchPageTreeTokens.RowPadding),
            style = MonarchTextStyle.Caption,
            color = MonarchPageTreeTokens.numberColor(actions.enabled, MonarchTheme.colors),
        )
        actions.onReorder != null -> MonarchReorderableList(
            items = group.pages,
            key = { it.id },
            itemLabel = { pageTreeTitle(it, texts.untitled) },
            onReorder = { from, to -> actions.movePageWithin(group.id, from, to) },
            modifier = Modifier.heightIn(max = MonarchPageTreeTokens.GroupListMaxHeight),
            enabled = actions.enabled,
            labels = labels.reorderableList,
        ) { page, _ -> row(page, Modifier.weight(1f)) }
        else -> group.pages.forEach { page -> row(page, Modifier.fillMaxWidth()) }
    }
}

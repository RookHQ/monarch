package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenu
import dev.monarchui.components.collaboration.slashmenu.MonarchSlashMenuItem
import dev.monarchui.components.files.editortoolbar.MonarchEditorTool
import dev.monarchui.components.files.editortoolbar.MonarchEditorToolbar
import dev.monarchui.components.files.embedblock.MonarchEmbedProvider
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListDragState
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.layout.divider.MonarchDividerVariant
import dev.monarchui.internal.MonarchAnnouncerNode
import dev.monarchui.internal.rememberMonarchAnnouncer
import dev.monarchui.components.files.blockeditor.BlockEditorModel as Model
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlockType as Type

/**
 * A block document editor: paragraphs, headings, lists, checklists, quotes,
 * code, callouts, toggles, tables and embeds, with a / menu to insert
 * blocks, a handle beside each block to move, duplicate, turn into another
 * kind or delete it, a formatting toolbar and formatting in the text
 * selection menu.
 *
 * Compose has no block editor, so Monarch builds one from a `LazyColumn` of
 * blocks, each with its own text state (a `MonarchRichTextEditorState` in a
 * `BasicTextField`, so inline formats work as in `MonarchRichTextEditor`).
 * Callouts, toggles, tables and embeds are `MonarchCalloutBlock`,
 * `MonarchToggleBlock`, `MonarchTableBlock` and `MonarchEmbedBlock`; the toolbar
 * is `MonarchEditorToolbar`, the / menu `MonarchSlashMenu` (a strip under the
 * document, so it sits above the keyboard), the handle `MonarchDragHandle`
 * with a `MonarchMenu`. Return starts a new block (a new item in a
 * list) and Return on an empty list item ends the list; Backspace at the start
 * of a block turns it into a paragraph, then joins it to the block above.
 * Typing "- ", "1. ", "[] ", "# ", "## ", "### ", "> " or "``` " at the start
 * of a paragraph turns it into that kind. Dragging a handle moves its block;
 * TalkBack, Switch Access and Alt+Shift+Up or Down move it too, and each move
 * is announced.
 *
 * The editor needs a bounded height (it scrolls itself): give it a size or a
 * weight, never a vertically scrolling parent. Selection runs inside one block
 * at a time. Experimental until the document interchange format is agreed
 * across platforms: save [MonarchBlockEditorState.blocks].
 *
 * @param label the spoken name of the document, already localized. Show a visible title nearby.
 * @param state the document, the block with the caret and the undo history.
 * @param modifier the modifier of the editor. Give it a bounded height.
 * @param blocks the kinds of block offered in the / menu, the toolbar and the "Turn into" menu. Paragraphs are
 * always offered. Blocks of other kinds already in the document are still shown and edited.
 * @param tools the toolbar's tools in groups; an empty list for no toolbar. Undo, redo, the text styles, the
 * inline formats, the lists, quote and code block are applied by the editor; custom tools go to [onTool].
 * @param enabled whether the document can be edited. A disabled editor shows its blocks in the disabled colours,
 * without the toolbar and the handles.
 * @param readOnly shows the document as read: text, callouts, toggles that open and close, tables and embeds,
 * without fields, toolbar or handles. Links open on a tap.
 * @param bubbleMenu adds bold, italic, strikethrough and code to the system menu of selected text.
 * @param dragHandles shows the handle beside each block, which moves the block and opens its actions.
 * @param slashMenu opens the / menu when / is typed at the start of a text block.
 * @param slashItems the app's entries of the / menu, after the built-in ones. Their ids must not be the built-in
 * ids (text, heading-1 to heading-3, quote, code, bullet-list, ordered-list, task-list, callout, toggle, table,
 * embed).
 * @param slashGroupOrder group headings of the / menu in display order.
 * @param embedProviders the sites an embed may frame; other links are shown as links.
 * @param icons the icons of the toolbar's tools. Monarch bundles no icons: a tool without one shows its name.
 * @param labels replacements for the built-in strings.
 * @param listState the scroll state of the document.
 * @param onSlashItem receives the app's entries of the / menu after the typed command is removed; insert or change
 * blocks with [state].
 * @param onTool receives the custom tools of the toolbar.
 * @param onOpenLink opens a link of an embed outside the app. `null` uses the system browser.
 */
@ExperimentalMonarchApi
@Suppress("LongParameterList", "LongMethod")
@Composable
public fun MonarchBlockEditor(
    label: String,
    state: MonarchBlockEditorState,
    modifier: Modifier = Modifier,
    blocks: Set<Type> = Type.entries.toSet(),
    tools: List<List<MonarchEditorTool>> = MonarchBlockEditorDefaults.Tools,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    bubbleMenu: Boolean = true,
    dragHandles: Boolean = true,
    slashMenu: Boolean = true,
    slashItems: List<MonarchSlashMenuItem> = emptyList(),
    slashGroupOrder: List<String> = emptyList(),
    embedProviders: List<MonarchEmbedProvider> = MonarchEmbedProvider.Defaults,
    icons: Map<MonarchEditorTool, Painter> = emptyMap(),
    labels: MonarchBlockEditorLabels = MonarchBlockEditorLabels(),
    listState: LazyListState = rememberLazyListState(),
    onSlashItem: ((MonarchSlashMenuItem) -> Unit)? = null,
    onTool: ((MonarchEditorTool) -> Unit)? = null,
    onOpenLink: ((String) -> Unit)? = null,
) {
    val tokens = MonarchBlockEditorTokens
    val texts = blockEditorTexts(labels)
    val offered = remember(blocks) { blocks + Type.Paragraph }
    val actions = remember(state, offered) { BlockEditorActions(state, offered) }
    val editable = enabled && !readOnly
    val groups = remember(tools, offered) { blockEditorGroups(tools, offered) }
    val drag = remember(listState) { MonarchReorderableListDragState(listState) }
    val announcer = rememberMonarchAnnouncer()
    val announce: (String) -> Unit = announcer::announce
    val document = state.blocks
    val shown = drag.order?.mapNotNull { document.getOrNull(it) } ?: document
    val config = BlockEditorConfig(
        editable = editable,
        enabled = enabled,
        readOnly = readOnly,
        bubbleMenu = bubbleMenu && editable,
        dragHandles = dragHandles && editable,
        offered = offered,
        providers = embedProviders,
        labels = labels,
        texts = texts,
        listState = listState,
        onOpenLink = onOpenLink,
    )
    BlockEditorScrollToRequest(state, listState)
    Column(modifier = modifier) {
        if (editable && groups.isNotEmpty()) BlockEditorToolbar(actions, groups, icons, labels, onTool)
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = label },
                state = listState,
                contentPadding = PaddingValues(tokens.ContentPadding),
                verticalArrangement = Arrangement.spacedBy(tokens.BlockGap),
                userScrollEnabled = !drag.active,
            ) {
                itemsIndexed(items = shown, key = { _, block -> block.id }) { index, block ->
                    BlockEditorRow(
                        block = block,
                        index = index,
                        document = shown,
                        state = state,
                        actions = actions,
                        config = config,
                        drag = drag,
                        announce = announce,
                    )
                }
            }
            MonarchAnnouncerNode(announcer)
        }
        if (editable && slashMenu) {
            val items = BlockEditorSlash.items(texts, offered, slashItems)
            BlockEditorSlashStrip(actions, items, slashGroupOrder, labels) { onSlashItem?.invoke(it) }
        }
    }
}

/** The toolbar groups without the styles that are not offered, and without empty groups. */
private fun blockEditorGroups(tools: List<List<MonarchEditorTool>>, offered: Set<Type>): List<List<MonarchEditorTool>> =
    tools.map { group -> group.filter { tool -> Model.style(tool)?.let { it.type in offered } ?: true } }
        .filter { it.isNotEmpty() }

/** Scrolls to a block the editor wants to focus when it is not on screen, so its field can take focus. */
@Composable
private fun BlockEditorScrollToRequest(state: MonarchBlockEditorState, listState: LazyListState) {
    val request = state.focusRequest
    LaunchedEffect(request) {
        val index = request?.let { wanted -> state.blocks.indexOfFirst { it.id == wanted.id } } ?: -1
        val visible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (index >= 0 && !visible) listState.animateScrollToItem(index)
    }
}

@Composable
private fun BlockEditorToolbar(
    actions: BlockEditorActions,
    groups: List<List<MonarchEditorTool>>,
    icons: Map<MonarchEditorTool, Painter>,
    labels: MonarchBlockEditorLabels,
    onTool: ((MonarchEditorTool) -> Unit)?,
) {
    MonarchEditorToolbar(
        onAction = { tool -> actions.applyTool(tool, onTool) },
        modifier = Modifier.fillMaxWidth(),
        groups = groups,
        active = actions.activeTools(),
        disabled = actions.disabledTools(groups),
        icons = icons,
        labels = labels.toolbar,
        onLink = actions::applyLink,
    )
    MonarchDivider(variant = MonarchDividerVariant.Subtle)
}

@Composable
private fun BlockEditorSlashStrip(
    actions: BlockEditorActions,
    items: List<MonarchSlashMenuItem>,
    groupOrder: List<String>,
    labels: MonarchBlockEditorLabels,
    onSlashItem: (MonarchSlashMenuItem) -> Unit,
) {
    val slash = actions.slashQuery()
    MonarchSlashMenu(
        query = slash?.second,
        items = items,
        onSelect = { item -> slash?.let { (id, query) -> actions.pickSlash(id, query, item, onSlashItem) } },
        modifier = Modifier.fillMaxWidth(),
        groupOrder = groupOrder,
        labels = labels.slashMenu,
    )
}

/** The settings every row of a MonarchBlockEditor reads. */
@Suppress("LongParameterList")
internal class BlockEditorConfig(
    val editable: Boolean,
    val enabled: Boolean,
    val readOnly: Boolean,
    val bubbleMenu: Boolean,
    val dragHandles: Boolean,
    val offered: Set<Type>,
    val providers: List<MonarchEmbedProvider>,
    val labels: MonarchBlockEditorLabels,
    val texts: BlockEditorTexts,
    val listState: LazyListState,
    val onOpenLink: ((String) -> Unit)?,
)

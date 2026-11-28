package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import dev.monarchui.components.interaction.draghandle.MonarchDragHandle
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleDirection
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleSize
import dev.monarchui.components.interaction.draghandle.MonarchDragHandleState
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListDragState
import dev.monarchui.components.overlays.menu.MonarchMenu
import dev.monarchui.components.overlays.menu.MonarchMenuDivider
import dev.monarchui.components.overlays.menu.MonarchMenuItem
import dev.monarchui.components.overlays.menu.MonarchMenuLabel
import dev.monarchui.internal.monarchAutoScrollWhileHeld
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import dev.monarchui.components.files.blockeditor.BlockEditorModel as Model
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock as Block
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlockType as Type

/** The name of a block for TalkBack and the menus: its kind, with the number of a numbered item. */
internal fun blockEditorName(block: Block, index: Int, document: List<Block>, texts: BlockEditorTexts): String {
    val name = texts.name(block.type)
    val numbered = (block as? Block.Text)?.style == MonarchBlockEditorTextStyle.NumberedList
    return if (numbered) "$name ${Model.number(document, index)}" else name
}

/**
 * The handle beside a block: a `MonarchDragHandle` that is also the button of
 * the block's menu. A tap or Enter opens the menu; a drag moves the block;
 * TalkBack actions and the arrow keys move it one place at a time. It is one
 * focus stop.
 */
@Suppress("LongParameterList")
@Composable
internal fun BlockEditorHandle(
    block: Block,
    index: Int,
    count: Int,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
    drag: MonarchReorderableListDragState,
    announce: (String) -> Unit,
) {
    val texts = config.texts
    var open by remember { mutableStateOf(false) }
    val name = blockEditorName(block, index, state.blocks, texts)
    val latest by rememberUpdatedState(BlockEditorMove(state, texts, name, announce))
    val scope = rememberCoroutineScope()
    val maxStep = with(LocalDensity.current) { MonarchBlockEditorTokens.AutoScrollStep.toPx() }
    val listState = config.listState
    val autoScroll = remember { arrayOfNulls<Job>(1) }
    Box {
        MonarchDragHandle(
            label = texts.handle(name),
            modifier = Modifier
                .pointerInput(block.id, drag) {
                    detectDragGestures(
                        onDragStart = {
                            val row = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == block.id }
                            if (row != null) drag.start(row.offset + row.size / 2f, state.blocks.size) { true }
                        },
                        onDrag = { change, amount ->
                            if (drag.active) {
                                change.consume()
                                drag.drag(amount.y)
                                if (drag.overscroll() != 0f && autoScroll[0]?.isActive != true) {
                                    autoScroll[0] = scope.launch {
                                        listState.monarchAutoScrollWhileHeld(
                                            maxStep,
                                            { drag.active },
                                            drag::overscroll,
                                            drag::retarget,
                                        )
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (drag.active) {
                                val from = drag.from
                                val to = drag.end()
                                latest.moveTo(from, to)
                            }
                        },
                        onDragCancel = { drag.reset() },
                    )
                }
                // One focus stop: the handle's own focus target. The click opens the menu from a tap, TalkBack
                // and Enter on the focused handle, but takes no focus of its own (Tab stopped twice before).
                .focusProperties { canFocus = false }
                .clickable(role = Role.Button, onClickLabel = null) { open = true },
            state = if (drag.key == block.id) MonarchDragHandleState.Grabbed else MonarchDragHandleState.Idle,
            size = MonarchDragHandleSize.Sm,
            onMove = { direction ->
                latest.moveTo(index, if (direction == MonarchDragHandleDirection.Up) index - 1 else index + 1)
            },
            actionLabels = mapOf(
                MonarchDragHandleDirection.Up to texts.moveUp,
                MonarchDragHandleDirection.Down to texts.moveDown,
            ),
        )
        MonarchMenu(expanded = open, onDismissRequest = { open = false }) {
            BlockEditorMenu(block, index, count, actions, config, latest) { open = false }
        }
    }
}

/** Moves a block and announces where it went. */
internal class BlockEditorMove(
    private val state: MonarchBlockEditorState,
    private val texts: BlockEditorTexts,
    private val name: String,
    private val announce: (String) -> Unit,
) {
    fun moveTo(from: Int, to: Int) {
        val total = state.blocks.size
        val target = to.coerceIn(0, total - 1)
        if (from == target || from !in 0 until total) return
        state.moveBlockTo(from, target)
        announce(texts.moved(name, target + 1, total))
    }
}

/** The items of a block's menu: move, duplicate, turn into, tone, delete. */
@Suppress("LongParameterList", "LongMethod")
@Composable
private fun BlockEditorMenu(
    block: Block,
    index: Int,
    count: Int,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
    move: BlockEditorMove,
    close: () -> Unit,
) {
    val texts = config.texts
    MonarchMenuItem(
        text = texts.moveUp,
        onClick = {
            move.moveTo(index, index - 1)
            close()
        },
        enabled = index > 0,
    )
    MonarchMenuItem(
        text = texts.moveDown,
        onClick = {
            move.moveTo(index, index + 1)
            close()
        },
        enabled = index < count - 1,
    )
    MonarchMenuItem(
        text = texts.duplicate,
        onClick = {
            actions.duplicate(block.id)
            close()
        },
    )
    val targets = if (Model.textOf(block) == null) {
        emptyList()
    } else {
        Type.entries.filter { it in config.offered && it != block.type && Model.convert(block, it) != null }
    }
    if (targets.isNotEmpty()) {
        MonarchMenuDivider()
        MonarchMenuLabel(text = texts.turnInto)
        targets.forEach { type ->
            MonarchMenuItem(
                text = texts.name(type),
                onClick = {
                    actions.turnInto(block.id, type)
                    close()
                },
            )
        }
    }
    if (block is Block.Callout) {
        MonarchMenuDivider()
        MonarchMenuLabel(text = texts.tone)
        MonarchBlockEditorTokens.CalloutTones.forEach { tone ->
            MonarchMenuItem(
                text = texts.tones.getValue(tone),
                onClick = {
                    actions.setTone(block.id, tone)
                    close()
                },
                selected = tone == block.tone,
            )
        }
    }
    MonarchMenuDivider()
    MonarchMenuItem(
        text = texts.delete,
        onClick = {
            actions.delete(block.id)
            close()
        },
        destructive = true,
    )
}

package dev.monarchui.components.files.blockeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.monarchui.components.interaction.reorderablelist.MonarchReorderableListDragState
import dev.monarchui.foundation.monarchShadow
import dev.monarchui.internal.MonarchShapeDefaults
import dev.monarchui.theme.MonarchTheme
import dev.monarchui.components.files.blockeditor.MonarchBlockEditorBlock as Block

/** One block of the document: its handle, when shown, and its content. */
@Composable
internal fun LazyItemScope.BlockEditorRow(
    block: Block,
    index: Int,
    document: List<Block>,
    state: MonarchBlockEditorState,
    actions: BlockEditorActions,
    config: BlockEditorConfig,
    drag: MonarchReorderableListDragState,
    announce: (String) -> Unit,
) {
    val dragged = drag.key == block.id
    val colors = MonarchTheme.colors
    val shape = MonarchTheme.shapes.md
    val placement = if (dragged) {
        Modifier
            .zIndex(MonarchBlockEditorTokens.DRAGGED_Z)
            .graphicsLayer { translationY = drag.translation() }
            .monarchShadow(MonarchTheme.elevation.md, shape)
            .background(MonarchBlockEditorTokens.draggedFill(colors), shape)
            .border(MonarchShapeDefaults.BorderWidth, MonarchBlockEditorTokens.dragged(colors), shape)
    } else {
        Modifier.animateItem(
            fadeInSpec = null,
            placementSpec = MonarchTheme.motion.spec<IntOffset>(),
            fadeOutSpec = null,
        )
    }
    Row(
        modifier = placement
            .fillMaxWidth()
            .onFocusChanged { if (it.hasFocus) state.focusedBlockId = block.id },
        horizontalArrangement = Arrangement.spacedBy(MonarchBlockEditorTokens.PartGap),
        verticalAlignment = Alignment.Top,
    ) {
        if (config.dragHandles) {
            BlockEditorHandle(
                block = block,
                index = index,
                count = document.size,
                state = state,
                actions = actions,
                config = config,
                drag = drag,
                announce = announce,
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            if (config.readOnly) {
                BlockEditorReadContent(block, index, document, config)
            } else {
                BlockEditorEditContent(block, index, document, state, actions, config)
            }
        }
    }
}

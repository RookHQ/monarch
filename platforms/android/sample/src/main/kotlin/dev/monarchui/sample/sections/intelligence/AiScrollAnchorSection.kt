package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

/**
 * The catalogue page of AiScrollAnchor: a reversed LazyColumn, which stays at the newest message while the
 * person is there, and a Jump to latest button while they have scrolled up.
 */
object AiScrollAnchorSection : CataloguePage {
    override val title = "AiScrollAnchor"

    private const val FIRST_MESSAGES = 12
    private const val HEIGHT_STEPS = 3

    @Composable
    override fun Content() {
        var count by rememberSaveable { mutableIntStateOf(FIRST_MESSAGES) }
        val state = rememberLazyListState()
        val scope = rememberCoroutineScope()
        // With reverseLayout, item 0 is the newest message, at the bottom.
        val atBottom by remember { derivedStateOf { state.firstVisibleItemIndex == 0 } }
        val reduceMotion = MonarchTheme.motion.reducedMotion
        CatalogueCaption("reverseLayout keeps the newest message in view")
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
            state = state,
            reverseLayout = true,
        ) {
            items(items = (count downTo 1).toList(), key = { it }) { number ->
                MonarchText(
                    text = "Message $number",
                    modifier = Modifier.padding(vertical = MonarchTheme.spacing.space2),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
            MonarchButton(text = "Add a message", onClick = { count++ }, size = MonarchButtonSize.Sm)
            if (!atBottom) {
                MonarchButton(
                    text = "Jump to latest",
                    onClick = {
                        scope.launch { if (reduceMotion) state.scrollToItem(0) else state.animateScrollToItem(0) }
                    },
                    size = MonarchButtonSize.Sm,
                )
            }
        }
    }
}

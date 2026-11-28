package dev.monarchui.sample.sections.files

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.files.pageoutline.MonarchPageOutline
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineHeading
import dev.monarchui.components.files.pageoutline.MonarchPageOutlineStats
import dev.monarchui.components.overlays.bottomsheet.MonarchBottomSheet
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.launch

private const val DocumentHeightSteps = 3

/** The sample document: each heading is followed by one paragraph, so a heading's item index is twice its index. */
private val Document = listOf(
    MonarchPageOutlineHeading("Overview", 1, "overview") to "What the new routing rules change and why.",
    MonarchPageOutlineHeading("Goals", 2, "goals") to "Answer every ticket within one working day.",
    MonarchPageOutlineHeading("Timeline", 2, "timeline") to "The rules go live in three steps over six weeks.",
    MonarchPageOutlineHeading("Risks", 3, "risks") to "Queues may grow while agents learn the new tags.",
    MonarchPageOutlineHeading("Open questions", 2, "questions") to "Who owns tickets that arrive at night?",
    MonarchPageOutlineHeading("Next steps", 1, "next") to "Agree on the tags with the team leads.",
)

/** The catalogue page of PageOutline. */
object MonarchPageOutlineSection : CataloguePage {
    override val title = "PageOutline"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val document = rememberLazyListState()
        val scope = rememberCoroutineScope()
        var open by rememberSaveable { mutableStateOf(false) }
        val current by remember { derivedStateOf { Document[document.firstVisibleItemIndex / 2].first.id } }
        val headings = Document.map { it.first }
        val stats = MonarchPageOutlineStats.fromText(Document.joinToString(" ") { "${it.first.text} ${it.second}" })
        CatalogueCaption("A document and its outline in a sheet")
        MonarchButton(text = "On this page", onClick = { open = true })
        LazyColumn(
            state = document,
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * DocumentHeightSteps),
        ) {
            itemsIndexed(Document.flatMap { listOf(it.first.text, it.second) }) { index, text ->
                MonarchText(
                    text = text,
                    style = if (index % 2 == 0) MonarchTextStyle.Heading else MonarchTextStyle.Body,
                    modifier = Modifier.padding(vertical = MonarchTheme.spacing.space2),
                )
            }
        }
        if (open) {
            MonarchBottomSheet(onDismissRequest = { open = false }) {
                MonarchPageOutline(
                    headings = headings,
                    onNavigate = { heading ->
                        val index = headings.indexOf(heading)
                        scope.launch { document.animateScrollToItem(index * 2) }
                        dismiss()
                    },
                    currentId = current,
                    stats = stats,
                )
            }
        }
        CatalogueCaption("No headings yet")
        MonarchPageOutline(
            headings = emptyList(),
            onNavigate = {},
            modifier = Modifier.height(MonarchTheme.spacing.space24),
        )
    }
}

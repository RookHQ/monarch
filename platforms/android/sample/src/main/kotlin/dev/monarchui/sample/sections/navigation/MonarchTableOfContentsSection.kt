package dev.monarchui.sample.sections.navigation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContents
import dev.monarchui.components.navigation.tableofcontents.MonarchTableOfContentsItem
import dev.monarchui.components.navigation.tableofcontents.rememberMonarchTableOfContentsSpy
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.tokens.MonarchTokens
import kotlinx.coroutines.launch

/** The catalogue page of TableOfContents: the list follows the scrolling page below it. */
object MonarchTableOfContentsSection : CataloguePage {
    override val title = "TableOfContents"

    private val entries = listOf(
        MonarchTableOfContentsItem("Overview", "overview"),
        MonarchTableOfContentsItem("Install", "install"),
        MonarchTableOfContentsItem("With Gradle", "gradle", level = 3),
        MonarchTableOfContentsItem("Usage", "usage"),
    )

    private const val ParagraphsPerSection = 4

    /** The height of the scrolling page in the catalogue. */
    private val pageHeight = (MonarchTokens.Space.space24 * 3).dp

    @Composable
    override fun Content() {
        val state = rememberLazyListState()
        val scope = rememberCoroutineScope()
        // Each section is a heading item followed by its paragraphs.
        val sections = entries.mapIndexed { i, item -> item.id to i * (ParagraphsPerSection + 1) }.toMap()
        val current by rememberMonarchTableOfContentsSpy(state, sections)
        CatalogueCaption("Choose an entry, or scroll the page below")
        MonarchTableOfContents(
            items = entries,
            selectedId = current,
            onSelect = { id -> scope.launch { state.animateScrollToItem(sections.getValue(id)) } },
        )
        LazyColumn(modifier = Modifier.height(pageHeight), state = state) {
            entries.forEach { item ->
                item(key = item.id) {
                    MonarchText(
                        text = item.title,
                        style = if (item.level > 2) MonarchTextStyle.Subheading else MonarchTextStyle.Heading,
                    )
                }
                items(ParagraphsPerSection) { n ->
                    MonarchText(text = "Paragraph ${n + 1} of ${item.title}. It is here so that the page scrolls.")
                }
            }
        }
    }
}

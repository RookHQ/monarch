package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Sticky: LazyColumn stickyHeader and the bars of a Scaffold. */
object StickySection : CataloguePage {
    override val title = "Sticky"

    private const val HEIGHT_STEPS = 3
    private const val FILTERS = 20

    private val groups = listOf(
        "A" to listOf("Ada", "Alan", "Anita", "Annie"),
        "B" to listOf("Barbara", "Bjarne", "Brian"),
        "G" to listOf("Grace", "Guido", "Gwen"),
        "K" to listOf("Katherine", "Ken", "Kristen"),
    )

    @Composable
    override fun Content() {
        val height = MonarchTheme.spacing.space24 * HEIGHT_STEPS
        val state = rememberLazyListState()
        val stuck by remember {
            derivedStateOf { state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset > 0 }
        }
        CatalogueCaption("stickyHeader, with a divider once content scrolls under it")
        MonarchText(text = "Contacts", style = MonarchTextStyle.Subheading)
        if (stuck) MonarchDivider()
        LazyColumn(state = state, modifier = Modifier.fillMaxWidth().height(height)) {
            groups.forEach { (letter, names) ->
                stickyHeader(key = letter) {
                    MonarchText(
                        text = letter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MonarchTheme.colors.bgCanvas)
                            .padding(vertical = MonarchTheme.spacing.space2),
                        style = MonarchTextStyle.Label,
                        heading = true,
                    )
                }
                items(items = names, key = { it }) { name ->
                    MonarchText(text = name, modifier = Modifier.padding(vertical = MonarchTheme.spacing.space2))
                }
            }
        }

        CatalogueCaption("Scaffold bottomBar fixed to the bottom")
        Scaffold(
            modifier = Modifier.fillMaxWidth().height(height),
            containerColor = MonarchTheme.colors.bgCanvas,
            bottomBar = {
                Surface(color = MonarchTheme.colors.bgSurface) {
                    MonarchButton(
                        text = "Apply filters",
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().padding(MonarchTheme.spacing.space4),
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            },
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                items(FILTERS) { MonarchText(text = "Filter ${it + 1}") }
            }
        }
    }
}

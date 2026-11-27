package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.card.MonarchCard
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ScrollArea: verticalScroll and horizontalScroll with a bounded size. */
object ScrollAreaSection : CataloguePage {
    override val title = "ScrollArea"

    private const val LINES = 40
    private const val REPORTS = 8
    private const val HEIGHT_STEPS = 2

    @Composable
    override fun Content() {
        CatalogueCaption("A log with a maximum height")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(MonarchTheme.spacing.px, MonarchTheme.colors.borderSubtle, MonarchTheme.shapes.md)
                .heightIn(max = MonarchTheme.spacing.space24 * HEIGHT_STEPS)
                .verticalScroll(rememberScrollState())
                .padding(MonarchTheme.spacing.space3),
        ) {
            repeat(LINES) { line ->
                MonarchText(text = "Deployment step ${line + 1} finished", style = MonarchTextStyle.Code)
            }
        }

        CatalogueCaption("A row of cards that scrolls sideways")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            repeat(REPORTS) { index -> MonarchCard { MonarchText(text = "Report ${index + 1}") } }
        }
    }
}

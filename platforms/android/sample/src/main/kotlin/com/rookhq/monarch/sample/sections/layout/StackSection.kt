package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.divider.MonarchDivider
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Stack: Column with Arrangement.spacedBy and the space scale. */
object StackSection : CataloguePage {
    override val title = "Stack"

    private val rows = listOf("Plan: Team", "Seats: 12", "Renews: 1 March")

    @Composable
    override fun Content() {
        CatalogueCaption("spacedBy(space4)")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space4)) {
            MonarchText(text = "Contact details", style = MonarchTextStyle.Heading)
            MonarchText(text = "We use these to send receipts.")
            MonarchButton(text = "Save", onClick = {}, variant = MonarchButtonVariant.Primary)
        }

        CatalogueCaption("spacedBy(space3) with dividers")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            rows.forEachIndexed { index, row ->
                if (index > 0) MonarchDivider()
                MonarchText(text = row)
            }
        }
    }
}

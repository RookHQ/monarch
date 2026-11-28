package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.divider.MonarchDivider
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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

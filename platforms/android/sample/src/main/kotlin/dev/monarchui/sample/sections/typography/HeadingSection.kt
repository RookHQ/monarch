package dev.monarchui.sample.sections.typography

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Heading: MonarchText in a heading role. */
object HeadingSection : CataloguePage {
    override val title = "Heading"

    @Composable
    override fun Content() {
        CatalogueCaption("Heading roles")
        MonarchText(text = "Display", style = MonarchTextStyle.Display)
        MonarchText(text = "Billing", style = MonarchTextStyle.Title)
        MonarchText(text = "Payment methods", style = MonarchTextStyle.Heading)
        MonarchText(text = "Cards", style = MonarchTextStyle.Subheading)
        CatalogueCaption("Small headings")
        MonarchText(text = "Card title", style = MonarchTextStyle.BodyStrong, heading = true)
        MonarchText(text = "Group label", style = MonarchTextStyle.Label, heading = true)
        CatalogueCaption("Muted")
        MonarchText(
            text = "Nothing here yet",
            style = MonarchTextStyle.Subheading,
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("One line")
        MonarchText(
            text = "Quarterly revenue by region and product line, before tax",
            style = MonarchTextStyle.Heading,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

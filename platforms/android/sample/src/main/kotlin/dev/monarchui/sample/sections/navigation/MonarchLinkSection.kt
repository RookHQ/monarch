package dev.monarchui.sample.sections.navigation

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import dev.monarchui.components.navigation.link.MonarchLink
import dev.monarchui.components.navigation.link.MonarchLinkDefaults
import dev.monarchui.components.navigation.link.MonarchLinkVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Link. */
object MonarchLinkSection : CataloguePage {
    override val title = "Link"

    @Composable
    override fun Content() {
        var taps by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Variants")
        MonarchLinkVariant.entries.forEach { variant ->
            MonarchLink(text = "${variant.name}: view all invoices", onClick = { taps++ }, variant = variant)
        }
        MonarchText(
            text = "Links opened: $taps",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        CatalogueCaption("Disabled")
        MonarchLink(text = "Resend the code", onClick = {}, enabled = false)
        CatalogueCaption("External, at caption size")
        ProvideTextStyle(MonarchTheme.typography.caption) {
            MonarchLink(text = "Terms of service", onClick = { taps++ }, external = true)
        }
        CatalogueCaption("Inside a sentence")
        MonarchText(
            text = buildAnnotatedString {
                append("Read the ")
                withLink(LinkAnnotation.Clickable("terms", MonarchLinkDefaults.styles()) { taps++ }) {
                    append("terms of service")
                }
                append(" before you continue.")
            },
        )
    }
}

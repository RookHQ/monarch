package dev.monarchui.sample.sections.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.layout.rookbar.MonarchRookBar
import dev.monarchui.components.layout.rookbar.MonarchRookBarAccount
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuFeatured
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuItem
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuLink
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuSection
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of RookBar. The menu opens the company menu; the last destination is named below. */
object MonarchRookBarSection : CataloguePage {
    override val title = "RookBar"

    private val items = listOf(
        MonarchMegaMenuItem(
            id = "products",
            label = "Products",
            sections = listOf(
                MonarchMegaMenuSection(
                    heading = "Sales & Support",
                    description = "Win customers, then look after them.",
                    links = listOf(
                        MonarchMegaMenuLink("Pipeline", "pipeline", "Leads, deals and invoices."),
                        MonarchMegaMenuLink(
                            "Help desk",
                            "https://desk.example.com",
                            "Support with a branded portal.",
                            external = true,
                        ),
                    ),
                ),
                MonarchMegaMenuSection(
                    heading = "Collaboration",
                    description = "Everyone on the same page.",
                    links = listOf(
                        MonarchMegaMenuLink("Team chat", "chat", "Channels, calls and search."),
                        MonarchMegaMenuLink("Documents", "docs", "Docs the whole workspace can find."),
                        MonarchMegaMenuLink("Planner", "planner", "Plan sprints across teams."),
                    ),
                ),
            ),
            featured = MonarchMegaMenuFeatured(
                title = "Every app",
                link = "signup",
                description = "One account opens all of them.",
                actionLabel = "Create your account",
            ),
        ),
        MonarchMegaMenuItem(
            id = "services",
            label = "Services",
            sections = listOf(
                MonarchMegaMenuSection(
                    heading = null,
                    links = listOf(
                        MonarchMegaMenuLink("Product engineering", "engineering", "Designed and built end to end."),
                        MonarchMegaMenuLink("Consulting", "consulting", "Plans, reviews and audits."),
                    ),
                ),
            ),
        ),
        MonarchMegaMenuItem(id = "pricing", label = "Pricing", link = "pricing"),
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var page by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Signed out")
        MonarchRookBar(
            brand = "Northwind",
            items = items,
            menuOpen = open,
            onMenuOpenChange = { open = it },
            onNavigate = { page = it },
            account = MonarchRookBarAccount.SignedOut,
            onAccountAction = { page = it.name },
            brandMark = { MonarchIcon(SampleIcons.Grid, contentDescription = null) },
            onBrandClick = { page = "home" },
        )
        CatalogueCaption("Signed in")
        MonarchRookBar(
            brand = "Northwind",
            items = items,
            menuOpen = false,
            onMenuOpenChange = { open = it },
            onNavigate = { page = it },
            account = MonarchRookBarAccount.SignedIn("Ada Lovelace"),
            onAccountAction = { page = it.name },
        )
        if (page.isNotEmpty()) MonarchText(text = "Chose $page")
    }
}

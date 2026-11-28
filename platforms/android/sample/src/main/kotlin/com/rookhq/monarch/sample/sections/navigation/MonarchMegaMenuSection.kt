package com.rookhq.monarch.sample.sections.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenu
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuFeatured
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuItem
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuLink
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuSection
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

/** The catalogue page of MegaMenu. Entries expand in place; the last destination is named below. */
object MonarchMegaMenuSection : CataloguePage {
    override val title = "MegaMenu"

    private fun icon(glyph: androidx.compose.ui.graphics.vector.ImageVector): @Composable () -> Unit =
        { MonarchIcon(glyph, contentDescription = null, size = MonarchIconSize.Sm) }

    private val items = listOf(
        MonarchMegaMenuItem(
            id = "product",
            label = "Product",
            sections = listOf(
                MonarchMegaMenuSection(
                    heading = "Plan",
                    links = listOf(
                        MonarchMegaMenuLink(
                            "Projects",
                            "projects",
                            "Track work from idea to launch",
                            icon = icon(SampleIcons.Grid),
                        ),
                        MonarchMegaMenuLink(
                            "Calendar",
                            "calendar",
                            "Deadlines and milestones in one view",
                            badge = "New",
                            icon = icon(SampleIcons.Calendar),
                        ),
                    ),
                ),
                MonarchMegaMenuSection(
                    heading = "Collaborate",
                    links = listOf(
                        MonarchMegaMenuLink(
                            "Teams",
                            "teams",
                            "Roles, access and ownership",
                            icon = icon(SampleIcons.Users),
                        ),
                        MonarchMegaMenuLink(
                            "Settings",
                            "settings",
                            "Workspace preferences",
                            icon = icon(SampleIcons.Settings),
                        ),
                    ),
                ),
            ),
            featured = MonarchMegaMenuFeatured(
                title = "What changed this month",
                link = "changelog",
                description = "A short tour of the newest features and fixes.",
                badge = "Updated",
                icon = icon(SampleIcons.Grid),
            ),
        ),
        MonarchMegaMenuItem(
            id = "company",
            label = "Company",
            sections = listOf(
                MonarchMegaMenuSection(
                    heading = "About",
                    links = listOf(
                        MonarchMegaMenuLink("Who we are", "about", "The team and how we work"),
                        MonarchMegaMenuLink("Status", "https://status.example.com", "Service health", external = true),
                    ),
                ),
            ),
        ),
        MonarchMegaMenuItem(id = "support", label = "Support", link = "support"),
    )

    @Composable
    override fun Content() {
        var expanded by rememberSaveable { mutableStateOf<String?>("product") }
        var page by rememberSaveable { mutableStateOf("") }
        MonarchMegaMenu(
            items = items,
            expandedItem = expanded,
            onExpandedItemChange = { expanded = it },
            onNavigate = { page = it },
        )
        if (page.isNotEmpty()) MonarchText(text = "Opened $page")
    }
}

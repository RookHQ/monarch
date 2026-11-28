package com.rookhq.monarch.sample.sections.layout

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteFooter
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteFooterColumn
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteNavItem
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteNavLink
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteShell
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of SiteHeader and SiteFooter. The body names the last destination chosen. */
object MonarchSiteShellSection : CataloguePage {
    override val title = "SiteHeader and SiteFooter"

    @Composable
    override fun Content() {
        var page by rememberSaveable { mutableStateOf("home") }
        MonarchSiteShell(
            brand = "Rook Example",
            nav = listOf(
                MonarchSiteNavItem(
                    label = "Product",
                    items = listOf(
                        MonarchSiteNavLink("Editor", "editor", "Pages and blocks"),
                        MonarchSiteNavLink("Search", "search", "Find any page"),
                    ),
                ),
                MonarchSiteNavItem("Pricing", link = "pricing"),
            ),
            onNavigate = { page = it },
            modifier = Modifier.height(560.dp),
            actions = {
                MonarchButton(
                    text = "Log in",
                    onClick = {},
                    variant = MonarchButtonVariant.Ghost,
                    size = MonarchButtonSize.Sm,
                )
                MonarchButton(
                    text = "Get started",
                    onClick = {},
                    variant = MonarchButtonVariant.Primary,
                    size = MonarchButtonSize.Sm,
                )
            },
            footer = {
                MonarchSiteFooter(
                    columns = listOf(
                        MonarchSiteFooterColumn("Product", listOf(MonarchSiteNavLink("Editor", "editor"))),
                        MonarchSiteFooterColumn("Company", listOf(MonarchSiteNavLink("About Rook", "about"))),
                    ),
                    onNavigate = { page = it },
                    legal = "© 2026 Rook",
                    legalLinks = listOf(MonarchSiteNavLink("Privacy", "privacy")),
                )
            },
        ) {
            MonarchText(text = "Page: $page", modifier = Modifier.padding(MonarchTheme.spacing.space6))
        }
    }
}

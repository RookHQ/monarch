package dev.monarchui.sample.sections.layout

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.siteshell.MonarchSiteFooter
import dev.monarchui.components.layout.siteshell.MonarchSiteFooterColumn
import dev.monarchui.components.layout.siteshell.MonarchSiteNavItem
import dev.monarchui.components.layout.siteshell.MonarchSiteNavLink
import dev.monarchui.components.layout.siteshell.MonarchSiteShell
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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

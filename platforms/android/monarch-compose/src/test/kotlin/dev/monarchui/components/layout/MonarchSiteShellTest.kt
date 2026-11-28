package dev.monarchui.components.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.layout.siteshell.MonarchSiteFooter
import dev.monarchui.components.layout.siteshell.MonarchSiteFooterColumn
import dev.monarchui.components.layout.siteshell.MonarchSiteNavItem
import dev.monarchui.components.layout.siteshell.MonarchSiteNavLink
import dev.monarchui.components.layout.siteshell.MonarchSiteShell
import dev.monarchui.components.layout.siteshell.MonarchSiteShellTokens
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private val Nav = listOf(
    MonarchSiteNavItem(
        "Product",
        items = listOf(
            MonarchSiteNavLink("Editor", "editor", "Pages and blocks"),
            MonarchSiteNavLink("Search", "search"),
        ),
    ),
    MonarchSiteNavItem("Pricing", link = "pricing"),
)
private val Columns = listOf(
    MonarchSiteFooterColumn("Product", listOf(MonarchSiteNavLink("Editor", "editor"))),
    MonarchSiteFooterColumn("Company", listOf(MonarchSiteNavLink("About Rook", "about"))),
)

internal object MonarchSiteShellFixtures : ComponentFixtures {
    override val id = "marketing-site-shell"
    override val fixtures = listOf(
        fixture("header with an action", tall = true) {
            MonarchSiteShell(
                brand = "Rook Example",
                nav = Nav,
                onNavigate = {},
                modifier = Modifier.height(440.dp),
                actions = { MonarchButton("Log in", {}, size = MonarchButtonSize.Sm) },
            ) { MonarchText("Welcome") }
        },
        fixture("footer", tall = true) {
            MonarchSiteFooter(
                Columns,
                onNavigate = {},
                legal = "© 2026 Rook",
                legalLinks = listOf(MonarchSiteNavLink("Privacy", "privacy")),
            )
        },
        fixture("drawer open", tall = true) {
            MonarchSiteShell(
                brand = "Rook Example",
                nav = Nav,
                onNavigate = {},
                modifier = Modifier.height(440.dp),
                drawerState = rememberDrawerState(DrawerValue.Open),
            ) {}
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSiteShellTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMenuButtonOpensTheDrawerAndADestinationCallsBack() {
        var page = ""
        rule.setMonarchContent {
            MonarchSiteShell(brand = "Rook Example", nav = Nav, onNavigate = { page = it }) { MonarchText("Welcome") }
        }
        rule.onNodeWithContentDescription("Open navigation").assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Site navigation")).assertExists()
        rule.onNodeWithText("Pricing").performClick()
        rule.waitForIdle()
        assertEquals("pricing", page)
    }

    @Test
    fun aGroupExpandsInTheDrawer() {
        var page = ""
        rule.setMonarchContent {
            MonarchSiteShell(
                brand = "Rook Example",
                nav = Nav,
                onNavigate = { page = it },
                drawerState = rememberDrawerState(DrawerValue.Open),
                navigationLabel = "Navegación",
            ) {}
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Navegación")).assertExists()
        rule.onNodeWithText("Product").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Editor", substring = true).performClick()
        rule.waitForIdle()
        assertEquals("editor", page)
    }

    @Test
    fun footerTitlesAreHeadingsAndLinksAreButtons() {
        var page = ""
        rule.setMonarchContent { MonarchSiteFooter(Columns, onNavigate = { page = it }, legal = "© 2026 Rook") }
        rule.onNodeWithText("Company").assert(isHeading())
        rule.onNodeWithText("About Rook")
            .assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("about", page)
    }

    @Test
    fun legalLinksFollowTheLegalLineInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Box { MonarchSiteFooter(
                emptyList(),
                {},
                legal = "© 2026 Rook",
                legalLinks = listOf(MonarchSiteNavLink("Privacy", "p")),
            ) }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("© 2026 Rook").assertIsBefore(rule.onNodeWithText("Privacy"), direction)
        }
    }

    @Test
    fun footerTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchSiteFooter(listOf(MonarchSiteFooterColumn("Resources for developers", emptyList())), {})
        }
        rule.onNodeWithText("Resources for developers").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, MonarchSiteShellTokens.barFill(colors), theme.id)
            assertEquals(colors.textMuted, MonarchSiteShellTokens.footerLink(colors), theme.id)
        }
    }
}

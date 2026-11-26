package com.rookhq.monarch.components.layout

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
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteFooter
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteFooterColumn
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteNavItem
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteNavLink
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteShell
import com.rookhq.monarch.components.layout.siteshell.MonarchSiteShellTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
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

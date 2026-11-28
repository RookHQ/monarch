package com.rookhq.monarch.components.navigation

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumb
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumbItem
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumbLabels
import com.rookhq.monarch.components.navigation.breadcrumb.MonarchBreadcrumbTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

private fun trail(vararg titles: String) = titles.map { MonarchBreadcrumbItem(it) }

internal object MonarchBreadcrumbFixtures : ComponentFixtures {
    override val id = "breadcrumb"
    override val fixtures = listOf(
        fixture("two levels") { MonarchBreadcrumb(items = trail("Projects", "Monarch"), onItemClick = {}) },
        fixture("collapsed") {
            MonarchBreadcrumb(items = trail("Drive", "Clients", "Acme", "2026", "Contracts"), onItemClick = {})
        },
        fixture("not linked and disabled") {
            MonarchBreadcrumb(
                items = listOf(MonarchBreadcrumbItem("Archive", linked = false), MonarchBreadcrumbItem("Q3")),
                onItemClick = {},
            )
            MonarchBreadcrumb(items = trail("Projects", "Monarch"), onItemClick = {}, enabled = false)
        },
        fixture("long titles scroll") {
            MonarchBreadcrumb(
                items = trail("Customer success operations", "Quarterly business reviews", "Northwind Traders"),
                onItemClick = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchBreadcrumbTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun ancestorsAreLinksAndTheLastLevelIsTheCurrentPage() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchBreadcrumb(items = trail("Projects", "Monarch"), onItemClick = { chosen += it.id })
        }
        rule.onNodeWithContentDescription("Breadcrumb").assertExists()
        rule.onNodeWithText("Projects").assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density).performClick()
        val current = rule.onNodeWithText("Monarch")
        current.assertHasNoClickAction()
        assertEquals("Current page", current.stateDescription())
        assertEquals(listOf("Projects"), chosen)
    }

    @Test
    fun theSeparatorsAreHidden() {
        rule.setMonarchContent { MonarchBreadcrumb(items = trail("A", "B", "C"), onItemClick = {}) }
        rule.onNodeWithText(MonarchBreadcrumbTokens.SEPARATOR).assertDoesNotExist()
    }

    @Test
    fun theMiddleLevelsCollapseIntoAMenu() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchBreadcrumb(
                items = listOf(
                    MonarchBreadcrumbItem("Drive"),
                    MonarchBreadcrumbItem("Clients"),
                    MonarchBreadcrumbItem("Acme", linked = false),
                    MonarchBreadcrumbItem("2026"),
                    MonarchBreadcrumbItem("Contracts"),
                ),
                onItemClick = { chosen += it.id },
            )
        }
        rule.onNodeWithText("Clients").assertDoesNotExist()
        rule.onNodeWithText("Drive").assertExists()
        rule.onNodeWithText("2026").assertExists()
        rule.onNodeWithContentDescription("Show 2 more levels")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Acme").assertIsNotEnabled()
        rule.onNodeWithText("Clients").performClick()
        assertEquals(listOf("Clients"), chosen)
    }

    @Test
    fun aDisabledTrailCannotBeUsed() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchBreadcrumb(items = trail("A", "B", "C", "D", "E"), onItemClick = { clicks++ }, enabled = false)
        }
        rule.onNodeWithText("A").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Show 2 more levels").assertIsNotEnabled()
        assertEquals(0, clicks)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchBreadcrumb(
                items = trail("A", "B", "C", "D", "E"),
                onItemClick = {},
                labels = MonarchBreadcrumbLabels(label = "Ruta", more = { "Mostrar $it niveles" }, current = "Actual"),
            )
        }
        rule.onNodeWithContentDescription("Ruta").assertExists()
        rule.onNodeWithContentDescription("Mostrar 2 niveles").assertExists()
        assertEquals("Actual", rule.onNodeWithText("E").stateDescription())
    }

    @Test
    fun theTrailRunsFromTheEndRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchBreadcrumb(items = trail("Projects", "Monarch"), onItemClick = {})
        }
        rule.onNodeWithText("Projects").assertIsBefore(rule.onNodeWithText("Monarch"), LayoutDirection.Rtl)
    }

    @Test
    fun linksAreNotCutAtTheLargestFontSizeAndTheTrailStartsAtTheCurrentLevel() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchBreadcrumb(items = trail("Customers", "Northwind Traders", "Invoices"), onItemClick = {})
        }
        rule.onNodeWithText("Invoices").assertTextFullyVisible()
        rule.onNodeWithText("Northwind Traders", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theLogicOfTheCollapse() {
        val t = MonarchBreadcrumbTokens
        assertEquals(listOf(0, 1, 2, 3), t.visible(4, 4))
        assertEquals(listOf(0, t.MORE, 4, 5), t.visible(6, 4))
        assertEquals(listOf(1, 2, 3), t.hidden(6, 4))
        assertEquals(listOf(0, t.MORE, 5), t.visible(6, 1))
        assertEquals(emptyList(), t.hidden(3, 4))
        assertEquals(false, t.isLink(2, 3, linked = true))
        assertEquals(false, t.isLink(0, 3, linked = false))
        assertEquals(true, t.isLink(0, 3, linked = true))
    }

    @Test
    fun theColoursComeFromTheTextRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, MonarchBreadcrumbTokens.textColor(current = true, enabled = true, c), theme.id)
            assertEquals(c.textMuted, MonarchBreadcrumbTokens.textColor(current = false, enabled = true, c), theme.id)
            val disabled = MonarchBreadcrumbTokens.textColor(current = true, enabled = false, c)
            assertEquals(c.textDisabled, disabled, theme.id)
            assertEquals(c.textSubtle, MonarchBreadcrumbTokens.separatorColor(c), theme.id)
        }
    }
}

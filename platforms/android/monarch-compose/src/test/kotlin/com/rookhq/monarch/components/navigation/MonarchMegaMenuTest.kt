package com.rookhq.monarch.components.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenu
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuFeatured
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuItem
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuLabels
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuLink
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuSection
import com.rookhq.monarch.components.navigation.megamenu.MonarchMegaMenuTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
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

private val Items = listOf(
    MonarchMegaMenuItem(
        id = "product",
        label = "Product",
        sections = listOf(
            MonarchMegaMenuSection(
                heading = "Plan",
                links = listOf(
                    MonarchMegaMenuLink(
                        label = "Calendar",
                        link = "calendar",
                        description = "Deadlines and milestones in one view",
                        badge = "New",
                        icon = { MonarchIcon(TestIcon, contentDescription = null, size = MonarchIconSize.Sm) },
                    ),
                    MonarchMegaMenuLink("Status", "https://status.example.com", "Service health", external = true),
                ),
            ),
        ),
        featured = MonarchMegaMenuFeatured("What changed this month", "changelog", badge = "Updated"),
    ),
    MonarchMegaMenuItem(id = "support", label = "Support", link = "support"),
)

internal object MonarchMegaMenuFixtures : ComponentFixtures {
    override val id = "mega-menu"
    override val fixtures = listOf(
        fixture("collapsed") { MonarchMegaMenu(
            Items,
            expandedItem = null,
            onExpandedItemChange = {},
            onNavigate = {},
        ) },
        fixture("product expanded", tall = true) {
            MonarchMegaMenu(Items, expandedItem = "product", onExpandedItemChange = {}, onNavigate = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMegaMenuTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun anEntryExpandsAndReadsItsState() {
        rule.setMonarchContent {
            var expanded by remember { mutableStateOf<String?>(null) }
            MonarchMegaMenu(Items, expanded, { expanded = it }, onNavigate = {})
        }
        val product = rule.onNodeWithText("Product")
        product.assertHasClickAction().assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Collapsed", product.stateDescription())
        rule.onNodeWithText("Calendar", substring = true).assertDoesNotExist()
        product.performClick()
        rule.waitForIdle()
        assertEquals("Expanded", rule.onNodeWithText("Product").stateDescription())
        rule.onNodeWithText("Plan").assert(isHeading())
    }

    @Test
    fun linksNavigateAndADirectEntryNavigates() {
        val visited = mutableListOf<String>()
        rule.setMonarchContent { MonarchMegaMenu(Items, "product", {}, onNavigate = { visited += it }) }
        rule.onNodeWithText("Calendar", substring = true).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("What changed this month", substring = true).performClick()
        rule.onNodeWithText("Support").performClick()
        assertEquals(listOf("calendar", "changelog", "support"), visited)
    }

    @Test
    fun anExternalLinkSaysItOpensOutsideTheAppAndLabelsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchMegaMenu(
                Items,
                "product",
                {},
                {},
                labels = MonarchMegaMenuLabels(external = "Se abre fuera", explore = "Ver"),
            )
        }
        rule.onNode(hasContentDescription("Status, Service health, Se abre fuera")).assertExists()
        rule.onNodeWithText("Ver", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun theDefaultCallToActionIsExplore() {
        rule.setMonarchContent { MonarchMegaMenu(Items, "product", {}, {}) }
        rule.onNodeWithText("Explore", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theBadgeFollowsTheLabelInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchMegaMenu(Items, "product", {}, {}) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Calendar", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("New", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun descriptionsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchMegaMenu(Items, "product", {}, {}) }
        rule.onNodeWithText("Deadlines and milestones in one view", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.accentSubtle, MonarchMegaMenuTokens.iconTile(colors), theme.id)
            assertEquals(colors.accentStrong, MonarchMegaMenuTokens.iconTint(colors), theme.id)
            assertEquals(colors.bgSubtle, MonarchMegaMenuTokens.expandedRow(true, colors), theme.id)
            assertEquals(colors.borderSubtle, MonarchMegaMenuTokens.featuredBorder(colors), theme.id)
        }
    }
}

package dev.monarchui.components.layout

import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.layout.rookbar.MonarchRookBar
import dev.monarchui.components.layout.rookbar.MonarchRookBarAccount
import dev.monarchui.components.layout.rookbar.MonarchRookBarAction
import dev.monarchui.components.layout.rookbar.MonarchRookBarLabels
import dev.monarchui.components.layout.rookbar.MonarchRookBarTokens
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuItem
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuLink
import dev.monarchui.components.navigation.megamenu.MonarchMegaMenuSection
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

private val Menu = listOf(
    MonarchMegaMenuItem(
        "products",
        "Products",
        sections = listOf(MonarchMegaMenuSection("Collaboration", listOf(MonarchMegaMenuLink("Team chat", "chat")))),
    ),
    MonarchMegaMenuItem("pricing", "Pricing", link = "pricing"),
)

internal object MonarchRookBarFixtures : ComponentFixtures {
    override val id = "rook-bar"
    override val fixtures = listOf(
        fixture("signed out") {
            MonarchRookBar(
                "Northwind",
                Menu,
                menuOpen = false,
                onMenuOpenChange = {},
                onNavigate = {},
                account = MonarchRookBarAccount.SignedOut,
            )
        },
        fixture("signed in, brand is a button") {
            MonarchRookBar(
                "Northwind",
                Menu,
                menuOpen = false,
                onMenuOpenChange = {},
                onNavigate = {},
                account = MonarchRookBarAccount.SignedIn("Ada Lovelace"),
                onBrandClick = {},
            )
        },
        fixture("no account") { MonarchRookBar("Northwind", Menu, false, {}, {}) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchRookBarTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theBarIsNamedRookAndTheMenuButtonOpensTheSheet() {
        var navigated = ""
        rule.setMonarchContent {
            var open by remember { mutableStateOf(false) }
            MonarchRookBar("Northwind", Menu, open, { open = it }, { navigated = it })
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Rook")).assertExists()
        rule.onNodeWithContentDescription("Open menu").assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pricing").performClick()
        rule.waitForIdle()
        assertEquals("pricing", navigated)
        rule.onNodeWithText("Pricing").assertDoesNotExist()
    }

    @Test
    fun signedOutButtonsSitInTheBarAndCallBack() {
        val actions = mutableListOf<MonarchRookBarAction>()
        rule.setMonarchContent {
            MonarchRookBar(
                "Northwind",
                Menu,
                false,
                {},
                {},
                Modifier.requiredWidth(480.dp),
                MonarchRookBarAccount.SignedOut,
                { actions += it },
            )
        }
        rule.onNodeWithText("Log in").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Sign up").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(MonarchRookBarAction.SignIn, MonarchRookBarAction.SignUp), actions)
    }

    @Test
    fun atTheLargestFontSizeAccountButtonsMoveToTheSheet() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchRookBar("Northwind", Menu, false, {}, {}, account = MonarchRookBarAccount.SignedOut)
        }
        rule.onNodeWithText("Log in").assertDoesNotExist()
        rule.onNodeWithText("Northwind", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aSignedInPersonFindsTheirActionsInTheSheet() {
        var action: MonarchRookBarAction? = null
        rule.setMonarchContent {
            MonarchRookBar(
                "Northwind",
                Menu,
                true,
                {},
                {},
                account = MonarchRookBarAccount.SignedIn("Ada Lovelace"),
                onAccountAction = { action = it },
                labels = MonarchRookBarLabels(signOut = "Cerrar sesión"),
            )
        }
        rule.waitForIdle()
        rule.onNodeWithText("Ada Lovelace").assertExists()
        rule.onNodeWithText("Open dashboard").assertExists()
        rule.onNodeWithText("Cerrar sesión").performClick()
        assertEquals(MonarchRookBarAction.SignOut, action)
    }

    @Test
    fun theBrandComesBeforeTheMenuInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchRookBar("Northwind", Menu, false, {}, {}) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Northwind").assertIsBefore(rule.onNodeWithContentDescription("Open menu"), direction)
        }
    }

    @Test
    fun signUpIsTheInverseSurfaceInEveryTheme() {
        assertEquals(MonarchButtonVariant.Inverse, MonarchRookBarTokens.SignUpVariant)
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, MonarchRookBarTokens.fill(colors), theme.id)
        }
    }
}

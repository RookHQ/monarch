package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceOption
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceSwitcher
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceSwitcherLabels
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceSwitcherRules
import com.rookhq.monarch.components.identity.workspaceswitcher.MonarchWorkspaceSwitcherTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Acme = MonarchWorkspaceOption("acme", "Acme", handle = "acme")
private val Labs = MonarchWorkspaceOption("rook", "Rook Labs", handle = "rooklabs", keywords = "Studio")
private val Many = (1..8).map { MonarchWorkspaceOption("c$it", "Client $it", handle = "client$it") }

internal object MonarchWorkspaceSwitcherFixtures : ComponentFixtures {
    override val id = "workspace-switcher"
    override val fixtures = listOf(
        fixture("current workspace") {
            MonarchWorkspaceSwitcher(workspaces = listOf(Acme, Labs), selection = "acme", onSelectionChange = {})
        },
        fixture("nothing chosen, small") {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(Acme),
                selection = null,
                onSelectionChange = {},
                size = MonarchButtonSize.Sm,
            )
        },
        fixture("icon only, many") {
            MonarchWorkspaceSwitcher(workspaces = Many, selection = "c2", onSelectionChange = {}, iconOnly = true)
        },
        fixture("disabled, loading") {
            MonarchWorkspaceSwitcher(
                workspaces = emptyList(),
                selection = null,
                onSelectionChange = {},
                enabled = false,
                loading = true,
            )
        },
        fixture("long name") {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(MonarchWorkspaceOption("x", "Northern regional operations workspace", "north")),
                selection = "x",
                onSelectionChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchWorkspaceSwitcherTest {
    @get:Rule
    val rule = createComposeRule()

    private val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test
    fun theButtonIsNamedWorkspaceWithTheNameAndHandleAsItsValue() {
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(workspaces = listOf(Acme, Labs), selection = "acme", onSelectionChange = {})
        }
        val button = rule.onNodeWithContentDescription("Workspace")
        button.assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Acme, @acme", button.stateDescription())
    }

    @Test
    fun nothingChosenReadsChooseAWorkspace() {
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(workspaces = listOf(Acme), selection = "gone", onSelectionChange = {})
        }
        assertEquals("Choose a workspace", rule.onNodeWithContentDescription("Workspace").stateDescription())
    }

    @Test
    fun aFewWorkspacesOpenAMenuThatSwitchesInOneTap() {
        var selection by mutableStateOf<String?>("acme")
        var changes = 0
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(Acme, Labs),
                selection = selection,
                onSelectionChange = {
                    changes++
                    selection = it
                },
                onCreate = {},
            )
        }
        rule.onNodeWithContentDescription("Workspace").performClick()
        rule.onNodeWithContentDescription("Acme, @acme").assertIsSelected().performClick()
        assertEquals(0, changes)
        rule.onNodeWithContentDescription("Workspace").performClick()
        rule.onNodeWithText("Create workspace").assertExists()
        rule.onNodeWithContentDescription("Rook Labs, @rooklabs")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("rook", selection)
        assertEquals(1, changes)
    }

    @Test
    fun manyWorkspacesOpenASearchableSheet() {
        var selection by mutableStateOf<String?>("c1")
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = Many + Labs,
                selection = selection,
                onSelectionChange = { selection = it },
            )
        }
        rule.onNodeWithContentDescription("Workspace").performClick()
        rule.onNode(radio and hasContentDescription("Client 1, @client1")).assertIsSelected()
        rule.onNode(hasSetTextAction()).performTextInput("studio")
        rule.onNode(radio and hasContentDescription("Client 1, @client1")).assertDoesNotExist()
        rule.onNode(radio and hasContentDescription("Rook Labs, @rooklabs"))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals("rook", selection)
    }

    @Test
    fun searchSaysWhenNothingMatchesAndLoadingOpensTheSheet() {
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(Acme),
                selection = null,
                onSelectionChange = {},
                loading = true,
            )
        }
        rule.onNodeWithContentDescription("Workspace").performClick()
        rule.onNodeWithText("Loading workspaces…").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Loading workspaces…").assertDoesNotExist()
    }

    @Test
    fun allWorkspacesClosesAndCallsBack() {
        var managed = 0
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = Many,
                selection = null,
                onSelectionChange = {},
                onManage = { managed++ },
            )
        }
        rule.onNodeWithContentDescription("Workspace").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("zzz")
        rule.onNodeWithText("No workspaces match.").assertExists()
        rule.onNodeWithText("All workspaces").performClick()
        assertEquals(1, managed)
        rule.onNodeWithText("No workspaces match.").assertDoesNotExist()
    }

    @Test
    fun aDisabledButtonDoesNotOpen() {
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(Acme),
                selection = null,
                onSelectionChange = {},
                enabled = false,
            )
        }
        rule.onNodeWithContentDescription("Workspace").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Acme, @acme").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchWorkspaceSwitcher(
                workspaces = listOf(Acme),
                selection = null,
                onSelectionChange = {},
                labels = MonarchWorkspaceSwitcherLabels(label = "Arbeitsbereich", placeholder = "Wählen"),
            )
        }
        assertEquals("Wählen", rule.onNodeWithContentDescription("Arbeitsbereich").stateDescription())
    }

    @Test
    fun theButtonKeepsItsTargetInRightToLeftAtTheLargestScale() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl, fontScale = LargestFontScale) {
            MonarchWorkspaceSwitcher(workspaces = listOf(Acme), selection = "acme", onSelectionChange = {})
        }
        rule.onNodeWithContentDescription("Workspace").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theRulesChooseThePresentationAndMatch() {
        val rules = MonarchWorkspaceSwitcherRules
        assertTrue(rules.usesMenu(5, loading = false))
        assertFalse(rules.usesMenu(6, loading = false))
        assertFalse(rules.usesMenu(1, loading = true))
        assertTrue(rules.matches(Labs, "STUDIO"))
        assertTrue(rules.matches(Labs, "rookl"))
        assertFalse(rules.matches(Acme, "labs"))
        assertEquals("Acme", rules.spoken(MonarchWorkspaceOption("a", "Acme", handle = " ")))
        assertNull(rules.current(listOf(Acme), null))
    }

    @Test
    fun theColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.textMuted, MonarchWorkspaceSwitcherTokens.handleColor(colors))
        }
    }
}

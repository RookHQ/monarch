package dev.monarchui.components.intelligence

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aimessagebranch.MonarchAiMessageBranch
import dev.monarchui.components.intelligence.aimessagebranch.MonarchAiMessageBranchTokens
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Composable
private fun Branch(
    selection: Int,
    count: Int,
    onSelectionChange: (Int) -> Unit = {},
    enabled: Boolean = true,
    showSingle: Boolean = false,
) {
    MonarchAiMessageBranch(
        selection = selection,
        count = count,
        onSelectionChange = onSelectionChange,
        previousIcon = { Text("‹") },
        nextIcon = { Text("›") },
        modifier = Modifier.testTag("branch"),
        enabled = enabled,
        showSingle = showSingle,
    )
}

internal object MonarchAiMessageBranchFixtures : ComponentFixtures {
    override val id = "ai-message-branch"
    override val fixtures = listOf(
        fixture("middle version") { Branch(selection = 1, count = 3) },
        fixture("first version") { Branch(selection = 0, count = 3) },
        fixture("last version") { Branch(selection = 2, count = 3) },
        fixture("disabled") { Branch(selection = 1, count = 3, enabled = false) },
        fixture("single version shown") { Branch(selection = 0, count = 1, showSingle = true) },
        fixture("large count") { Branch(selection = 120, count = 240) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiMessageBranchTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun thePositionIsShownAsTwoOfThreeAndReadAsVersionTwoOfThree() {
        rule.setMonarchContent { Branch(selection = 1, count = 3) }
        rule.onNodeWithText("2 / 3").assertContentDescriptionEquals("Version 2 of 3")
        rule.onNodeWithTag("branch").assertContentDescriptionEquals("Versions")
    }

    @Test
    fun theButtonsStepThroughTheVersions() {
        var selection by mutableIntStateOf(1)
        rule.setMonarchContent { Branch(selection = selection, count = 3, onSelectionChange = { selection = it }) }
        rule.onNodeWithContentDescription("Next version").performClick()
        assertEquals(2, selection)
        rule.onNodeWithContentDescription("Previous version").performClick()
        rule.onNodeWithContentDescription("Previous version").performClick()
        assertEquals(0, selection)
    }

    @Test
    fun theButtonsAreDisabledAtTheEnds() {
        var selection by mutableIntStateOf(0)
        rule.setMonarchContent { Branch(selection = selection, count = 2, onSelectionChange = { selection = it }) }
        rule.onNodeWithContentDescription("Previous version").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Next version").assertIsEnabled().performClick()
        rule.onNodeWithContentDescription("Next version").assertIsNotEnabled()
        rule.onNodeWithContentDescription("Previous version").assertIsEnabled()
    }

    @Test
    fun aDisabledSwitcherDoesNotChange() {
        var changes = 0
        rule.setMonarchContent { Branch(selection = 1, count = 3, onSelectionChange = { changes++ }, enabled = false) }
        rule.onNodeWithContentDescription("Next version").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theNewPositionIsAnnouncedAfterAButtonButNotAfterAChangeFromTheApp() {
        var selection by mutableIntStateOf(0)
        rule.setMonarchContent { Branch(selection = selection, count = 3, onSelectionChange = { selection = it }) }
        val live = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        val notLive = SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion)
        rule.onNodeWithContentDescription("Version 1 of 3").assert(notLive)
        rule.onNodeWithContentDescription("Next version").performClick()
        rule.onNodeWithContentDescription("Version 2 of 3").assert(live)
        selection = 0
        rule.onNodeWithContentDescription("Version 1 of 3").assert(notLive)
    }

    @Test
    fun withFewerThanTwoVersionsNothingIsShownUnlessAsked() {
        var single by mutableIntStateOf(0)
        rule.setMonarchContent { Branch(selection = 0, count = 1, showSingle = single == 1) }
        rule.onNodeWithTag("branch").assertDoesNotExist()
        single = 1
        rule.onNodeWithContentDescription("Version 1 of 1").assertExists()
    }

    @Test
    fun aSelectionOutsideTheRangeShowsTheNearestVersion() {
        assertEquals(2, MonarchAiMessageBranchTokens.current(7, 3))
        assertEquals(0, MonarchAiMessageBranchTokens.current(-4, 3))
        assertEquals(1, MonarchAiMessageBranchTokens.total(0))
        assertFalse(MonarchAiMessageBranchTokens.visible(1, showSingle = false))
        assertTrue(MonarchAiMessageBranchTokens.visible(0, showSingle = true))
    }

    @Test
    fun bothButtonsHaveA48dpTarget() {
        rule.setMonarchContent { Branch(selection = 1, count = 3) }
        rule.onNodeWithContentDescription("Previous version").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Next version").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun previousComesBeforeNextInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { Branch(selection = 1, count = 3) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Previous version")
                .assertIsBefore(rule.onNodeWithContentDescription("Next version"), direction)
        }
    }

    @Test
    fun thePositionFitsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Branch(selection = 119, count = 240) }
        rule.onNodeWithText("120 / 240", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun everyBuiltInStringCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiMessageBranch(
                selection = 0,
                count = 2,
                onSelectionChange = {},
                previousIcon = { Text("‹") },
                nextIcon = { Text("›") },
                modifier = Modifier.testTag("branch"),
                label = "Versions de la réponse",
                previousLabel = "Version précédente",
                nextLabel = "Version suivante",
                formatPosition = { i, n -> "$i sur $n" },
                formatAnnouncement = { i, n -> "Version $i sur $n" },
            )
        }
        rule.onNodeWithTag("branch").assertContentDescriptionEquals("Versions de la réponse")
        rule.onNodeWithContentDescription("Version précédente").assertExists()
        rule.onNodeWithContentDescription("Version suivante").assertExists()
        rule.onNodeWithContentDescription("Version 1 sur 2").assertExists()
    }

    @Test
    fun theContentIsMutedAndTheDisabledStateUsesTheDisabledRole() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchAiMessageBranchTokens.contentColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchAiMessageBranchTokens.contentColor(false, colors), theme.id)
        }
    }
}

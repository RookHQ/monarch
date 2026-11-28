package dev.monarchui.components.datagrid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicateMatch
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicatePanel
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicatePanelTokens
import dev.monarchui.components.datagrid.duplicatepanel.MonarchDuplicatePanelVariant
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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

private val Matches = listOf(
    MonarchDuplicateMatch(
        id = "1",
        title = "Example Ltd",
        kind = "Client",
        reasons = listOf("Same email", "Same company domain"),
        score = 0.92f,
    ),
    MonarchDuplicateMatch(id = "2", title = "Example Limited", detail = "Archived"),
)

internal object MonarchDuplicatePanelFixtures : ComponentFixtures {
    override val id = "duplicate-panel"
    override val fixtures = MonarchDuplicatePanelVariant.entries.flatMap { variant ->
        listOf(
            fixture("${variant.name}, two matches, every action") {
                MonarchDuplicatePanel(
                    matches = Matches,
                    variant = variant,
                    footer = "You can still create it.",
                    icon = { MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm) },
                    onOpen = {},
                    onMerge = {},
                    onDismiss = {},
                )
            },
            fixture("${variant.name}, one match, no actions") {
                MonarchDuplicatePanel(matches = Matches.take(1), variant = variant)
            },
        )
    } + listOf(
        fixture("long text") {
            MonarchDuplicatePanel(
                matches = listOf(
                    MonarchDuplicateMatch(
                        id = "1",
                        title = "International Business Consulting Limited",
                        kind = "Open lead",
                        reasons = listOf("Same email address", "Same phone number"),
                        score = 1.4f,
                    ),
                ),
                onMerge = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDuplicatePanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeadingIsSingularOrPluralAndAHeading() {
        var matches by mutableStateOf(Matches.take(1))
        rule.setMonarchContent { MonarchDuplicatePanel(matches = matches) }
        rule.onNodeWithText("Possible duplicate").assert(isHeading())
        matches = Matches
        rule.onNodeWithText("Possible duplicates").assert(isHeading())
    }

    @Test
    fun theTitleReplacesTheBuiltInHeading() {
        rule.setMonarchContent { MonarchDuplicatePanel(matches = Matches, title = "Already in your CRM") }
        rule.onNodeWithText("Already in your CRM").assert(isHeading())
        rule.onNodeWithText("Possible duplicates").assertDoesNotExist()
    }

    @Test
    fun theHintHeadingIsAPoliteLiveRegion() {
        rule.setMonarchContent { MonarchDuplicatePanel(matches = Matches) }
        rule.onNodeWithText("Possible duplicates")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun thePanelHeadingIsNotALiveRegion() {
        rule.setMonarchContent {
            MonarchDuplicatePanel(matches = Matches, variant = MonarchDuplicatePanelVariant.Panel)
        }
        rule.onNodeWithText("Possible duplicates")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.LiveRegion))
    }

    @Test
    fun eachMatchIsOneElementWithItsReasonsAndScore() {
        rule.setMonarchContent { MonarchDuplicatePanel(matches = Matches) }
        rule.onNodeWithText("Example Ltd")
            .assertTextContains("Client", substring = true)
            .assertTextContains("92% match", substring = true)
            .assertTextContains("Same email · Same company domain", substring = true)
            .assertHasNoClickAction()
        rule.onNodeWithText("Example Limited").assertTextContains("Archived")
    }

    @Test
    fun theButtonsAreNamedWithTheRecordAndHandBackTheMatch() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDuplicatePanel(
                matches = Matches,
                onOpen = { calls += "open ${it.id}" },
                onMerge = { calls += "merge ${it.id}" },
                onDismiss = { calls += "dismiss ${it.id}" },
            )
        }
        rule.onNodeWithContentDescription("Open: Example Ltd").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Merge: Example Limited").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Not a duplicate: Example Ltd").assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("open 1", "merge 2", "dismiss 1"), calls)
    }

    @Test
    fun theLabelsReplaceTheBuiltInButtonLabels() {
        rule.setMonarchContent {
            MonarchDuplicatePanel(
                matches = Matches.take(1),
                onOpen = {},
                onMerge = {},
                onDismiss = {},
                openLabel = "View",
                mergeLabel = "Combine",
                dismissLabel = "Different",
            )
        }
        rule.onNodeWithContentDescription("View: Example Ltd").assertExists()
        rule.onNodeWithContentDescription("Combine: Example Ltd").assertExists()
        rule.onNodeWithContentDescription("Different: Example Ltd").assertExists()
    }

    @Test
    fun withoutCallbacksThereAreNoButtons() {
        rule.setMonarchContent { MonarchDuplicatePanel(matches = Matches) }
        rule.onNodeWithText("Open").assertDoesNotExist()
        rule.onNodeWithText("Merge").assertDoesNotExist()
    }

    @Test
    fun noMatchesDrawNothing() {
        rule.setMonarchContent {
            Box(Modifier.testTag("frame")) { MonarchDuplicatePanel(matches = emptyList(), footer = "Footer") }
        }
        rule.onNodeWithText("Footer").assertDoesNotExist()
        val size = rule.onNodeWithTag("frame").fetchSemanticsNode().size
        assertEquals(0, size.height)
    }

    @Test
    fun theIconIsHiddenFromTalkBack() {
        rule.setMonarchContent {
            MonarchDuplicatePanel(
                matches = Matches,
                icon = { Box(Modifier.testTag("icon").size(14.dp)) },
            )
        }
        rule.onNodeWithTag("icon").assertDoesNotExist()
        assertEquals(0, rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription))
            .fetchSemanticsNodes().size)
    }

    @Test
    fun theScoreIsAClampedWholePercentage() {
        assertEquals(92, MonarchDuplicatePanelTokens.percent(0.921f))
        assertEquals(100, MonarchDuplicatePanelTokens.percent(1.4f))
        assertEquals(0, MonarchDuplicatePanelTokens.percent(-0.2f))
    }

    @Test
    fun coloursComeFromTheWarningRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val hint = MonarchDuplicatePanelTokens.hintColors(colors)
            assertEquals(colors.warningSubtle, hint.container, theme.id)
            assertEquals(colors.warningBorder, hint.border, theme.id)
            assertEquals(
                colors.warningStrong,
                MonarchDuplicatePanelTokens.headingColor(MonarchDuplicatePanelVariant.Hint, colors),
                theme.id,
            )
            assertEquals(
                colors.textDefault,
                MonarchDuplicatePanelTokens.headingColor(MonarchDuplicatePanelVariant.Panel, colors),
                theme.id,
            )
        }
    }

    @Test
    fun longNamesAreNotCutOffAtTheLargestFontSize() {
        val name = "International Business Consulting and Accounting Services Limited"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDuplicatePanel(matches = listOf(MonarchDuplicateMatch(id = "1", title = name)), onMerge = {})
        }
        rule.onNodeWithText(name, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithContentDescription("Merge: $name").assertTouchTargetAtLeast48dp(rule.density)
    }
}

package dev.monarchui.components.files

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.toggleblock.MonarchToggleBlock
import dev.monarchui.components.files.toggleblock.MonarchToggleBlockTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.MonarchTestFrame
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

internal object MonarchToggleBlockFixtures : ComponentFixtures {
    override val id = "toggle-block"
    override val fixtures = listOf(
        fixture("collapsed") {
            MonarchToggleBlock(summary = "Why some records are skipped") { Text("Hidden") }
        },
        fixture("expanded, with a chevron") {
            MonarchToggleBlock(
                summary = "Details",
                initiallyExpanded = true,
                trailingIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            ) { Text("Records changed while the import runs are picked up the next night.") }
        },
        fixture("disabled") { MonarchToggleBlock(summary = "Locked section", enabled = false) { Text("Hidden") } },
        fixture("long summary") {
            MonarchToggleBlock(summary = "Why some records are skipped during the nightly import of contacts") {
                Text("Hidden")
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchToggleBlockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theSummaryIsAButtonThatShowsAndHidesTheContent() {
        rule.setMonarchContent {
            MonarchToggleBlock(summary = "Details") { Text("Paid on 10 March") }
        }
        val summary = rule.onNodeWithText("Details")
        summary.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Collapsed", summary.stateDescription())
        rule.onNodeWithText("Paid on 10 March").assertDoesNotExist()
        summary.performClick()
        assertEquals("Expanded", summary.stateDescription())
        rule.onNodeWithText("Paid on 10 March").assertExists()
    }

    @Test
    fun theControlledOverloadReportsChangesAndTalkBackCanExpand() {
        val changes = mutableListOf<Boolean>()
        rule.setMonarchContent {
            var expanded by remember { mutableStateOf(false) }
            MonarchToggleBlock(
                summary = "Details",
                expanded = expanded,
                onExpandedChange = {
                    changes += it
                    expanded = it
                },
            ) { Text("Paid") }
        }
        rule.onNodeWithText("Details").performSemanticsAction(SemanticsActions.Expand)
        rule.onNodeWithText("Paid").assertExists()
        rule.onNodeWithText("Details").performSemanticsAction(SemanticsActions.Collapse)
        rule.onNodeWithText("Paid").assertDoesNotExist()
        assertEquals(listOf(true, false), changes)
    }

    @Test
    fun aDisabledSummaryDoesNotRespond() {
        var calls = 0
        rule.setMonarchContent {
            MonarchToggleBlock(summary = "Details", expanded = false, onExpandedChange = { calls++ }, enabled = false) {
                Text("Paid")
            }
        }
        rule.onNodeWithText("Details").assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun theStateWordsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchToggleBlock(summary = "Détails", expandedLabel = "Développé", collapsedLabel = "Réduit") {
                Text("Payé")
            }
        }
        assertEquals("Réduit", rule.onNodeWithText("Détails").stateDescription())
        rule.onNodeWithText("Détails").performClick()
        assertEquals("Développé", rule.onNodeWithText("Détails").stateDescription())
    }

    @Test
    fun theOwnStateSurvivesRecreation() {
        val restoration = StateRestorationTester(rule)
        restoration.setContent {
            MonarchTestFrame(MonarchTestEnvironment()) {
                MonarchToggleBlock(summary = "Details") { Text("Paid") }
            }
        }
        rule.onNodeWithText("Details").performClick()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithText("Paid").assertExists()
    }

    @Test
    fun theContentIsIndentedFromTheStartInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchToggleBlock(summary = "Details", modifier = Modifier.testTag("block"), initiallyExpanded = true) {
                Text("Paid", modifier = Modifier.testTag("content"))
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val block = rule.onNodeWithTag("block").getUnclippedBoundsInRoot()
            val content = rule.onNodeWithTag("content").getUnclippedBoundsInRoot()
            val indent = if (direction == LayoutDirection.Ltr) {
                content.left - block.left
            } else {
                block.right - content.right
            }
            assertEquals(MonarchToggleBlockTokens.ContentIndent, indent, direction.name)
        }
    }

    @Test
    fun theSummaryIsATouchTargetAndWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchToggleBlock(summary = "Why some records are skipped during the nightly import") { Text("Paid") }
        }
        rule.onNodeWithText("Why some records are skipped during the nightly import")
            .assertTouchTargetAtLeast48dp(rule.density)
            .assertTextFullyVisible()
    }
}

package com.rookhq.monarch.components.datadisplay

import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsibleTokens
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
import kotlin.test.assertTrue

internal object MonarchCollapsibleFixtures : ComponentFixtures {
    override val id = "collapsible"
    override val fixtures = listOf(
        fixture("collapsed") {
            MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "Advanced settings") { Text("Hidden") }
        },
        fixture("expanded with an icon") {
            MonarchCollapsible(
                expanded = true,
                onExpandedChange = {},
                label = "Details",
                trailingIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, size = MonarchIconSize.Sm)
                },
            ) { Text("The invoice was sent on 3 March and paid on 10 March.") }
        },
        fixture("disabled") {
            MonarchCollapsible(
                expanded = false,
                onExpandedChange = {},
                label = "Managed by your admin",
                enabled = false,
            ) { Text("Hidden") }
        },
        fixture("long label") {
            MonarchCollapsible(
                expanded = true,
                onExpandedChange = {},
                label = "Show the settings that only administrators of this workspace can change",
            ) { Text("Content") }
        },
        fixture("custom header, heading") {
            MonarchCollapsible(
                expanded = false,
                onExpandedChange = {},
                header = {
                    Text("Open tasks", modifier = Modifier.weight(1f, fill = false))
                    Text("12")
                },
                heading = true,
            ) { Text("Content") }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCollapsibleTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeaderIsAButtonThatShowsAndHidesTheContent() {
        rule.setMonarchContent {
            var expanded by remember { mutableStateOf(false) }
            MonarchCollapsible(expanded = expanded, onExpandedChange = { expanded = it }, label = "Details") {
                Text("Paid on 10 March")
            }
        }
        val header = rule.onNodeWithText("Details")
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Collapsed", header.stateDescription())
        rule.onNodeWithText("Paid on 10 March").assertDoesNotExist()

        header.performClick()
        assertEquals("Expanded", header.stateDescription())
        rule.onNodeWithText("Paid on 10 March").assertExists()

        header.performClick()
        rule.waitForIdle()
        assertEquals("Collapsed", header.stateDescription())
        rule.onNodeWithText("Paid on 10 March").assertDoesNotExist()
    }

    @Test
    fun talkBackCanExpandAndCollapseWithItsActions() {
        rule.setMonarchContent {
            var expanded by remember { mutableStateOf(false) }
            MonarchCollapsible(expanded = expanded, onExpandedChange = { expanded = it }, label = "Details") {
                Text("Paid")
            }
        }
        val header = rule.onNodeWithText("Details")
        header.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Collapse))
        header.performSemanticsAction(SemanticsActions.Expand)
        rule.onNodeWithText("Paid").assertExists()
        header.assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
        header.performSemanticsAction(SemanticsActions.Collapse)
        rule.waitForIdle()
        rule.onNodeWithText("Paid").assertDoesNotExist()
    }

    @Test
    fun aDisabledHeaderDoesNotRespond() {
        var calls = 0
        rule.setMonarchContent {
            MonarchCollapsible(expanded = false, onExpandedChange = { calls++ }, label = "Locked", enabled = false) {
                Text("Hidden")
            }
        }
        rule.onNodeWithText("Locked")
            .assertIsNotEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.Expand))
            .performClick()
        assertEquals(0, calls)
    }

    @Test
    fun theStateWordsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCollapsible(
                expanded = true,
                onExpandedChange = {},
                label = "Détails",
                expandedLabel = "Développé",
                collapsedLabel = "Réduit",
            ) { Text("Contenu") }
        }
        assertEquals("Développé", rule.onNodeWithText("Détails").stateDescription())
    }

    @Test
    fun theHeaderIsAtLeast48dpHigh() {
        rule.setMonarchContent {
            MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "A") { Text("B") }
        }
        rule.onNodeWithText("A").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theLabelComesBeforeTheIconInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCollapsible(
                expanded = false,
                onExpandedChange = {},
                label = "Details",
                trailingIcon = {
                    MonarchIcon(imageVector = TestIcon, contentDescription = null, modifier = Modifier.testTag("icon"))
                },
            ) { Text("B") }
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Details", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithTag("icon", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCollapsible(
                expanded = false,
                onExpandedChange = {},
                label = "Show the settings that only administrators can change",
            ) { Text("B") }
        }
        rule.onNodeWithText("Show the settings that only administrators can change", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theContentAppearsAtOnceWhenAnimationsAreOff() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        rule.mainClock.autoAdvance = false
        var expanded by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCollapsible(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                label = "Details",
                modifier = Modifier.testTag("root"),
            ) {
                Text("Paid", modifier = Modifier.testTag("content"))
            }
        }
        rule.mainClock.advanceTimeByFrame()
        val collapsed = rule.onNodeWithTag("root").fetchSemanticsNode().size.height
        expanded = true
        Snapshot.sendApplyNotifications()
        // Two frames: far less than the 180 ms the animation takes when animations are on.
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        val content = rule.onNodeWithTag("content").fetchSemanticsNode().size.height
        val whole = rule.onNodeWithTag("root").fetchSemanticsNode().size.height
        assertTrue(content > 0 && whole >= collapsed + content, "The content has its full height at once")
    }

    @Test
    fun theLabelAndIconUseTheDisabledColourWhenDisabled() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchCollapsibleTokens.labelColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchCollapsibleTokens.labelColor(false, colors), theme.id)
            assertEquals(colors.textMuted, MonarchCollapsibleTokens.iconColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchCollapsibleTokens.iconColor(false, colors), theme.id)
        }
    }

    @Test
    fun theHeaderSlotIsOneButtonWithTheStateAndCanBeAHeading() {
        var open by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCollapsible(
                expanded = open,
                onExpandedChange = { open = it },
                header = {
                    Text("Open tasks")
                    Text("12")
                },
                heading = true,
            ) { Text("Task list") }
        }
        val header = rule.onNode(hasText("Open tasks") and hasText("12") and isHeading())
        header.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Collapsed", header.stateDescription())
        header.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertTrue(open)
        rule.onNodeWithText("Task list").assertExists()
    }

    @Test
    fun theStringOverloadIsNotAHeadingUnlessAsked() {
        rule.setMonarchContent {
            MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "Details") { Text("x") }
        }
        rule.onNode(hasText("Details") and isHeading()).assertDoesNotExist()
    }
}

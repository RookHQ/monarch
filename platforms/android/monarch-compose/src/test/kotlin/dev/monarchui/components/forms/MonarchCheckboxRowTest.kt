package dev.monarchui.components.forms

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.forms.checkbox.MonarchCheckbox
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal object MonarchCheckboxRowFixtures : ComponentFixtures {
    override val id = "checkbox"
    override val fixtures = listOf(
        fixture("row, checked") { MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Send updates") },
        fixture("row, unchecked with description") {
            MonarchCheckboxRow(
                checked = false,
                onCheckedChange = {},
                label = "Share usage data",
                description = "Helps improve the product.",
            )
        },
        fixture("row, disabled") {
            MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Required by policy", enabled = false)
        },
        fixture("row, long label") {
            MonarchCheckboxRow(
                checked = false,
                onCheckedChange = {},
                label = "A label that is long enough to wrap onto a second line on a narrow phone screen",
            )
        },
        fixture("bare boxes: checked, unchecked, partly checked, invalid, disabled") {
            Row {
                MonarchCheckbox(
                    checked = true,
                    onCheckedChange = {},
                    modifier = Modifier.semantics { contentDescription = "Checked" },
                )
                MonarchCheckbox(
                    checked = false,
                    onCheckedChange = {},
                    modifier = Modifier.semantics { contentDescription = "Unchecked" },
                )
                MonarchCheckbox(
                    state = ToggleableState.Indeterminate,
                    onClick = {},
                    modifier = Modifier.semantics { contentDescription = "Partly checked" },
                )
                MonarchCheckbox(
                    checked = false,
                    onCheckedChange = {},
                    invalid = true,
                    modifier = Modifier.semantics { contentDescription = "Invalid" },
                )
                MonarchCheckbox(
                    checked = true,
                    onCheckedChange = {},
                    enabled = false,
                    modifier = Modifier.semantics { contentDescription = "Disabled" },
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCheckboxRowTest {
    @get:Rule
    val rule = createComposeRule()

    private val isCheckbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)

    @Test
    fun theRowIsOneToggleableElementWithTheLabel() {
        var checked by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchCheckboxRow(
                checked = checked,
                onCheckedChange = { checked = it },
                label = "Send updates",
                description = "At most once a week.",
            )
        }
        // Exactly one toggleable node: the drawn box is not a second target.
        assertEquals(1, rule.onAllNodes(isToggleable()).fetchSemanticsNodes().size)
        val row = rule.onNodeWithText("Send updates")
        row.assert(isCheckbox).assertIsOff().assertTextContains("At most once a week.")
        row.assertHeightIsAtLeast(48.dp)
        row.performClick()
        assertTrue(checked)
        row.assertIsOn()
    }

    @Test
    fun aDisabledRowIsAnnouncedAsDisabledAndIgnoresTaps() {
        var changes = 0
        rule.setMonarchContent {
            MonarchCheckboxRow(checked = false, onCheckedChange = { changes++ }, label = "Box", enabled = false)
        }
        rule.onNodeWithText("Box").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun theBoxIsAtTheStartInBothLayoutDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchCheckboxRow(checked = false, onCheckedChange = {}, label = "Box")
        }
        fun label() = rule.onNodeWithText("Box", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        fun row() = rule.onNodeWithText("Box").fetchSemanticsNode().boundsInRoot
        assertTrue(label().left > row().left)
        environment.layoutDirection = LayoutDirection.Rtl
        rule.waitForIdle()
        assertTrue(label().right < row().right)
    }

    @Test
    fun theRowGrowsWithTheFontSizeSetting() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCheckboxRow(
                checked = false,
                onCheckedChange = {},
                label = "A label that is long enough to wrap onto a second line at this size",
            )
        }
        rule.onNodeWithText("A label that is long enough", substring = true).assertHeightIsAtLeast(64.dp)
    }
}

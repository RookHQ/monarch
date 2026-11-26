package com.rookhq.monarch.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroup
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOption
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupOrientation
import com.rookhq.monarch.components.forms.checkboxgroup.MonarchCheckboxGroupTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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

private val channels = listOf(
    MonarchCheckboxGroupOption("email", "Email", description = "A summary every morning."),
    MonarchCheckboxGroupOption("push", "Push"),
    MonarchCheckboxGroupOption("sms", "Text message", enabled = false),
)

private fun stateIs(state: ToggleableState) = SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, state)

internal object MonarchCheckboxGroupFixtures : ComponentFixtures {
    override val id = "checkbox-group"
    override val fixtures = listOf(
        fixture("vertical with a label") {
            MonarchCheckboxGroup(
                options = channels,
                value = listOf("email"),
                onValueChange = {},
                label = "Notify me by",
                helperText = "Pick any.",
            )
        },
        fixture("select all, mixed") {
            MonarchCheckboxGroup(options = channels, value = listOf("push"), onValueChange = {}, selectAll = true)
        },
        fixture("horizontal") {
            MonarchCheckboxGroup(
                options = channels,
                value = listOf("email", "push"),
                onValueChange = {},
                orientation = MonarchCheckboxGroupOrientation.Horizontal,
            )
        },
        fixture("required and invalid") {
            MonarchCheckboxGroup(
                options = channels.take(2),
                value = emptyList(),
                onValueChange = {},
                required = true,
                label = "Channels",
                errorText = "Choose at least one channel.",
            )
        },
        fixture("disabled") {
            MonarchCheckboxGroup(
                options = channels,
                value = listOf("email"),
                onValueChange = {},
                enabled = false,
                selectAll = true,
            )
        },
        fixture("long text") {
            MonarchCheckboxGroup(
                options = listOf(
                    MonarchCheckboxGroupOption(
                        "weekly",
                        "Send me the weekly summary of every project I follow, with comments",
                    ),
                ),
                value = emptyList(),
                onValueChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCheckboxGroupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachRowIsACheckboxThatTogglesItsValueInOptionOrder() {
        var value by mutableStateOf(listOf("push"))
        rule.setMonarchContent {
            MonarchCheckboxGroup(options = channels, value = value, onValueChange = { value = it })
        }
        rule.onNodeWithText("Email")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsOff()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("email", "push"), value)
        rule.onNodeWithText("Email").assertIsOn()
        rule.onNodeWithText("Push").performClick()
        assertEquals(listOf("email"), value)
    }

    @Test
    fun aDisabledChoiceAndADisabledGroupIgnoreTaps() {
        var changes = 0
        var enabled by mutableStateOf(true)
        rule.setMonarchContent {
            MonarchCheckboxGroup(
                options = channels,
                value = emptyList(),
                onValueChange = { changes++ },
                enabled = enabled,
            )
        }
        rule.onNodeWithText("Text message").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        enabled = false
        rule.onNodeWithText("Email").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun selectAllIsMixedThenChecksEveryEnabledChoiceThenClearsThem() {
        var value by mutableStateOf(listOf("push", "sms"))
        rule.setMonarchContent {
            MonarchCheckboxGroup(options = channels, value = value, onValueChange = { value = it }, selectAll = true)
        }
        val all = rule.onNodeWithText("Select all")
        all.assert(stateIs(ToggleableState.Indeterminate)).assertIsEnabled().assertTouchTargetAtLeast48dp(rule.density)
        all.performClick()
        assertEquals(listOf("email", "push", "sms"), value)
        all.assert(stateIs(ToggleableState.On))
        all.performClick()
        // The disabled choice keeps its state.
        assertEquals(listOf("sms"), value)
        all.assert(stateIs(ToggleableState.Off))
    }

    @Test
    fun selectAllIsDisabledWhenNoChoiceCanChange() {
        rule.setMonarchContent {
            MonarchCheckboxGroup(
                options = channels,
                value = emptyList(),
                onValueChange = {},
                enabled = false,
                selectAll = true,
                selectAllLabel = "Every channel",
            )
        }
        rule.onNodeWithText("Every channel").assertIsNotEnabled()
    }

    @Test
    fun theLabelNamesTheGroupAndTheErrorIsSetOnIt() {
        rule.setMonarchContent {
            MonarchCheckboxGroup(
                options = channels,
                value = emptyList(),
                onValueChange = {},
                required = true,
                label = "Channels",
                errorText = "Choose at least one channel.",
            )
        }
        rule.onNode(
            hasContentDescription("Channels", substring = true) and
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose at least one channel."),
        ).assertExists()
        rule.onNodeWithText("Choose at least one channel.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun rowsMirrorRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchCheckboxGroup(
                options = channels,
                value = emptyList(),
                onValueChange = {},
                orientation = MonarchCheckboxGroupOrientation.Horizontal,
            )
        }
        rule.onNodeWithText("Email").assertIsBefore(rule.onNodeWithText("Push"), LayoutDirection.Rtl)
    }

    @Test
    fun longLabelsWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCheckboxGroup(
                options = listOf(MonarchCheckboxGroupOption(1, "Send me the weekly summary of every project I follow")),
                value = emptyList(),
                onValueChange = {},
                orientation = MonarchCheckboxGroupOrientation.Horizontal,
            )
        }
        rule.onNodeWithText("Send me", substring = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheValue() {
        val t = MonarchCheckboxGroupTokens
        assertEquals(ToggleableState.Off, t.selectAllState(channels, listOf("sms"), enabled = true))
        assertEquals(ToggleableState.On, t.selectAllState(channels, listOf("email", "push"), enabled = true))
        assertEquals(ToggleableState.Off, t.selectAllState(channels, listOf("email", "push"), enabled = false))
        assertEquals(listOf("email", "sms"), t.toggle(channels, listOf("sms"), "email", checked = true))
        assertEquals(listOf("email", "push"), t.toggleAll(channels, emptyList(), enabled = true))
    }
}

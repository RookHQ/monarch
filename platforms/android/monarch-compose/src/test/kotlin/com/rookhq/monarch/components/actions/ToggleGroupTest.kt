package com.rookhq.monarch.components.actions

import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.foundation.icon.MonarchIcon
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
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The web ToggleGroup with type multiple: Material 3 multi-choice segmented buttons, styled by MonarchTheme. */
@Composable
private fun MultipleGroup(
    options: List<String>,
    pressed: Set<String>,
    onPressedChange: (Set<String>) -> Unit = {},
    disabled: Set<String> = emptySet(),
    name: String = "Text style",
) {
    MultiChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = name }) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                checked = option in pressed,
                onCheckedChange = { on -> onPressedChange(if (on) pressed + option else pressed - option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                enabled = option !in disabled,
                label = { Text(option) },
            )
        }
    }
}

/** The web ToggleGroup with type single: one pressed item that can be released unless it is required. */
@Composable
private fun SingleGroup(
    options: List<String>,
    pressed: String?,
    onPressedChange: (String?) -> Unit = {},
    required: Boolean = false,
    withIcon: Boolean = false,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.semantics { contentDescription = "Alignment" }) {
        options.forEachIndexed { index, option ->
            val selected = option == pressed
            SegmentedButton(
                selected = selected,
                onClick = { onPressedChange(if (selected && !required) null else option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                icon = if (withIcon) {
                    { MonarchIcon(imageVector = TestIcon, contentDescription = null) }
                } else {
                    { SegmentedButtonDefaults.Icon(selected) }
                },
                label = { Text(option) },
            )
        }
    }
}

internal object ToggleGroupFixtures : ComponentFixtures {
    override val id = "toggle-group"
    override val fixtures = listOf(
        fixture("multiple, none pressed") { MultipleGroup(listOf("Bold", "Italic", "Underline"), emptySet()) },
        fixture("multiple, two pressed") {
            MultipleGroup(listOf("Bold", "Italic", "Underline"), setOf("Bold", "Underline"))
        },
        fixture("multiple, one disabled") {
            MultipleGroup(listOf("Bold", "Italic"), setOf("Bold"), disabled = setOf("Italic"))
        },
        fixture("single, one pressed") { SingleGroup(listOf("Start", "Centre", "End"), "Centre") },
        fixture("single, nothing pressed, icons") { SingleGroup(listOf("Start", "End"), null, withIcon = true) },
        fixture("counts in the labels") { MultipleGroup(listOf("Open 12", "Closed 3"), setOf("Open 12")) },
    )
}

@RunWith(RobolectricTestRunner::class)
class ToggleGroupTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun multipleItemsAreTogglesThatChangeOnTheirOwn() {
        rule.setMonarchContent {
            var pressed by remember { mutableStateOf(setOf("Bold")) }
            MultipleGroup(listOf("Bold", "Italic"), pressed, onPressedChange = { pressed = it })
        }
        rule.onNodeWithContentDescription("Text style").assertExists()
        rule.onNodeWithText("Bold")
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState))
            .assertIsOn()
        rule.onNodeWithText("Italic").assertIsOff().performClick()
        rule.onNodeWithText("Italic").assertIsOn()
        rule.onNodeWithText("Bold").assertIsOn().performClick()
        rule.onNodeWithText("Bold").assertIsOff()
    }

    @Test
    fun aSingleGroupPressesOneAndCanReleaseItUnlessRequired() {
        var value by mutableStateOf<String?>("Start")
        var required by mutableStateOf(false)
        rule.setMonarchContent {
            SingleGroup(listOf("Start", "End"), value, onPressedChange = { value = it }, required = required)
        }
        rule.onNodeWithText("Start")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))
            .assertIsSelected()
        rule.onNodeWithText("End").performClick()
        rule.onNodeWithText("End").assertIsSelected()
        rule.onNodeWithText("Start").assertIsNotSelected()
        rule.onNodeWithText("End").performClick()
        assertEquals(null, value)
        required = true
        rule.onNodeWithText("Start").performClick()
        rule.onNodeWithText("Start").performClick()
        assertEquals("Start", value)
    }

    @Test
    fun aDisabledItemCannotBePressed() {
        var pressed = setOf<String>()
        rule.setMonarchContent {
            MultipleGroup(
                listOf("Bold", "Italic"),
                pressed,
                onPressedChange = { pressed = it },
                disabled = setOf("Italic"),
            )
        }
        rule.onNodeWithText("Italic").assertIsNotEnabled().performClick()
        assertEquals(emptySet(), pressed)
    }

    @Test
    fun itemsHave48dpTargetsAndFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MultipleGroup(listOf("Bold", "Italic"), emptySet()) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Bold").assertTouchTargetAtLeast48dp(rule.density)
            rule.onNodeWithText("Bold", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Italic", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun labelsAreNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MultipleGroup(listOf("Bold", "Italic"), setOf("Bold")) }
        rule.onNodeWithText("Italic", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun monarchThemeGivesPressedItemsTheAccentRoles() {
        var segments: SegmentedButtonColors? = null
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { segments = SegmentedButtonDefaults.colors() }
        AllThemes.forEach { theme ->
            environment.tokens = theme
            rule.waitForIdle()
            val colors = MonarchColors(theme.colors)
            val c = assertNotNull(segments)
            assertEquals(colors.accentSubtle, c.activeContainerColor, theme.id)
            assertEquals(colors.accentStrong, c.activeContentColor, theme.id)
            assertEquals(colors.textDefault, c.inactiveContentColor, theme.id)
            assertEquals(colors.borderInput, c.activeBorderColor, theme.id)
        }
    }
}

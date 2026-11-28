package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.select.MonarchSelect
import dev.monarchui.components.forms.select.MonarchSelectOption
import dev.monarchui.components.forms.select.MonarchSelectTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private val regions = listOf(
    MonarchSelectOption("eu", "Europe", group = "Recommended"),
    MonarchSelectOption("us", "United States", description = "Virginia", group = "Recommended"),
    MonarchSelectOption("ap", "Asia Pacific", group = "Other"),
    MonarchSelectOption("sa", "South America", enabled = false, group = "Other"),
)

internal object MonarchSelectFixtures : ComponentFixtures {
    override val id = "select"
    override val fixtures = listOf(
        fixture("chosen") { MonarchSelect(options = regions, selected = "eu", onSelect = {}, label = "Region") },
        fixture("empty with helper") {
            MonarchSelect(options = regions, selected = null, onSelect = {}, label = "Region", helperText = "Help.")
        },
        fixture("error") {
            MonarchSelect(regions, selected = null, onSelect = {}, label = "Region", errorText = "Choose one.")
        },
        fixture("long label and value") {
            MonarchSelect(
                options = listOf(MonarchSelectOption(1, "A very long option name that will not fit on one line")),
                selected = 1,
                onSelect = {},
                label = "The region where every workspace file is stored",
            )
        },
        fixture("disabled") {
            MonarchSelect(options = regions, selected = "us", onSelect = {}, label = "Region", enabled = false)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSelectTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theFieldOpensAMenuAndChoosesAnOption() {
        var chosen: String? = null
        rule.setMonarchContent {
            var region by remember { mutableStateOf<String?>(null) }
            MonarchSelect(
                options = regions,
                selected = region,
                onSelect = {
                    region = it
                    chosen = it
                },
                label = "Region",
            )
        }
        rule.onNodeWithText("Choose an option").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Recommended") and isHeading()).assertExists()
        rule.onNode(hasText("Other") and isHeading()).assertExists()
        rule.onNode(hasText("Asia Pacific") and hasClickAction()).assertIsNotSelected().performClick()
        rule.waitForIdle()
        assertEquals("ap", chosen)
        rule.onNodeWithText("Asia Pacific").assertExists()
        rule.onNodeWithText("Recommended").assertDoesNotExist()
    }

    @Test
    fun theChosenOptionIsSelectedInTheMenu() {
        rule.setMonarchContent { MonarchSelect(options = regions, selected = "us", onSelect = {}, label = "Region") }
        rule.onNodeWithText("United States").performClick()
        rule.onNode(hasText("United States") and hasClickAction() and hasText("Virginia")).assertIsSelected()
        rule.onNode(hasText("Europe") and hasClickAction()).assertIsNotSelected()
    }

    @Test
    fun aDisabledOptionCannotBeChosen() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchSelect(options = regions, selected = null, onSelect = { chosen = it }, label = "Region")
        }
        rule.onNodeWithText("Choose an option").performClick()
        rule.onNode(hasText("South America") and hasClickAction()).assertIsNotEnabled().performClick()
        assertNull(chosen)
    }

    @Test
    fun aDisabledFieldDoesNotOpen() {
        rule.setMonarchContent {
            MonarchSelect(options = regions, selected = "eu", onSelect = {}, label = "Region", enabled = false)
        }
        rule.onNodeWithText("Europe").performClick()
        rule.onNodeWithText("Recommended").assertDoesNotExist()
    }

    @Test
    fun aDisabledFieldIsReadAsDisabled() {
        rule.setMonarchContent {
            MonarchSelect(options = regions, selected = "eu", onSelect = {}, label = "Region", enabled = false)
        }
        rule.onNodeWithContentDescription("Region").assertIsNotEnabled()
    }

    @Test
    fun anErrorReplacesTheHelperAndIsTheFieldsError() {
        rule.setMonarchContent {
            MonarchSelect(
                options = regions,
                selected = null,
                onSelect = {},
                label = "Region",
                helperText = "Help",
                errorText = "Choose a region.",
                placeholder = "Pick",
            )
        }
        rule.onNodeWithText("Help").assertDoesNotExist()
        rule.onNodeWithText("Choose a region.", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Region")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Choose a region."))
    }

    @Test
    fun longTextFitsAtTheLargestFontSizeRightToLeft() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchSelect(regions, selected = null, onSelect = {}, label = "Region", helperText = "Where data lives.")
        }
        rule.onNodeWithText("Where data lives.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun optionsAreGroupedInRunsAndTheChoiceIsFound() {
        val sections = MonarchSelectTokens.sections(regions + MonarchSelectOption("x", "X"))
        assertEquals(listOf("Recommended", "Other", null), sections.map { it.group })
        assertEquals(listOf(2, 2, 1), sections.map { it.options.size })
        assertEquals("Europe", MonarchSelectTokens.chosen(regions, "eu")?.label)
        assertNull(MonarchSelectTokens.chosen(regions, "zz"))
        assertNull(MonarchSelectTokens.chosen(regions, null))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, MonarchSelectTokens.valueColor(chosen = true, enabled = true, c), theme.id)
            assertEquals(c.textMuted, MonarchSelectTokens.valueColor(chosen = false, enabled = true, c), theme.id)
            assertEquals(c.textDisabled, MonarchSelectTokens.valueColor(chosen = true, enabled = false, c), theme.id)
            assertEquals(c.textMuted, MonarchSelectTokens.headerColor(c), theme.id)
            assertEquals(c.textDisabled, MonarchSelectTokens.descriptionColor(false, c), theme.id)
            assertEquals(c.accentStrong, MonarchSelectTokens.markColor(selected = true, enabled = true, c), theme.id)
            assertEquals(c.borderInput, MonarchSelectTokens.markColor(selected = false, enabled = true, c), theme.id)
            assertEquals(c.textDisabled, MonarchSelectTokens.markColor(selected = true, enabled = false, c), theme.id)
        }
    }

    @Test
    fun theNameCanBeGivenWithoutAVisibleLabel() {
        rule.setMonarchContent {
            MonarchSelect(options = regions, selected = "eu", onSelect = {}, contentDescription = "Region")
        }
        rule.onNodeWithContentDescription("Region").assertExists()
    }
}

package dev.monarchui.components.workflow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.workflow.statuspicker.MonarchStatusCategory
import dev.monarchui.components.workflow.statuspicker.MonarchStatusGlyph
import dev.monarchui.components.workflow.statuspicker.MonarchStatusOption
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPicker
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPickerLabels
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPickerSize
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPickerTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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

private val Statuses = listOf(
    MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted),
    MonarchStatusOption("doing", "Doing", MonarchStatusCategory.Started),
    MonarchStatusOption("triage", "Triage", MonarchStatusCategory.Backlog),
    MonarchStatusOption("review", "In review", MonarchStatusCategory.Started),
    MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed),
)

internal object MonarchStatusPickerFixtures : ComponentFixtures {
    override val id = "status-picker"
    override val fixtures = listOf(
        fixture("each category, md") {
            Column {
                listOf("triage", "todo", "doing", "done").forEach {
                    MonarchStatusPicker(statuses = Statuses, value = it, onValueChange = {})
                }
            }
        },
        fixture("sm, glyph only, disabled, unset") {
            Column {
                MonarchStatusPicker(
                    statuses = Statuses,
                    value = "review",
                    onValueChange = {},
                    size = MonarchStatusPickerSize.Sm,
                )
                MonarchStatusPicker(statuses = Statuses, value = "done", onValueChange = {}, showLabel = false)
                MonarchStatusPicker(statuses = Statuses, value = "todo", onValueChange = {}, enabled = false)
                MonarchStatusPicker(statuses = Statuses, value = null, onValueChange = {})
            }
        },
        fixture("long name") {
            MonarchStatusPicker(
                statuses = listOf(
                    MonarchStatusOption(
                        "w",
                        "Waiting for a second approval from finance",
                        MonarchStatusCategory.Started,
                    ),
                ),
                value = "w",
                onValueChange = {},
            )
        },
        fixture("glyphs") {
            Row { MonarchStatusCategory.entries.forEach { MonarchStatusGlyph(it) } }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchStatusPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val isDropdown = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)

    @Test
    fun theTriggerIsADropdownNamedWithTheStatus() {
        rule.setMonarchContent { MonarchStatusPicker(statuses = Statuses, value = "doing", onValueChange = {}) }
        rule.onNodeWithContentDescription("Status: Doing")
            .assert(isDropdown)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun choosingAStatusCallsBackWithItsIdAndTheCurrentOneIsSelected() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchStatusPicker(statuses = Statuses, value = "todo", onValueChange = { chosen = it })
        }
        rule.onNodeWithContentDescription("Status: Todo").performClick()
        rule.onNode(hasText("Todo") and hasAnyAncestor(isPopup())).assertIsSelected()
        rule.onNode(hasText("Done") and hasAnyAncestor(isPopup())).assertIsNotSelected().performClick()
        assertEquals("done", chosen)
        rule.onNode(hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @Test
    fun choosingTheCurrentStatusOnlyClosesTheMenu() {
        var calls = 0
        rule.setMonarchContent {
            MonarchStatusPicker(statuses = Statuses, value = "todo", onValueChange = { calls++ })
        }
        rule.onNodeWithContentDescription("Status: Todo").performClick()
        rule.onNode(hasText("Todo") and hasAnyAncestor(isPopup())).performClick()
        assertEquals(0, calls)
    }

    @Test
    fun theMenuHasAHeadingPerCategoryWithStatuses() {
        rule.setMonarchContent { MonarchStatusPicker(statuses = Statuses, value = "todo", onValueChange = {}) }
        rule.onNodeWithContentDescription("Status: Todo").performClick()
        listOf("Backlog", "Not started", "In progress", "Completed").forEach {
            rule.onNode(hasText(it) and isHeading() and hasAnyAncestor(isPopup())).assertExists()
        }
        rule.onNode(hasText("Cancelled") and hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @Test
    fun groupsFollowTheCategoryOrderAndKeepTheGivenOrderInside() {
        val groups = MonarchStatusPickerTokens.groups(Statuses)
        assertEquals(
            listOf(MonarchStatusCategory.Backlog, MonarchStatusCategory.Unstarted, MonarchStatusCategory.Started,
                MonarchStatusCategory.Completed),
            groups.map { it.first },
        )
        assertEquals(listOf("doing", "review"), groups[2].second.map { it.id })
        assertEquals(emptyList(), MonarchStatusPickerTokens.groups(emptyList()))
    }

    @Test
    fun anUnknownOrMissingValueShowsNoStatus() {
        rule.setMonarchContent {
            Column {
                MonarchStatusPicker(statuses = Statuses, value = null, onValueChange = {})
                MonarchStatusPicker(statuses = Statuses, value = "gone", onValueChange = {}, showLabel = false)
            }
        }
        rule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Status: No status")),
        )
            .assertCountEquals(2)
    }

    @Test
    fun aDisabledPickerDoesNotOpen() {
        rule.setMonarchContent {
            MonarchStatusPicker(statuses = Statuses, value = "todo", onValueChange = {}, enabled = false)
        }
        rule.onNodeWithContentDescription("Status: Todo").assertIsNotEnabled().performClick()
        rule.onNode(hasAnyAncestor(isPopup())).assertDoesNotExist()
    }

    @Test
    fun theGlyphOnlyTriggerIsStillNamedAndHas48dp() {
        rule.setMonarchContent {
            MonarchStatusPicker(statuses = Statuses, value = "done", onValueChange = {}, showLabel = false)
        }
        rule.onNodeWithContentDescription("Status: Done")
            .assert(isDropdown)
            .assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchStatusPicker(
                statuses = Statuses,
                value = null,
                onValueChange = {},
                labels = MonarchStatusPickerLabels(
                    trigger = { "Estado: $it" },
                    none = "Sin estado",
                    backlog = "Pendiente",
                ),
            )
        }
        rule.onNodeWithContentDescription("Estado: Sin estado").performClick()
        rule.onNode(hasText("Pendiente") and hasAnyAncestor(isPopup())).assertExists()
    }

    @Test
    fun theGlyphComesBeforeTheNameRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchStatusPicker(statuses = Statuses, value = "doing", onValueChange = {}, trailingIcon = null)
        }
        val name = rule.onNode(hasText("Doing"), useUnmergedTree = true).getUnclippedBoundsInRoot()
        val trigger = rule.onNodeWithContentDescription("Status: Doing").getUnclippedBoundsInRoot()
        assertTrue(trigger.right - name.right > name.left - trigger.left)
    }

    @Test
    fun theNameIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchStatusPicker(statuses = Statuses, value = "review", onValueChange = {})
        }
        rule.onNode(hasText("In review"), useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun onlyCompletedAndCancelledAreClosed() {
        assertEquals(
            listOf(false, false, false, true, true),
            MonarchStatusCategory.entries.map { it.isClosed },
        )
        assertFalse(MonarchStatusCategory.Started.isClosed)
    }

    @Test
    fun glyphColoursComeFromTheThemeByCategory() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchStatusPickerTokens
            assertEquals(colors.textSubtle, tokens.glyphColors(colors, MonarchStatusCategory.Backlog).mark)
            assertEquals(colors.textMuted, tokens.glyphColors(colors, MonarchStatusCategory.Unstarted).mark)
            assertEquals(colors.warningStrong, tokens.glyphColors(colors, MonarchStatusCategory.Started).mark)
            assertEquals(colors.successStrong, tokens.glyphColors(colors, MonarchStatusCategory.Completed).mark)
            assertEquals(colors.textSubtle, tokens.glyphColors(colors, MonarchStatusCategory.Cancelled).mark)
            assertEquals(colors.bgSurface, tokens.glyphColors(colors, MonarchStatusCategory.Completed).cutout)
        }
    }

    @Test
    fun optionsAreEqualByValue() {
        assertEquals(
            MonarchStatusOption("a", "A", MonarchStatusCategory.Started),
            MonarchStatusOption("a", "A", MonarchStatusCategory.Started),
        )
        assertFalse(
            MonarchStatusOption("a", "A", MonarchStatusCategory.Started) ==
                MonarchStatusOption("a", "A", MonarchStatusCategory.Completed),
        )
    }
}

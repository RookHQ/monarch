package com.rookhq.monarch.components.search

import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.search.facetedfilterpanel.FacetedFilterPanelFormat
import com.rookhq.monarch.components.search.facetedfilterpanel.FacetedFilterPanelModel
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacet
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetKind
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetOption
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetSelection
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetedFilterPanel
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetedFilterPanelLabels
import com.rookhq.monarch.components.search.facetedfilterpanel.MonarchFacetedFilterPanelTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

private val Status = MonarchFacet.options(
    id = "status",
    label = "Status",
    options = listOf(
        MonarchFacetOption("open", "Open", count = 128),
        MonarchFacetOption("done", "Done", count = 1_204),
        MonarchFacetOption("archived", "Archived", count = 0, enabled = false),
    ),
    maxVisible = 2,
)
private val Size = MonarchFacet.range("size", "File size", bounds = 0f..100f, step = 5f, unit = "MB")
private val Owner = MonarchFacet.options("owner", "Owner", options = emptyList(), initiallyExpanded = false)

internal object MonarchFacetedFilterPanelFixtures : ComponentFixtures {
    override val id = "faceted-filter-panel"
    override val fixtures = listOf(
        fixture("no filters, options with counts and show more") {
            MonarchFacetedFilterPanel(facets = listOf(Status), value = emptyMap(), onValueChange = {})
        },
        fixture("active filters with clear buttons and an icon") {
            MonarchFacetedFilterPanel(
                facets = listOf(Status, Owner),
                value = mapOf("status" to MonarchFacetSelection.Options(setOf("archived", "done"))),
                onValueChange = {},
                expandIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("range, no title") {
            MonarchFacetedFilterPanel(
                facets = listOf(Size),
                value = mapOf("size" to MonarchFacetSelection.Range(10f..50f)),
                onValueChange = {},
                showTitle = false,
            )
        },
        fixture("disabled") {
            MonarchFacetedFilterPanel(
                facets = listOf(Owner, Size),
                value = mapOf("size" to MonarchFacetSelection.Range(10f..50f)),
                onValueChange = {},
                enabled = false,
            )
        },
        fixture("long labels") {
            MonarchFacetedFilterPanel(
                facets = listOf(
                    MonarchFacet.options(
                        "kind",
                        "The kind of document that was uploaded to this workspace",
                        listOf(MonarchFacetOption("contract", "Signed contracts and their amendments", count = 12_345)),
                    ),
                ),
                value = emptyMap(),
                onValueChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchFacetedFilterPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)

    @Test
    fun selectionRulesRemoveEmptyEntriesAndCountActiveFilters() {
        var value = FacetedFilterPanelModel.toggle(Status, "open", true, emptyMap())
        assertEquals(mapOf("status" to MonarchFacetSelection.Options(setOf("open"))), value)
        value = FacetedFilterPanelModel.toggle(Status, "done", true, value)
        assertEquals(2, FacetedFilterPanelModel.activeCount(Status, value))
        value = FacetedFilterPanelModel.toggle(Status, "open", false, value)
        value = FacetedFilterPanelModel.toggle(Status, "done", false, value)
        assertEquals(emptyMap(), value)
        value = FacetedFilterPanelModel.setRange(Size, 10f..100f, value)
        assertEquals(1, FacetedFilterPanelModel.totalActive(listOf(Status, Size), value))
        assertEquals(emptyMap(), FacetedFilterPanelModel.setRange(Size, 0f..100f, value))
        assertEquals(emptyMap(), FacetedFilterPanelModel.clear(Size, value))
    }

    @Test
    fun checkedOptionsStayVisibleAndStepsAndTextFollowTheFacet() {
        val kind = Status.kind as MonarchFacetKind.Options
        assertEquals(listOf("open", "done"), FacetedFilterPanelModel.visible(kind, emptySet(), false).map { it.id })
        assertEquals(
            listOf("open", "done", "archived"),
            FacetedFilterPanelModel.visible(kind, setOf("archived"), false).map { it.id },
        )
        assertEquals(19, FacetedFilterPanelFormat.steps(Size.kind as MonarchFacetKind.Range))
        assertEquals(0, FacetedFilterPanelFormat.steps(MonarchFacetKind.Range(0f..1f, step = 0f)))
        assertEquals(0, FacetedFilterPanelFormat.steps(MonarchFacetKind.Range(0f..1_000_000f)))
        assertEquals("25 MB", FacetedFilterPanelFormat.valueText(25f, "MB", Locale.US))
        assertEquals("2.5", FacetedFilterPanelFormat.valueText(2.5f, null, Locale.US))
        assertEquals("1,204", FacetedFilterPanelFormat.countText(1_204, Locale.US))
    }

    @Test
    fun optionsAreCheckboxesReadWithTheirCountsAndToggleTheValue() {
        var value by mutableStateOf<Map<String, MonarchFacetSelection>>(emptyMap())
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(facets = listOf(Status), value = value, onValueChange = { value = it })
        }
        rule.onNodeWithText("Filters").assert(isHeading)
        val done = rule.onNode(hasText("Done") and hasText("1,204"))
        done.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).performClick()
        done.assertIsOn()
        assertEquals(mapOf("status" to MonarchFacetSelection.Options(setOf("done"))), value)
        rule.onNodeWithText("1 selected", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Clear Status filter")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(emptyMap(), value)
        rule.onNodeWithText("Clear all").assertIsNotEnabled()
    }

    @Test
    fun showMoreRevealsTheRestAndADisabledOptionCannotChange() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(facets = listOf(Status), value = emptyMap(), onValueChange = {})
        }
        rule.onNodeWithText("Archived").assertDoesNotExist()
        rule.onNodeWithText("Show 1 more").performClick()
        rule.onNode(hasText("Archived") and hasText("0")).assertIsNotEnabled()
        rule.onNodeWithText("Show fewer").performClick()
        rule.onNodeWithText("Archived").assertDoesNotExist()
    }

    @Test
    fun groupHeadersAreHeadingButtonsWithTheirState() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(facets = listOf(Status, Owner), value = emptyMap(), onValueChange = {})
        }
        val owner = rule.onNode(hasText("Owner") and hasClickAction())
        owner.assert(isHeading).assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("Collapsed", owner.stateDescription())
        rule.onNodeWithText("No options").assertDoesNotExist()
        owner.performClick()
        rule.waitForIdle()
        assertEquals("Expanded", owner.stateDescription())
        rule.onNodeWithText("No options").assertExists()
        val status = rule.onNode(hasText("Status") and hasClickAction())
        status.performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Open").assertDoesNotExist()
    }

    @Test
    fun aRangeIsMinimumAndMaximumFieldsNamedForTheFacetWithItsValues() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(
                facets = listOf(Size),
                value = mapOf("size" to MonarchFacetSelection.Range(10f..50f)),
                onValueChange = {},
            )
        }
        val slider = rule.onNode(hasContentDescription("File size"))
        assertEquals("10 MB – 50 MB", slider.stateDescription())
        // The fields are labelled text fields now: the label is a child of the field node.
        rule.onNodeWithText("Minimum", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Maximum", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("10").assertExists()
        rule.onNodeWithText("50").assertExists()
    }

    @Test
    fun aDisabledPanelDisablesEveryControl() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(
                facets = listOf(Status),
                value = mapOf("status" to MonarchFacetSelection.Options(setOf("open"))),
                onValueChange = {},
                enabled = false,
            )
        }
        rule.onNode(hasText("Open") and hasText("128")).assertIsNotEnabled()
        rule.onNodeWithText("Clear all").assertIsNotEnabled()
        rule.onNode(hasText("Status") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun withoutTheTitleThePanelIsStillNamed() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(facets = listOf(Owner), value = emptyMap(), onValueChange = {}, showTitle = false)
        }
        rule.onNodeWithText("Filters").assertDoesNotExist()
        rule.onNodeWithContentDescription("Filters").assertExists()
    }

    @Test
    fun rightToLeftMirrorsTheRows() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFacetedFilterPanel(facets = listOf(Status), value = emptyMap(), onValueChange = {})
        }
        rule.onNodeWithText("Open", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("128", useUnmergedTree = true), LayoutDirection.Rtl)
        rule.onNodeWithText("Filters").assertIsBefore(rule.onNodeWithText("Clear all"), LayoutDirection.Rtl)
    }

    @Test
    fun textFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFacetedFilterPanel(
                facets = listOf(
                    MonarchFacet.options(
                        "kind",
                        "Kind",
                        listOf(MonarchFacetOption("contract", "Signed contracts and their amendments", count = 12_345)),
                    ),
                ),
                value = emptyMap(),
                onValueChange = {},
            )
        }
        rule.onNodeWithText("Signed contracts and their amendments", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("12,345", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchFacetedFilterPanel(
                facets = listOf(Status),
                value = mapOf("status" to MonarchFacetSelection.Options(setOf("open"))),
                onValueChange = {},
                labels = MonarchFacetedFilterPanelLabels(
                    title = "Refine",
                    clearAll = "Reset",
                    clearGroupLabel = { "Reset $it" },
                    activeCount = { "$it on" },
                    showMore = { "$it hidden" },
                    expanded = "Open",
                ),
            )
        }
        rule.onNodeWithText("Refine").assertExists()
        rule.onNodeWithText("Reset").assertIsEnabled()
        rule.onNodeWithContentDescription("Reset Status").assertExists()
        rule.onNodeWithText("1 on", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("1 hidden").assertExists()
        assertEquals("Open", rule.onNode(hasText("Status") and hasClickAction()).stateDescription())
    }

    @Test
    fun colorsComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val roles = MonarchColors(theme.colors)
            val colors = MonarchFacetedFilterPanelTokens.colors(roles, enabled = true)
            assertEquals(roles.accentStrong, colors.active)
            assertEquals(roles.textMuted, colors.count)
            assertEquals(roles.textDefault, colors.title)
            val disabled = MonarchFacetedFilterPanelTokens.colors(roles, enabled = false)
            assertEquals(roles.textDisabled, disabled.active)
            val tokens = MonarchFacetedFilterPanelTokens
            assertEquals(roles.textDisabled, tokens.optionColor(roles, enabled = false, muted = true))
            assertEquals(roles.textMuted, tokens.optionColor(roles, enabled = true, muted = true))
        }
    }
}

package dev.monarchui.components.technical

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.technical.scopeselector.MonarchScopeGroup
import dev.monarchui.components.technical.scopeselector.MonarchScopeOption
import dev.monarchui.components.technical.scopeselector.MonarchScopeSelector
import dev.monarchui.components.technical.scopeselector.MonarchScopeSelectorAutoSelect
import dev.monarchui.components.technical.scopeselector.MonarchScopeSelectorLabels
import dev.monarchui.components.technical.scopeselector.MonarchScopeSelectorTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Projects = MonarchScopeGroup(
    id = "projects",
    label = "Projects API",
    description = "Owned by Platform",
    scopes = listOf(
        MonarchScopeOption("projects:read", "Read projects"),
        MonarchScopeOption("projects:write", "Change projects", description = "Create, rename and archive."),
        MonarchScopeOption("projects:admin", enabled = false),
    ),
)
private val Billing = MonarchScopeGroup(
    id = "billing",
    label = "Billing API",
    scopes = listOf(MonarchScopeOption("billing:read", "Read invoices")),
)
private val Loading = MonarchScopeGroup(id = "files", label = "Files API", scopes = emptyList(), loading = true)
private val Empty = MonarchScopeGroup(id = "none", label = "Search API", scopes = emptyList())

internal object MonarchScopeSelectorFixtures : ComponentFixtures {
    override val id = "scope-selector"
    override val fixtures = listOf(
        fixture("collapsed groups") {
            MonarchScopeSelector(groups = listOf(Projects, Billing), value = listOf("billing:read"), onValueChange = {})
        },
        fixture("expanded group") {
            MonarchScopeSelector(
                groups = listOf(Billing),
                value = emptyList(),
                onValueChange = {},
                defaultExpanded = setOf("billing"),
            )
        },
        fixture("loading and empty groups") {
            MonarchScopeSelector(groups = listOf(Loading, Empty), value = emptyList(), onValueChange = {})
        },
        fixture("no groups") { MonarchScopeSelector(groups = emptyList(), value = emptyList(), onValueChange = {}) },
        fixture("paged, disabled, without heading") {
            MonarchScopeSelector(
                groups = listOf(Billing, Projects),
                value = emptyList(),
                onValueChange = {},
                enabled = false,
                showLabel = false,
                pageSize = 1,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchScopeSelectorTest {
    @get:Rule
    val rule = createComposeRule()

    private fun show(
        groups: List<MonarchScopeGroup>,
        start: List<String> = emptyList(),
        onChange: (List<String>) -> Unit = {},
    ) {
        rule.setMonarchContent {
            var value by remember { mutableStateOf(start) }
            MonarchScopeSelector(
                groups = groups,
                value = value,
                onValueChange = {
                    value = it
                    onChange(it)
                },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }

    @Test
    fun eachGroupIsACollapsibleWithACount() {
        show(listOf(Projects), listOf("projects:read"))
        val header = rule.onNodeWithText("Projects API")
        assertEquals("Collapsed", header.stateDescription())
        header.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("1 of 3 scopes").assertExists()
        rule.onNodeWithText("Owned by Platform").assertExists()
        rule.onNodeWithText("Read projects").assertDoesNotExist()
        header.performClick()
        assertEquals("Expanded", header.stateDescription())
        rule.onNodeWithText("Read projects").assertIsOn()
        rule.onNodeWithText("Change projects").assertIsOff()
        rule.onNodeWithText("projects:admin").assertIsNotEnabled()
    }

    @Test
    fun checkingAScopeKeepsOtherGroupsSelections() {
        var result: List<String>? = null
        show(listOf(Projects, Billing), listOf("billing:read"), { result = it })
        rule.onNodeWithText("Projects API").performClick()
        rule.onNodeWithText("Change projects").performClick()
        assertEquals(listOf("billing:read", "projects:write"), result)
    }

    @Test
    fun selectAllSkipsDisabledScopesThenBecomesClearAll() {
        var result: List<String>? = null
        show(listOf(Projects), onChange = { result = it })
        rule.onNodeWithContentDescription("Select all in Projects API")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("projects:read", "projects:write"), result)
        rule.onNodeWithContentDescription("Clear all in Projects API").performClick()
        assertEquals(emptyList(), result)
    }

    @Test
    fun loadingAndEmptyGroupsSayWhy() {
        val explained = MonarchScopeGroup(Empty.id, Empty.label, Empty.scopes, emptyMessage = "Ask an admin.")
        show(listOf(Loading, explained))
        rule.onNodeWithText("Loading scopes…").assertExists()
        rule.onNodeWithText("Ask an admin.").assertExists()
        rule.onNodeWithText("Select all").assertDoesNotExist()
    }

    @Test
    fun showMoreRevealsTheNextPage() {
        rule.setMonarchContent {
            MonarchScopeSelector(
                groups = listOf(Billing, Projects, Empty),
                value = emptyList(),
                onValueChange = {},
                pageSize = 1,
            )
        }
        rule.onNodeWithText("Projects API").assertDoesNotExist()
        rule.onNodeWithText("Show 1 more of 2").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Projects API").assertExists()
        rule.onNodeWithText("Show 1 more of 1").assertExists()
    }

    @Test
    fun autoSelectFillsGroupsWithNothingSelectedOnce() {
        var result: List<String>? = null
        rule.setMonarchContent {
            MonarchScopeSelector(
                groups = listOf(Billing, Loading),
                value = emptyList(),
                onValueChange = { result = it },
                autoSelect = MonarchScopeSelectorAutoSelect.All,
            )
        }
        rule.waitForIdle()
        assertEquals(listOf("billing:read"), result)
    }

    @Test
    fun autoSelectLogicSkipsLoadingSeenAndAlreadySelectedGroups() {
        val seen = mutableSetOf<String>()
        assertEquals(
            listOf("billing:read", "projects:read", "projects:write"),
            MonarchScopeSelectorTokens.autoSelected(listOf(Billing, Projects, Loading), emptyList(), seen),
        )
        assertEquals(setOf("billing", "projects"), seen)
        assertNull(MonarchScopeSelectorTokens.autoSelected(listOf(Billing), emptyList(), seen))
        assertNull(MonarchScopeSelectorTokens.autoSelected(listOf(Projects), listOf("projects:read"), mutableSetOf()))
    }

    @Test
    fun disabledSelectorHidesSelectAllButStillExpands() {
        rule.setMonarchContent {
            MonarchScopeSelector(
                groups = listOf(Billing),
                value = emptyList(),
                onValueChange = { error("changed") },
                enabled = false,
            )
        }
        rule.onNodeWithText("Select all").assertDoesNotExist()
        rule.onNodeWithText("Billing API").performClick()
        rule.onNodeWithText("Read invoices").assertIsNotEnabled()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchScopeSelector(
                groups = listOf(Billing),
                value = emptyList(),
                onValueChange = {},
                labels = MonarchScopeSelectorLabels(label = "Permissions", count = { s, t -> "$s/$t" }),
            )
        }
        rule.onNodeWithText("Permissions").assertExists()
        rule.onNodeWithText("0/1").assertExists()
    }

    @Test
    fun theCountComesBeforeSelectAllRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchScopeSelector(groups = listOf(Billing), value = emptyList(), onValueChange = {})
        }
        rule.onNodeWithText("0 of 1 scope").assertIsBefore(
            rule.onNodeWithContentDescription("Select all in Billing API"),
            LayoutDirection.Rtl,
        )
    }

    @Test
    fun aLongGroupNameIsNotCutAtTheLargestFontScale() {
        val long = MonarchScopeGroup(id = "x", label = "Customer relationship management API", scopes = Billing.scopes)
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchScopeSelector(groups = listOf(long), value = emptyList(), onValueChange = {})
        }
        rule.onNodeWithText("Customer relationship management API").assertTextFullyVisible()
    }

    @Test
    fun selectionLogicKeepsOrderAndDisabledScopes() {
        val value = listOf("billing:read", "projects:admin")
        assertEquals(listOf("billing:read", "projects:admin", "projects:read", "projects:write"),
            MonarchScopeSelectorTokens.toggledAll(Projects, value))
        assertEquals(listOf("billing:read", "projects:read"),
            MonarchScopeSelectorTokens.withGroup(Projects, value, listOf("projects:read")))
        assertTrue(MonarchScopeSelectorTokens.allOn(Projects, listOf("projects:read", "projects:write")))
        assertEquals(1, MonarchScopeSelectorTokens.selectedCount(Projects, value))
    }

    @Test
    fun aGroupWithSelectedScopesTakesTheAccentBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.borderSubtle, MonarchScopeSelectorTokens.groupColors(colors).border)
            assertEquals(colors.bgSurface, MonarchScopeSelectorTokens.groupColors(colors).container)
        }
    }
}

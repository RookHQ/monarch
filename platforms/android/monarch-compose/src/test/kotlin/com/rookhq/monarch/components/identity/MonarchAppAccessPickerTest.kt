@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.identity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessOption
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessPicker
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessPickerLabels
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessPickerRules
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessPickerTokens
import com.rookhq.monarch.components.identity.appaccesspicker.MonarchAppAccessSummary
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
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

private val Crm = MonarchAppAccessOption("crm", "CRM", description = "Contacts and deals")
private val Books = MonarchAppAccessOption("books", "Books", description = "Invoices", badge = "Pro")
private val Id = MonarchAppAccessOption("id", "Rook ID", description = "Included for everyone", enabled = false)
private val Apps = listOf(Crm, Books, Id)

internal object MonarchAppAccessPickerFixtures : ComponentFixtures {
    override val id = "app-access-picker"
    override val fixtures = listOf(
        fixture("apps with a tag and a fixed app") {
            MonarchAppAccessPicker(apps = Apps, value = listOf("crm", "id"), onValueChange = {})
        },
        fixture("disabled with help text") {
            MonarchAppAccessPicker(
                apps = listOf(Crm),
                value = emptyList(),
                onValueChange = {},
                enabled = false,
                description = "Access changes apply at the next sign-in.",
            )
        },
        fixture("loading") {
            MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, loading = true)
        },
        fixture("empty, hidden label") {
            MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, hideLabel = true)
        },
        fixture("long names") {
            MonarchAppAccessPicker(
                apps = listOf(
                    MonarchAppAccessOption(
                        "long",
                        "Customer relationship management for every regional sales team",
                        description = "Contacts, companies, deals, pipelines and forecasts for the whole company",
                        badge = "Enterprise",
                    ),
                ),
                value = listOf("long"),
                onValueChange = {},
                showBulkActions = false,
            )
        },
        fixture("summary") { MonarchAppAccessSummary(apps = Apps, value = listOf("crm", "books", "id")) },
        fixture("summary, none") { MonarchAppAccessSummary(apps = Apps, value = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAppAccessPickerTest {
    @get:Rule
    val rule = createComposeRule()

    private val checkbox = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox)
    private val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Test
    fun eachRowIsOneCheckboxThatTogglesTheApp() {
        var value by mutableStateOf(listOf<String>())
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = Apps, value = value, onValueChange = { value = it })
        }
        rule.onNode(checkbox and hasText("CRM"))
            .assertIsOff()
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("crm"), value)
        rule.onNode(checkbox and hasText("CRM")).assertIsOn()
        rule.onNode(checkbox and hasText("Books")).assert(hasText("Pro")).assert(hasText("Invoices")).performClick()
        assertEquals(listOf("crm", "books"), value)
        rule.onNode(checkbox and hasText("CRM")).performClick()
        assertEquals(listOf("books"), value)
    }

    @Test
    fun anAppThatCannotBeChangedIsDisabledAndTheCountIsAnnounced() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = Apps, value = listOf("id"), onValueChange = { changes++ })
        }
        rule.onNode(checkbox and hasText("Rook ID")).assertIsOn().assertIsNotEnabled().performClick()
        assertEquals(0, changes)
        rule.onNodeWithText("1 of 3 selected").assert(liveRegion)
    }

    @Test
    fun selectAllAddsTheChangeableAppsAndClearAllKeepsTheFixedOnes() {
        var value by mutableStateOf(listOf("id"))
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = Apps, value = value, onValueChange = { value = it })
        }
        rule.onNodeWithText("Select all").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("id", "crm", "books"), value)
        rule.onNodeWithText("Clear all").performClick()
        assertEquals(listOf("id"), value)
    }

    @Test
    fun aDisabledPickerChangesNothing() {
        var changes = 0
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = Apps, value = emptyList(), onValueChange = { changes++ }, enabled = false)
        }
        rule.onNode(checkbox and hasText("CRM")).assertIsNotEnabled().performClick()
        rule.onNodeWithText("Select all").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun loadingIsReadOnceAndEmptyHasText() {
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {}, loading = true)
        }
        rule.onNodeWithContentDescription("Loading apps").assert(liveRegion)
        rule.onNodeWithText("Select all").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAppAccessPicker(
                apps = listOf(Crm),
                value = emptyList(),
                onValueChange = {},
                labels = MonarchAppAccessPickerLabels(
                    label = "Apps",
                    selectAll = "Alle",
                    count = { s, t -> "$s/$t" },
                ),
            )
        }
        rule.onNodeWithText("Apps").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Alle").assertExists()
        rule.onNodeWithText("0/1").assertExists()
    }

    @Test
    fun theEmptyTextIsBuiltIn() {
        rule.setMonarchContent {
            MonarchAppAccessPicker(apps = emptyList(), value = emptyList(), onValueChange = {})
        }
        rule.onNodeWithText("No apps are available yet.").assertExists()
        rule.onNodeWithText("App access").assertExists()
    }

    @Test
    fun theCheckboxComesFirstInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAppAccessPicker(apps = listOf(Crm), value = emptyList(), onValueChange = {})
        }
        rule.onNodeWithText("0 of 1 selected").assertIsBefore(rule.onNodeWithText("Select all"), LayoutDirection.Rtl)
    }

    @Test
    fun longNamesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAppAccessPicker(
                apps = listOf(MonarchAppAccessOption("a", "Customer relationship management for every team")),
                value = emptyList(),
                onValueChange = {},
                showBulkActions = false,
            )
        }
        rule.onNodeWithText("Customer relationship management for every team", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theSummaryShowsSortedNamesAndReadsTheHiddenOnes() {
        rule.setMonarchContent {
            MonarchAppAccessSummary(apps = Apps, value = listOf("id", "crm", "books", "zzz"))
        }
        rule.onNodeWithContentDescription("Books, CRM, and 2 more: Rook ID, zzz").assertExists()
        rule.onNodeWithText("+2", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theSummaryHasANoneTextAndOverrides() {
        rule.setMonarchContent {
            MonarchAppAccessSummary(apps = Apps, value = emptyList(), noneLabel = "Nothing")
        }
        rule.onNodeWithText("Nothing").assertExists()
    }

    @Test
    fun theSummaryMoreTextCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAppAccessSummary(
                apps = Apps,
                value = listOf("crm", "books"),
                max = 1,
                moreLabel = { n, names -> " +$n ($names)" },
            )
        }
        rule.onNodeWithContentDescription("Books +1 (CRM)").assertExists()
    }

    @Test
    fun theRulesKeepFixedIdsAndOrder() {
        val rules = MonarchAppAccessPickerRules
        assertEquals(listOf("b", "a"), rules.toggle(listOf("a", "b"), "a", true))
        assertEquals(listOf("b"), rules.toggle(listOf("a", "b"), "a", false))
        assertEquals(listOf("x"), rules.toggleAll(listOf(Id), listOf("x")))
        assertEquals(false, rules.allSelected(listOf(Id), listOf("id")))
        assertEquals(listOf("Books", "CRM"), rules.names(Apps, listOf("crm", "books", "crm"), Locale.US))
        assertEquals(listOf("a") to listOf("b"), rules.split(listOf("a", "b"), 0))
    }

    @Test
    fun theRowColoursAreRolesInEveryTheme() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            val on = MonarchAppAccessPickerTokens.rowColors(true, colors)
            val off = MonarchAppAccessPickerTokens.rowColors(false, colors)
            assertEquals(colors.textDefault, on.name)
            assertEquals(colors.textMuted, on.description)
            assertEquals(colors.textDisabled, off.name)
            assertNotEquals(on.name, off.name)
            assertEquals(colors.textMuted, MonarchAppAccessPickerTokens.noteColor(colors))
        }
    }
}

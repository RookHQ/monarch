@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.geo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.geo.addressautocomplete.AddressAutocompleteContent
import dev.monarchui.components.geo.addressautocomplete.AddressAutocompleteSearch
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocomplete
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteAddress
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteField
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteLabels
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteMode
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteSuggestion
import dev.monarchui.components.geo.addressautocomplete.MonarchAddressAutocompleteTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val lisbon = MonarchAddressAutocompleteAddress(
    line1 = "10 Rua Augusta",
    city = "Lisbon",
    postalCode = "1100-053",
    countryCode = "PT",
)

private val places = listOf(
    MonarchAddressAutocompleteSuggestion("lisbon", "10 Rua Augusta", "Lisbon, Portugal", lisbon),
    MonarchAddressAutocompleteSuggestion("porto", "5 Rua das Flores", "Porto, Portugal"),
)

internal object MonarchAddressAutocompleteFixtures : ComponentFixtures {
    override val id = "address-autocomplete"
    override val fixtures = listOf(
        fixture("search, empty with helper and icon") {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = { places },
                helperText = "Where we send the order.",
                searchIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("search, filled") {
            MonarchAddressAutocomplete(
                address = lisbon,
                onAddressChange = {},
                search = { places },
                mode = MonarchAddressAutocompleteMode.Search,
                locale = Locale.US,
            )
        },
        fixture("error, no manual entry") {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = { places },
                allowManualEntry = false,
                errorText = "Enter an address.",
            )
        },
        fixture("manual form with a field error") {
            // The form is taller than the test window: it is laid out at full height and the window clips it.
            MonarchAddressAutocomplete(
                address = lisbon,
                onAddressChange = {},
                search = { places },
                modifier = Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true),
                locale = Locale.US,
                fieldErrors = mapOf(MonarchAddressAutocompleteField.PostalCode to "Check the postal code."),
            )
        },
        fixture("disabled") {
            MonarchAddressAutocomplete(
                address = lisbon,
                onAddressChange = {},
                search = { places },
                mode = MonarchAddressAutocompleteMode.Search,
                enabled = false,
                locale = Locale.US,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAddressAutocompleteTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAddressAutocompleteTokens

    /** The search field: the exposed dropdown anchor named "Address". */
    private val field =
        hasContentDescription("Address") and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)

    private fun openSheet() {
        rule.onNode(field).performClick()
        rule.waitForIdle()
    }

    /** The scrolling list of the sheet. */
    private val isSheetList = SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollToIndex)

    private fun type(text: String) {
        rule.onNode(hasSetTextAction()).performTextInput(text)
        rule.waitForIdle()
    }

    private fun announcement(text: String) = SemanticsMatcher.expectValue(
        SemanticsProperties.LiveRegion,
        LiveRegionMode.Polite,
    ) and hasContentDescription(text)

    @Test
    fun theModeStartsOnTheFormOnlyWithAStreetLine() {
        assertEquals(MonarchAddressAutocompleteMode.Search, tokens.initialMode(MonarchAddressAutocompleteAddress()))
        assertEquals(MonarchAddressAutocompleteMode.Manual, tokens.initialMode(lisbon))
        val cityOnly = MonarchAddressAutocompleteAddress(city = "Porto")
        assertEquals(MonarchAddressAutocompleteMode.Search, tokens.initialMode(cityOnly))
    }

    @Test
    fun theSummaryIsOneLineWithTheCountryName() {
        assertEquals("10 Rua Augusta, Lisbon, 1100-053, Portugal", tokens.summary(lisbon, Locale.US))
        assertEquals("10 Rua Augusta, Lisbon, 1100-053, Portugal", tokens.summary(lisbon.copy(region = " "), Locale.US))
        assertNull(tokens.summary(MonarchAddressAutocompleteAddress(), Locale.US))
        assertEquals(true, MonarchAddressAutocompleteAddress(countryCode = " ").isEmpty)
        assertEquals(false, lisbon.isEmpty)
        assertEquals("10 Rua Augusta", tokens.manualLine1("10 Rua Augusta", "other"))
        assertEquals("rua", tokens.manualLine1(" ", " rua "))
    }

    @Test
    fun theListStateFollowsTheTextAndTheSearch() {
        val found = AddressAutocompleteSearch(query = "rua", results = places)
        assertEquals(AddressAutocompleteContent.Idle, tokens.content(" ", 3, found))
        assertEquals(AddressAutocompleteContent.TooShort, tokens.content("ru", 3, found))
        assertEquals(AddressAutocompleteContent.Searching, tokens.content("rua d", 3, found))
        assertEquals(AddressAutocompleteContent.Searching, tokens.content("rua", 3, found.copy(running = true)))
        assertEquals(AddressAutocompleteContent.Failed, tokens.content("rua", 3, found.copy(failed = true)))
        assertEquals(AddressAutocompleteContent.Empty, tokens.content("rua", 3, found.copy(results = emptyList())))
        assertEquals(AddressAutocompleteContent.Suggestions, tokens.content(" rua ", 3, found))
    }

    @Test
    fun pickingASuggestionFillsTheFormAndAnnouncesIt() {
        var current = MonarchAddressAutocompleteAddress()
        var picked: String? = null
        rule.setMonarchContent {
            var address by remember { mutableStateOf(MonarchAddressAutocompleteAddress()) }
            MonarchAddressAutocomplete(
                address = address,
                onAddressChange = {
                    address = it
                    current = it
                },
                search = { query -> places.filter { it.label.contains(query, ignoreCase = true) } },
                locale = Locale.US,
                debounceMillis = 0,
                onSuggestionSelect = { suggestion, _ -> picked = suggestion.id },
            )
        }
        rule.onNode(field).assertTouchTargetAtLeast48dp(rule.density)
        openSheet()
        rule.onNode(hasText("Address") and isHeading()).assertExists()
        type("augusta")
        rule.onNodeWithText("1 address found").assertExists()
        rule.onNode(hasText("10 Rua Augusta") and hasClickAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(hasText("Lisbon, Portugal"))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(lisbon, current)
        assertEquals("lisbon", picked)
        rule.onNode(hasText("Address") and isHeading()).assertDoesNotExist()
        rule.onNodeWithText("10 Rua Augusta, Lisbon, 1100-053, Portugal").assertExists()
        rule.onNode(hasContentDescription("Street address")).assert(hasText("10 Rua Augusta"))
        rule.onNode(announcement("Address filled in. Check the details.")).assertExists()
    }

    @Test
    fun searchWaitsForTheDebounce() {
        var calls = 0
        rule.setMonarchContent {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = {
                    calls++
                    places
                },
                debounceMillis = 500,
            )
        }
        openSheet()
        type("rua")
        rule.onNodeWithText("Searching…").assertExists()
        assertEquals(0, calls)
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        rule.onNodeWithText("2 addresses found").assertExists()
        assertEquals(1, calls)
    }

    @Test
    fun shortTextNoResultsAndFailureEachSaySo() {
        var fail = true
        var calls = 0
        rule.setMonarchContent {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = { query ->
                    calls++
                    if (fail) error("offline") else places.filter { it.label.contains(query) }
                },
                debounceMillis = 0,
            )
        }
        openSheet()
        type("ru")
        rule.onNodeWithText("Type at least 3 characters to search.").assertExists()
        assertEquals(0, calls)
        type("a")
        rule.onNodeWithText("Couldn’t search addresses.", substring = true).assertExists()
        fail = false
        rule.onNode(hasText("Try again", substring = true) and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(2, calls)
        rule.onNodeWithText("No addresses found").assertExists()
    }

    @Test
    fun aSuggestionWithoutAnAddressIsResolvedOrFailsWithAnError() {
        var current = MonarchAddressAutocompleteAddress()
        var fail = false
        rule.setMonarchContent {
            MonarchAddressAutocomplete(
                address = current,
                onAddressChange = { current = it },
                search = { places },
                debounceMillis = 0,
                resolve = {
                    if (fail) error("gone") else MonarchAddressAutocompleteAddress(line1 = it.label, city = "Porto")
                },
            )
        }
        openSheet()
        type("flores")
        rule.onNode(hasText("5 Rua das Flores") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals("Porto", current.city)
        fail = true
        current = MonarchAddressAutocompleteAddress()
        rule.onNode(hasText("Search for an address") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        openSheet()
        type("flores")
        rule.onNode(hasText("5 Rua das Flores") and hasClickAction()).performClick()
        rule.waitForIdle()
        val message = "Couldn’t get that address. Enter it manually or try another."
        rule.onNode(field)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, message))
        rule.onNode(announcement(message)).assertExists()
    }

    @Test
    fun manualEntryKeepsTheTypedTextAsTheStreetLine() {
        var current = MonarchAddressAutocompleteAddress()
        var mode: MonarchAddressAutocompleteMode? = null
        rule.setMonarchContent {
            var address by remember { mutableStateOf(MonarchAddressAutocompleteAddress()) }
            MonarchAddressAutocomplete(
                address = address,
                onAddressChange = {
                    address = it
                    current = it
                },
                search = { emptyList() },
                onModeChange = { mode = it },
                debounceMillis = 0,
            )
        }
        openSheet()
        type("1 Long Lane")
        rule.onNodeWithText("No addresses found").assertExists()
        rule.onNode(hasText("Enter the address manually") and hasClickAction() and hasAnyAncestor(isSheetList))
            .performClick()
        rule.waitForIdle()
        assertEquals("1 Long Lane", current.line1)
        assertEquals(MonarchAddressAutocompleteMode.Manual, mode)
        rule.onNode(hasContentDescription("City") and hasSetTextAction())
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.AddressLocality))
            .performTextInput("Leeds")
        assertEquals("Leeds", current.city)
        rule.onNodeWithText("Optional").assertExists()
        rule.onNode(hasText("Search for an address") and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        rule.waitForIdle()
        assertEquals(MonarchAddressAutocompleteMode.Search, mode)
        rule.onNode(hasContentDescription("City") and hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = { places },
                debounceMillis = 0,
                labels = MonarchAddressAutocompleteLabels(
                    label = "Adresse",
                    enterManually = "Saisir l’adresse",
                    results = { "$it résultats" },
                ),
            )
        }
        rule.onNode(hasText("Saisir l’adresse") and hasClickAction()).assertExists()
        val dropdown = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.DropdownList)
        rule.onNode(hasContentDescription("Adresse") and dropdown).performClick()
        rule.waitForIdle()
        type("rua")
        rule.onNodeWithText("2 résultats").assertExists()
    }

    @Test
    fun disabledDoesNotOpen() {
        rule.setMonarchContent {
            MonarchAddressAutocomplete(
                address = MonarchAddressAutocompleteAddress(),
                onAddressChange = {},
                search = { places },
                enabled = false,
            )
        }
        rule.onNode(field).assertIsNotEnabled().performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Address") and isHeading()).assertDoesNotExist()
        rule.onNode(hasText("Enter the address manually") and hasClickAction()).assertIsNotEnabled()
    }

    @Test
    fun aLongAddressAndTheErrorFitAtTheLargestFontSize() {
        val long = lisbon.copy(line2 = "Building 4, floor 3, door on the left past the courtyard")
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            Column {
                for (message in listOf(null, "We cannot deliver here.")) {
                    MonarchAddressAutocomplete(
                        address = long,
                        onAddressChange = {},
                        search = { places },
                        mode = MonarchAddressAutocompleteMode.Search,
                        errorText = message,
                        locale = Locale.US,
                        modifier = Modifier.testTag(if (message == null) "plain" else "invalid"),
                    )
                }
            }
        }
        rule.onNode(field and hasAnyAncestor(hasTestTag("invalid")))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "We cannot deliver here."))
        rule.onAllNodesWithText("Enter the address manually", useUnmergedTree = true).onFirst().assertTextFullyVisible()
        // The error line is hidden from the semantics tree (the field node carries it), so its room is measured:
        // the invalid field is taller than the plain one by at least one 12sp line at font scale 2.0. A line that
        // was clipped to nothing, or drawn over the helper text, would not add that height.
        val plain = rule.onNodeWithTag("plain").fetchSemanticsNode().size.height
        val invalid = rule.onNodeWithTag("invalid").fetchSemanticsNode().size.height
        val oneLine = with(rule.density) { (12 * LargestFontScale).sp.toPx() }
        assertTrue(invalid - plain >= oneLine, "the error line needs ${oneLine}px, the field grew ${invalid - plain}px")
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, tokens.labelColor(c), theme.id)
            assertEquals(c.textMuted, tokens.mutedColor(c), theme.id)
        }
    }
}

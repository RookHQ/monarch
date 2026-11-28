package dev.monarchui.components.scheduling

import androidx.compose.material3.Icon
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEditable
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import dev.monarchui.components.forms.combobox.MonarchComboboxLabels
import dev.monarchui.components.forms.combobox.MonarchComboboxOption
import dev.monarchui.components.scheduling.timezoneselect.MonarchTimezoneSelect
import dev.monarchui.components.scheduling.timezoneselect.MonarchTimezoneSelectLabels
import dev.monarchui.components.scheduling.timezoneselect.TimezoneSelectRequest
import dev.monarchui.components.scheduling.timezoneselect.TimezoneSelectSections
import dev.monarchui.components.scheduling.timezoneselect.TimezoneSelectZones
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Wednesday 4 November 2026, 12:00 UTC: New York is on standard time. */
private const val November = 1_793_793_600_000L

/** Saturday 4 July 2026, 12:00 UTC: New York is on daylight time. */
private const val July = 1_783_166_400_000L

private val Office = listOf("Europe/London", "America/New_York")

internal object MonarchTimezoneSelectFixtures : ComponentFixtures {
    override val id = "timezone-select"
    override val fixtures = listOf(
        fixture("chosen with helper and clear icon") {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "Asia/Kolkata",
                onSelectedChange = {},
                helperText = "Meeting times are shown in this zone.",
                pinned = Office,
                now = November,
                clearIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("empty with placeholder and error") {
            MonarchTimezoneSelect(
                label = "Meeting time zone",
                selected = null,
                onSelectedChange = {},
                placeholder = "Choose a time zone",
                errorText = "Choose a time zone.",
                zones = listOf("UTC", "Europe/Paris", "Asia/Kolkata"),
                showDetected = false,
                now = November,
            )
        },
        fixture("disabled, old id") {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "Asia/Calcutta",
                onSelectedChange = {},
                enabled = false,
                now = November,
            )
        },
        fixture("long label") {
            MonarchTimezoneSelect(
                label = "The time zone every reminder and every report of this workspace uses",
                selected = "America/Argentina/Buenos_Aires",
                onSelectedChange = {},
                now = November,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchTimezoneSelectTest {
    @get:Rule
    val rule = createComposeRule()

    private val sections = TimezoneSelectSections("Detected", "Pinned", "Recent", "All time zones")
    private val selectable = SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)

    @Test
    fun oldIdsMapToCurrentNamesAndTheListHasUtcFirstWithoutAliases() {
        assertEquals("Asia/Kolkata", TimezoneSelectZones.canonical("Asia/Calcutta"))
        assertEquals("UTC", TimezoneSelectZones.canonical("Etc/UTC"))
        assertEquals("Europe/Paris", TimezoneSelectZones.canonical("Europe/Paris"))
        val all = TimezoneSelectZones.available()
        assertEquals("UTC", all.first())
        assertTrue("Asia/Kolkata" in all)
        assertFalse("Asia/Calcutta" in all)
        assertEquals(all.size, all.toSet().size)
    }

    @Test
    fun offsetsFollowDaylightSavingAndLabelsReadAsOnTheWeb() {
        val winter = TimezoneSelectZones.info("America/New_York", November, Locale.US)
        assertEquals(-5 * 3_600, winter.offsetSeconds)
        assertEquals("UTC-05:00", winter.offsetLabel)
        assertEquals("New York", winter.city)
        assertEquals("America", winter.region)
        assertEquals("Eastern Standard Time", winter.longName)
        val summer = TimezoneSelectZones.info("America/New_York", July, Locale.US)
        assertEquals("UTC-04:00", summer.offsetLabel)
        assertEquals("Eastern Daylight Time", summer.longName)
        val kolkata = TimezoneSelectZones.info("Asia/Kolkata", November, Locale.US)
        assertEquals("(UTC+05:30) Kolkata", TimezoneSelectZones.defaultLabel(kolkata))
        val utc = TimezoneSelectZones.info("UTC", November, Locale.US)
        assertEquals("UTC", utc.city)
        assertEquals("", utc.region)
        assertEquals("UTC+00:00", utc.offsetLabel)
        assertEquals(0, TimezoneSelectZones.info("Nowhere/Atlantis", November, Locale.US).offsetSeconds)
        assertEquals("", TimezoneSelectZones.info("Nowhere/Atlantis", November, Locale.US).longName)
    }

    @Test
    fun zonesSortByOffsetThenId() {
        assertEquals(
            listOf("America/Los_Angeles", "America/New_York", "Europe/London", "UTC", "Asia/Kolkata"),
            TimezoneSelectZones.sorted(
                listOf("Asia/Kolkata", "UTC", "Europe/London", "America/New_York", "America/Los_Angeles"),
                November,
            ),
        )
    }

    @Test
    fun everyTypedWordMustMatchTheLabelIdLongNameOffsetOrAlias() {
        val info = TimezoneSelectZones.info("Asia/Kolkata", November, Locale.US)
        val option = TimezoneSelectZones.option(info, TimezoneSelectZones.defaultLabel(info), null)
        listOf("kolkata", "India", "+5:30", "utc+5:30", "+530", "asia kolkata", "india standard").forEach {
            assertTrue(TimezoneSelectZones.matches(option, it), it)
        }
        assertFalse(TimezoneSelectZones.matches(option, "india paris"))
        assertEquals("Asia/Kolkata · India Standard Time", option.description)
    }

    @Test
    fun theListHasSectionsEachZoneOnceAmongThemThenEveryZone() {
        val all = listOf("America/New_York", "Europe/London", "UTC")
        val build = { zone: String, group: String? -> MonarchComboboxOption(zone, zone, group = group) }
        val request =
            TimezoneSelectRequest(all, "Asia/Kolkata", "Europe/London", Office, listOf("Asia/Calcutta"), false)
        val options = TimezoneSelectZones.options(request, sections, build)
        assertEquals(
            listOf(
                "Detected" to "Europe/London",
                "Pinned" to "America/New_York",
                "Recent" to "Asia/Kolkata",
                "All time zones" to "Asia/Kolkata",
                "All time zones" to "America/New_York",
                "All time zones" to "Europe/London",
                "All time zones" to "UTC",
            ),
            options.map { it.group to it.id },
        )
        val searching = TimezoneSelectZones.options(
            TimezoneSelectRequest(all, null, "Europe/London", Office, emptyList(), true),
            sections,
            build,
        )
        assertEquals(all, searching.map { it.id })
        assertTrue(searching.all { it.group == null })
        val plainRequest = TimezoneSelectRequest(all, null, null, emptyList(), emptyList(), false)
        val plain = TimezoneSelectZones.options(plainRequest, sections, build)
        assertTrue(plain.all { it.group == null })
    }

    @Test
    fun anOldIdShowsItsCurrentLabelInTheField() {
        rule.setMonarchContent {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "Asia/Calcutta",
                onSelectedChange = {},
                now = November,
            )
        }
        rule.onNodeWithText("(UTC+05:30) Kolkata").assertExists().assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theSheetShowsSectionsAndChoosingReportsTheId() {
        var chosen: String? = "Asia/Kolkata"
        rule.setMonarchContent {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = chosen,
                onSelectedChange = { chosen = it },
                detectedZone = "Europe/London",
                pinned = listOf("America/New_York"),
                now = November,
            )
        }
        rule.onNodeWithText("(UTC+05:30) Kolkata").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Detected") and isHeading()).assertExists()
        rule.onNode(hasText("Pinned") and isHeading()).assertExists()
        rule.onNode(hasText("(UTC-05:00) New York") and selectable).performClick()
        rule.waitForIdle()
        assertEquals("America/New_York", chosen)
    }

    @Test
    fun searchingListsEachMatchOnceWithoutSections() {
        var chosen: String? = null
        rule.setMonarchContent {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = null,
                onSelectedChange = { chosen = it },
                detectedZone = "Asia/Kolkata",
                now = November,
            )
        }
        rule.onNode(hasContentDescription("Time zone") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasSetTextAction() and isEditable()).performTextInput("calcutta")
        rule.waitForIdle()
        rule.onNode(hasText("Detected") and isHeading()).assertDoesNotExist()
        rule.onNode(hasText("(UTC+05:30) Kolkata") and selectable).performClick()
        rule.waitForIdle()
        assertEquals("Asia/Kolkata", chosen)
    }

    @Test
    fun noMatchesSaysSoAndStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "UTC",
                onSelectedChange = {},
                zones = listOf("UTC", "Europe/Paris"),
                detectedZone = "Europe/Paris",
                now = November,
                labels = MonarchTimezoneSelectLabels(
                    detected = "Here",
                    empty = "Nothing found",
                    combobox = MonarchComboboxLabels(search = "Find a zone"),
                ),
            )
        }
        rule.onNodeWithText("(UTC+00:00) UTC").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Here") and isHeading()).assertExists()
        rule.onNode(hasSetTextAction() and isEditable()).performTextInput("tokyo")
        rule.waitForIdle()
        rule.onNodeWithText("Nothing found").assertExists()
    }

    @Test
    fun aDisabledSelectCannotOpenAndTextFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "America/Argentina/Buenos_Aires",
                onSelectedChange = {},
                enabled = false,
                errorText = "This zone is not supported for billing.",
                now = November,
            )
        }
        rule.onNodeWithText("(UTC-03:00) Buenos Aires").assertIsNotEnabled()
        rule.onNodeWithText("This zone is not supported for billing.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aCustomLabelBuildsEveryOption() {
        rule.setMonarchContent {
            MonarchTimezoneSelect(
                label = "Time zone",
                selected = "Europe/Paris",
                onSelectedChange = {},
                zones = listOf("Europe/Paris"),
                showDetected = false,
                now = November,
                formatLabel = { "${it.city} · ${it.longName}" },
            )
        }
        rule.onNodeWithText("Paris · Central European Standard Time").performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Paris · Central European Standard Time") and selectable).assertIsSelected()
    }
}

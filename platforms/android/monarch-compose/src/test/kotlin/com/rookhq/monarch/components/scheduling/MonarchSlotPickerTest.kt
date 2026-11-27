package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.scheduling.slotpicker.MonarchSlotPicker
import com.rookhq.monarch.components.scheduling.slotpicker.MonarchSlotPickerLabels
import com.rookhq.monarch.components.scheduling.slotpicker.MonarchSlotPickerSlot
import com.rookhq.monarch.components.scheduling.slotpicker.MonarchSlotPickerTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Nov2 = 1_793_577_600_000L
private const val Nov4 = 1_793_750_400_000L
private const val Nov5 = 1_793_836_800_000L
private const val Nine = 1_793_782_800_000L
private const val HalfHour = 1_800_000L

private val Slots = listOf(
    MonarchSlotPickerSlot(Nine),
    MonarchSlotPickerSlot(Nine + HalfHour, seatsLeft = 3),
    MonarchSlotPickerSlot(Nine + 2 * HalfHour, enabled = false),
)

/** The picker in a scrolling column, as on a booking screen: it is taller than the 470dp test window. */
@Composable
private fun Picker(
    slots: List<MonarchSlotPickerSlot> = Slots,
    selection: Long? = null,
    onSelectionChange: (Long?) -> Unit = {},
    day: Long? = Nov4,
    onDayChange: (Long) -> Unit = {},
    timeZone: String = "UTC",
    allowDeselect: Boolean = false,
    loading: Boolean = false,
    error: String? = null,
    onRetry: (() -> Unit)? = null,
    onChooseOutside: (() -> Unit)? = null,
    onTimeZoneChange: ((String) -> Unit)? = null,
    businessTimeZone: String? = null,
    availableDays: Set<Long>? = null,
    enabled: Boolean = true,
    labels: MonarchSlotPickerLabels = MonarchSlotPickerLabels(),
) {
    Column(Modifier.verticalScroll(rememberScrollState())) {
        MonarchSlotPicker(
            slots = slots,
            selection = selection,
            onSelectionChange = onSelectionChange,
            day = day,
            onDayChange = onDayChange,
            timeZone = timeZone,
            previousMonthIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            nextMonthIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            allowDeselect = allowDeselect,
            availableDays = availableDays,
            today = Nov2,
            is24Hour = false,
            onTimeZoneChange = onTimeZoneChange,
            businessTimeZone = businessTimeZone,
            loading = loading,
            error = error,
            onRetry = onRetry,
            onChooseOutside = onChooseOutside,
            enabled = enabled,
            labels = labels,
        )
    }
}

internal object MonarchSlotPickerFixtures : ComponentFixtures {
    override val id = "slot-picker"
    override val fixtures = listOf(
        fixture("times with a selection, seats and a taken time") { Picker(selection = Nine + HalfHour) },
        fixture("no day chosen") { Picker(day = null, slots = emptyList()) },
        fixture("loading") { Picker(slots = emptyList(), loading = true) },
        fixture("error with retry") { Picker(slots = emptyList(), error = "Times could not be loaded.", onRetry = {}) },
        fixture("empty with a way out") { Picker(slots = emptyList(), onChooseOutside = {}) },
        fixture("selection no longer open") { Picker(selection = Nine + 4 * HalfHour, onChooseOutside = {}) },
        fixture("zone picker and converted note") {
            Picker(onTimeZoneChange = {}, businessTimeZone = "America/New_York", availableDays = setOf(Nov4, Nov5))
        },
        fixture("disabled") { Picker(enabled = false) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchSlotPickerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theDayIsAHeadingAndTimesAreAGroupInTheZone() {
        rule.setMonarchContent { Picker() }
        rule.onNode(hasText("Wednesday, November 4") and isHeading()).assertExists()
        rule.onNodeWithContentDescription("Available times on Wednesday, November 4").assertExists()
        rule.onNodeWithText("9:00\u202FAM").assertExists()
        rule.onNodeWithText("3 left").assertExists()
        rule.onNodeWithText("Times shown in Coordinated Universal Time (UTC)").assertExists()
    }

    @Test
    fun timesAreShownInTheGivenZone() {
        rule.setMonarchContent { Picker(timeZone = "America/New_York", businessTimeZone = "UTC") }
        rule.onNodeWithText("4:00\u202FAM").assertExists()
        rule.onNodeWithText("Times are converted from Coordinated Universal Time (UTC).").assertExists()
    }

    @Test
    fun choosingATimeCallsBackWithItsStart() {
        var selection by mutableStateOf<Long?>(null)
        rule.setMonarchContent { Picker(selection = selection, onSelectionChange = { selection = it }) }
        rule.onNodeWithText("9:30\u202FAM").performScrollTo().assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Nine + HalfHour, selection)
        rule.onNodeWithText("9:30\u202FAM").assertIsSelected()
        rule.onNodeWithText("9:00\u202FAM").assertIsNotSelected()
        rule.onNodeWithText("9:30\u202FAM").performClick()
        assertEquals(Nine + HalfHour, selection)
    }

    @Test
    fun allowDeselectClearsOnASecondPress() {
        var selection by mutableStateOf<Long?>(Nine)
        rule.setMonarchContent {
            Picker(selection = selection, onSelectionChange = { selection = it }, allowDeselect = true)
        }
        rule.onNodeWithText("9:00\u202FAM").performScrollTo().performClick()
        assertEquals(null, selection)
    }

    @Test
    fun takenTimesAndADisabledPickerCannotBeChosen() {
        var calls = 0
        rule.setMonarchContent { Picker(onSelectionChange = { calls++ }) }
        rule.onNodeWithText("10:00\u202FAM").performScrollTo().assertIsNotEnabled().performClick()
        assertEquals(0, calls)
    }

    @Test
    fun choosingADayCallsBack() {
        var day by mutableStateOf<Long?>(null)
        rule.setMonarchContent { Picker(day = day, slots = emptyList(), onDayChange = { day = it }) }
        rule.onNodeWithText("Choose a day to see available times.").assertExists()
        rule.onNodeWithContentDescription("Thursday, November 5, 2026", substring = true).performClick()
        assertEquals(Nov5, day)
    }

    @Test
    fun daysOutsideTheAvailableOnesAreDisabled() {
        rule.setMonarchContent { Picker(availableDays = setOf(Nov4)) }
        rule.onNodeWithContentDescription("Thursday, November 5, 2026", substring = true).assertIsNotEnabled()
    }

    @Test
    fun theStatusAnnouncesLoadingAndTheCount() {
        var loading by mutableStateOf(true)
        rule.setMonarchContent { Picker(loading = loading) }
        rule.onNodeWithContentDescription("Loading available times")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("9:00\u202FAM").assertDoesNotExist()
        loading = false
        rule.onNodeWithContentDescription("2 times on Wednesday, November 4").assertExists()
    }

    @Test
    fun emptyErrorAndOffListOfferTheirActions() {
        var retried = 0
        var outside = 0
        var error by mutableStateOf<String?>("Times could not be loaded.")
        var slots by mutableStateOf(emptyList<MonarchSlotPickerSlot>())
        rule.setMonarchContent {
            Picker(
                slots = slots,
                selection = Nine + 4 * HalfHour,
                error = error,
                onRetry = { retried++ },
                onChooseOutside = { outside++ },
            )
        }
        rule.onNodeWithText("Times could not be loaded.").assertExists()
        rule.onNodeWithText("Try again").performScrollTo().performClick()
        assertEquals(1, retried)
        error = null
        rule.onNodeWithText("No times are available on this day. Try another day.").assertExists()
        rule.onNodeWithContentDescription("No times available on Wednesday, November 4").assertExists()
        slots = Slots
        rule.onNodeWithText("11:00\u202FAM is not an open time on this day.").performScrollTo().assertExists()
        rule.onNodeWithText("Choose another time").performScrollTo().performClick()
        assertEquals(1, outside)
    }

    @Test
    fun theZonePickerIsShownWhenTheZoneCanChange() {
        rule.setMonarchContent { Picker(onTimeZoneChange = {}) }
        rule.onNodeWithContentDescription("Time zone", substring = true).assertExists()
        rule.onNodeWithText("Times shown in", substring = true).assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            Picker(
                labels = MonarchSlotPickerLabels(
                    times = { "Freie Zeiten am $it" },
                    seatsLeft = { "Noch $it" },
                    shownIn = { "Zeiten in $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Freie Zeiten am Wednesday, November 4").assertExists()
        rule.onNodeWithText("Noch 3").assertExists()
        rule.onNodeWithText("Zeiten in Coordinated Universal Time (UTC)").assertExists()
    }

    @Test
    fun timesFollowTheLayoutDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) { Picker() }
        rule.onNodeWithText("9:00\u202FAM").assertIsBefore(rule.onNodeWithText("9:30\u202FAM"), LayoutDirection.Rtl)
    }

    @Test
    fun timesWrapAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { Picker() }
        rule.onNodeWithText("9:30\u202FAM").performScrollTo().assertTextFullyVisible()
        rule.onNodeWithText("Wednesday, November 4").performScrollTo().assertTextFullyVisible()
    }

    @Test
    fun theLogicAndColoursHoldInEveryTheme() {
        val tokens = MonarchSlotPickerTokens
        assertEquals(Nov4, tokens.dayOf(Nine, 0))
        assertEquals(Nov4, tokens.dayOf(Nov5 + 1_000L, -5 * 3_600_000))
        assertTrue(tokens.dayEnabled(Nov4, setOf(Nov4), null))
        assertFalse(tokens.dayEnabled(Nov5, setOf(Nov4), null))
        assertFalse(tokens.dayEnabled(Nov4, null) { it == Nov4 })
        assertEquals(2, tokens.openCount(Slots))
        assertEquals(null, tokens.next(Nine, Slots[0], allowDeselect = true))
        assertEquals(Nine, tokens.next(Nine, Slots[0], allowDeselect = false))
        assertTrue(tokens.offList(Nine + 4 * HalfHour, Nov4, Nov4, Slots))
        assertFalse(tokens.offList(Nine, Nov4, Nov4, Slots))
        assertFalse(tokens.offList(Nine + 4 * HalfHour, Nov5, Nov4, Slots))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, tokens.heading(colors), theme.id)
            assertEquals(colors.textMuted, tokens.muted(colors), theme.id)
        }
    }
}

package dev.monarchui.components.scheduling

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbar
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarLabels
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarTokens
import dev.monarchui.components.scheduling.calendartoolbar.MonarchCalendarToolbarView
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchCalendarToolbarFixtures : ComponentFixtures {
    override val id = "calendar-toolbar"
    override val fixtures = listOf(
        fixture("every part") {
            MonarchCalendarToolbar(
                title = "November 2026",
                view = MonarchCalendarToolbarView.Month,
                onToday = {},
                onPrevious = {},
                onNext = {},
                previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                onViewChange = {},
                actions = { MonarchButton(text = "New", onClick = {}, size = MonarchButtonSize.Sm) },
            )
        },
        fixture("busy, disabled controls, long title") {
            MonarchCalendarToolbar(
                title = "Monday 30 November – Sunday 6 December 2026",
                view = MonarchCalendarToolbarView.Week,
                onToday = {},
                onPrevious = {},
                onNext = {},
                previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                busy = true,
                todayEnabled = false,
                previousEnabled = false,
                nextEnabled = false,
            )
        },
        fixture("one view, no switcher") {
            MonarchCalendarToolbar(
                title = "Agenda",
                view = MonarchCalendarToolbarView.Agenda,
                onToday = {},
                onPrevious = {},
                onNext = {},
                previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                views = listOf(MonarchCalendarToolbarView.Agenda),
                onViewChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCalendarToolbarTest {
    @get:Rule
    val rule = createComposeRule()

    private val isHeading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
    private val isPolite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private var today = 0
    private var previous = 0
    private var next = 0
    private var chosen: MonarchCalendarToolbarView? = null

    private fun show(
        view: MonarchCalendarToolbarView = MonarchCalendarToolbarView.Week,
        enabled: Boolean = true,
        busy: Boolean = false,
        labels: MonarchCalendarToolbarLabels = MonarchCalendarToolbarLabels(),
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        fontScale: Float = 1f,
    ) {
        rule.setMonarchContent(layoutDirection = layoutDirection, fontScale = fontScale) {
            MonarchCalendarToolbar(
                title = "2 – 8 November 2026",
                view = view,
                onToday = { today++ },
                onPrevious = { previous++ },
                onNext = { next++ },
                previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                onViewChange = { chosen = it },
                busy = busy,
                todayEnabled = enabled,
                previousEnabled = enabled,
                nextEnabled = enabled,
                labels = labels,
            )
        }
    }

    @Test
    fun theArrowsAreNamedForTheUnitOfTheViewAndCallBack() {
        show()
        rule.onNodeWithContentDescription("Previous week")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithContentDescription("Next week")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.onNodeWithText("Today").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf(1, 1, 1), listOf(previous, next, today))
    }

    @Test
    fun disabledControlsDoNotCallBack() {
        show(enabled = false)
        rule.onNodeWithContentDescription("Previous week").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Next week").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Today").assertIsNotEnabled().performClick()
        assertEquals(listOf(0, 0, 0), listOf(previous, next, today))
    }

    @Test
    fun theTitleIsAPoliteHeading() {
        show()
        rule.onNodeWithText("2 – 8 November 2026").assert(isHeading).assert(isPolite)
    }

    @Test
    fun theSwitcherIsNamedShowsTheViewAndChangesIt() {
        show(view = MonarchCalendarToolbarView.Month)
        val switcher = rule.onNodeWithContentDescription("Calendar view")
        assertEquals("Month", switcher.stateDescription())
        rule.onNodeWithText("Agenda").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(MonarchCalendarToolbarView.Agenda, chosen)
    }

    @Test
    fun theCurrentViewIsSelected() {
        show(view = MonarchCalendarToolbarView.Day)
        rule.onNodeWithText("Day").assertIsSelected()
        rule.onNodeWithText("Week").assertIsEnabled()
    }

    @Test
    fun withOneViewTheSwitcherIsHidden() {
        assertFalse(MonarchCalendarToolbarTokens.showsSwitcher(listOf(MonarchCalendarToolbarView.Day), true))
        assertFalse(MonarchCalendarToolbarTokens.showsSwitcher(MonarchCalendarToolbarView.entries, false))
        assertTrue(MonarchCalendarToolbarTokens.showsSwitcher(MonarchCalendarToolbarView.entries, true))
    }

    @Test
    fun theBusyIndicatorIsNamedLoading() {
        rule.mainClock.autoAdvance = false
        show(busy = true)
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading").assertExists()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        show(
            labels = MonarchCalendarToolbarLabels(
                today = "Hoy",
                previous = { "Anterior $it" },
                next = { "Siguiente $it" },
                view = "Vista",
                viewName = { if (it == MonarchCalendarToolbarView.Week) "Semana" else it.name },
                unit = { "semana" },
            ),
        )
        rule.onNodeWithText("Hoy").assertExists()
        rule.onNodeWithContentDescription("Anterior semana").assertExists()
        rule.onNodeWithContentDescription("Siguiente semana").assertExists()
        assertEquals("Semana", rule.onNodeWithContentDescription("Vista").stateDescription())
    }

    @Test
    fun todayComesBeforeTheArrowsRightToLeft() {
        show(layoutDirection = LayoutDirection.Rtl)
        rule.onNodeWithText("Today")
            .assertIsBefore(rule.onNodeWithContentDescription("Previous week"), LayoutDirection.Rtl)
        rule.onNodeWithContentDescription("Previous week")
            .assertIsBefore(rule.onNodeWithContentDescription("Next week"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsInsteadOfBeingCutAtTheLargestFontScale() {
        show(fontScale = LargestFontScale)
        rule.onNodeWithText("2 – 8 November 2026").assertTextFullyVisible()
    }

    @Test
    fun theTitleColourComesFromTheTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textDefault, MonarchCalendarToolbarTokens.titleColor(colors))
        }
    }

    @Test
    fun aNarrowToolbarStillShowsEveryPart() {
        rule.setMonarchContent {
            Column(Modifier.width(200.dp)) {
                MonarchCalendarToolbar(
                    title = "November 2026",
                    view = MonarchCalendarToolbarView.Month,
                    onToday = {},
                    onPrevious = {},
                    onNext = {},
                    previousIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                    nextIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                    onViewChange = {},
                )
            }
        }
        rule.onNodeWithText("November 2026").assertTextFullyVisible()
        rule.onNodeWithContentDescription("Calendar view").assertTouchTargetAtLeast48dp(rule.density)
    }
}

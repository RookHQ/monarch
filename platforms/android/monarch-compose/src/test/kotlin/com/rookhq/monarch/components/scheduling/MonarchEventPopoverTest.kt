package com.rookhq.monarch.components.scheduling

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.style.TextDecoration
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.overlays.popover.MonarchPopover
import com.rookhq.monarch.components.scheduling.eventchip.MonarchEventChipStatus
import com.rookhq.monarch.components.scheduling.eventpopover.MonarchEventPopover
import com.rookhq.monarch.components.scheduling.eventpopover.MonarchEventPopoverDetail
import com.rookhq.monarch.components.scheduling.eventpopover.MonarchEventPopoverLabels
import com.rookhq.monarch.components.scheduling.eventpopover.MonarchEventPopoverTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
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
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Sat 14 Nov 2026, 14:30 UTC: 9:30 AM in New York. */
private const val EventStart = 1_794_666_600_000L
private const val FortyFiveMinutes = 2_700_000L
private const val OneDay = 86_400_000L

/** Sat 14 Nov 2026 as a UTC midnight. */
private const val EventDay = 1_794_614_400_000L
private val NewYork: TimeZone = TimeZone.getTimeZone("America/New_York")

/** ICU writes a narrow no-break space before AM and PM; compare with plain spaces. */
private fun String.plain(): String = replace(' ', ' ').replace(' ', ' ')

private fun range(from: String, to: String) = "$from – $to"

internal object MonarchEventPopoverFixtures : ComponentFixtures {
    override val id = "event-popover"
    override val fixtures = listOf(
        fixture("timed with details") {
            MonarchEventPopover(
                title = "Design review",
                subtitle = "Product calendar",
                start = EventStart,
                end = EventStart + FortyFiveMinutes,
                details = listOf(MonarchEventPopoverDetail("Location", "Room 4")),
                timeZone = NewYork.id,
            )
        },
        fixture("every status") {
            Column {
                MonarchEventPopover(title = "Sync", status = MonarchEventChipStatus.Tentative)
                MonarchEventPopover(title = "Retro", status = MonarchEventChipStatus.Declined)
            }
        },
        fixture("all day, cancelled, note") {
            MonarchEventPopover(
                title = "Offsite",
                start = EventDay,
                end = EventDay + OneDay,
                allDay = true,
                status = MonarchEventChipStatus.Cancelled,
                note = "Cancelled because of the rail strike.",
            )
        },
        fixture("actions and a custom color") {
            MonarchEventPopover(
                title = "Call with Acme",
                color = Color.Transparent,
                status = MonarchEventChipStatus.Confirmed,
            ) {
                MonarchButton(text = "Join", onClick = {})
                MonarchButton(text = "Edit", onClick = {})
            }
        },
        fixture("long title, no time") {
            MonarchEventPopover(
                title = "Quarterly planning with the regional sales leads and the finance partners",
                subtitle = "Shared calendar of the commercial team",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchEventPopoverTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheTimeRowIsOneElement() {
        rule.setMonarchContent {
            MonarchEventPopover(
                title = "Design review",
                start = EventStart,
                end = EventStart + FortyFiveMinutes,
                timeZone = NewYork.id,
            )
        }
        rule.onNodeWithText("Design review").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val time = rule.onNodeWithContentDescription("Time", substring = true)
            .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single().plain()
        assertEquals("Time, Sat, Nov 14, 2026, 9:30 AM – 10:15 AM EST", time)
    }

    @Test
    fun theTimesAreFormattedInTheGivenZone() {
        val tokens = MonarchEventPopoverTokens
        val end = EventStart + FortyFiveMinutes
        val sameDay = tokens.formatWhen(EventStart, end, false, NewYork, Locale.US, "All day", ::range)
        assertEquals("Sat, Nov 14, 2026", sameDay.date)
        assertEquals("9:30 AM – 10:15 AM EST", sameDay.time.plain())
        val spans = tokens.formatWhen(EventStart, EventStart + OneDay, false, NewYork, Locale.US, "All day", ::range)
        assertEquals("Sat, Nov 14, 2026 9:30 AM –", spans.date.plain())
        assertEquals("Sun, Nov 15, 2026 9:30 AM EST", spans.time.plain())
        val noEnd = tokens.formatWhen(EventStart, EventStart - 1, false, NewYork, Locale.US, "All day", ::range)
        assertEquals("9:30 AM EST", noEnd.time.plain())
        val allDay = tokens.formatWhen(EventDay, EventDay + OneDay, true, NewYork, Locale.US, "All day", ::range)
        assertEquals("Sat, Nov 14, 2026 – Sun, Nov 15, 2026", allDay.date)
        assertEquals("All day", allDay.time)
        val oneDay = tokens.formatWhen(EventDay, null, true, NewYork, Locale.US, "All day", ::range)
        assertEquals("Sat, Nov 14, 2026", oneDay.date)
        assertNull(tokens.validEnd(EventStart, EventStart - 1))
    }

    @Test
    fun theStatusIsWrittenAndInactiveTitlesAreStruckThrough() {
        rule.setMonarchContent { MonarchEventPopover(title = "Retro", status = MonarchEventChipStatus.Declined) }
        rule.onNodeWithText("Declined", useUnmergedTree = true).assertExists()
        assertEquals(
            TextDecoration.LineThrough,
            MonarchEventPopoverTokens.titleDecoration(MonarchEventChipStatus.Cancelled),
        )
        assertNull(MonarchEventPopoverTokens.titleDecoration(MonarchEventChipStatus.Tentative))
        assertEquals(MonarchTone.Success, MonarchEventPopoverTokens.tone(MonarchEventChipStatus.Confirmed))
        assertEquals(MonarchTone.Warning, MonarchEventPopoverTokens.tone(MonarchEventChipStatus.Tentative))
        assertEquals(MonarchTone.Neutral, MonarchEventPopoverTokens.tone(MonarchEventChipStatus.Cancelled))
        assertEquals(MonarchTone.Danger, MonarchEventPopoverTokens.tone(MonarchEventChipStatus.Declined))
    }

    @Test
    fun detailRowsAreReadWithTheirLabels() {
        rule.setMonarchContent {
            MonarchEventPopover(
                title = "Design review",
                details = listOf(MonarchEventPopoverDetail("Location", "Room 4")),
            )
        }
        rule.onNodeWithText("Location", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Room 4", useUnmergedTree = true).assertExists()
    }

    @Test
    fun actionsAreSeparateButtons() {
        var joined = 0
        rule.setMonarchContent {
            MonarchEventPopover(title = "Call") {
                MonarchButton(text = "Join", onClick = { joined++ })
            }
        }
        rule.onNodeWithText("Join").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, joined)
    }

    @Test
    fun itCanBeShownInAMonarchPopover() {
        rule.setMonarchContent {
            Box {
                MonarchPopover(expanded = true, onDismissRequest = {}, label = "Design review") {
                    MonarchEventPopover(title = "Design review", status = MonarchEventChipStatus.Confirmed)
                }
            }
        }
        rule.onNodeWithText("Confirmed", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEventPopover(
                title = "Quarterly planning with the regional sales leads",
                note = "Bring the forecast.",
            )
        }
        rule.onNodeWithText("Quarterly planning with the regional sales leads").assertTextFullyVisible()
        rule.onNodeWithText("Bring the forecast.").assertTextFullyVisible()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchEventPopover(
                title = "Offsite",
                start = EventDay,
                allDay = true,
                status = MonarchEventChipStatus.Confirmed,
                labels = MonarchEventPopoverLabels(time = "When", allDay = "Whole day", status = { "Going" }),
            )
        }
        rule.onNodeWithContentDescription("When, Sat, Nov 14, 2026, Whole day").assertExists()
        rule.onNodeWithText("Going", useUnmergedTree = true).assertExists()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val tokens = MonarchEventPopoverTokens
            assertEquals(colors.accentSolid, tokens.swatch(Color.Unspecified, colors), theme.id)
            assertEquals(colors.bgSubtle, tokens.noteFill(colors), theme.id)
        }
        val custom = Color(0xFF336699)
        assertEquals(custom, MonarchEventPopoverTokens.swatch(custom, MonarchColors(AllThemes.first().colors)))
    }
}

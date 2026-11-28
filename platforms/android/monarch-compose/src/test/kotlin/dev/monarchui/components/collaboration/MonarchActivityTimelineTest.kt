package dev.monarchui.components.collaboration

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimeline
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActor
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineActorKind
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineDays
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineDensity
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineEvent
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineLabels
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineMarker
import dev.monarchui.components.collaboration.activitytimeline.MonarchActivityTimelineTokens
import dev.monarchui.foundation.MonarchTone
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
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val Now = 1_772_636_400_000L
private const val Hour = 3_600_000L
private const val LastYear = 1_767_096_000_000L
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")
private val ana = MonarchActivityTimelineActor("Ana Ruiz")
private val automation = MonarchActivityTimelineActor(
    "Follow-up rule",
    kind = MonarchActivityTimelineActorKind.Automation,
)
private val feed = listOf(
    MonarchActivityTimelineEvent(
        id = "1",
        verb = "moved",
        time = Now - Hour,
        actor = ana,
        target = "Acme renewal",
        tone = MonarchTone.Info,
        tags = listOf("Negotiation"),
        details = "Stage: Proposal → Negotiation",
    ),
    MonarchActivityTimelineEvent(
        id = "2",
        verb = "sent a reminder to",
        time = Now - 2 * Hour,
        actor = automation,
        target = "Ben Okafor",
    ),
    MonarchActivityTimelineEvent(id = "3", verb = "Ticket created from email", time = Now - 26 * Hour),
    MonarchActivityTimelineEvent(
        id = "4",
        verb = "commented on",
        time = LastYear,
        actor = ana,
        target = "Acme renewal",
        body = "Sent the draft.",
    ),
)

/** A node whose content description starts with [prefix]. */
private fun descriptionStarts(prefix: String) = SemanticsMatcher("description starts with $prefix") { node ->
    node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }.any { it.startsWith(prefix) }
}

internal object MonarchActivityTimelineFixtures : ComponentFixtures {
    override val id = "activity-timeline"
    override val fixtures = listOf(
        fixture("grouped by day, dots") {
            MonarchActivityTimeline(events = feed, now = Now, timeZone = Utc.id)
        },
        fixture("avatars, compact, details open") {
            MonarchActivityTimeline(
                events = feed.take(2),
                marker = MonarchActivityTimelineMarker.Avatar,
                density = MonarchActivityTimelineDensity.Compact,
                expandedIds = setOf("1"),
                onExpandedChange = {},
                now = Now,
                timeZone = Utc.id,
            )
        },
        fixture("ungrouped, loading more") {
            MonarchActivityTimeline(
                events = feed.take(2),
                groupByDay = false,
                hasMore = true,
                loadingMore = true,
                now = Now,
                timeZone = Utc.id,
                onLoadMore = {},
            )
        },
        fixture("more failed") {
            MonarchActivityTimeline(
                events = feed.take(1),
                hasMore = true,
                loadMoreFailed = true,
                now = Now,
                timeZone = Utc.id,
                onLoadMore = {},
            )
        },
        fixture("loading") { MonarchActivityTimeline(events = emptyList(), loading = true) },
        fixture("failed with retry") { MonarchActivityTimeline(events = emptyList(), failed = true, onRetry = {}) },
        fixture("failed without retry") { MonarchActivityTimeline(events = emptyList(), failed = true) },
        fixture("empty") { MonarchActivityTimeline(events = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchActivityTimelineTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun daysAreHeadingsInTheOrderOfTheirFirstEvent() {
        compose.setMonarchContent { MonarchActivityTimeline(events = feed, now = Now, timeZone = Utc.id) }
        compose.onNodeWithText("Today").assert(isHeading())
        compose.onNodeWithText("Yesterday").assert(isHeading())
        compose.onNodeWithText("Tuesday, December 30, 2025").assert(isHeading())
        val today = compose.onNodeWithText("Today").getUnclippedBoundsInRoot()
        val yesterday = compose.onNodeWithText("Yesterday").getUnclippedBoundsInRoot()
        assertTrue(today.top < yesterday.top)
    }

    @Test
    fun eachEventIsReadAsOneSentenceWithItsDateTagsAndBody() {
        compose.setMonarchContent { MonarchActivityTimeline(events = feed, now = Now, timeZone = Utc.id) }
        compose.onNode(descriptionStarts("Ana Ruiz moved Acme renewal, Mar 4, 2026, 2:00")).assertExists()
            .assert(SemanticsMatcher("ends with the tag") { node ->
                node.config[SemanticsProperties.ContentDescription].single().endsWith("PM, Negotiation")
            })
        compose.onNode(descriptionStarts("Follow-up rule sent a reminder to Ben Okafor, Mar 4, 2026, 1:00"))
            .assert(SemanticsMatcher("is automated") { node ->
                node.config[SemanticsProperties.ContentDescription].single().endsWith("PM, Automated")
            })
        compose.onNode(descriptionStarts("Ticket created from email, Mar 3, 2026")).assertExists()
        compose.onNode(descriptionStarts("Ana Ruiz commented on Acme renewal, Dec 30, 2025"))
            .assert(SemanticsMatcher("ends with the body") { node ->
                node.config[SemanticsProperties.ContentDescription].single().endsWith(", Sent the draft.")
            })
    }

    @Test
    fun detailsOpenBehindADisclosureTheTimelineKeeps() {
        compose.setMonarchContent { MonarchActivityTimeline(events = feed.take(1), now = Now, timeZone = Utc.id) }
        compose.onNodeWithText("Stage: Proposal → Negotiation").assertDoesNotExist()
        compose.onNode(hasText("Details") and hasClickAction()).assertTouchTargetAtLeast48dp(compose.density)
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Stage: Proposal → Negotiation").assertExists()
    }

    @Test
    fun controlledDetailsReportTheNewSet() {
        val reported = mutableListOf<Set<String>>()
        compose.setMonarchContent {
            MonarchActivityTimeline(
                events = feed.take(1),
                expandedIds = setOf("1"),
                onExpandedChange = { reported += it },
                now = Now,
                timeZone = Utc.id,
            )
        }
        compose.onNodeWithText("Stage: Proposal → Negotiation").assertExists()
        compose.onNode(hasText("Hide details") and hasClickAction()).performClick()
        assertEquals(listOf(emptySet<String>()), reported)
    }

    @Test
    fun anUngroupedFeedIsNamedActivityAndHasNoHeadings() {
        compose.setMonarchContent {
            MonarchActivityTimeline(events = feed, groupByDay = false, now = Now, timeZone = Utc.id)
        }
        compose.onNodeWithContentDescription("Activity").assertExists()
        compose.onNodeWithText("Today").assertDoesNotExist()
    }

    @Test
    fun loadMoreCallsTheAppAndHidesWithoutIt() {
        var loads = 0
        compose.setMonarchContent {
            MonarchActivityTimeline(
                events = feed.take(1),
                hasMore = true,
                now = Now,
                timeZone = Utc.id,
                onLoadMore = { loads += 1 },
            )
        }
        compose.onNode(hasText("Load more") and hasClickAction()).assertTouchTargetAtLeast48dp(compose.density)
            .performClick()
        assertEquals(1, loads)
    }

    @Test
    fun loadingIsOneElement() {
        compose.setMonarchContent { MonarchActivityTimeline(events = feed, loading = true) }
        val indeterminate = SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo,
            ProgressBarRangeInfo.Indeterminate,
        )
        compose.onNodeWithContentDescription("Loading activity").assert(indeterminate)
        compose.onNode(descriptionStarts("Ana Ruiz")).assertDoesNotExist()
    }

    @Test
    fun aFailureOffersTryAgain() {
        var retries = 0
        compose.setMonarchContent {
            MonarchActivityTimeline(events = emptyList(), failed = true, onRetry = { retries += 1 })
        }
        compose.onNodeWithText("Activity didn't load.", substring = true).assertExists()
        compose.onNode(hasClickAction()).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun noEventsShowTheEmptyState() {
        compose.setMonarchContent { MonarchActivityTimeline(events = emptyList()) }
        compose.onNodeWithText("No activity yet").assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        compose.setMonarchContent {
            MonarchActivityTimeline(
                events = feed.take(3),
                now = Now,
                timeZone = Utc.id,
                labels = MonarchActivityTimelineLabels(
                    today = "Heute",
                    yesterday = "Gestern",
                    automated = "Automatisch",
                    details = "Mehr",
                ),
            )
        }
        compose.onNodeWithText("Heute").assertExists()
        compose.onNodeWithText("Gestern").assertExists()
        compose.onNode(hasText("Mehr") and hasClickAction()).assertExists()
        compose.onNode(descriptionStarts("Follow-up rule")).assert(SemanticsMatcher("is automated") { node ->
            node.config[SemanticsProperties.ContentDescription].single().endsWith("Automatisch")
        })
    }

    @Test
    fun theTextStartsAfterTheMarker() {
        compose.setMonarchContent {
            MonarchActivityTimeline(
                events = feed.take(1),
                modifier = Modifier.testTag("timeline"),
                now = Now,
                timeZone = Utc.id,
            )
        }
        val timeline = compose.onNodeWithTag("timeline").getUnclippedBoundsInRoot()
        val text = compose.onNode(descriptionStarts("Ana Ruiz")).getUnclippedBoundsInRoot()
        assertTrue(text.left >= timeline.left + 24.dp, "$text in $timeline")
    }

    @Test
    fun theTextIsMirroredInRightToLeft() {
        compose.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchActivityTimeline(
                events = feed.take(1),
                modifier = Modifier.testTag("timeline"),
                now = Now,
                timeZone = Utc.id,
            )
        }
        val timeline = compose.onNodeWithTag("timeline").getUnclippedBoundsInRoot()
        val text = compose.onNode(descriptionStarts("Ana Ruiz")).getUnclippedBoundsInRoot()
        assertTrue(text.right <= timeline.right - 24.dp, "$text in $timeline")
    }

    @Test
    fun dayHeadingsWrapAtTheLargestFontSize() {
        compose.setMonarchContent(fontScale = LargestFontScale) {
            MonarchActivityTimeline(events = feed.takeLast(1), now = Now, timeZone = Utc.id)
        }
        compose.onNodeWithText("Tuesday, December 30, 2025").assertTextFullyVisible()
    }

    @Test
    fun daysFollowTheTimeZoneAndKeepTheirOrder() {
        val lateUtc = MonarchActivityTimelineEvent("a", "late", Now + 8 * Hour)
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        assertEquals(
            MonarchActivityTimelineDays.dayNumber(Now, Utc) + 1,
            MonarchActivityTimelineDays.dayNumber(lateUtc.time, tokyo),
        )
        val groups = MonarchActivityTimelineDays.group(feed.reversed(), Utc)
        assertEquals(listOf(listOf("4"), listOf("3"), listOf("2", "1")), groups.map { day -> day.events.map { it.id } })
        val heading: (Long) -> String = {
            MonarchActivityTimelineDays.heading(it, Now, Utc, "Today", "Yesterday") { _, withYear -> "date $withYear" }
        }
        assertEquals("Today", heading(Now - Hour))
        assertEquals("Yesterday", heading(Now - 26 * Hour))
        assertEquals("date false", heading(Now - 72 * Hour))
        assertEquals("date true", heading(LastYear))
        assertEquals("Ticket created from email", MonarchActivityTimelineDays.sentence(feed[2]))
        assertTrue(MonarchActivityTimelineDays.automated(feed[1]))
        assertTrue(!MonarchActivityTimelineDays.automated(feed[0]) && !MonarchActivityTimelineDays.automated(feed[2]))
    }

    @Test
    fun coloursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val neutral = MonarchActivityTimelineTokens.markerColors(MonarchTone.Neutral, colors)
            assertEquals(colors.borderStrong, neutral.dot, theme.id)
            assertEquals(colors.textMuted, neutral.icon, theme.id)
            assertEquals(colors.bgSurface, neutral.ring, theme.id)
            assertEquals(colors.borderSubtle, neutral.outline, theme.id)
            val success = MonarchActivityTimelineTokens.markerColors(MonarchTone.Success, colors)
            assertEquals(colors.successSolid, success.dot, theme.id)
            assertEquals(colors.successStrong, success.icon, theme.id)
            assertEquals(colors.dangerSolid, MonarchActivityTimelineTokens.markerColors(MonarchTone.Danger, colors).dot)
            assertEquals(colors.borderSubtle, MonarchActivityTimelineTokens.line(colors), theme.id)
            assertEquals(colors.textMuted, MonarchActivityTimelineTokens.time(colors), theme.id)
            assertEquals(colors.bgCanvas, MonarchActivityTimelineTokens.headingBackground(colors), theme.id)
            assertEquals(colors.dangerStrong, MonarchActivityTimelineTokens.error(colors), theme.id)
        }
    }
}

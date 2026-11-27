package com.rookhq.monarch.components.technical

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentStage
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentTimeline
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentTimelineDensity
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentTimelineLabels
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentTimelineRules
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentTimelineTokens
import com.rookhq.monarch.components.technical.incidenttimeline.MonarchIncidentUpdate
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import java.util.Locale
import java.util.TimeZone

private const val Start = 1_772_636_400_000L
private const val Quarter = 900_000L
private val first = MonarchIncidentUpdate(
    id = "1",
    stage = MonarchIncidentStage.Investigating,
    body = "Checkout is failing.",
    time = Start,
    automated = true,
)
private val second = MonarchIncidentUpdate(
    id = "2",
    stage = MonarchIncidentStage.Identified,
    body = "A migration locked a table.",
    time = Start + Quarter,
    author = "Ada",
)
private val third = MonarchIncidentUpdate(id = "3", stage = "resolved", body = "", time = Start + 4 * Quarter)

internal object MonarchIncidentTimelineFixtures : ComponentFixtures {
    override val id = "incident-timeline"
    override val fixtures = listOf(
        fixture("standard stages") { MonarchIncidentTimeline(updates = listOf(first, second)) },
        fixture("compact, empty body") {
            MonarchIncidentTimeline(updates = listOf(second, third), density = MonarchIncidentTimelineDensity.Compact)
        },
        fixture("product stage, monitoring, other zone") {
            MonarchIncidentTimeline(
                updates = listOf(
                    MonarchIncidentUpdate("m", "maintenance", "Moving hardware.", Start),
                    MonarchIncidentUpdate("n", MonarchIncidentStage.Monitoring, "Watching.", Start + Quarter),
                ),
                stages = mapOf("maintenance" to MonarchStatusDefinition("Maintenance", MonarchTone.Info)),
                timeZone = "America/New_York",
            )
        },
        fixture("unknown stage") {
            MonarchIncidentTimeline(updates = listOf(MonarchIncidentUpdate("x", "partial_outage", "Slow.", Start)))
        },
        fixture("empty") { MonarchIncidentTimeline(updates = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchIncidentTimelineTest {
    @get:Rule
    val rule = createComposeRule()

    private val t = MonarchIncidentTimelineTokens
    private val r = MonarchIncidentTimelineRules

    @Test
    fun stagesResolveProductKeysThenStandardOnesThenReadableKeys() {
        val product = MonarchStatusDefinition("Maintenance", MonarchTone.Info)
        val label: (MonarchIncidentStage) -> String = { it.name }
        assertEquals(product, r.resolve("maintenance", mapOf("maintenance" to product), label))
        val standard = r.resolve("RESOLVED", emptyMap(), label)
        assertEquals("Resolved", standard.label)
        assertEquals(MonarchTone.Success, standard.tone)
        val unknown = r.resolve("partial_outage", emptyMap(), label)
        assertEquals("Partial outage", unknown.label)
        assertEquals(MonarchTone.Neutral, unknown.tone)
        assertEquals("Major outage now", r.readable("MAJOR-outage__now"))
        assertEquals("__", r.readable("__"))
        assertEquals(MonarchTone.Danger, r.tone(MonarchIncidentStage.Investigating))
        assertEquals(MonarchTone.Warning, r.tone(MonarchIncidentStage.Identified))
        assertEquals(MonarchTone.Info, r.tone(MonarchIncidentStage.Monitoring))
    }

    @Test
    fun timesAreInOneZoneWithItsName() {
        assertEquals("Mar 4, 15:00 UTC", r.time(Start, TimeZone.getTimeZone("UTC"), Locale.US))
        assertEquals("Mar 4, 10:00 EST", r.time(Start, TimeZone.getTimeZone("America/New_York"), Locale.US))
    }

    @Test
    fun theSpokenUpdateJoinsTheHeaderAndTheBody() {
        assertEquals("Identified, Mar 4, Ada. Text", r.spoken("Identified", "Mar 4", "Ada", null, "Text"))
        assertEquals("Resolved, Mar 4, Automated", r.spoken("Resolved", "Mar 4", "", "Automated", ""))
        assertEquals(third, r.newUpdate(listOf(first, third), 1))
        assertNull(r.newUpdate(listOf(first), 2))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.borderStrong, t.marker(MonarchTone.Neutral, colors))
            assertEquals(colors.dangerSolid, t.marker(MonarchTone.Danger, colors))
            assertEquals(colors.successSolid, t.marker(MonarchTone.Success, colors))
            assertEquals(colors.borderDefault, t.line(colors))
            assertEquals(colors.textMuted, t.muted(colors))
        }
    }

    @Test
    fun eachUpdateIsOneElementInTheNamedGroup() {
        rule.setMonarchContent { MonarchIncidentTimeline(updates = listOf(first, second)) }
        rule.onNodeWithContentDescription("Incident updates").assertExists()
        rule.onNodeWithContentDescription("Investigating, Mar 4, 15:00 UTC, Automated. Checkout is failing.")
            .assertExists()
        rule.onNodeWithContentDescription("Identified, Mar 4, 15:15 UTC, Ada. A migration locked a table.")
            .assertExists()
        rule.onNodeWithText("Automated", useUnmergedTree = true).assertExists()
    }

    @Test
    fun noUpdatesShowTheEmptyText() {
        rule.setMonarchContent {
            MonarchIncidentTimeline(
                updates = emptyList(),
                labels = MonarchIncidentTimelineLabels(empty = "Nothing yet"),
            )
        }
        rule.onNodeWithText("Nothing yet").assertExists()
    }

    @Test
    fun labelsReplaceStageNamesAndTheTag() {
        rule.setMonarchContent {
            MonarchIncidentTimeline(
                updates = listOf(first),
                labels = MonarchIncidentTimelineLabels(investigating = "Untersuchung", automated = "Automatisch"),
            )
        }
        rule.onNodeWithContentDescription("Untersuchung, Mar 4, 15:00 UTC, Automatisch. Checkout is failing.")
            .assertExists()
    }

    @Test
    fun anAddedUpdateIsAnnouncedOnlyWhenLive() {
        var updates by mutableStateOf(listOf(first))
        var live by mutableStateOf(false)
        rule.setMonarchContent { MonarchIncidentTimeline(updates = updates, live = live) }
        updates = listOf(first, second)
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Identified, Mar 4, 15:15 UTC, Ada. A migration locked a table.")
            .assertCountEquals(1)
        live = true
        updates = listOf(first, second, third)
        rule.waitForIdle()
        rule.onAllNodesWithContentDescription("Resolved, Mar 4, 16:00 UTC").assertCountEquals(2)
        rule.onAllNodesWithContentDescription("Resolved, Mar 4, 16:00 UTC")[1]
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theStageComesBeforeTheTimeInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) { MonarchIncidentTimeline(updates = listOf(second)) }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Identified", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithText("Mar 4, 15:15 UTC", useUnmergedTree = true), direction)
        }
    }

    @Test
    fun textIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchIncidentTimeline(updates = listOf(second)) }
        rule.onNodeWithText("A migration locked a table.", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Mar 4, 15:15 UTC", useUnmergedTree = true).assertTextFullyVisible()
    }
}

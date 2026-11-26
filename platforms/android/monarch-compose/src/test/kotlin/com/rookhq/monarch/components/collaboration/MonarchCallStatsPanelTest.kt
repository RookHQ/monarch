package com.rookhq.monarch.components.collaboration

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanel
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelFormat
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelLabels
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelPeer
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelQuality
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelTokens
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelUnits
import com.rookhq.monarch.components.collaboration.callstatspanel.MonarchCallStatsPanelUsage
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val usage = MonarchCallStatsPanelUsage(
    sentBytes = 6_200_000,
    receivedBytes = 9_400_000,
    upBitsPerSecond = 420_000.0,
    downBitsPerSecond = 1_250_000.0,
)
private val robin = MonarchCallStatsPanelPeer(
    id = "a",
    name = "Robin Park",
    roundTripMilliseconds = 38.4,
    inboundLossPercent = 0.2,
    outboundLossPercent = 0.1,
    quality = MonarchCallStatsPanelQuality.Good,
)
private val alex = MonarchCallStatsPanelPeer(
    id = "b",
    name = "Alex Chen",
    roundTripMilliseconds = 185.6,
    inboundLossPercent = Double.NaN,
    outboundLossPercent = 3.4,
    quality = MonarchCallStatsPanelQuality.Fair,
)
private val jordan = MonarchCallStatsPanelPeer(id = "c", name = "Jordan Diaz")
private val units = MonarchCallStatsPanelUnits(
    bytes = listOf("%1\$s B", "%1\$s KB", "%1\$s MB", "%1\$s GB", "%1\$s TB"),
    rates = listOf("%1\$s bps", "%1\$s kbps", "%1\$s Mbps"),
    milliseconds = "%1\$s ms",
)
private val plain: (Double, Int) -> String = { value, digits -> "%.${digits}f".format(java.util.Locale.ROOT, value) }

internal object MonarchCallStatsPanelFixtures : ComponentFixtures {
    override val id = "call-stats-panel"
    override val fixtures = listOf(
        fixture("three people, every grade state") {
            MonarchCallStatsPanel(usage = usage, peers = listOf(robin, alex, jordan))
        },
        fixture("poor connection, low data, footnote") {
            MonarchCallStatsPanel(
                usage = MonarchCallStatsPanelUsage(sentBytes = 120_000, receivedBytes = 80_000),
                peers = listOf(
                    MonarchCallStatsPanelPeer("p", "Alex Chen", 640.0, 12.0, 8.0, MonarchCallStatsPanelQuality.Poor),
                ),
                lowData = true,
                footnote = "Example footnote: say here how your product routes and protects call media.",
            )
        },
        fixture("nobody else") {
            MonarchCallStatsPanel(usage = usage, peers = emptyList())
        },
        fixture("a very long name") {
            MonarchCallStatsPanel(
                usage = usage,
                peers = listOf(MonarchCallStatsPanelPeer("l", "Maximilian Alexander von Hohenberg-Lindqvist", 42.0)),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCallStatsPanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theHeadingAndTheUsageLineComeFirst() {
        rule.setMonarchContent { MonarchCallStatsPanel(usage = usage, peers = listOf(robin)) }
        rule.onNodeWithText("Data used this call").assert(isHeading())
        rule.onNodeWithText("15 MB total · up 420 kbps · down 1.3 Mbps").assertExists()
    }

    @Test
    fun eachPersonIsOneElementWithTheGrade() {
        rule.setMonarchContent { MonarchCallStatsPanel(usage = usage, peers = listOf(robin, alex, jordan)) }
        rule.onNodeWithContentDescription("Robin Park, Round trip 38 ms, Loss 0.2%, Good connection").assertExists()
        rule.onNodeWithContentDescription("Alex Chen, Round trip 186 ms, Loss 3.4%, Unstable connection")
            .assertExists()
        rule.onNodeWithContentDescription(
            "Jordan Diaz, Round trip Not measured yet, Loss Not measured yet, Not measured yet",
        ).assertExists()
        val group = rule.onNodeWithContentDescription("Connection to each person")
        group.assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
    }

    @Test
    fun peopleAreListedTopToBottomInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchCallStatsPanel(usage = usage, peers = listOf(robin, alex))
        }
        val first = rule.onNodeWithContentDescription("Robin Park, Round trip 38 ms, Loss 0.2%, Good connection")
        val second = rule.onNodeWithContentDescription("Alex Chen, Round trip 186 ms, Loss 3.4%, Unstable connection")
        val a = first.fetchSemanticsNode().boundsInRoot
        val b = second.fetchSemanticsNode().boundsInRoot
        assertTrue(a.bottom <= b.top, "Robin is above Alex: $a, $b")
    }

    @Test
    fun withNobodyElseTheNoteIsShown() {
        rule.setMonarchContent { MonarchCallStatsPanel(usage = usage, peers = emptyList(), lowData = true) }
        rule.onNodeWithText("No one else is connected yet.").assertExists()
        rule.onNodeWithText("Low data mode is on: audio only.").assertExists()
        rule.onNodeWithContentDescription("Connection to each person").assertDoesNotExist()
    }

    @Test
    fun theFootnoteIsTheAppsOwn() {
        rule.setMonarchContent {
            MonarchCallStatsPanel(usage = usage, peers = emptyList(), footnote = "Media is relayed by our servers.")
        }
        rule.onNodeWithText("Media is relayed by our servers.").assertExists()
        rule.onNodeWithText("Low data mode is on: audio only.").assertDoesNotExist()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchCallStatsPanel(
                usage = usage,
                peers = listOf(alex, jordan),
                labels = MonarchCallStatsPanelLabels(
                    dataUsed = "Datenverbrauch",
                    usageSummary = { total, up, down -> "$total gesamt, $up hoch, $down runter" },
                    connections = "Verbindungen",
                    roundTrip = "Laufzeit",
                    loss = "Verlust",
                    notMeasured = "Noch nicht gemessen",
                    qualityNames = mapOf(MonarchCallStatsPanelQuality.Fair to "Instabil"),
                ),
            )
        }
        rule.onNodeWithText("Datenverbrauch").assertExists()
        rule.onNodeWithText("15 MB gesamt, 420 kbps hoch, 1.3 Mbps runter").assertExists()
        rule.onNodeWithContentDescription("Verbindungen").assertExists()
        rule.onNodeWithContentDescription("Alex Chen, Laufzeit 186 ms, Verlust 3.4%, Instabil").assertExists()
        rule.onNodeWithContentDescription(
            "Jordan Diaz, Laufzeit Noch nicht gemessen, Verlust Noch nicht gemessen, Noch nicht gemessen",
        ).assertExists()
    }

    @Test
    fun theUsageLineWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCallStatsPanel(usage = usage, peers = emptyList())
        }
        rule.onNodeWithText("15 MB total · up 420 kbps · down 1.3 Mbps").assertTextFullyVisible()
        rule.onNodeWithText("No one else is connected yet.").assertTextFullyVisible()
    }

    @Test
    fun bytesUsePowersOf1024AndOneDecimalUnderTen() {
        assertEquals("512 B", MonarchCallStatsPanelFormat.bytes(512, units, plain))
        assertEquals("1.4 KB", MonarchCallStatsPanelFormat.bytes(1434, units, plain))
        assertEquals("15 MB", MonarchCallStatsPanelFormat.bytes(15_600_000, units, plain))
        assertEquals("1.0 GB", MonarchCallStatsPanelFormat.bytes(1_073_741_824, units, plain))
        assertEquals("0 B", MonarchCallStatsPanelFormat.bytes(-5, units, plain))
    }

    @Test
    fun ratesUsePowersOf1000() {
        assertEquals("800 bps", MonarchCallStatsPanelFormat.rate(800.0, units, plain))
        assertEquals("64 kbps", MonarchCallStatsPanelFormat.rate(64_200.0, units, plain))
        assertEquals("1.5 Mbps", MonarchCallStatsPanelFormat.rate(1_500_000.0, units, plain))
        assertEquals("12 Mbps", MonarchCallStatsPanelFormat.rate(12_000_000.0, units, plain))
        assertEquals("0 bps", MonarchCallStatsPanelFormat.rate(-3.0, units, plain))
        assertEquals("0 bps", MonarchCallStatsPanelFormat.rate(null, units, plain))
        assertEquals("0 bps", MonarchCallStatsPanelFormat.rate(Double.NaN, units, plain))
    }

    @Test
    fun theRoundTripIsWholeAndTheLossIsTheWorseDirection() {
        assertEquals("186 ms", MonarchCallStatsPanelFormat.roundTrip(185.6, units, plain))
        assertNull(MonarchCallStatsPanelFormat.roundTrip(null, units, plain))
        assertNull(MonarchCallStatsPanelFormat.roundTrip(Double.POSITIVE_INFINITY, units, plain))
        assertEquals(3.4, MonarchCallStatsPanelFormat.loss(alex))
        assertEquals(0.2, MonarchCallStatsPanelFormat.loss(robin))
        assertNull(MonarchCallStatsPanelFormat.loss(jordan))
    }

    @Test
    fun gradesMapToTones() {
        assertEquals(MonarchTone.Success, MonarchCallStatsPanelTokens.tone(MonarchCallStatsPanelQuality.Good))
        assertEquals(MonarchTone.Warning, MonarchCallStatsPanelTokens.tone(MonarchCallStatsPanelQuality.Fair))
        assertEquals(MonarchTone.Danger, MonarchCallStatsPanelTokens.tone(MonarchCallStatsPanelQuality.Poor))
        assertEquals(MonarchTone.Neutral, MonarchCallStatsPanelTokens.tone(MonarchCallStatsPanelQuality.Unknown))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchCallStatsPanelTokens.muted(colors), theme.id)
        }
    }
}

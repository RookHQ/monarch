package com.rookhq.monarch.components.geo

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMap
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapDatum
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapDetail
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapExtra
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapLabels
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapTokens
import com.rookhq.monarch.components.geo.worldmap.MonarchWorldMapVariant
import com.rookhq.monarch.components.geo.worldmap.WorldMapData
import com.rookhq.monarch.components.geo.worldmap.WorldMapGeometry
import com.rookhq.monarch.components.geo.worldmap.WorldMapText
import com.rookhq.monarch.components.geo.worldmap.WorldMapWords
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val usage = listOf(
    MonarchWorldMapDatum("US", 1200.0, listOf(MonarchWorldMapDetail("Users", "340"))),
    MonarchWorldMapDatum("de ", 300.0),
    MonarchWorldMapDatum("SG", 40.0),
)

private const val SUMMARY = "3 countries. Highest: United States 1,200, Germany 300, Singapore 40."

private val plainWords = WorldMapWords(
    locale = Locale.US,
    format = { it.toInt().toString() },
    name = { it },
    countryValue = "%1\$s: %2\$s",
    noValue = "No value",
)

internal object MonarchWorldMapFixtures : ComponentFixtures {
    override val id = "world-map"
    override val fixtures = listOf(
        fixture("choropleth with legend") { MonarchWorldMap(title = "Workspaces", data = usage) },
        fixture("bubbles, color, selectable") {
            MonarchWorldMap(
                title = "Workspaces",
                data = usage,
                variant = MonarchWorldMapVariant.Bubble,
                color = MonarchChartColor.Data4,
                onSelect = {},
            )
        },
        fixture("extras and a code not on the map") {
            MonarchWorldMap(
                title = "Workspaces",
                data = listOf(MonarchWorldMapDatum("XX", 5.0)),
                extras = listOf(MonarchWorldMapExtra("Global", 75.0), MonarchWorldMapExtra("EU", 12.0)),
                showLegend = false,
            )
        },
        fixture("thresholds, no legend, description and footer") {
            MonarchWorldMap(
                title = "Workspaces by country in the last thirty days",
                data = usage,
                description = "Last 30 days",
                thresholds = listOf(50.0, 100.0, 500.0),
                showLegend = false,
                footer = "Source: billing.",
            )
        },
        fixture("no values") { MonarchWorldMap(title = "Workspaces", data = emptyList()) },
        fixture("loading") {
            MonarchWorldMap(title = "Workspaces", data = usage, status = MonarchChartFrameStatus.Loading)
        },
        fixture("error with retry") {
            MonarchWorldMap(title = "Workspaces", data = usage, status = MonarchChartFrameStatus.Error, onRetry = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchWorldMapTest {
    @get:Rule
    val rule = createComposeRule()

    private fun traversalIndex(index: Float) = SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, index)

    /** A country's element, not the details under the map that repeat its text. */
    private fun country(text: String) =
        SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf(text)) and
            SemanticsMatcher.keyIsDefined(SemanticsProperties.CollectionItemInfo)

    @Test
    fun theMapIsNamedWithTheSummaryAndEveryCountryIsReadLargestFirst() {
        rule.setMonarchContent { MonarchWorldMap(title = "Workspaces", data = usage) }
        rule.onNodeWithContentDescription(SUMMARY)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        rule.onNode(country("United States: 1,200, Users 340"))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.TraversalIndex, 0f))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
            .assertIsNotSelected()
        rule.onNode(country("Germany: 300")).assert(traversalIndex(1f))
        rule.onNode(country("Singapore: 40")).assert(traversalIndex(2f))
        rule.onNodeWithContentDescription("Legend, 0, Below 300, 300 to 600, 600 to 900, 900 or more").assertExists()
    }

    @Test
    fun aTapOnACountryInspectsItAndSelectsIt() {
        val chosen = mutableListOf<MonarchWorldMapDatum>()
        rule.setMonarchContent { MonarchWorldMap(title = "Workspaces", data = usage, onSelect = { chosen += it }) }
        rule.onNodeWithContentDescription(SUMMARY).performTouchInput {
            val ratio = width / WorldMapGeometry.WIDTH
            // Germany's centroid.
            val centre = WorldMapGeometry.centroids.getValue("DE")
            click(Offset(centre.x * ratio, centre.y * ratio))
        }
        rule.waitForIdle()
        rule.onNode(country("Germany: 300")).assertIsSelected()
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
                SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("Germany: 300")),
        ).assertExists()
        assertEquals(listOf("DE"), chosen.map { it.country })
        assertEquals(300.0, chosen.single().value)
    }

    @Test
    fun aTapOnTheSeaClearsTheInspectedCountry() {
        rule.setMonarchContent { MonarchWorldMap(title = "Workspaces", data = usage) }
        val map = rule.onNodeWithContentDescription(SUMMARY)
        map.performTouchInput {
            val ratio = width / WorldMapGeometry.WIDTH
            val centre = WorldMapGeometry.centroids.getValue("US")
            click(Offset(centre.x * ratio, centre.y * ratio))
        }
        rule.waitForIdle()
        rule.onNode(country("United States: 1,200, Users 340")).assertIsSelected()
        // The middle of the Pacific.
        map.performTouchInput { click(Offset(width * 0.02f, height * 0.6f)) }
        rule.waitForIdle()
        rule.onNode(country("United States: 1,200, Users 340")).assertIsNotSelected()
        rule.onNodeWithText("United States: 1,200, Users 340", useUnmergedTree = true).assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theSelectActionChoosesACountryWithTalkBack() {
        val chosen = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchWorldMap(title = "Workspaces", data = usage, onSelect = { chosen += it.country })
        }
        rule.onNode(country("Singapore: 40")).performCustomAccessibilityActionWithLabel("Select")
        rule.waitForIdle()
        assertEquals(listOf("SG"), chosen)
        rule.onNode(country("Singapore: 40")).assertIsSelected()
    }

    @Test
    fun viewAsTableListsEveryCountryDetailAndExtra() {
        rule.setMonarchContent {
            MonarchWorldMap(title = "Workspaces", data = usage, extras = listOf(MonarchWorldMapExtra("Global", 75.0)))
        }
        rule.onNode(hasText("View as table") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Country, Value, Users").assertExists()
        rule.onNodeWithContentDescription("United States, Value 1,200, Users 340").assertExists()
        rule.onNodeWithContentDescription("Germany, Value 300, Users No value").assertExists()
        rule.onNodeWithContentDescription("Global, Value 75, Users No value").assertExists()
    }

    @Test
    fun extrasAreTilesInANamedGroupAndUnplacedCodesAreListed() {
        rule.setMonarchContent {
            MonarchWorldMap(
                title = "Workspaces",
                data = listOf(MonarchWorldMapDatum("FR", 10.0), MonarchWorldMapDatum("QQ", 5.0)),
                extras = listOf(MonarchWorldMapExtra("Global", 75.0)),
                countryName = { if (it == "QQ") "Elsewhere" else "France" },
            )
        }
        rule.onNodeWithContentDescription("Other values")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true))
        rule.onNodeWithText("Global").assert(hasText("75"))
        rule.onNodeWithText("Not on the map: Elsewhere (5)").assertExists()
        rule.onNodeWithContentDescription("2 countries. Highest: France 10, Elsewhere 5.").assertExists()
    }

    @Test
    fun withoutValuesTheEmptyStateShowsAndExtrasAloneAreEnough() {
        rule.setMonarchContent {
            MonarchWorldMap(title = "Workspaces", data = listOf(MonarchWorldMapDatum("DE", Double.NaN)))
        }
        rule.onNodeWithText("No data yet").assertExists()
        assertEquals(
            MonarchChartFrameStatus.Ready,
            WorldMapData.status(
                MonarchChartFrameStatus.Ready,
                emptyList(),
                listOf(MonarchWorldMapExtra("EU", 1.0)),
            ),
        )
        assertEquals(
            MonarchChartFrameStatus.Loading,
            WorldMapData.status(MonarchChartFrameStatus.Loading, emptyList(), emptyList()),
        )
    }

    @Test
    fun bubblesHaveNoLegend() {
        rule.setMonarchContent {
            MonarchWorldMap(title = "Workspaces", data = usage, variant = MonarchWorldMapVariant.Bubble)
        }
        rule.onNodeWithContentDescription(SUMMARY).assertExists()
        rule.onNodeWithText("Less", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchWorldMap(
                title = "Arbeitsbereiche",
                data = usage + MonarchWorldMapDatum("QQ", 2.0),
                extras = listOf(MonarchWorldMapExtra("Weltweit", 3.0)),
                countryName = { it },
                formatValue = { "${it.toInt()} WS" },
                labels = MonarchWorldMapLabels(
                    extras = "Weitere Werte",
                    less = "Weniger",
                    more = "Mehr",
                    rangeBelow = "Unter %1\$s",
                    range = "%1\$s bis %2\$s",
                    rangeAbove = "Ab %1\$s",
                    unplaced = { names, total -> "Nicht auf der Karte: $names, $total" },
                    summary = { count, top -> "$count Länder: $top" },
                ),
            )
        }
        rule.onNodeWithContentDescription("4 Länder: US 1200 WS, DE 300 WS, SG 40 WS").assertExists()
        rule.onNodeWithText("Nicht auf der Karte: QQ, 2 WS").assertExists()
        rule.onNodeWithContentDescription("Weitere Werte").assertExists()
        rule.onNodeWithContentDescription(
            "Legend, 0 WS, Unter 300 WS, 300 WS bis 600 WS, 600 WS bis 900 WS, Ab 900 WS",
        ).assertExists()
    }

    @Test
    fun theMapKeepsItsGeographyRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchWorldMap(title = "Workspaces", data = usage)
        }
        // The United States stays to the left of Germany: the map is not mirrored.
        rule.onNode(country("United States: 1,200, Users 340"))
            .assertIsBefore(rule.onNode(country("Germany: 300")), LayoutDirection.Ltr)
    }

    @Test
    fun theDetailsAndLegendFitAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchWorldMap(title = "Workspaces", data = usage)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Less", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("More", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun rowsAreCleanedAndSortedLargestFirst() {
        val rows = WorldMapData.rows(
            listOf(
                MonarchWorldMapDatum(" de", 1.0),
                MonarchWorldMapDatum("FR", 5.0),
                MonarchWorldMapDatum("DE", 9.0),
                MonarchWorldMapDatum("IT", Double.POSITIVE_INFINITY),
                MonarchWorldMapDatum("  ", 3.0),
                MonarchWorldMapDatum("AT", 5.0),
            ),
        )
        assertEquals(listOf("DE" to 9.0, "AT" to 5.0, "FR" to 5.0), rows.map { it.country to it.value })
        val withUnknown = rows + MonarchWorldMapDatum("QQ", 1.0)
        assertEquals(listOf("DE", "AT", "FR"), WorldMapData.placed(withUnknown).map { it.country })
        assertEquals(listOf("QQ"), WorldMapData.unplaced(withUnknown).map { it.country })
        assertEquals(9.0, WorldMapData.maximum(rows))
        assertEquals(0.0, WorldMapData.maximum(listOf(MonarchWorldMapDatum("DE", -4.0))))
    }

    @Test
    fun theOutlineIsTheWebsNaturalEarthData() {
        assertEquals(174, WorldMapGeometry.shapes.size)
        assertEquals(235, WorldMapGeometry.centroids.size)
        assertEquals(Offset(788f, 229f), WorldMapGeometry.centroids["SG"])
        assertFalse(WorldMapGeometry.shapes.containsKey("SG"))
        assertTrue(WorldMapGeometry.isPlaced("SG"))
        assertFalse(WorldMapGeometry.isPlaced("QQ"))
        val us = WorldMapGeometry.shapes.getValue("US")
        assertTrue(WorldMapGeometry.contains(us, WorldMapGeometry.centroids.getValue("US")))
        assertFalse(WorldMapGeometry.contains(us, WorldMapGeometry.centroids.getValue("DE")))
    }

    @Test
    fun pathsAreReadWithMovesLinesAndCloses() {
        val shapes = WorldMapGeometry.parseShapes("AA M0 0l10 0l0 10z m20 0l5 0l0 5z\n  BB M1 1L4 1L4 4z CC M0 0l1 1z")
        assertEquals(setOf("AA", "BB"), shapes.keys)
        val aa = shapes.getValue("AA")
        assertEquals(2, aa.size)
        assertEquals(listOf(0f, 0f, 10f, 0f, 10f, 10f), aa[0].toList())
        // A relative move after a close starts from the ring's first point.
        assertEquals(listOf(20f, 0f, 25f, 0f, 25f, 5f), aa[1].toList())
        assertEquals(listOf(1f, 1f, 4f, 1f, 4f, 4f), shapes.getValue("BB")[0].toList())
        assertTrue(WorldMapGeometry.contains(aa, Offset(8f, 2f)))
        assertFalse(WorldMapGeometry.contains(aa, Offset(2f, 8f)))
        assertEquals(
            mapOf("AA" to Offset(1f, 2f), "BB" to Offset(3.5f, 4f)),
            WorldMapGeometry.parseCentroids(" AA 1 2  BB 3.5 4 "),
        )
        assertEquals(Offset(5f, 5f), WorldMapGeometry.boundsCentre(listOf(floatArrayOf(0f, 0f, 10f, 0f, 10f, 10f))))
    }

    @Test
    fun hitsFindMarksOutlinesThenTheNearestCountry() {
        val rows = WorldMapData.rows(usage)
        val sg = WorldMapGeometry.centroids.getValue("SG")
        val us = WorldMapGeometry.centroids.getValue("US")
        val choropleth = MonarchWorldMapVariant.Choropleth
        assertEquals("SG", WorldMapData.hit(sg, rows, choropleth, 1200.0, tolerance = 0f))
        assertEquals("US", WorldMapData.hit(us, rows, choropleth, 1200.0, tolerance = 0f))
        assertEquals("SG", WorldMapData.hit(sg + Offset(10f, 0f), rows, choropleth, 1200.0, tolerance = 12f))
        assertNull(WorldMapData.hit(sg + Offset(10f, 0f), rows, choropleth, 1200.0, tolerance = 2f))
        // A bubble covers the point near its centre; the smallest bubble is on top.
        assertEquals("SG", WorldMapData.hit(sg, rows, MonarchWorldMapVariant.Bubble, 1200.0, tolerance = 0f))
        assertEquals(listOf("SG"), WorldMapData.marks(rows, choropleth).map { it.country })
        val bubbles = WorldMapData.marks(rows, MonarchWorldMapVariant.Bubble)
        assertEquals(listOf("US", "DE", "SG"), bubbles.map { it.country })
    }

    @Test
    fun marksAndLevelsFollowTheValues() {
        val bubble = MonarchWorldMapVariant.Bubble
        val choropleth = MonarchWorldMapVariant.Choropleth
        assertEquals(MonarchWorldMapTokens.DOT_RADIUS, WorldMapData.markRadius(5.0, 10.0, choropleth))
        assertEquals(26f, WorldMapData.markRadius(100.0, 100.0, bubble))
        assertEquals(15f, WorldMapData.markRadius(25.0, 100.0, bubble))
        assertEquals(4f, WorldMapData.markRadius(-5.0, 100.0, bubble))
        assertEquals(4f, WorldMapData.markRadius(5.0, 0.0, bubble))
        val rows = WorldMapData.rows(usage)
        val scale = WorldMapData.scale(rows, null)
        assertEquals(listOf(300.0, 600.0, 900.0), scale.thresholds)
        assertEquals(4, WorldMapData.level(rows[0], scale, MonarchWorldMapVariant.Choropleth))
        assertNull(WorldMapData.level(rows[0], scale, bubble))
        assertNull(WorldMapData.level(null, scale, MonarchWorldMapVariant.Choropleth))
        assertEquals(listOf(1.0, 2.0, 3.0), WorldMapData.scale(rows, listOf(1.0, 2.0, 3.0)).thresholds)
    }

    @Test
    fun theWordsDescribeSummariseAndTabulate() {
        val rows = WorldMapData.rows(usage)
        assertEquals("US: 1200, Users 340", WorldMapText.describe(rows[0], plainWords))
        assertEquals("US 1200, DE 300, SG 40", WorldMapText.top(rows, plainWords))
        assertNull(WorldMapText.unplacedNote(emptyList(), plainWords) { n, t -> "$n $t" })
        assertEquals(
            "QQ, ZZ = 3",
            WorldMapText.unplacedNote(
                listOf(MonarchWorldMapDatum("QQ", 1.0), MonarchWorldMapDatum("ZZ", 2.0)),
                plainWords,
            ) { n, t -> "$n = $t" },
        )
        val table = WorldMapText.table(rows, listOf(MonarchWorldMapExtra("EU", 7.0)), plainWords, "C", "V")
        assertEquals(listOf("US", "DE", "SG", "EU"), table.categories)
        assertEquals(listOf("V", "Users"), table.series.map { it.name })
        assertEquals(listOf("340", "", "", ""), table.series[1].formattedValues)
        assertEquals(listOf(340.0, null, null, null), table.series[1].values)
        assertEquals("Germany", WorldMapText.countryName("DE", Locale.US, null))
        assertEquals("Allemagne", WorldMapText.countryName("DE", Locale.FRANCE, null))
        assertEquals("1", WorldMapText.countryName("1", Locale.US, null))
        assertEquals("Mine", WorldMapText.countryName("DE", Locale.US) { "Mine" })
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val slot = MonarchChartColor.Data3
            assertEquals(c.bgMuted, MonarchWorldMapTokens.fill(null, slot, c), theme.id)
            assertEquals(c.bgMuted, MonarchWorldMapTokens.fill(0, slot, c), theme.id)
            assertEquals(c.data3, MonarchWorldMapTokens.fill(4, slot, c), theme.id)
            assertEquals(lerp(c.bgSurface, c.data3, 0.25f), MonarchWorldMapTokens.bubbleFill(false, slot, c), theme.id)
            assertEquals(lerp(c.bgSurface, c.data3, 0.5f), MonarchWorldMapTokens.bubbleFill(true, slot, c), theme.id)
            assertEquals(c.data3, MonarchWorldMapTokens.bubbleBorder(slot, c), theme.id)
            assertEquals(c.bgSurface, MonarchWorldMapTokens.border(c), theme.id)
            assertEquals(c.textDefault, MonarchWorldMapTokens.activeBorder(c), theme.id)
            assertEquals(c.textDefault, MonarchWorldMapTokens.detailColor(c), theme.id)
            assertEquals(c.textMuted, MonarchWorldMapTokens.mutedColor(c), theme.id)
            assertEquals(c.bgSubtle, MonarchWorldMapTokens.tileFill(c), theme.id)
            assertEquals(c.borderSubtle, MonarchWorldMapTokens.tileBorder(c), theme.id)
        }
    }
}

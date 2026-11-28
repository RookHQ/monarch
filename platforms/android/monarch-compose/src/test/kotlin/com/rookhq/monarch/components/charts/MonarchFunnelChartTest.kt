package com.rookhq.monarch.components.charts

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameLabels
import com.rookhq.monarch.components.charts.chartframe.MonarchChartFrameStatus
import com.rookhq.monarch.components.charts.chartlegend.MonarchChartColor
import com.rookhq.monarch.components.charts.funnelchart.FunnelChartModel
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChart
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartAlignment
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartLabels
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartStage
import com.rookhq.monarch.components.charts.funnelchart.MonarchFunnelChartTokens
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val signup = listOf(
    MonarchFunnelChartStage("Visited pricing", 4800.0),
    MonarchFunnelChartStage("Started sign-up", 1900.0),
    MonarchFunnelChartStage("Paid", 310.0),
)
private val two = signup.take(2)

internal object MonarchFunnelChartFixtures : ComponentFixtures {
    override val id = "funnel-chart"
    override val fixtures = listOf(
        fixture("centered with steps") { MonarchFunnelChart(title = "Sign-up", stages = two) },
        fixture("start aligned, no steps, selectable") {
            MonarchFunnelChart(
                title = "Hiring",
                stages = two,
                alignment = MonarchFunnelChartAlignment.Start,
                showSteps = false,
                onSelect = {},
            )
        },
        fixture("stage colors, zero and growth") {
            MonarchFunnelChart(
                title = "Trials",
                stages = listOf(
                    MonarchFunnelChartStage("Started", 0.0, color = MonarchChartColor.Data2),
                    MonarchFunnelChartStage("Upgraded", 12.0, color = MonarchChartColor.Data3),
                ),
            )
        },
        fixture("long labels") {
            MonarchFunnelChart(
                title = "Sign-up",
                stages = listOf(
                    MonarchFunnelChartStage("Opened the pricing page from a marketing email", 4800.0),
                    MonarchFunnelChartStage("Finished every onboarding step", 2.0),
                ),
                showSteps = false,
            )
        },
        fixture("no stages") { MonarchFunnelChart(title = "Sign-up", stages = emptyList()) },
        fixture("loading") {
            MonarchFunnelChart(title = "Sign-up", stages = two, status = MonarchChartFrameStatus.Loading)
        },
        fixture("error with retry") {
            MonarchFunnelChart(title = "Sign-up", stages = two, status = MonarchChartFrameStatus.Error, onRetry = {})
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFunnelChartTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theStagesAreAGroupNamedWithTheSummary() {
        rule.setMonarchContent { MonarchFunnelChart(title = "Sign-up funnel", stages = signup) }
        rule.onNodeWithContentDescription(
            "3 stages from Visited pricing to Paid. Overall conversion 6.5%. Largest drop: Started sign-up to Paid, " +
                "83.7% lost",
        ).assertExists()
        rule.onNodeWithContentDescription("Visited pricing: 4,800").assertExists()
        rule.onNodeWithContentDescription("Started sign-up: 1,900, 39.6% of previous, 39.6% of first")
            .assertHasNoClickAction()
        rule.onNodeWithText("39.6% continued · 2,900 dropped", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("6.5%", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aSummaryReplacesTheGeneratedOne() {
        rule.setMonarchContent {
            MonarchFunnelChart(title = "Sign-up funnel", stages = signup, summary = "Most visitors leave at payment")
        }
        rule.onNodeWithContentDescription("Most visitors leave at payment").assertExists()
    }

    @Test
    fun selectableStagesAreButtonsWithA48dpTarget() {
        val selected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchFunnelChart(
                title = "Sign-up funnel",
                stages = signup,
                showSteps = false,
                onSelect = { selected += it.key },
            )
        }
        rule.onNodeWithContentDescription("Paid: 310, 16.3% of previous, 6.5% of first")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(listOf("Paid"), selected)
    }

    @Test
    fun viewAsTableListsEveryCountAndShare() {
        rule.setMonarchContent { MonarchFunnelChart(title = "Sign-up funnel", stages = signup) }
        rule.onNode(hasText("View as table") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Stage, Count, From previous, Of first").assertExists()
        rule.onNodeWithContentDescription("Visited pricing, Count 4,800, From previous No value, Of first 100%")
            .assertExists()
        rule.onNodeWithContentDescription("Paid, Count 310, From previous 16.3%, Of first 6.5%").assertExists()
    }

    @Test
    fun noStagesShowTheEmptyState() {
        rule.setMonarchContent {
            MonarchFunnelChart(title = "Sign-up funnel", stages = emptyList(), emptyMessage = "Nothing yet.")
        }
        rule.onNodeWithText("No data yet").assertExists()
        rule.onNodeWithText("Nothing yet.").assertExists()
    }

    @Test
    fun theStringsAndFormatsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchFunnelChart(
                title = "Inscriptions",
                stages = signup.take(2),
                formatValue = { "${it.toInt()} pers." },
                formatRate = { "${(it * 100).toInt()} pc" },
                labels = MonarchFunnelChartLabels(
                    frame = MonarchChartFrameLabels(viewTable = "Voir le tableau"),
                    step = "%1\$s ont continué, %2\$s perdus",
                    stages = { count, first, last -> "$count étapes de $first à $last" },
                    overall = "Conversion %1\$s",
                    largestDrop = "Perte : %1\$s à %2\$s, %3\$s",
                    describe = "%1\$s, %2\$s, %3\$s, %4\$s",
                ),
            )
        }
        rule.onNodeWithContentDescription(
            "2 étapes de Visited pricing à Started sign-up. Conversion 39 pc. Perte : Visited pricing à " +
                "Started sign-up, 60 pc",
        ).assertExists()
        rule.onNodeWithText("39 pc ont continué, 2900 pers. perdus", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Started sign-up, 1900 pers., 39 pc, 39 pc").assertExists()
        rule.onNodeWithText("Voir le tableau").assertExists()
    }

    @Test
    fun theLabelComesBeforeTheValueRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFunnelChart(title = "Sign-up funnel", stages = signup, alignment = MonarchFunnelChartAlignment.Start)
        }
        rule.onNodeWithText("Paid", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("310", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun theChartGrowsWithTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFunnelChart(title = "Sign-up funnel", stages = signup)
        }
        rule.waitForIdle()
        rule.onNodeWithText("Started sign-up", useUnmergedTree = true).assertTextFullyVisible()
        val lastStage = rule.onNodeWithText("16.3% continued · 1,590 dropped", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val toggle = rule.onNode(hasText("View as table") and hasClickAction()).getUnclippedBoundsInRoot()
        // "View as table" is in the header, as on the web: the stages start below it.
        assertTrue(toggle.bottom <= lastStage.top, "The stages overlap the header toggle: $lastStage and $toggle")
    }

    @Test
    fun theModelHandlesZeroGrowthAndBadValues() {
        val model = FunnelChartModel(
            listOf(
                MonarchFunnelChartStage("a", 100.0),
                MonarchFunnelChartStage("b", 0.0),
                MonarchFunnelChartStage("c", 10.0),
                MonarchFunnelChartStage("d", -5.0),
                MonarchFunnelChartStage("e", Double.NaN),
            ),
        )
        assertEquals(listOf(100.0, 0.0, 10.0, 0.0, 0.0), model.values)
        assertEquals(listOf(null, 0.0, 0.0, 0.0, 0.0), model.fromPrevious)
        assertEquals(listOf(1.0, 0.0, 0.1, 0.0, 0.0), model.ofFirst)
        assertEquals(0, model.largestDrop?.from)
        assertEquals(1.0, model.largestDrop?.lost)
        assertEquals(100.0, model.dropped(1))
        assertEquals(0.0, model.dropped(2))
        val growth = FunnelChartModel(listOf(MonarchFunnelChartStage("a", 10.0), MonarchFunnelChartStage("b", 20.0)))
        assertEquals(2.0, growth.fromPrevious[1])
        assertNull(growth.largestDrop)
        assertEquals(0f, MonarchFunnelChartTokens.barFraction(0.0, 100.0))
        assertEquals(0.005f, MonarchFunnelChartTokens.barFraction(0.01, 100_000.0))
        assertEquals(0.5f, MonarchFunnelChartTokens.barFraction(50.0, 100.0))
        assertEquals(0f, MonarchFunnelChartTokens.barFraction(5.0, 0.0))
    }

    @Test
    fun barsAreTheFirstDataColourOnAMutedTrackUnlessAStageNamesItsSlot() {
        // PREMIUM rule 8: a quiet 8dp pill in the first data colour (not the red accent) on a muted track,
        // and labels at the title's edge.
        assertEquals(20.dp, MonarchFunnelChartTokens.BarHeight)
        assertEquals(0.dp, MonarchFunnelChartTokens.StageHorizontalPadding)
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.data1, MonarchFunnelChartTokens.barColor(null, colors))
            assertEquals(androidx.compose.ui.graphics.Color.Transparent, MonarchFunnelChartTokens.trackColor())
            assertEquals(colors.data3, MonarchFunnelChartTokens.barColor(MonarchChartColor.Data3, colors))
            assertEquals(colors.textSubtle, MonarchFunnelChartTokens.barColor(MonarchChartColor.Neutral, colors))
            assertEquals(colors.textDefault, MonarchFunnelChartTokens.textColor(colors))
            assertEquals(colors.textMuted, MonarchFunnelChartTokens.mutedColor(colors))
        }
    }
}

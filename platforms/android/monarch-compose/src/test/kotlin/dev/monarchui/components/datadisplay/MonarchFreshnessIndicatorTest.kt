package dev.monarchui.components.datadisplay

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicator
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicatorLabels
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicatorTokens
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessIndicatorVariant
import dev.monarchui.components.datadisplay.freshnessindicator.MonarchFreshnessStatus
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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

private const val HOUR = DateUtils.HOUR_IN_MILLIS
private fun ago(millis: Long): Long = System.currentTimeMillis() - millis

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

internal object MonarchFreshnessIndicatorFixtures : ComponentFixtures {
    override val id = "freshness-indicator"
    override val fixtures = MonarchFreshnessIndicatorVariant.entries.map { variant ->
        fixture("${variant.name}, fresh with source and refresh") {
            MonarchFreshnessIndicator(
                updatedAt = ago(3 * DateUtils.MINUTE_IN_MILLIS),
                variant = variant,
                source = "Directory",
                onRefresh = {},
            )
        }
    } + listOf(
        fixture("stale and expired") {
            Column {
                MonarchFreshnessIndicator(updatedAt = ago(2 * HOUR), staleAfterMillis = HOUR)
                MonarchFreshnessIndicator(
                    updatedAt = ago(10 * HOUR),
                    staleAfterMillis = HOUR,
                    expiredAfterMillis = 5 * HOUR,
                )
            }
        },
        fixture("never, checking and error") {
            Column {
                MonarchFreshnessIndicator(updatedAt = null)
                MonarchFreshnessIndicator(updatedAt = null, status = MonarchFreshnessStatus.Checking)
                MonarchFreshnessIndicator(updatedAt = null, status = MonarchFreshnessStatus.Error)
            }
        },
        fixture("refreshing") {
            MonarchFreshnessIndicator(updatedAt = ago(HOUR), refreshing = true, onRefresh = {})
        },
        fixture("long text") {
            MonarchFreshnessIndicator(
                updatedAt = ago(2 * HOUR),
                variant = MonarchFreshnessIndicatorVariant.Banner,
                source = "Customer directory synchronised from the identity provider",
                details = listOf("Events on", "3 failures in the last day"),
                onRefresh = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFreshnessIndicatorTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLineIsOneElementWithCommasBetweenItsParts() {
        rule.setMonarchContent {
            MonarchFreshnessIndicator(
                updatedAt = ago(3 * HOUR),
                source = "Directory",
                details = listOf("Events on", ""),
            )
        }
        rule.onNodeWithContentDescription("Directory, Updated 3 hours ago, Events on").assertExists()
    }

    @Test
    fun theStatusIsWrittenNextToTheTime() {
        rule.setMonarchContent {
            Column {
                MonarchFreshnessIndicator(updatedAt = ago(2 * HOUR), staleAfterMillis = HOUR)
                MonarchFreshnessIndicator(
                    updatedAt = ago(10 * HOUR),
                    staleAfterMillis = HOUR,
                    expiredAfterMillis = 5 * HOUR,
                )
                MonarchFreshnessIndicator(updatedAt = null)
                MonarchFreshnessIndicator(updatedAt = ago(HOUR), status = MonarchFreshnessStatus.Checking)
                MonarchFreshnessIndicator(
                    updatedAt = ago(HOUR),
                    status = MonarchFreshnessStatus.Error,
                    error = "Sync failed",
                )
            }
        }
        rule.onNodeWithContentDescription("Updated 2 hours ago, Out of date").assertExists()
        rule.onNodeWithContentDescription("Updated 10 hours ago, Very out of date").assertExists()
        rule.onNodeWithContentDescription("Never updated").assertExists()
        rule.onNodeWithContentDescription("Checking…").assertExists()
        rule.onNodeWithContentDescription("Updated 1 hour ago, Sync failed").assertExists()
    }

    @Test
    fun theRefreshButtonCallsBackAndIsBusyWhileRefreshing() {
        var refreshes = 0
        var refreshing by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchFreshnessIndicator(updatedAt = ago(HOUR), refreshing = refreshing, onRefresh = { refreshes++ })
        }
        rule.onNodeWithText("Refresh").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, refreshes)
        refreshing = true
        assertEquals("Refreshing", rule.onNodeWithText("Refresh").stateDescription())
        rule.onNodeWithText("Refresh").performClick()
        assertEquals(1, refreshes)
    }

    @Test
    fun theEndOfARefreshIsAnnouncedPolitely() {
        var refreshing by mutableStateOf(true)
        var status by mutableStateOf<MonarchFreshnessStatus?>(null)
        rule.setMonarchContent {
            MonarchFreshnessIndicator(updatedAt = ago(HOUR), status = status, refreshing = refreshing, onRefresh = {})
        }
        rule.onNode(polite and hasContentDescription("Refreshed.")).assertDoesNotExist()
        refreshing = false
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Refreshed.")).assertExists()
        refreshing = true
        rule.waitForIdle()
        status = MonarchFreshnessStatus.Error
        refreshing = false
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Refresh failed.")).assertExists()
    }

    @Test
    fun aChangeToStaleIsAnnouncedButNotTheFirstStatus() {
        var status by mutableStateOf(MonarchFreshnessStatus.Stale)
        rule.setMonarchContent { MonarchFreshnessIndicator(updatedAt = ago(HOUR), status = status) }
        rule.onNode(polite and hasContentDescription("Out of date")).assertDoesNotExist()
        status = MonarchFreshnessStatus.Fresh
        rule.waitForIdle()
        status = MonarchFreshnessStatus.Expired
        rule.waitForIdle()
        rule.onNode(polite and hasContentDescription("Very out of date")).assertExists()
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchFreshnessIndicator(
                updatedAt = ago(2 * HOUR),
                staleAfterMillis = HOUR,
                onRefresh = {},
                labels = MonarchFreshnessIndicatorLabels(
                    updated = { "Synced $it" },
                    stale = "Old",
                    refresh = "Sync now",
                ),
            )
        }
        rule.onNodeWithContentDescription("Synced 2 hours ago, Old").assertExists()
        rule.onNodeWithText("Sync now").assertExists()
    }

    @Test
    fun theButtonFollowsTheLineInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchFreshnessIndicator(updatedAt = ago(HOUR), modifier = Modifier.width(320.dp), onRefresh = {})
        }
        rule.onNodeWithContentDescription("Updated 1 hour ago")
            .assertIsBefore(rule.onNodeWithText("Refresh"), LayoutDirection.Rtl)
    }

    @Test
    fun theButtonMovesUnderTheLineAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFreshnessIndicator(
                updatedAt = ago(HOUR),
                modifier = Modifier.width(300.dp),
                source = "Customer directory",
                onRefresh = {},
            )
        }
        rule.onNodeWithText("Customer directory", substring = true, useUnmergedTree = true).assertTextFullyVisible()
        val line = rule.onNodeWithContentDescription("Customer directory", substring = true).getUnclippedBoundsInRoot()
        assertTrue(rule.onNodeWithText("Refresh").getUnclippedBoundsInRoot().top >= line.bottom)
    }

    @Test
    fun theStatusComesFromTheAgeAndTheLimits() {
        val now = 10 * HOUR
        val t = MonarchFreshnessIndicatorTokens
        assertEquals(MonarchFreshnessStatus.Never, t.status(null, now, HOUR, 2 * HOUR))
        assertEquals(MonarchFreshnessStatus.Fresh, t.status(now - HOUR, now, HOUR, 2 * HOUR))
        assertEquals(MonarchFreshnessStatus.Stale, t.status(now - HOUR - 1, now, HOUR, 2 * HOUR))
        assertEquals(MonarchFreshnessStatus.Expired, t.status(now - 3 * HOUR, now, HOUR, 2 * HOUR))
        assertEquals(MonarchFreshnessStatus.Fresh, t.status(0, now, null, null))
        assertFalse(t.showsTime(MonarchFreshnessStatus.Checking, now))
        assertFalse(t.showsTime(MonarchFreshnessStatus.Error, null))
        assertTrue(t.showsTime(MonarchFreshnessStatus.Error, now))
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        val t = MonarchFreshnessIndicatorTokens
        assertEquals(MonarchTone.Success, t.tone(MonarchFreshnessStatus.Fresh))
        assertEquals(MonarchTone.Danger, t.tone(MonarchFreshnessStatus.Error))
        assertEquals(MonarchTone.Info, t.tone(MonarchFreshnessStatus.Checking))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.warningSolid, t.dotColor(MonarchFreshnessStatus.Stale, colors), theme.id)
            assertEquals(colors.textSubtle, t.dotColor(MonarchFreshnessStatus.Never, colors), theme.id)
            assertEquals(colors.dangerStrong, t.flagColor(MonarchFreshnessStatus.Expired, colors), theme.id)
            val banner = t.colors(MonarchFreshnessIndicatorVariant.Banner, colors)
            assertEquals(colors.bgSubtle, banner.container, theme.id)
            assertEquals(colors.borderSubtle, banner.border, theme.id)
            assertEquals(colors.textMuted, banner.content, theme.id)
            val inline = t.colors(MonarchFreshnessIndicatorVariant.Inline, colors)
            assertEquals(Color.Transparent, inline.container, theme.id)
        }
    }
}

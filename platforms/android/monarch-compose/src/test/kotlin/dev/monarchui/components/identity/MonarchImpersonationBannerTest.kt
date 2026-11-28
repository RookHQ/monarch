package dev.monarchui.components.identity

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.impersonationbanner.MonarchImpersonationBanner
import dev.monarchui.components.identity.impersonationbanner.MonarchImpersonationBannerLabels
import dev.monarchui.components.identity.impersonationbanner.MonarchImpersonationBannerTokens
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
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MinuteMillis = 60_000L

internal object MonarchImpersonationBannerFixtures : ComponentFixtures {
    override val id = "impersonation-banner"
    override val fixtures = listOf(
        fixture("name only") { MonarchImpersonationBanner(name = "Sam Lee", onStop = {}) },
        fixture("detail, reason and start with icon") {
            MonarchImpersonationBanner(
                name = "Sam Lee",
                onStop = {},
                detail = "sam@example.com",
                reason = "Ticket 4821",
                startedAtMillis = System.currentTimeMillis() - 5 * MinuteMillis,
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("stopping") { MonarchImpersonationBanner(name = "Sam Lee", onStop = {}, stopping = true) },
        fixture("long name") {
            MonarchImpersonationBanner(
                name = "Maria Fernanda de los Santos Albuquerque",
                onStop = {},
                detail = "maria.fernanda.albuquerque@example.com",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchImpersonationBannerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTextIsOneElementWithTheNameDetailAndMeta() {
        rule.setMonarchContent {
            MonarchImpersonationBanner(name = "Sam Lee", onStop = {}, detail = "sam@example.com", reason = "T-1")
        }
        rule.onNodeWithText("You are viewing as Sam Lee (sam@example.com)").assertExists()
        rule.onNodeWithText("Reason: T-1", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theStartIsShownAsARelativeTime() {
        val started = System.currentTimeMillis() - 5 * MinuteMillis
        rule.setMonarchContent { MonarchImpersonationBanner(name = "Sam", onStop = {}, startedAtMillis = started) }
        rule.onNodeWithText("Started 5 minutes ago", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theMainLineIsAnnouncedOnceWhenTheBannerAppears() {
        rule.setMonarchContent { MonarchImpersonationBanner(name = "Sam Lee", onStop = {}) }
        rule.onNodeWithContentDescription("You are viewing as Sam Lee")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun stopIsNamedWithThePersonAndRunsOnStop() {
        var stops = 0
        rule.setMonarchContent { MonarchImpersonationBanner(name = "Sam Lee", onStop = { stops++ }) }
        rule.onNodeWithContentDescription("Stop viewing as Sam Lee")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        rule.waitForIdle()
        assertEquals(1, stops)
    }

    @Test
    fun stopIsBusyWhileItRunsAndIgnoresTaps() {
        var stops = 0
        rule.setMonarchContent {
            MonarchImpersonationBanner(name = "Sam", onStop = { stops++; awaitCancellation() })
        }
        val button = rule.onNodeWithContentDescription("Stop viewing as Sam")
        button.performClick()
        rule.waitForIdle()
        assertEquals("Loading", button.stateDescription())
        button.performClick()
        rule.waitForIdle()
        assertEquals(1, stops)
    }

    @Test
    fun aFailedStopIsShownAndAnnounced() {
        rule.setMonarchContent { MonarchImpersonationBanner(name = "Sam", onStop = { throw IOException("offline") }) }
        rule.onNodeWithContentDescription("Stop viewing as Sam").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Couldn’t stop. Try again.").assertExists()
        rule.onNodeWithContentDescription("Couldn’t stop. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchImpersonationBanner(
                name = "Sam",
                onStop = {},
                labels = MonarchImpersonationBannerLabels(
                    viewingAs = { "Ves la cuenta de $it" },
                    stop = "Salir",
                    stopLabel = { "Dejar de ver como $it" },
                ),
            )
        }
        rule.onNodeWithText("Ves la cuenta de Sam").assertExists()
        rule.onNodeWithContentDescription("Dejar de ver como Sam").assertExists()
    }

    @Test
    fun theTextComesBeforeStopRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchImpersonationBanner(name = "Sam", onStop = {})
        }
        rule.onNodeWithText("You are viewing as Sam", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithContentDescription("Stop viewing as Sam"), LayoutDirection.Rtl)
    }

    @Test
    fun stopMovesUnderTheTextAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchImpersonationBanner(
                name = "Sam Lee",
                onStop = {},
                detail = "sam@example.com",
                modifier = Modifier.testTag("banner"),
            )
        }
        rule.onNodeWithText("You are viewing as Sam Lee (sam@example.com)", useUnmergedTree = true)
            .assertTextFullyVisible()
        val text = rule.onNodeWithText("You are viewing as Sam Lee (sam@example.com)", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val stop = rule.onNodeWithContentDescription("Stop viewing as Sam Lee").getUnclippedBoundsInRoot()
        assertTrue(stop.top >= text.bottom)
        rule.onNodeWithTag("banner").assertExists()
    }

    @Test
    fun theLogicOfTheLinesAndTheBusyState() {
        val t = MonarchImpersonationBannerTokens
        assertNull(t.metaLine(null, null))
        assertEquals("Started now · Reason: X", t.metaLine("Started now", "Reason: X"))
        assertTrue(t.busy(stopping = null, running = true))
        assertFalse(t.busy(stopping = false, running = true))
        assertTrue(t.stacked(LargestFontScale))
        assertFalse(t.stacked(1f))
    }

    @Test
    fun theColoursAreTheInverseRolesWithAWarningEdgeInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val colors = MonarchImpersonationBannerTokens.colors(c)
            assertEquals(c.bgInverse, colors.container, theme.id)
            assertEquals(c.textInverse, colors.text, theme.id)
            assertEquals(c.textInverseMuted, colors.muted, theme.id)
            assertEquals(c.warningSolid, colors.edge, theme.id)
            assertEquals(c.dangerStrongInverse, colors.error, theme.id)
        }
    }
}

package com.rookhq.monarch.components.technical

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrame
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrameLabels
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrameState
import com.rookhq.monarch.components.technical.embeddedappframe.MonarchEmbeddedAppFrameTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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

internal object MonarchEmbeddedAppFrameFixtures : ComponentFixtures {
    override val id = "embedded-app-frame"
    override val fixtures = listOf(
        fixture("ready") {
            MonarchEmbeddedAppFrame("Example Desk: Email notifications", MonarchEmbeddedAppFrameState.Ready) {
                MonarchText("Choose which updates reach your inbox.", Modifier.padding(20.dp))
            }
        },
        fixture("error with both actions") {
            MonarchEmbeddedAppFrame(
                "Example Desk: Billing",
                MonarchEmbeddedAppFrameState.Error,
                onRetry = {},
                onOpen = {},
            ) {}
        },
        fixture("error, fixed height, no actions") {
            MonarchEmbeddedAppFrame("Example Desk", MonarchEmbeddedAppFrameState.Error, height = 220.dp) {}
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEmbeddedAppFrameTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theFrameIsAPaneNamedByItsTitleAndShowsTheContentWhenReady() {
        rule.setMonarchContent {
            MonarchEmbeddedAppFrame(
                "Example Desk: Email notifications",
                MonarchEmbeddedAppFrameState.Ready,
            ) { MonarchText("Mentions") }
        }
        rule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Example Desk: Email notifications"),
        ).assertExists()
        rule.onNodeWithText("Mentions").assertExists()
    }

    @Test
    fun loadingShowsTheSpinnerAndNotTheContent() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchEmbeddedAppFrame(
            "Desk",
            MonarchEmbeddedAppFrameState.Loading,
        ) { MonarchText("Mentions") } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading…", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Mentions").assertDoesNotExist()
    }

    @Test
    fun theErrorOffersRetryAndOpenAndIsAnnounced() {
        var retried = 0
        var opened = 0
        rule.setMonarchContent {
            MonarchEmbeddedAppFrame(
                "Desk",
                MonarchEmbeddedAppFrameState.Error,
                onRetry = { retried++ },
                onOpen = { opened++ },
                labels = MonarchEmbeddedAppFrameLabels(open = "Open in Example Desk"),
            ) {}
        }
        rule.onNodeWithText("This page didn’t open", substring = true).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                androidx.compose.ui.semantics.LiveRegionMode.Polite,
            ),
        )
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Open in Example Desk").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retried)
        assertEquals(1, opened)
    }

    @Test
    fun theErrorTitleIsAHeadingWithTheDefaultOpenLabel() {
        rule.setMonarchContent { MonarchEmbeddedAppFrame("Desk", MonarchEmbeddedAppFrameState.Error, onOpen = {}) {} }
        rule.onNodeWithText("Open the page").assertExists()
        rule.onNodeWithText("This page didn’t open", useUnmergedTree = true).assert(isHeading())
    }

    @Test
    fun retryComesBeforeOpenInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchEmbeddedAppFrame("Desk", MonarchEmbeddedAppFrameState.Error, onRetry = {}, onOpen = {}) {}
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Try again").assertIsBefore(rule.onNodeWithText("Open the page"), direction)
        }
    }

    @Test
    fun theErrorWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEmbeddedAppFrame("Desk", MonarchEmbeddedAppFrameState.Error) {}
        }
        rule.onNodeWithText("This page didn’t open", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theFrameUsesTheSurfaceAndDefaultBorderInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgSurface, MonarchEmbeddedAppFrameTokens.fill(colors), theme.id)
            assertEquals(colors.borderDefault, MonarchEmbeddedAppFrameTokens.border(colors), theme.id)
        }
    }
}

package dev.monarchui.components.feedback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.feedback.errorstate.MonarchErrorState
import dev.monarchui.components.feedback.errorstate.MonarchErrorStateKind
import dev.monarchui.components.feedback.errorstate.MonarchErrorStateTokens
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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

internal object MonarchErrorStateFixtures : ComponentFixtures {
    override val id = "error-state"
    override val fixtures = MonarchErrorStateKind.entries.map { kind ->
        fixture("${kind.name}, icon, retry and request id") {
            MonarchErrorState(
                title = "Invoices did not load",
                kind = kind,
                message = "The billing service did not answer.",
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
                requestId = "req_8f2a91",
                onRetry = {},
            )
        }
    } + MonarchEmptyStateSize.entries.map { size ->
        fixture("${size.name}, retrying with an extra action") {
            MonarchErrorState(
                title = "Invoices did not load",
                size = size,
                retrying = true,
                onRetry = {},
                actions = { MonarchButton(text = "Status page", onClick = {}) },
            )
        }
    } + listOf(
        fixture("long text") {
            MonarchErrorState(
                title = "The invoices of the last quarter could not be loaded",
                message = "The billing service did not answer in time. Try again, or contact support with the id.",
                requestId = "req_8f2a91c4b7e0d3",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchErrorStateTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndRetryCallsBack() {
        var retries = 0
        rule.setMonarchContent { MonarchErrorState(title = "Invoices did not load", onRetry = { retries++ }) }
        rule.onNodeWithText("Invoices did not load").assert(isHeading())
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun forbiddenAndNotFoundOfferNoRetry() {
        var kind by mutableStateOf(MonarchErrorStateKind.Forbidden)
        rule.setMonarchContent { MonarchErrorState(title = "Unavailable", kind = kind, onRetry = {}) }
        rule.onNodeWithText("Try again").assertDoesNotExist()
        kind = MonarchErrorStateKind.NotFound
        rule.onNodeWithText("Try again").assertDoesNotExist()
        kind = MonarchErrorStateKind.Maintenance
        rule.onNodeWithText("Try again").assertExists()
    }

    @Test
    fun whileRetryingTheButtonIgnoresTapsAndSaysRetrying() {
        var retries = 0
        rule.setMonarchContent {
            MonarchErrorState(title = "Did not load", retrying = true, onRetry = { retries++ })
        }
        val button = rule.onNodeWithText("Try again")
        assertEquals("Retrying", button.stateDescription())
        button.performClick()
        assertEquals(0, retries)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchErrorState(
                title = "Did not load",
                retrying = true,
                requestId = "req_1",
                onRetry = {},
                retryLabel = "Reload",
                retryingLabel = "Reloading",
                requestIdLabel = "Reference",
            )
        }
        assertEquals("Reloading", rule.onNodeWithText("Reload").stateDescription())
        rule.onNodeWithText("Reference: req_1").assertExists()
    }

    @Test
    fun theRequestIdIsOneElementWithItsLabel() {
        rule.setMonarchContent { MonarchErrorState(title = "Did not load", requestId = "req_8f2a91") }
        rule.onNodeWithText("Request ID: req_8f2a91").assertHasNoClickAction()
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchErrorState(
                title = "The invoices could not be loaded",
                message = "The billing service did not answer.",
                requestId = "req_8f2a91",
            )
        }
        rule.onNodeWithText("The invoices", substring = true).assertTextFullyVisible()
        rule.onNodeWithText("Request ID", substring = true).assertTextFullyVisible()
    }

    @Test
    fun retryIsOfferedOnlyWhereItCanHelp() {
        assertTrue(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.Error))
        assertTrue(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.Offline))
        assertTrue(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.Server))
        assertTrue(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.Maintenance))
        assertFalse(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.Forbidden))
        assertFalse(MonarchErrorStateTokens.offersRetry(MonarchErrorStateKind.NotFound))
    }

    @Test
    fun theTileColourFollowsTheKindInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val error = MonarchErrorStateTokens.tileColors(MonarchErrorStateKind.Error, colors)
            assertEquals(colors.dangerSubtle, error.container, theme.id)
            assertEquals(colors.dangerStrong, error.content, theme.id)
            val offline = MonarchErrorStateTokens.tileColors(MonarchErrorStateKind.Offline, colors)
            assertEquals(colors.warningSubtle, offline.container, theme.id)
            val forbidden = MonarchErrorStateTokens.tileColors(MonarchErrorStateKind.Forbidden, colors)
            assertEquals(colors.bgMuted, forbidden.container, theme.id)
            assertEquals(colors.textMuted, forbidden.content, theme.id)
        }
    }
}

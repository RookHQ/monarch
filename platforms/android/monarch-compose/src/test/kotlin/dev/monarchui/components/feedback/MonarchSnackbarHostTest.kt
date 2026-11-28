package dev.monarchui.components.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.feedback.snackbarhost.MonarchSnackbarHost
import dev.monarchui.components.feedback.snackbarhost.MonarchSnackbarHostTokens
import dev.monarchui.components.feedback.snackbarhost.MonarchSnackbarVisuals
import dev.monarchui.components.feedback.snackbarhost.showMonarchSnackbar
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A host that shows one snackbar in its first frame and records the result.
 * The unconfined start sets the current snackbar before the host composes,
 * so the matrix test, which renders one frame per setting, sees it.
 */
@Composable
private fun ShowingHost(visuals: MonarchSnackbarVisuals, onResult: (SnackbarResult) -> Unit = {}) {
    val state = remember {
        SnackbarHostState().also { state ->
            CoroutineScope(Dispatchers.Unconfined).launch { onResult(state.showSnackbar(visuals)) }
        }
    }
    MonarchSnackbarHost(hostState = state)
}

internal object MonarchSnackbarHostFixtures : ComponentFixtures {
    override val id = "toast"
    override val fixtures = MonarchTone.entries.map { tone ->
        fixture("${tone.name}, action and dismiss") {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "Ticket archived.",
                    tone = tone,
                    actionLabel = "Undo",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                ),
            )
        }
    } + listOf(
        fixture("long text") {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "The export of all invoices of the last quarter is ready in the downloads folder.",
                    actionLabel = "Open folder",
                    duration = SnackbarDuration.Indefinite,
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchSnackbarHostTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theActionReturnsActionPerformed() {
        var result: SnackbarResult? = null
        rule.setMonarchContent {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "Ticket archived.",
                    tone = MonarchTone.Success,
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Indefinite,
                ),
            ) { result = it }
        }
        rule.onNodeWithText("Undo").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(SnackbarResult.ActionPerformed, result)
    }

    @Test
    fun theDismissActionReturnsDismissed() {
        var result: SnackbarResult? = null
        rule.setMonarchContent {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "Could not sync.",
                    tone = MonarchTone.Danger,
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite,
                ),
            ) { result = it }
        }
        rule.onNodeWithContentDescription("Dismiss").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertEquals(SnackbarResult.Dismissed, result)
    }

    @Test
    fun theMessageIsInAPoliteLiveRegion() {
        rule.setMonarchContent {
            ShowingHost(MonarchSnackbarVisuals(message = "Saved.", duration = SnackbarDuration.Indefinite))
        }
        val liveRegion = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(hasText("Saved.") and hasAnyAncestor(liveRegion), useUnmergedTree = true).assertExists()
    }

    @Test
    fun showMonarchSnackbarShowsTheMessageWithItsTone() {
        var shown: SnackbarVisuals? = null
        rule.setMonarchContent {
            val state = remember { SnackbarHostState() }
            LaunchedEffect(state) {
                state.showMonarchSnackbar(
                    message = "Payment failed.",
                    tone = MonarchTone.Danger,
                    duration = SnackbarDuration.Indefinite,
                )
            }
            MonarchSnackbarHost(hostState = state)
            shown = state.currentSnackbarData?.visuals
        }
        rule.onNodeWithText("Payment failed.").assertExists()
        assertEquals(MonarchTone.Danger, (shown as MonarchSnackbarVisuals).tone)
        assertEquals(SnackbarDuration.Indefinite, shown?.duration)
    }

    @Test
    fun theTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "The export of the invoices is ready.",
                    actionLabel = "Open",
                    duration = SnackbarDuration.Indefinite,
                ),
            )
        }
        rule.onNodeWithText("The export of the invoices is ready.", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aDangerToneOrAnActionKeepsTheSnackbarLonger() {
        assertEquals(SnackbarDuration.Short, MonarchSnackbarHostTokens.defaultDuration(MonarchTone.Success, null))
        assertEquals(SnackbarDuration.Long, MonarchSnackbarHostTokens.defaultDuration(MonarchTone.Danger, null))
        assertEquals(SnackbarDuration.Long, MonarchSnackbarHostTokens.defaultDuration(MonarchTone.Neutral, "Undo"))
        assertEquals(SnackbarDuration.Long, MonarchSnackbarVisuals("Undo?", actionLabel = "Undo").duration)
    }

    @Test
    fun everyToneIsTheRaisedSurfaceWithTheToneOnlyOnTheIcon() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            MonarchTone.entries.forEach { tone ->
                val snackbar = MonarchSnackbarHostTokens.colors(tone, colors)
                assertEquals(colors.bgRaised, snackbar.container, "${theme.id} $tone")
                assertEquals(colors.textDefault, snackbar.content, "${theme.id} $tone")
                assertEquals(colors.borderSubtle, snackbar.border, "${theme.id} $tone")
            }
            assertEquals(colors.dangerStrong, MonarchSnackbarHostTokens.colors(MonarchTone.Danger, colors).icon)
            assertEquals(colors.successStrong, MonarchSnackbarHostTokens.colors(MonarchTone.Success, colors).icon)
            assertEquals(colors.textMuted, MonarchSnackbarHostTokens.colors(MonarchTone.Neutral, colors).icon)
            assertEquals(
                colors.borderStrong,
                MonarchSnackbarHostTokens.colors(MonarchTone.Neutral, colors, highContrast = true).border,
            )
        }
    }

    @Test
    fun theOwnIconIsDecorativeAndTheMessageCarriesTheMeaning() {
        rule.setMonarchContent {
            ShowingHost(
                MonarchSnackbarVisuals(
                    message = "Invoice 1042 sent.",
                    tone = MonarchTone.Success,
                    duration = SnackbarDuration.Indefinite,
                    icon = Icons.AutoMirrored.Outlined.Send,
                ),
            )
        }
        rule.onNodeWithText("Invoice 1042 sent.").assertExists()
        rule.onAllNodes(hasContentDescription("Send", substring = true), useUnmergedTree = true)
            .assertCountEquals(0)
    }

    @Test
    fun theIconIsPartOfTheVisualsAndOfEquality() {
        val a = MonarchSnackbarVisuals("Sent.", icon = Icons.AutoMirrored.Outlined.Send)
        assertEquals(a, MonarchSnackbarVisuals("Sent.", icon = Icons.AutoMirrored.Outlined.Send))
        assertNotEquals(a, MonarchSnackbarVisuals("Sent."))
        assertEquals(a.hashCode(), MonarchSnackbarVisuals("Sent.", icon = Icons.AutoMirrored.Outlined.Send).hashCode())
    }
}

package dev.monarchui.components.feedback

import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.feedback.celebration.MonarchCelebration
import dev.monarchui.components.feedback.celebration.MonarchCelebrationDecoration
import dev.monarchui.components.feedback.celebration.MonarchCelebrationTokens
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

internal object MonarchCelebrationFixtures : ComponentFixtures {
    override val id = "celebration"
    override val fixtures = listOf(
        fixture("success with an icon and an action") {
            MonarchCelebration(
                title = "Workspace ready",
                message = "Invite your team.",
                icon = { MonarchIcon(TestIcon, contentDescription = null) },
            ) {
                MonarchButton(text = "Invite people", onClick = {}, variant = MonarchButtonVariant.Primary)
            }
        },
        fixture("every tone without decoration") {
            Row {
                MonarchTone.entries.forEach { tone ->
                    MonarchCelebration(
                        title = tone.name,
                        modifier = Modifier.weight(1f),
                        tone = tone,
                        decoration = MonarchCelebrationDecoration.None,
                        announces = false,
                        icon = { MonarchIcon(TestIcon, contentDescription = null) },
                    )
                }
            }
        },
        fixture("no icon, no message") { MonarchCelebration(title = "Done") },
        fixture("long text") {
            MonarchCelebration(
                title = "Your workspace and its eleven projects were moved",
                message = "Everyone keeps their access and the old links forward to the new place.",
                decoration = MonarchCelebrationDecoration.None,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchCelebrationTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingAndTheVisualIsHidden() {
        rule.setMonarchContent {
            MonarchCelebration(
                title = "Domain verified",
                message = "Email is now signed.",
                icon = { MonarchIcon(TestIcon, contentDescription = "Tick") },
            )
        }
        rule.onNodeWithText("Domain verified").assert(isHeading())
        rule.onNodeWithText("Email is now signed.").assertExists()
        rule.onNodeWithContentDescription("Tick").assertDoesNotExist()
    }

    @Test
    fun theTitleAndMessageAreAnnouncedWhenItAppears() {
        rule.setMonarchContent { MonarchCelebration(title = "Domain verified", message = "Email is now signed.") }
        rule.onNode(polite and hasContentDescription("Domain verified. Email is now signed.")).assertExists()
    }

    @Test
    fun announcingCanBeTurnedOff() {
        rule.setMonarchContent { MonarchCelebration(title = "Domain verified", announces = false) }
        rule.onNode(polite and hasContentDescription("Domain verified")).assertDoesNotExist()
    }

    @Test
    fun theActionsCanBeUsedAndHaveFullTargets() {
        var continued = 0
        rule.setMonarchContent {
            MonarchCelebration(title = "Ready") {
                MonarchButton(text = "Continue", onClick = { continued++ })
            }
        }
        rule.onNodeWithText("Continue").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, continued)
    }

    @Test
    fun theConfettiFallsOnceThenStops() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchCelebration(title = "Ready") }
        rule.mainClock.advanceTimeBy(MonarchCelebrationTokens.TOTAL_MILLIS + 100L)
        rule.onNodeWithText("Ready").assertExists()
        val t = MonarchCelebrationTokens
        val confetti = MonarchCelebrationDecoration.Confetti
        assertTrue(t.confettiVisible(confetti, 0, reducedMotion = false))
        assertFalse(t.confettiVisible(confetti, t.TOTAL_MILLIS.toLong(), reducedMotion = false))
        assertTrue(t.confettiVisible(confetti, t.TOTAL_MILLIS.toLong(), reducedMotion = true))
        assertFalse(t.confettiVisible(MonarchCelebrationDecoration.None, 0, reducedMotion = true))
    }

    @Test
    fun aPieceFadesInFallsAndFadesOut() {
        val t = MonarchCelebrationTokens
        val piece = t.Pieces[1]
        assertEquals(0f, t.frame(piece, 0, reducedMotion = false).opacity)
        val start = t.frame(piece, piece.delayMillis.toLong(), reducedMotion = false)
        assertEquals(0f, start.opacity)
        assertEquals(t.START_OFFSET, start.dy)
        val middle = t.frame(piece, piece.delayMillis + t.FALL_MILLIS / 2L, reducedMotion = false)
        assertEquals(1f, middle.opacity)
        val end = t.frame(piece, piece.delayMillis + t.FALL_MILLIS.toLong(), reducedMotion = false)
        assertEquals(0f, end.opacity)
        assertEquals(t.FALL_DISTANCE, end.dy)
        assertEquals(piece.drift, end.dx)
        assertEquals(piece.rotation + 180f, end.rotation)
        assertEquals(12, t.Pieces.size)
        assertTrue(t.Pieces.all { it.delayMillis + t.FALL_MILLIS <= t.TOTAL_MILLIS })
    }

    @Test
    fun withoutMotionThePiecesRestStill() {
        val t = MonarchCelebrationTokens
        val piece = t.Pieces[3]
        val early = t.frame(piece, 0, reducedMotion = true)
        val late = t.frame(piece, 5_000, reducedMotion = true)
        assertEquals(early, late)
        assertEquals(t.REST_OPACITY, early.opacity)
        assertEquals(piece.drift, early.dx)
    }

    @Test
    fun longTextWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchCelebration(
                title = "Your workspace was moved",
                message = "Old links forward to the new place.",
                decoration = MonarchCelebrationDecoration.None,
            )
        }
        rule.onNodeWithText("Your workspace was moved").assertTextFullyVisible()
        rule.onNodeWithText("Old links forward to the new place.").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        val t = MonarchCelebrationTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.successSubtle, t.tile(MonarchTone.Success, colors).container, theme.id)
            assertEquals(colors.successStrong, t.tile(MonarchTone.Success, colors).content, theme.id)
            assertEquals(colors.infoSubtle, t.tile(MonarchTone.Info, colors).container, theme.id)
            assertEquals(colors.textDefault, t.titleColor(colors), theme.id)
            assertEquals(colors.textMuted, t.messageColor(colors), theme.id)
            assertEquals(
                listOf(colors.data1, colors.data2, colors.data3, colors.data4, colors.data5),
                (1..5).map { t.pieceColor(it, colors) },
                theme.id,
            )
        }
    }
}

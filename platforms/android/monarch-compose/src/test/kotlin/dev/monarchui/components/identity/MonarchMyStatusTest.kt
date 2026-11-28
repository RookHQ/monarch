package dev.monarchui.components.identity

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.mystatus.MonarchMyStatus
import dev.monarchui.components.identity.mystatus.MonarchMyStatusCustomStatus
import dev.monarchui.components.identity.mystatus.MonarchMyStatusLabels
import dev.monarchui.components.identity.mystatus.MonarchMyStatusLineKind
import dev.monarchui.components.identity.mystatus.MonarchMyStatusRow
import dev.monarchui.components.identity.mystatus.MonarchMyStatusTokens
import dev.monarchui.components.identity.presencedot.MonarchPresenceStatus
import dev.monarchui.foundation.MonarchSpacing
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Now = 1_700_000_000_000L
private const val Minute = 60_000L
private val Holiday = MonarchMyStatusCustomStatus(text = "On holiday", emoji = "🌴")

internal object MonarchMyStatusFixtures : ComponentFixtures {
    override val id = "my-status"
    override val fixtures = listOf(
        fixture("custom status") {
            MonarchMyStatus(
                "Priya Raman",
                MonarchPresenceStatus.Online,
                onClick = {},
                customStatus = Holiday,
                now = Now,
            )
        },
        fixture("presence only and no status") {
            Column {
                MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Busy, onClick = {})
                MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Offline, onClick = {})
            }
        },
        fixture("long name and status, disabled") {
            MonarchMyStatus(
                name = "Priya Raman-Venkataraman of the Northern Operations Team",
                presence = MonarchPresenceStatus.Away,
                onClick = {},
                customStatus = MonarchMyStatusCustomStatus(text = "Travelling to the quarterly planning offsite"),
                now = Now,
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMyStatusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theRowIsOneButtonNamedByThePersonWithPresenceAndStatus() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Online, onClick = { clicks++ }, customStatus = Holiday)
        }
        val row = rule.onNodeWithContentDescription("Priya Raman")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        assertEquals("Online, 🌴 On holiday", row.stateDescription())
        assertEquals(
            "open your status and settings",
            row.fetchSemanticsNode().config[SemanticsActions.OnClick].label,
        )
        row.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun presenceIsWrittenWhenThereIsNoStatus() {
        rule.setMonarchContent { MonarchMyStatus("Priya Raman", MonarchPresenceStatus.DoNotDisturb, onClick = {}) }
        rule.onNodeWithText("Do not disturb", useUnmergedTree = true).assertExists()
        assertEquals("Do not disturb", rule.onNodeWithContentDescription("Priya Raman").stateDescription())
    }

    @Test
    fun onlineWithoutAStatusInvitesToSetOne() {
        rule.setMonarchContent { MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Online, onClick = {}) }
        rule.onNodeWithText("Set a status", useUnmergedTree = true).assertExists()
        assertEquals("Online, Set a status", rule.onNodeWithContentDescription("Priya Raman").stateDescription())
    }

    @Test
    fun aFixedNowPastUntilHidesTheStatus() {
        rule.setMonarchContent {
            MonarchMyStatus(
                "Priya Raman",
                MonarchPresenceStatus.Online,
                onClick = {},
                customStatus = Holiday.copy(until = Now - 1),
                now = Now,
            )
        }
        rule.onNodeWithText("Set a status", useUnmergedTree = true).assertExists()
    }

    @Test
    fun withoutNowTheStatusEndsAtItsUntilTime() {
        var clock = Now
        rule.setMonarchContent {
            MonarchMyStatusRow(
                name = "Priya Raman",
                presence = MonarchPresenceStatus.Online,
                onClick = {},
                modifier = androidx.compose.ui.Modifier,
                image = null,
                customStatus = Holiday.copy(until = Now + Minute),
                now = null,
                enabled = true,
                labels = MonarchMyStatusLabels(),
                interactionSource = null,
                clock = { clock },
            )
        }
        rule.onNodeWithText("🌴 On holiday", useUnmergedTree = true).assertExists()
        clock = Now + Minute
        rule.mainClock.advanceTimeBy(Minute + 1)
        rule.waitForIdle()
        rule.onNodeWithText("Set a status", useUnmergedTree = true).assertExists()
    }

    @Test
    fun disabledRowDoesNotOpen() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Online, onClick = { clicks++ }, enabled = false)
        }
        rule.onNodeWithContentDescription("Priya Raman").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchMyStatus(
                "Priya Raman",
                MonarchPresenceStatus.Away,
                onClick = {},
                labels = MonarchMyStatusLabels(presence = { "Absente" }, setStatus = "Définir un statut"),
            )
        }
        rule.onNodeWithText("Absente", useUnmergedTree = true).assertExists()
    }

    @Test
    fun avatarComesBeforeTheTextInBothDirectionsAndTextWraps() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchMyStatus("Priya Raman", MonarchPresenceStatus.Online, onClick = {}, customStatus = Holiday)
        }
        rule.onNodeWithText("Priya Raman", useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("🌴 On holiday", useUnmergedTree = true).assertTextFullyVisible()
        // Right to left the avatar is at the right, so the name ends at least an avatar's width before the row.
        val row = rule.onNodeWithContentDescription("Priya Raman").fetchSemanticsNode().boundsInRoot
        val name = rule.onNodeWithText("Priya Raman", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val avatar = with(rule.density) { MonarchSpacing.Default.space6.toPx() * LargestFontScale }
        assertTrue(name.right <= row.right - avatar)
    }

    @Test
    fun logicIgnoresBlankAndExpiredStatusesAndColoursTheLine() {
        val tokens = MonarchMyStatusTokens
        assertNull(tokens.activeStatus(MonarchMyStatusCustomStatus(text = "  "), Now))
        assertNull(tokens.activeStatus(Holiday.copy(until = Now), Now))
        assertEquals(Holiday, tokens.activeStatus(Holiday.copy(until = null), Now))
        val emojiOnly =
            tokens.line(MonarchMyStatusCustomStatus(emoji = "🎧"), MonarchPresenceStatus.Online, "Online", "Set")
        assertEquals("🎧", emojiOnly.text)
        assertEquals(Minute, tokens.waitMillis(Holiday.copy(until = Now + Minute), Now))
        assertNull(tokens.waitMillis(Holiday.copy(until = Now - 1), Now))
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textSubtle, tokens.lineColor(MonarchMyStatusLineKind.Invitation, colors))
            assertEquals(colors.textMuted, tokens.lineColor(MonarchMyStatusLineKind.Custom, colors))
        }
    }
}

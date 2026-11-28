package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncher
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncherBadge
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncherTokens
import dev.monarchui.components.intelligence.ailauncher.MonarchAiLauncherVariant
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val glyph: @Composable () -> Unit = { Text("AI") }

internal object MonarchAiLauncherFixtures : ComponentFixtures {
    override val id = "ai-launcher"
    override val fixtures = listOf(
        fixture("solid") { MonarchAiLauncher(open = false, onOpenChange = {}, icon = glyph) },
        fixture("solid, open, with label") {
            MonarchAiLauncher(open = true, onOpenChange = {}, icon = glyph, showLabel = true, closeIcon = { Text("X") })
        },
        fixture("unread and count") {
            Column {
                MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Unread)
                MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Count(120))
            }
        },
        fixture("working") { MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Working) },
        fixture("subtle") { MonarchAiLauncher(false, {}, glyph, variant = MonarchAiLauncherVariant.Subtle) },
        fixture("subtle, open") { MonarchAiLauncher(true, {}, glyph, variant = MonarchAiLauncherVariant.Subtle) },
        fixture("disabled") {
            Column {
                MonarchAiLauncher(false, {}, glyph, enabled = false)
                MonarchAiLauncher(false, {}, glyph, variant = MonarchAiLauncherVariant.Subtle, enabled = false)
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiLauncherTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLauncherIsAButtonNamedAssistantThatTogglesOpen() {
        var open by mutableStateOf(false)
        rule.setMonarchContent { MonarchAiLauncher(open = open, onOpenChange = { open = it }, icon = glyph) }
        val button = rule.onNodeWithContentDescription("Assistant")
        button.assertHasClickAction()
        assertNull(button.stateDescription())
        button.performClick()
        assertTrue(open)
        assertEquals("Open", rule.onNodeWithContentDescription("Assistant").stateDescription())
        rule.onNodeWithContentDescription("Assistant").performClick()
        assertFalse(open)
    }

    @Test
    fun theBadgeIsReadAfterTheOpenState() {
        var badge: MonarchAiLauncherBadge? by mutableStateOf(MonarchAiLauncherBadge.Unread)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiLauncher(open = true, onOpenChange = {}, icon = glyph, badge = badge) }
        fun spoken(next: MonarchAiLauncherBadge): String? {
            badge = next
            Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            return rule.onNodeWithContentDescription("Assistant").stateDescription()
        }
        assertEquals("Open, New messages", spoken(MonarchAiLauncherBadge.Unread))
        assertEquals("Open, Working", spoken(MonarchAiLauncherBadge.Working))
        assertEquals("Open, 1 unread message", spoken(MonarchAiLauncherBadge.Count(1)))
        assertEquals("Open, 3 unread messages", spoken(MonarchAiLauncherBadge.Count(3)))
    }

    @Test
    fun theCountIsShownUpTo99() {
        rule.setMonarchContent {
            Column {
                MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Count(7))
                MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Count(150), label = "Other")
            }
        }
        rule.onNodeWithText("7", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("99+", useUnmergedTree = true).assertExists()
        assertNull(MonarchAiLauncherTokens.countText(0))
        assertEquals("99", MonarchAiLauncherTokens.countText(99))
    }

    @Test
    fun theSolidVariantShowsTheCloseIconWhileOpen() {
        var open by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchAiLauncher(open = open, onOpenChange = { open = it }, icon = glyph, closeIcon = { Text("X") })
        }
        rule.onNodeWithText("AI", useUnmergedTree = true).assertExists()
        open = true
        rule.onNodeWithText("X", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("AI", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theLabelShowsForTheSubtleVariantByDefault() {
        assertTrue(MonarchAiLauncherTokens.showsLabel(MonarchAiLauncherVariant.Subtle, null))
        assertFalse(MonarchAiLauncherTokens.showsLabel(MonarchAiLauncherVariant.Solid, null))
        assertTrue(MonarchAiLauncherTokens.showsLabel(MonarchAiLauncherVariant.Solid, true))
        rule.setMonarchContent { MonarchAiLauncher(false, {}, glyph, variant = MonarchAiLauncherVariant.Subtle) }
        rule.onNodeWithText("Assistant", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aDisabledLauncherDoesNotToggle() {
        var changes = 0
        rule.setMonarchContent {
            Column {
                MonarchAiLauncher(false, { changes++ }, glyph, enabled = false)
                MonarchAiLauncher(
                    false,
                    { changes++ },
                    glyph,
                    variant = MonarchAiLauncherVariant.Subtle,
                    enabled = false,
                    label = "Subtle",
                )
            }
        }
        rule.onNodeWithContentDescription("Assistant").assertIsNotEnabled().performClick()
        rule.onNodeWithContentDescription("Subtle").assertIsNotEnabled().performClick()
        assertEquals(0, changes)
    }

    @Test
    fun everyStringCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiLauncher(
                open = true,
                onOpenChange = {},
                icon = glyph,
                badge = MonarchAiLauncherBadge.Count(2),
                label = "Assistant IA",
                badgeLabel = "2 nouveaux messages",
                openLabel = "Ouvert",
            )
        }
        assertEquals(
            "Ouvert, 2 nouveaux messages",
            rule.onNodeWithContentDescription("Assistant IA").stateDescription(),
        )
    }

    @Test
    fun bothVariantsHaveA48dpTarget() {
        rule.setMonarchContent {
            Column {
                MonarchAiLauncher(false, {}, glyph, label = "Solid")
                MonarchAiLauncher(false, {}, glyph, variant = MonarchAiLauncherVariant.Subtle, label = "Subtle")
            }
        }
        rule.onNodeWithContentDescription("Solid").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithContentDescription("Subtle").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theBadgeSitsAtTheEndCornerInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchAiLauncher(false, {}, glyph, badge = MonarchAiLauncherBadge.Count(4))
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val button = rule.onNodeWithContentDescription("Assistant").fetchSemanticsNode().boundsInRoot
            val count = rule.onNodeWithText("4", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val atEnd = if (direction == LayoutDirection.Ltr) {
                count.center.x > button.center.x
            } else {
                count.center.x < button.center.x
            }
            assertTrue(atEnd, "$direction: the count is at the end corner")
        }
    }

    @Test
    fun theSubtleLabelIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiLauncher(false, {}, glyph, variant = MonarchAiLauncherVariant.Subtle)
        }
        rule.onNodeWithText("Assistant", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theColoursOfEachVariantInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val solid = MonarchAiLauncherTokens.colors(MonarchAiLauncherVariant.Solid, false, true, false, colors)
            val pressed = MonarchAiLauncherTokens.colors(MonarchAiLauncherVariant.Solid, false, true, true, colors)
            val subtle = MonarchAiLauncherTokens.colors(MonarchAiLauncherVariant.Subtle, false, true, false, colors)
            val subtleOpen = MonarchAiLauncherTokens.colors(MonarchAiLauncherVariant.Subtle, true, true, false, colors)
            val disabled = MonarchAiLauncherTokens.colors(MonarchAiLauncherVariant.Solid, false, false, false, colors)
            assertEquals(colors.aiSolid, solid.container, theme.id)
            assertEquals(colors.aiOnSolid, solid.content, theme.id)
            assertEquals(colors.aiSolidActive, pressed.container, theme.id)
            assertEquals(colors.bgSurface, subtle.container, theme.id)
            assertEquals(colors.borderDefault, subtle.border, theme.id)
            assertEquals(colors.aiSolid, subtleOpen.border, theme.id)
            assertEquals(colors.textDisabled, disabled.content, theme.id)
            assertEquals(colors.aiThinking, MonarchAiLauncherTokens.ringColor(colors), theme.id)
        }
    }

    @Test
    fun theSpokenStateJoinsOpenAndBadge() {
        assertNull(MonarchAiLauncherTokens.stateDescription(false, "Open", null))
        assertEquals("Open", MonarchAiLauncherTokens.stateDescription(true, "Open", null))
        assertEquals("Working", MonarchAiLauncherTokens.stateDescription(false, "Open", "Working"))
    }
}

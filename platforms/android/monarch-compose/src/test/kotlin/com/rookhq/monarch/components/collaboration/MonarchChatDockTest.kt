package com.rookhq.monarch.components.collaboration

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.collaboration.chatdock.MonarchChatDock
import com.rookhq.monarch.components.collaboration.chatdock.MonarchChatDockTokens
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertIsBefore
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

internal object MonarchChatDockFixtures : ComponentFixtures {
    override val id = "chat-dock"
    override val fixtures = listOf(
        fixture("three unread") { MonarchChatDock(
            open = false,
            onOpenChange = {},
            unread = 3,
        ) { MonarchText("Chat") } },
        fixture("no unread") { MonarchChatDock(open = false, onOpenChange = {}) { MonarchText("Chat") } },
        fixture("many unread") { MonarchChatDock(
            open = false,
            onOpenChange = {},
            unread = 120,
            label = "Team chat",
        ) { } },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchChatDockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theLauncherReadsItsLabelAndUnreadCountAndOpensTheSheet() {
        rule.setMonarchContent {
            var open by remember { mutableStateOf(false) }
            MonarchChatDock(open, { open = it }, unread = 3) { MonarchText("Design review") }
        }
        rule.onNodeWithText("Design review").assertDoesNotExist()
        rule.onNodeWithContentDescription("Rook Chat, 3 unread").assertHasClickAction()
            .assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Design review").assertExists()
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Rook Chat")).assertExists()
    }

    @Test
    fun withoutUnreadTheLauncherIsItsLabel() {
        rule.setMonarchContent { MonarchChatDock(false, {}, label = "Team chat") {} }
        rule.onNodeWithContentDescription("Team chat").assertExists()
    }

    @Test
    fun loadingShowsTheLoadingTextInsteadOfTheContent() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchChatDock(true, {}, loading = true) { MonarchText("Design review") } }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Loading chat…", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Design review").assertDoesNotExist()
    }

    @Test
    fun theBadgeTextCapsAt99() {
        assertNull(MonarchChatDockTokens.badgeText(0))
        assertNull(MonarchChatDockTokens.badgeText(-2))
        assertEquals("3", MonarchChatDockTokens.badgeText(3))
        assertEquals("99+", MonarchChatDockTokens.badgeText(120))
    }

    @Test
    fun launchersFollowTheLayoutDirection() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            Row {
                MonarchChatDock(false, {}, label = "First") {}
                MonarchChatDock(false, {}, label = "Second") {}
            }
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithContentDescription("First").assertIsBefore(
                rule.onNodeWithContentDescription("Second"),
                direction,
            )
        }
    }

    @Test
    fun theLauncherIsTheInverseSurfaceInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgInverse, MonarchChatDockTokens.launcherFill(colors), theme.id)
            assertEquals(colors.textInverse, MonarchChatDockTokens.launcherContent(colors), theme.id)
            assertEquals(colors.dangerSolid, MonarchChatDockTokens.badgeFill(colors), theme.id)
        }
    }
}

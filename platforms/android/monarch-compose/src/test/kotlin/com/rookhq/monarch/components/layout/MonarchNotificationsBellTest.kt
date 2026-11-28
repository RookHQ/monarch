package com.rookhq.monarch.components.layout

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBell
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBellTokens
import com.rookhq.monarch.components.layout.notificationsbell.MonarchNotificationsBellVariant
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchNotificationsBellFixtures : ComponentFixtures {
    override val id = "notifications-bell"
    override val fixtures = MonarchNotificationsBellVariant.entries.flatMap { variant ->
        listOf(0, 3, 120).map { count ->
            fixture("${variant.name}, $count unread") {
                MonarchNotificationsBell(onClick = {}, unreadCount = count, variant = variant) {
                    MonarchIcon(TestIcon, contentDescription = null)
                }
            }
        } + fixture("${variant.name}, disabled") {
            MonarchNotificationsBell(onClick = {}, unreadCount = 2, variant = variant, enabled = false) {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        }
    } + fixture("row, long title") {
        MonarchNotificationsBell(
            onClick = {},
            unreadCount = 4,
            variant = MonarchNotificationsBellVariant.Row,
            title = "Notifications from every workspace you belong to",
        ) {
            MonarchIcon(TestIcon, contentDescription = null)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchNotificationsBellTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theIconButtonIsNamedNotificationsWithTheCountAsItsState() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchNotificationsBell(onClick = { clicks++ }, unreadCount = 3) {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        }
        val bell = rule.onNodeWithContentDescription("Notifications")
        bell.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("3 unread", bell.stateDescription())
        // The badge is hidden, so the count is read once.
        rule.onAllNodesWithText("3").assertCountEquals(0)
        bell.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun withNothingUnreadThereIsNoState() {
        rule.setMonarchContent {
            MonarchNotificationsBell(onClick = {}, unreadCount = 0) { MonarchIcon(TestIcon, contentDescription = null) }
        }
        assertNull(rule.onNodeWithContentDescription("Notifications").stateDescription())
    }

    @Test
    fun theExactCountIsReadWhenTheBadgeShowsNinetyNinePlus() {
        rule.setMonarchContent {
            MonarchNotificationsBell(onClick = {}, unreadCount = 120) {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        }
        assertEquals("120 unread", rule.onNodeWithContentDescription("Notifications").stateDescription())
        assertEquals("99+", MonarchNotificationsBellTokens.badgeText(120))
        assertEquals("99", MonarchNotificationsBellTokens.badgeText(99))
        assertTrue(MonarchNotificationsBellTokens.showsBadge(1))
        assertFalse(MonarchNotificationsBellTokens.showsBadge(0))
        assertFalse(MonarchNotificationsBellTokens.showsBadge(-2))
    }

    @Test
    fun theRowShowsTheTitleAndIsOneButtonWithTheCount() {
        var clicks = 0
        rule.setMonarchContent {
            MonarchNotificationsBell(
                onClick = { clicks++ },
                unreadCount = 2,
                variant = MonarchNotificationsBellVariant.Row,
            ) {
                MonarchIcon(TestIcon, contentDescription = "Bell")
            }
        }
        val row = rule.onNodeWithText("Notifications")
        row.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
        assertEquals("2 unread", row.stateDescription())
        // The icon is hidden from TalkBack even when the app gives it a description.
        rule.onAllNodesWithText("Bell").assertCountEquals(0)
        row.performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun aLongRowTitleWrapsAtTheLargestFontScale() {
        val title = "Notifications from every workspace you belong to"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchNotificationsBell(
                onClick = {},
                unreadCount = 5,
                variant = MonarchNotificationsBellVariant.Row,
                title = title,
            ) {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        }
        rule.onNodeWithText(title, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun aDisabledBellIgnoresTaps() {
        var clicks = 0
        rule.setMonarchContent {
            Column {
                MonarchNotificationsBell(onClick = { clicks++ }, enabled = false, title = "Icon") {
                    MonarchIcon(TestIcon, contentDescription = null)
                }
                MonarchNotificationsBell(
                    onClick = { clicks++ },
                    enabled = false,
                    variant = MonarchNotificationsBellVariant.Row,
                    title = "Row",
                ) {
                    MonarchIcon(TestIcon, contentDescription = null)
                }
            }
        }
        rule.onNodeWithContentDescription("Icon").assertIsNotEnabled().performClick()
        rule.onNodeWithText("Row").assertIsNotEnabled().performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun theTitleAndTheUnreadPhraseCanBeReplaced() {
        rule.setMonarchContent {
            MonarchNotificationsBell(onClick = {}, unreadCount = 4, title = "Alerts", unreadLabel = "4 new alerts") {
                MonarchIcon(TestIcon, contentDescription = null)
            }
        }
        assertEquals("4 new alerts", rule.onNodeWithContentDescription("Alerts").stateDescription())
    }

    @Test
    fun theBadgeUsesTheSolidDangerColoursInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val badge = MonarchNotificationsBellTokens.badgeColors(colors)
            assertEquals(colors.dangerSolid, badge.container, theme.id)
            assertEquals(colors.dangerOnSolid, badge.content, theme.id)
            assertEquals(colors.textDefault, MonarchNotificationsBellTokens.contentColor(true, colors), theme.id)
            assertEquals(colors.textDisabled, MonarchNotificationsBellTokens.contentColor(false, colors), theme.id)
        }
    }
}

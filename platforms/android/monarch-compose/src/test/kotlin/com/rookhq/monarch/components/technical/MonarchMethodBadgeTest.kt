package com.rookhq.monarch.components.technical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.rookhq.monarch.components.technical.methodbadge.MonarchMethodBadge
import com.rookhq.monarch.components.technical.methodbadge.MonarchMethodBadgeDefaults
import com.rookhq.monarch.components.technical.methodbadge.MonarchMethodBadgeTokens
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalLayoutApi::class)
internal object MonarchMethodBadgeFixtures : ComponentFixtures {
    override val id = "method-badge"
    override val fixtures = listOf(
        fixture("every method") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS").forEach {
                    MonarchMethodBadge(method = it)
                }
            }
        },
        fixture("tone override and lower case") { MonarchMethodBadge(method = "purge", tone = MonarchTone.Danger) },
        fixture("empty method") { MonarchMethodBadge(method = "") },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchMethodBadgeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theMethodIsShownTrimmedInUpperCaseAndIsNotActionable() {
        rule.setMonarchContent { MonarchMethodBadge(method = " delete ") }
        rule.onNodeWithText("DELETE").assertHasNoClickAction()
    }

    @Test
    fun eachMethodHasItsToneIgnoringCase() {
        assertEquals(MonarchTone.Success, MonarchMethodBadgeDefaults.toneFor("get"))
        assertEquals(MonarchTone.Info, MonarchMethodBadgeDefaults.toneFor("POST"))
        assertEquals(MonarchTone.Warning, MonarchMethodBadgeDefaults.toneFor("Put"))
        assertEquals(MonarchTone.Warning, MonarchMethodBadgeDefaults.toneFor("PATCH"))
        assertEquals(MonarchTone.Danger, MonarchMethodBadgeDefaults.toneFor(" delete "))
        assertEquals(MonarchTone.Neutral, MonarchMethodBadgeDefaults.toneFor("HEAD"))
        assertEquals(MonarchTone.Neutral, MonarchMethodBadgeDefaults.toneFor("OPTIONS"))
        assertEquals(MonarchTone.Neutral, MonarchMethodBadgeDefaults.toneFor(""))
    }

    @Test
    fun theBadgeKeepsItsMinimumWidthAndGrowsWithTheFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMethodBadge(method = "GET", modifier = Modifier.testTag("badge"))
        }
        rule.onNodeWithText("GET").assertTextFullyVisible()
        rule.onNodeWithTag("badge").assertWidthIsAtLeast(MonarchMethodBadgeTokens.MinWidth * LargestFontScale)
    }

    @Test
    fun coloursComeFromTheToneRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val danger = MonarchMethodBadgeTokens.colors(MonarchTone.Danger, colors)
            assertEquals(colors.dangerSubtle, danger.container, theme.id)
            assertEquals(colors.dangerStrong, danger.content, theme.id)
            assertEquals(colors.dangerBorder, danger.border, theme.id)
            val success = MonarchMethodBadgeTokens.colors(MonarchTone.Success, colors)
            assertEquals(colors.successSubtle, success.container, theme.id)
            assertEquals(colors.successBorder, success.border, theme.id)
            val neutral = MonarchMethodBadgeTokens.colors(MonarchTone.Neutral, colors)
            assertEquals(colors.bgMuted, neutral.container, theme.id)
            assertEquals(colors.textMuted, neutral.content, theme.id)
            assertNull(neutral.border, theme.id)
        }
    }
}

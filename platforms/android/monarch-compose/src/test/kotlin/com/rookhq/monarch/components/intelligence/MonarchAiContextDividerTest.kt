package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDivider
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerKind
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerLabels
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiContextDividerTokens
import com.rookhq.monarch.components.intelligence.aicontextdivider.MonarchAiDayRelation
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val Now = 1_741_608_000_000L // 2025-03-10T12:00:00Z
private const val Day = 86_400_000L

internal object MonarchAiContextDividerFixtures : ComponentFixtures {
    override val id = "ai-context-divider"
    override val fixtures = listOf(
        fixture("today") {
            MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = Now, nowMillis = Now)
        },
        fixture("a date last year") {
            MonarchAiContextDivider(
                kind = MonarchAiContextDividerKind.Day,
                dateMillis = Now - Day * 400,
                nowMillis = Now,
            )
        },
        fixture("day without a date") {
            MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, nowMillis = Now)
        },
        fixture("context reset") { MonarchAiContextDivider(kind = MonarchAiContextDividerKind.ContextReset) },
        fixture("summarized, plain") { MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Summarized) },
        fixture("summarized with controls") {
            MonarchAiContextDivider(
                kind = MonarchAiContextDividerKind.Summarized,
                summary = "The team agreed to ship the report on Friday.",
                onExpand = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiContextDividerTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aContextResetIsOneElementReadWithItsDescription() {
        rule.setMonarchContent { MonarchAiContextDivider(kind = MonarchAiContextDividerKind.ContextReset) }
        rule.onNodeWithContentDescription(
            "Context reset. The assistant does not see the messages above this line.",
        ).assertHasNoClickAction()
    }

    @Test
    fun aDayIsLabelledTodayAndYesterday() {
        rule.setMonarchContent {
            Column {
                MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Day, dateMillis = Now, nowMillis = Now)
                MonarchAiContextDivider(
                    kind = MonarchAiContextDividerKind.Day,
                    dateMillis = Now - Day,
                    nowMillis = Now,
                    labels = MonarchAiContextDividerLabels(yesterday = "Hier"),
                )
            }
        }
        rule.onNodeWithContentDescription("Today").assertExists()
        rule.onNodeWithContentDescription("Hier").assertExists()
    }

    @Test
    fun theDayRelationIsCountedInTheTimeZone() {
        val utc = TimeZone.getTimeZone("UTC")
        assertEquals(MonarchAiDayRelation.Today, MonarchAiContextDividerTokens.relation(Now, Now, utc))
        assertEquals(MonarchAiDayRelation.Yesterday, MonarchAiContextDividerTokens.relation(Now - Day, Now, utc))
        assertEquals(MonarchAiDayRelation.ThisYear, MonarchAiContextDividerTokens.relation(Now - Day * 30, Now, utc))
        assertEquals(MonarchAiDayRelation.OtherYear, MonarchAiContextDividerTokens.relation(Now - Day * 400, Now, utc))
        // 20:00 on the 9th in UTC is already 05:00 on the 10th in Tokyo.
        val eveningOfTheNinth = Now - 16 * 3_600_000L
        val tokyo = TimeZone.getTimeZone("Asia/Tokyo")
        val inUtc = MonarchAiContextDividerTokens.relation(eveningOfTheNinth, Now, utc)
        val inTokyo = MonarchAiContextDividerTokens.relation(eveningOfTheNinth, Now, tokyo)
        assertEquals(MonarchAiDayRelation.Yesterday, inUtc)
        assertEquals(MonarchAiDayRelation.Today, inTokyo)
    }

    @Test
    fun theSummaryIsBehindADisclosureAndReadAsAiGenerated() {
        var expanded = 0
        rule.setMonarchContent {
            MonarchAiContextDivider(
                kind = MonarchAiContextDividerKind.Summarized,
                summary = "Ship on Friday.",
                onExpand = { expanded++ },
            )
        }
        rule.onNodeWithText("Earlier messages were summarized").assertExists()
        rule.onNodeWithContentDescription("AI-generated summary: Ship on Friday.").assertDoesNotExist()
        rule.onNodeWithText("Show summary").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("AI-generated summary: Ship on Friday.").assertExists()
        rule.onNodeWithText("Show earlier messages")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, expanded)
    }

    @Test
    fun theLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiContextDivider(kind = MonarchAiContextDividerKind.Summarized, onExpand = {})
        }
        rule.onNodeWithText("Earlier messages were summarized", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun onlyAResetIsDashedAndOnlyASummaryHasControls() {
        assertTrue(MonarchAiContextDividerTokens.dashed(MonarchAiContextDividerKind.ContextReset))
        assertFalse(MonarchAiContextDividerTokens.dashed(MonarchAiContextDividerKind.Day))
        assertTrue(MonarchAiContextDividerTokens.interactive(MonarchAiContextDividerKind.Summarized, true, false))
        assertFalse(MonarchAiContextDividerTokens.interactive(MonarchAiContextDividerKind.Summarized, false, false))
        assertFalse(MonarchAiContextDividerTokens.interactive(MonarchAiContextDividerKind.Day, true, true))
    }

    @Test
    fun theColoursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            fun line(kind: MonarchAiContextDividerKind) = MonarchAiContextDividerTokens.lineColor(kind, c)
            assertEquals(c.borderSubtle, line(MonarchAiContextDividerKind.Day), theme.id)
            assertEquals(c.borderDefault, line(MonarchAiContextDividerKind.ContextReset), theme.id)
            assertEquals(c.aiBorder, line(MonarchAiContextDividerKind.Summarized), theme.id)
            fun label(kind: MonarchAiContextDividerKind) = MonarchAiContextDividerTokens.labelColor(kind, c)
            assertEquals(c.aiStrong, label(MonarchAiContextDividerKind.Summarized), theme.id)
            assertEquals(c.textMuted, label(MonarchAiContextDividerKind.Day), theme.id)
            assertEquals(c.aiSubtle, MonarchAiContextDividerTokens.summaryColors(c).container, theme.id)
        }
    }
}

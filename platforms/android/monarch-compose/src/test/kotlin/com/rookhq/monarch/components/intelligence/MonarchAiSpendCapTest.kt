@file:OptIn(ExperimentalMonarchApi::class)

package com.rookhq.monarch.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.ExperimentalMonarchApi
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCap
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapBehavior
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapBreakdownItem
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapCheck
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapFormat
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapLabels
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapMath
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapPeriod
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapSettings
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapState
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapTokens
import com.rookhq.monarch.components.intelligence.aispendcap.MonarchAiSpendCapUnit
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The card in a scrolling column, as on a settings screen: it is taller than the 470dp test window. */
@Composable
private fun Scrolling(content: @Composable () -> Unit) {
    Column(Modifier.verticalScroll(rememberScrollState())) { content() }
}

@Composable
private fun Monthly(spent: Long) {
    MonarchAiSpendCap(spent = spent, limit = 5_000, currencyCode = "USD", period = MonarchAiSpendCapPeriod.Month)
}

internal object MonarchAiSpendCapFixtures : ComponentFixtures {
    override val id = "ai-spend-cap"
    override val fixtures = listOf(
        // Taller than a phone at font scale 2.0: checked whole in the tall matrix window.
        fixture("within limit with a breakdown", tall = true) {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_240,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    breakdown = listOf(MonarchAiSpendCapBreakdownItem("r", "Research agent", 820)),
                )
            }
        },
        fixture("close to the limit, notify") {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 4_200,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Week,
                    behavior = MonarchAiSpendCapBehavior.Notify,
                )
            }
        },
        fixture("reached and paused") {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 5_210,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Day,
                )
            }
        },
        fixture("no limit, micros, with a reset") {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 12_400_000,
                    limit = null,
                    currencyCode = "JPY",
                    period = MonarchAiSpendCapPeriod.Month,
                    unit = MonarchAiSpendCapUnit.Micros,
                    resetsAtMillis = 1_772_000_000_000,
                    timeZone = "UTC",
                )
            }
        },
        fixture("read only with a request") {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 100,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    canEdit = false,
                    onRequestIncrease = {},
                )
            }
        },
        fixture("editing") {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    editing = true,
                    saving = true,
                )
            }
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiSpendCapTest {
    @get:Rule
    val rule = createComposeRule()

    private fun button(text: String) = rule.onNode(hasText(text) and hasClickAction())

    private fun tap(text: String) {
        button(text).performScrollTo().performClick()
    }

    @Test
    fun theCardShowsTheStateTheAmountAndTheBar() {
        rule.setMonarchContent {
            Scrolling {
                Monthly(spent = 1_240)
            }
        }
        rule.onNode(hasText("AI spending limit") and isHeading()).assertExists()
        rule.onNodeWithText("Within limit", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$12.40 of $50.00 this month").assertExists()
        val bar = rule.onNodeWithContentDescription("Spent against the limit")
        assertEquals("$12.40 of $50.00 this month", bar.stateDescription())
        rule.onNodeWithText("$37.60 left").assertExists()
        rule.onNodeWithText("Warns at 80%").assertExists()
        rule.onNodeWithText("At the limit, AI pauses and asks").assertExists()
        button("Edit limit").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun atTheLimitAiIsPausedWithRaiseLimit() {
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 5_210,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Day,
                )
            }
        }
        rule.onNodeWithText("Limit reached", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$2.10 over the limit").assertExists()
        rule.onNodeWithText("AI is paused.", substring = true, useUnmergedTree = true).assertExists()
        tap("Raise limit")
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).assertExists()
    }

    @Test
    fun withoutALimitThereIsNoBarAndSetALimit() {
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_240,
                    limit = null,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Week,
                )
            }
        }
        rule.onNodeWithText("No limit set", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$12.40 spent this week").assertExists()
        rule.onNodeWithContentDescription("Spent against the limit").assertDoesNotExist()
        button("Set a limit").assertExists()
    }

    @Test
    fun readOnlyShowsWhoCanChangeItAndTheRequest() {
        var requests = 0
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 100,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    canEdit = false,
                    onRequestIncrease = { requests++ },
                )
            }
        }
        rule.onNodeWithText("Only a workspace admin can change this limit.").assertExists()
        button("Edit limit").assertDoesNotExist()
        tap("Request a higher limit")
        assertEquals(1, requests)
    }

    @Test
    fun savingTheFormReportsTheNewSettingsAndCloses() {
        var saved: MonarchAiSpendCapSettings? = null
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    onSave = { saved = it },
                )
            }
        }
        tap("Edit limit")
        rule.waitForIdle()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextClearance()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("80")
        tap("Keep going and tell me")
        tap("Save")
        rule.waitForIdle()
        assertEquals(MonarchAiSpendCapSettings(8_000, 0.8, MonarchAiSpendCapBehavior.Notify), saved)
        rule.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test
    fun anEmptyAmountIsAnError() {
        var saved: MonarchAiSpendCapSettings? = null
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    editing = true,
                    onSave = { saved = it },
                )
            }
        }
        rule.onNode(hasSetTextAction()).performScrollTo().performTextClearance()
        tap("Save")
        rule.waitForIdle()
        rule.onNodeWithText("To have no limit, use Remove limit.", substring = true, useUnmergedTree = true)
            .assertExists()
        assertNull(saved)
    }

    @Test
    fun aLimitBelowWhatIsSpentAsksToSaveAgain() {
        var saved: MonarchAiSpendCapSettings? = null
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 3_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    editing = true,
                    onSave = { saved = it },
                )
            }
        }
        rule.onNode(hasSetTextAction()).performScrollTo().performTextClearance()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("20")
        tap("Save")
        rule.waitForIdle()
        assertNull(saved)
        rule.onNodeWithText("already spent", substring = true, useUnmergedTree = true).assertExists()
        tap("Save anyway")
        rule.waitForIdle()
        assertEquals(2_000L, saved?.limit)
    }

    @Test
    fun removingTheLimitIsConfirmedInADialog() {
        var saved: MonarchAiSpendCapSettings? = null
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    editing = true,
                    onSave = { saved = it },
                )
            }
        }
        tap("Remove limit")
        rule.waitForIdle()
        rule.onNodeWithText("Remove the spending limit?").assertExists()
        button("Keep limit").performClick()
        rule.waitForIdle()
        assertNull(saved)
        tap("Remove limit")
        rule.waitForIdle()
        rule.onAllNodes(hasText("Remove limit") and hasClickAction())[1].performClick()
        rule.waitForIdle()
        assertEquals(MonarchAiSpendCapSettings(null, 0.8, MonarchAiSpendCapBehavior.Pause), saved)
    }

    @Test
    fun aNewStateIsAnnouncedOnce() {
        var spent by mutableLongStateOf(1_000)
        rule.setMonarchContent {
            Scrolling {
                Monthly(spent = spent)
            }
        }
        rule.onNodeWithContentDescription("AI spending limit:", substring = true).assertDoesNotExist()
        spent = 4_500
        rule.waitForIdle()
        rule.onNodeWithContentDescription("AI spending limit: Close to the limit. $45.00 of $50.00 this month")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        spent = 4_600
        rule.waitForIdle()
        rule.onNodeWithContentDescription("AI spending limit: Close to the limit. $45.00 of $50.00 this month")
            .assertExists()
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Scrolling {
                MonarchAiSpendCap(
                    spent = 1_000,
                    limit = 5_000,
                    currencyCode = "USD",
                    period = MonarchAiSpendCapPeriod.Month,
                    labels = MonarchAiSpendCapLabels(
                        title = "Límite de IA",
                        state = { "Bien" },
                        period = { "este mes" },
                        edit = "Editar",
                    ),
                )
            }
        }
        rule.onNode(hasText("Límite de IA") and isHeading()).assertExists()
        rule.onNodeWithText("Bien", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("$10.00 of $50.00 este mes").assertExists()
        button("Editar").assertExists()
    }

    @Test
    fun theBadgeFollowsTheTitleRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            Scrolling {
                Monthly(spent = 1_000)
            }
        }
        rule.onNodeWithText("AI spending limit", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("Within limit", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun textFitsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Scrolling {
                Monthly(spent = 1_000)
            }
        }
        rule.onNodeWithText("$10.00 of $50.00 this month", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theStateRules() {
        val t = MonarchAiSpendCapTokens
        assertEquals(MonarchAiSpendCapState.NoLimit, t.state(10, null, 0.8))
        assertEquals(MonarchAiSpendCapState.Reached, t.state(0, 0, 0.8))
        assertEquals(MonarchAiSpendCapState.Reached, t.state(100, 100, 0.8))
        assertEquals(MonarchAiSpendCapState.Warning, t.state(80, 100, 0.8))
        assertEquals(MonarchAiSpendCapState.Ok, t.state(79, 100, 0.8))
        assertEquals(MonarchAiSpendCapState.Ok, t.state(-5, 100, 0.8))
        assertEquals(0.5f, t.fraction(50, 100))
        assertEquals(1f, t.fraction(150, 100))
        assertNull(t.fraction(1, 0))
        assertEquals(MonarchTone.Danger, t.tone(MonarchAiSpendCapState.Reached))
    }

    @Test
    fun theChecksOfTheForm() {
        val t = MonarchAiSpendCapTokens
        assertEquals(MonarchAiSpendCapCheck.Required, t.check(null, 0, null, null))
        assertEquals(MonarchAiSpendCapCheck.Positive, t.check(0, 0, null, null))
        assertEquals(MonarchAiSpendCapCheck.Positive, t.check(-1, 0, null, null))
        assertEquals(MonarchAiSpendCapCheck.Max, t.check(200, 0, 100, null))
        assertEquals(MonarchAiSpendCapCheck.BelowSpent, t.check(50, 80, null, null))
        assertEquals(MonarchAiSpendCapCheck.Ok, t.check(50, 80, null, 50))
        assertEquals(MonarchAiSpendCapCheck.BelowSpent, t.check(40, 80, null, 50))
        assertEquals(MonarchAiSpendCapCheck.Ok, t.check(100, 80, 100, null))
    }

    @Test
    fun amountsConvertWithoutFloats() {
        val m = MonarchAiSpendCapMath
        assertEquals("12.40", m.toMajor(1_240, "GBP", MonarchAiSpendCapUnit.Minor).toPlainString())
        assertEquals("12.400000", m.toMajor(12_400_000, "GBP", MonarchAiSpendCapUnit.Micros).toPlainString())
        assertEquals(1_241L, m.toMinor(12_405_000, "GBP", MonarchAiSpendCapUnit.Micros))
        assertEquals(12_400_000L, m.fromMinor(1_240, "GBP", MonarchAiSpendCapUnit.Micros))
        assertEquals(500L, m.fromMinor(500, "JPY", MonarchAiSpendCapUnit.Minor))
        assertEquals(500_000_000L, m.fromMinor(500, "JPY", MonarchAiSpendCapUnit.Micros))
        assertEquals(listOf(0.5, 0.75, 0.8, 0.9), m.shares(listOf(0.5, 0.75, 0.8, 0.9), 0.8))
        assertEquals(listOf(0.5, 0.6, 0.9), m.shares(listOf(0.5, 0.9, 1.0, 0.0), 0.6))
        assertEquals(0.9, m.initialShare(listOf(0.5, 0.9), 1.0))
    }

    @Test
    fun moneyPercentagesAndDatesFollowTheLocale() {
        val utc = TimeZone.getTimeZone("UTC")
        val us = MonarchAiSpendCapFormat(Locale.US, "USD", MonarchAiSpendCapUnit.Minor, utc)
        assertEquals("$12.40", us.money(1_240))
        assertEquals("80%", us.percent(0.8))
        assertEquals("February 25", us.date(1_772_000_000_000, MonarchAiSpendCapPeriod.Month))
        val yen = MonarchAiSpendCapFormat(Locale.US, "JPY", MonarchAiSpendCapUnit.Micros, utc)
        assertEquals("¥12", yen.money(12_400_000))
    }

    @Test
    fun theColoursComeFromTheTheme() {
        val t = MonarchAiSpendCapTokens
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, t.barColor(MonarchAiSpendCapState.Ok, c), theme.id)
            assertEquals(c.warningStrong, t.barColor(MonarchAiSpendCapState.Warning, c), theme.id)
            assertEquals(c.dangerStrong, t.barColor(MonarchAiSpendCapState.Reached, c), theme.id)
            assertEquals(c.bgEmphasis, t.trackColor(c), theme.id)
            assertEquals(c.bgSurface, t.cardColors(c).container, theme.id)
            assertEquals(c.borderSubtle, t.cardColors(c).border, theme.id)
        }
    }
}

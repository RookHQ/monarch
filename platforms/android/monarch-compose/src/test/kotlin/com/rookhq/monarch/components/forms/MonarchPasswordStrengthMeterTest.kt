package com.rookhq.monarch.components.forms

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordRule
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrength
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeter
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeterLabels
import com.rookhq.monarch.components.forms.passwordstrengthmeter.MonarchPasswordStrengthMeterTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal object MonarchPasswordStrengthMeterFixtures : ComponentFixtures {
    override val id = "password-strength-meter"
    override val fixtures = listOf(
        fixture("empty") { MonarchPasswordStrengthMeter(password = "") },
        fixture("very weak, no rules") { MonarchPasswordStrengthMeter(password = "abc", showRules = false) },
        fixture("fair, no rules") { MonarchPasswordStrengthMeter(password = "abcdefg1!", showRules = false) },
        fixture("good with rules") { MonarchPasswordStrengthMeter(password = "Abcdefg1!") },
        fixture("strong with a long custom rule") {
            MonarchPasswordStrengthMeter(
                password = "Correct horse battery 9",
                rules = listOf(
                    MonarchPasswordRule("words", "At least three words, separated by spaces or punctuation") {
                        it.split(' ').size >= 3
                    },
                ),
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchPasswordStrengthMeterTest {
    @get:Rule
    val rule = createComposeRule()

    private val rules = MonarchPasswordRule.defaults("Length", "Case", "Number", "Symbol")

    @Test
    fun theDefaultScoreFollowsTheShareOfRulesMet() {
        assertEquals(MonarchPasswordStrength.VeryWeak, MonarchPasswordStrength.score("", rules))
        assertEquals(MonarchPasswordStrength.VeryWeak, MonarchPasswordStrength.score("abc", rules))
        assertEquals(MonarchPasswordStrength.Weak, MonarchPasswordStrength.score("abcdefgh1", rules))
        assertEquals(MonarchPasswordStrength.Fair, MonarchPasswordStrength.score("abcdefg1!", rules))
        assertEquals(MonarchPasswordStrength.Good, MonarchPasswordStrength.score("Abcdefg1!", rules))
        assertEquals(MonarchPasswordStrength.Strong, MonarchPasswordStrength.score("Abcdefg1!abcdef", rules))
        assertEquals(MonarchPasswordStrength.Good, MonarchPasswordStrength.score("x", emptyList()))
    }

    @Test
    fun theDefaultRulesCountCodePointsAndUnicodeCategories() {
        val length = rules.first { it.id == "length" }
        // Seven emoji are fourteen UTF-16 units but seven characters.
        assertTrue(!length.test("😀".repeat(7)))
        assertTrue(length.test("😀".repeat(8)))
        assertTrue(rules.first { it.id == "case" }.test("Éé"))
        assertTrue(rules.first { it.id == "number" }.test("٣"))
        assertTrue(rules.first { it.id == "symbol" }.test("a b"))
        assertTrue(!rules.first { it.id == "symbol" }.test("ab12"))
    }

    @Test
    fun theStrengthIsALiveLineOfText() {
        rule.setMonarchContent { MonarchPasswordStrengthMeter(password = "Abcdefg1!") }
        val line = rule.onNodeWithText("Password strength: Good")
        assertEquals(LiveRegionMode.Polite, line.fetchSemanticsNode().config.getOrNull(SemanticsProperties.LiveRegion))
    }

    @Test
    fun anEmptyPasswordIsNotEnteredAndMeetsNoRule() {
        rule.setMonarchContent { MonarchPasswordStrengthMeter(password = "") }
        rule.onNodeWithText("Password strength: Not entered").assertExists()
        rule.onNodeWithContentDescription("Not met: At least 8 characters").assertExists()
        rule.onNodeWithContentDescription("Not met: A symbol or space").assertExists()
    }

    @Test
    fun eachRuleIsReadAsMetOrNotMetInAList() {
        rule.setMonarchContent { MonarchPasswordStrengthMeter(password = "abcdefgh") }
        rule.onNodeWithContentDescription("Met: At least 8 characters").assertExists()
        rule.onNodeWithContentDescription("Not met: A number").assertExists()
        val list = rule.onNodeWithContentDescription("Password requirements").fetchSemanticsNode()
        assertEquals(4, list.config.getOrNull(SemanticsProperties.CollectionInfo)?.rowCount)
    }

    @Test
    fun aCustomScoreAndNoRules() {
        rule.setMonarchContent {
            MonarchPasswordStrengthMeter(
                password = "anything",
                rules = emptyList(),
                score = { MonarchPasswordStrength.Strong },
            )
        }
        rule.onNodeWithText("Password strength: Strong").assertExists()
        rule.onNodeWithContentDescription("Password requirements").assertDoesNotExist()
    }

    @Test
    fun builtInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchPasswordStrengthMeter(
                password = "abc",
                labels = MonarchPasswordStrengthMeterLabels(
                    strengthText = { "Strength ${it?.name}" },
                    rulesLabel = "Rules",
                    ruleText = { label, met -> "$label ${if (met) "yes" else "no"}" },
                ),
            )
        }
        rule.onNodeWithText("Strength VeryWeak").assertExists()
        rule.onNodeWithContentDescription("Rules").assertExists()
        rule.onNodeWithContentDescription("A number no").assertExists()
    }

    @Test
    fun theTextWrapsAtTheLargestFontSizeInBothDirections() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) { MonarchPasswordStrengthMeter(password = "Abc", showRules = false) }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Password strength: Very weak").assertTextFullyVisible()
        }
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val t = MonarchPasswordStrengthMeterTokens
            assertEquals(colors.dangerSolid, t.fill(MonarchPasswordStrength.Weak, colors), theme.id)
            assertEquals(colors.warningSolid, t.fill(MonarchPasswordStrength.Fair, colors), theme.id)
            assertEquals(colors.successSolid, t.fill(MonarchPasswordStrength.Strong, colors), theme.id)
            assertEquals(colors.bgEmphasis, t.segment(0, null, colors), theme.id)
            assertEquals(colors.dangerSolid, t.segment(0, MonarchPasswordStrength.VeryWeak, colors), theme.id)
            assertEquals(colors.bgEmphasis, t.segment(1, MonarchPasswordStrength.VeryWeak, colors), theme.id)
            assertEquals(colors.successSolid, t.segment(3, MonarchPasswordStrength.Strong, colors), theme.id)
            assertEquals(colors.successStrong, t.rule(true, colors), theme.id)
            assertEquals(colors.textMuted, t.rule(false, colors), theme.id)
            assertNotEquals(t.rule(true, colors), t.rule(false, colors), theme.id)
        }
    }

    @Test
    fun theBarFillsOneSegmentPerLevelAndAtLeastOne() {
        val t = MonarchPasswordStrengthMeterTokens
        assertEquals(0, t.filledSegments(null))
        assertEquals(1, t.filledSegments(MonarchPasswordStrength.VeryWeak))
        assertEquals(1, t.filledSegments(MonarchPasswordStrength.Weak))
        assertEquals(2, t.filledSegments(MonarchPasswordStrength.Fair))
        assertEquals(3, t.filledSegments(MonarchPasswordStrength.Good))
        assertEquals(4, t.filledSegments(MonarchPasswordStrength.Strong))
    }
}

package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.forms.durationinput.DurationInputCommit
import dev.monarchui.components.forms.durationinput.DurationInputMath
import dev.monarchui.components.forms.durationinput.DurationInputParse
import dev.monarchui.components.forms.durationinput.DurationInputProblem
import dev.monarchui.components.forms.durationinput.MonarchDurationInput
import dev.monarchui.components.forms.durationinput.MonarchDurationInputLabels
import dev.monarchui.components.forms.durationinput.MonarchDurationInputTokens
import dev.monarchui.components.forms.durationinput.MonarchDurationInputUnit
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal object MonarchDurationInputFixtures : ComponentFixtures {
    override val id = "duration-input"
    override val fixtures = listOf(
        fixture("value with helper") {
            MonarchDurationInput(minutes = 90, onMinutesChange = {}, label = "Estimate", helperText = "Up to 8h.")
        },
        fixture("at the maximum") {
            MonarchDurationInput(minutes = 120, onMinutesChange = {}, label = "Estimate", maximum = 120)
        },
        fixture("empty") { MonarchDurationInput(minutes = null, onMinutesChange = {}, label = "Break") },
        fixture("days, without steppers") {
            MonarchDurationInput(
                minutes = 1560,
                onMinutesChange = {},
                label = "Time off",
                largestUnit = MonarchDurationInputUnit.Days,
                minutesPerDay = 480,
                showSteppers = false,
            )
        },
        fixture("error from the app") {
            MonarchDurationInput(
                minutes = 0,
                onMinutesChange = {},
                label = "Estimate",
                errorText = "Add an estimate before you start the timer.",
            )
        },
        fixture("disabled") {
            MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Buffer", enabled = false)
        },
        fixture("read only") {
            MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Buffer", readOnly = true)
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchDurationInputTest {
    @get:Rule
    val rule = createComposeRule()

    private val field get() = rule.onNode(hasSetTextAction())
    private val increase get() = rule.onNodeWithContentDescription("Increase")
    private val decrease get() = rule.onNodeWithContentDescription("Decrease")

    private fun SemanticsNodeInteraction.text(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.EditableText)?.text

    private fun SemanticsNodeInteraction.error(): String? =
        fetchSemanticsNode().config.getOrNull(SemanticsProperties.Error)

    @Test
    fun readsTheFormsTheWebReads() {
        fun minutes(text: String, perDay: Int = 1440) = DurationInputMath.parse(text, perDay)
        assertEquals(DurationInputParse.Empty, minutes("  "))
        assertEquals(DurationInputParse.Minutes(90), minutes("90"))
        assertEquals(DurationInputParse.Minutes(90), minutes("90m"))
        assertEquals(DurationInputParse.Minutes(90), minutes("1h 30m"))
        assertEquals(DurationInputParse.Minutes(90), minutes("1h30"))
        assertEquals(DurationInputParse.Minutes(90), minutes("1.5h"))
        assertEquals(DurationInputParse.Minutes(90), minutes("1,5 H"))
        assertEquals(DurationInputParse.Minutes(90), minutes("1:30"))
        assertEquals(DurationInputParse.Minutes(135), minutes("2 hours 15 minutes"))
        assertEquals(DurationInputParse.Minutes(1680), minutes("1d 4h"))
        assertEquals(DurationInputParse.Minutes(720), minutes("1d 4h", perDay = 480))
        assertEquals(DurationInputParse.Minutes(1), minutes("0.5"))
        assertEquals(DurationInputParse.Invalid, minutes("1:75"))
        assertEquals(DurationInputParse.Invalid, minutes("abc"))
        assertEquals(DurationInputParse.Invalid, minutes("2 weeks"))
        assertEquals(DurationInputParse.Invalid, minutes("30m 5"))
        assertEquals(DurationInputParse.Invalid, minutes("-5m"))
    }

    @Test
    fun writesHoursAndMinutesOrDays() {
        assertEquals("0m", DurationInputMath.format(0))
        assertEquals("0m", DurationInputMath.format(-5))
        assertEquals("45m", DurationInputMath.format(45))
        assertEquals("1h 30m", DurationInputMath.format(90))
        assertEquals("26h", DurationInputMath.format(1560))
        assertEquals("1d 2h", DurationInputMath.format(1560, MonarchDurationInputUnit.Days))
        assertEquals("3d 1h", DurationInputMath.format(1500, MonarchDurationInputUnit.Days, minutesPerDay = 480))
    }

    @Test
    fun stepsSnapToTheGridAndStopAtTheBounds() {
        assertEquals(60, DurationInputMath.stepped(50, 15, 1, minimum = 0, maximum = null))
        assertEquals(45, DurationInputMath.stepped(50, 15, -1, minimum = 0, maximum = null))
        assertEquals(75, DurationInputMath.stepped(60, 15, 1, minimum = 0, maximum = null))
        assertEquals(0, DurationInputMath.stepped(10, 15, -1, minimum = 0, maximum = null))
        assertEquals(120, DurationInputMath.stepped(115, 15, 1, minimum = 0, maximum = 120))
        assertEquals(31, DurationInputMath.stepped(30, 0, 1, minimum = 0, maximum = null))
        assertEquals(10, DurationInputMath.clamped(5, minimum = 10, maximum = 5))
        assertTrue(DurationInputMath.outOfRange(130, minimum = 0, maximum = 120))
        assertFalse(DurationInputMath.outOfRange(120, minimum = 0, maximum = 120))
    }

    @Test
    fun theCommitRulesSaveEmptyAndInRangeValuesOnly() {
        val tokens = MonarchDurationInputTokens
        assertEquals(DurationInputCommit.Save(null), tokens.commit(DurationInputParse.Empty, 0, null))
        assertEquals(DurationInputCommit.Save(45), tokens.commit(DurationInputParse.Minutes(45), 0, 60))
        assertEquals(
            DurationInputCommit.Reject(DurationInputProblem.OutOfRange),
            tokens.commit(DurationInputParse.Minutes(90), 0, 60),
        )
        assertEquals(
            DurationInputCommit.Reject(DurationInputProblem.Invalid),
            tokens.commit(DurationInputParse.Invalid, 0, null),
        )
        assertEquals(30, tokens.stepBase(DurationInputParse.Minutes(30), 10, 0))
        assertEquals(10, tokens.stepBase(DurationInputParse.Invalid, 10, 0))
        assertEquals(5, tokens.stepBase(DurationInputParse.Empty, null, 5))
        assertEquals("App", tokens.shownError("App", "Own"))
        assertEquals("Own", tokens.shownError(" ", "Own"))
        assertNull(tokens.shownError(null, ""))
        assertEquals(DurationInputParse.Minutes(7), tokens.read(" x ", { if (it == "x") 7 else null }, 1440))
        assertEquals(DurationInputParse.Invalid, tokens.read("y", { null }, 1440))
        assertEquals(1 to 15, tokens.keyDelta(Key.DirectionUp, 15))
        assertEquals(-1 to 60, tokens.keyDelta(Key.PageDown, 15))
        assertNull(tokens.keyDelta(Key.A, 15))
    }

    @Test
    fun theDurationIsSpokenInWords() {
        assertEquals("1 hour, 30 minutes", MonarchDurationInputTokens.valueText(90, Locale.US))
        assertEquals("45 minutes", MonarchDurationInputTokens.valueText(45, Locale.US))
        assertEquals("2 hours", MonarchDurationInputTokens.valueText(120, Locale.US))
        assertEquals("0 minutes", MonarchDurationInputTokens.valueText(0, Locale.US))
    }

    @Test
    fun typedTextIsReadOnDoneAndWrittenBack() {
        var minutes by mutableStateOf<Int?>(null)
        rule.setMonarchContent {
            MonarchDurationInput(minutes = minutes, onMinutesChange = { minutes = it }, label = "Estimate")
        }
        field.performTextInput("1.5h")
        assertNull(minutes)
        field.performImeAction()
        assertEquals(90, minutes)
        assertEquals("1h 30m", field.text())
        assertEquals("1 hour, 30 minutes", field.stateDescription())
        field.performTextClearance()
        field.performImeAction()
        assertNull(minutes)
    }

    @Test
    fun unreadableAndOutOfRangeTextIsFlaggedAndNotSaved() {
        var changes = 0
        rule.setMonarchContent {
            MonarchDurationInput(minutes = 30, onMinutesChange = { changes++ }, label = "Estimate", maximum = 120)
        }
        field.performTextClearance()
        field.performTextInput("soon")
        field.performImeAction()
        assertEquals("Use a duration like 1h 30m, 90m or 1.5h.", field.error())
        assertEquals("soon", field.text())
        field.performTextClearance()
        field.performTextInput("3h")
        field.performImeAction()
        assertEquals("Enter at most 2h.", field.error())
        assertEquals(0, changes)
    }

    @Test
    fun theRangeMessageNamesBothBoundsWithAMinimum() {
        rule.setMonarchContent {
            MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Estimate", minimum = 15, maximum = 120)
        }
        field.performTextClearance()
        field.performTextInput("5")
        field.performImeAction()
        assertEquals("Enter between 15m and 2h.", field.error())
    }

    @Test
    fun theButtonsStepAndStopAtTheBounds() {
        var minutes by mutableStateOf<Int?>(105)
        rule.setMonarchContent {
            MonarchDurationInput(
                minutes = minutes,
                onMinutesChange = { minutes = it },
                label = "Estimate",
                maximum = 120,
            )
        }
        increase.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(120, minutes)
        increase.assertIsNotEnabled()
        decrease.assertIsEnabled().performClick()
        assertEquals(105, minutes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun theFieldHasStepActionsAndArrowKeys() {
        var minutes by mutableStateOf<Int?>(50)
        rule.setMonarchContent {
            MonarchDurationInput(minutes = minutes, onMinutesChange = { minutes = it }, label = "Estimate")
        }
        field.performCustomAccessibilityActionWithLabel("Increase")
        assertEquals(60, minutes)
        field.requestFocus()
        field.performKeyInput { pressKey(Key.PageUp) }
        assertEquals(120, minutes)
        field.performKeyInput { pressKey(Key.DirectionDown) }
        assertEquals(105, minutes)
    }

    @Test
    fun disabledAndReadOnlyFieldsDoNotStep() {
        rule.setMonarchContent {
            MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Buffer", readOnly = true)
        }
        increase.assertIsNotEnabled()
        decrease.assertIsNotEnabled()
    }

    @Test
    fun theStringsAndFormatCanBeReplaced() {
        rule.setMonarchContent {
            MonarchDurationInput(
                minutes = 30,
                onMinutesChange = {},
                label = "Dauer",
                maximum = 60,
                format = { "$it Min." },
                parse = { it.removeSuffix(" Min.").toIntOrNull() },
                labels = MonarchDurationInputLabels(
                    invalid = "Ungültig",
                    range = { _, max -> "Höchstens $max" },
                    valueText = { "$it Minuten" },
                    increment = "Mehr",
                    decrement = "Weniger",
                ),
            )
        }
        assertEquals("30 Min.", field.text())
        assertEquals("30 Minuten", field.stateDescription())
        rule.onNodeWithContentDescription("Mehr").assertExists()
        rule.onNodeWithContentDescription("Weniger").assertExists()
        field.performTextClearance()
        field.performTextInput("viel")
        field.performImeAction()
        assertEquals("Ungültig", field.error())
        field.performTextClearance()
        field.performTextInput("90")
        field.performImeAction()
        assertEquals("Höchstens 60", field.error())
    }

    @Test
    fun theButtonsMirrorRightToLeftAndFitTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchDurationInput(minutes = 30, onMinutesChange = {}, label = "Estimate")
        }
        decrease.assertIsBefore(increase, LayoutDirection.Rtl)
        increase.assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theStepperColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textMuted, MonarchDurationInputTokens.stepper(true, c), theme.id)
            assertEquals(c.textDisabled, MonarchDurationInputTokens.stepper(false, c), theme.id)
        }
    }
}

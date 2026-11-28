package com.rookhq.monarch.components.identity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityCheck
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInput
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInputLabels
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityInputTokens
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityResult
import com.rookhq.monarch.components.identity.usernameavailabilityinput.MonarchUsernameAvailabilityStatus
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
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchUsernameAvailabilityInputFixtures : ComponentFixtures {
    override val id = "username-availability-input"
    override val fixtures = listOf(
        fixture("idle with prefix and helper") {
            MonarchUsernameAvailabilityInput(
                value = "",
                label = "Handle",
                onValueChange = {},
                validate = { null },
                prefix = "rook.com/",
                helperText = "Letters, numbers and dashes.",
            )
        },
        fixture("checking") {
            MonarchUsernameAvailabilityInput(value = "ada", label = "Handle", onValueChange = {}, validate = { null })
        },
        fixture("valid") {
            MonarchUsernameAvailabilityInput(
                value = "ada",
                label = "Handle",
                onValueChange = {},
                validate = { MonarchUsernameAvailabilityResult(valid = true) },
                debounceMillis = 0L,
            )
        },
        fixture("invalid with a message") {
            MonarchUsernameAvailabilityInput(
                value = "admin",
                label = "Handle",
                onValueChange = {},
                validate = { MonarchUsernameAvailabilityResult(valid = false, message = "That handle is reserved.") },
                debounceMillis = 0L,
            )
        },
        fixture("error with retry") {
            MonarchUsernameAvailabilityInput(
                value = "ada",
                label = "Handle",
                onValueChange = {},
                validate = { throw IOException("offline") },
                debounceMillis = 0L,
            )
        },
        fixture("disabled") {
            MonarchUsernameAvailabilityInput(
                value = "ada",
                label = "Handle",
                onValueChange = {},
                validate = { null },
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchUsernameAvailabilityInputTest {
    @get:Rule
    val rule = createComposeRule()

    @Composable
    private fun Host(
        validate: suspend (String) -> MonarchUsernameAvailabilityResult?,
        initial: String = "",
        transform: ((String) -> String)? = null,
        onStatusChange: ((MonarchUsernameAvailabilityStatus, String?) -> Unit)? = null,
        labels: MonarchUsernameAvailabilityInputLabels = MonarchUsernameAvailabilityInputLabels(),
    ) {
        var text by remember { mutableStateOf(initial) }
        MonarchUsernameAvailabilityInput(
            value = text,
            label = "Handle",
            onValueChange = { text = it },
            validate = validate,
            transform = transform,
            onStatusChange = onStatusChange,
            labels = labels,
        )
    }

    private fun field() = rule.onNode(hasSetTextAction())

    /** Lets the debounce pass and the check finish. */
    private fun settle() {
        rule.mainClock.advanceTimeBy(MonarchUsernameAvailabilityInputTokens.DEFAULT_DEBOUNCE_MILLIS * 2)
        rule.waitForIdle()
    }

    @Test
    fun theCheckRunsOnceAfterTypingPausesWithTheLatestText() {
        val checked = mutableListOf<String>()
        rule.setMonarchContent { Host(validate = { checked += it; MonarchUsernameAvailabilityResult(valid = true) }) }
        rule.mainClock.autoAdvance = false
        field().performTextInput("a")
        rule.mainClock.advanceTimeBy(100)
        field().performTextInput("da")
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Checking availability…", useUnmergedTree = true).assertExists()
        rule.mainClock.advanceTimeBy(500)
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertEquals(listOf("ada"), checked)
        rule.onNodeWithText("Available", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aSettledResultIsAnnouncedPolitely() {
        rule.setMonarchContent { Host(initial = "ada", validate = { MonarchUsernameAvailabilityResult(valid = true) }) }
        settle()
        rule.onNodeWithContentDescription("Available")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anInvalidResultIsAnErrorWithTheChecksMessage() {
        rule.setMonarchContent {
            Host(
                initial = "admin",
                validate = { MonarchUsernameAvailabilityResult(valid = false, message = "Reserved") },
            )
        }
        settle()
        field().assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Reserved"))
        rule.onNodeWithText("Reserved", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aFailedCheckOffersARetryThatRunsAtOnce() {
        var calls = 0
        rule.setMonarchContent {
            Host(initial = "ada", validate = {
                calls++
                if (calls == 1) throw IOException("offline") else MonarchUsernameAvailabilityResult(valid = true)
            })
        }
        settle()
        rule.onNodeWithText("Couldn’t check availability.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Try again").assertTouchTargetAtLeast48dp(rule.density).performClick()
        settle()
        assertEquals(2, calls)
        rule.onNodeWithText("Available", useUnmergedTree = true).assertExists()
    }

    @Test
    fun emptyTextIsNeverChecked() {
        var calls = 0
        rule.setMonarchContent { Host(validate = { calls++; null }) }
        settle()
        assertEquals(0, calls)
        rule.onNodeWithText("Checking availability…", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theTransformRewritesTypedText() {
        val checked = mutableListOf<String>()
        rule.setMonarchContent {
            Host(transform = { it.lowercase() }, validate = { checked += it; MonarchUsernameAvailabilityResult(true) })
        }
        field().performTextInput("ADA")
        settle()
        assertEquals(listOf("ada"), checked)
    }

    @Test
    fun theStatusIsReportedOnChangesOnly() {
        val reported = mutableListOf<MonarchUsernameAvailabilityStatus>()
        rule.setMonarchContent {
            Host(
                initial = "ada",
                validate = { MonarchUsernameAvailabilityResult(valid = false) },
                onStatusChange = { status, _ -> reported += status },
            )
        }
        settle()
        assertEquals(
            listOf(MonarchUsernameAvailabilityStatus.Checking, MonarchUsernameAvailabilityStatus.Invalid),
            reported,
        )
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            Host(
                initial = "ada",
                validate = { MonarchUsernameAvailabilityResult(valid = true) },
                labels = MonarchUsernameAvailabilityInputLabels(valid = "Libre"),
            )
        }
        settle()
        rule.onNodeWithText("Libre", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theMessageIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            Host(initial = "ada", validate = { MonarchUsernameAvailabilityResult(valid = false) })
        }
        settle()
        rule.onNodeWithText("Not available", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theResultOfACheckBecomesAStatus() {
        fun check(outcome: Result<MonarchUsernameAvailabilityResult?>) =
            MonarchUsernameAvailabilityInputTokens.checkOf(outcome)
        assertEquals(MonarchUsernameAvailabilityStatus.Idle, check(Result.success(null)).status)
        assertEquals(
            MonarchUsernameAvailabilityCheck(MonarchUsernameAvailabilityStatus.Valid, "Free"),
            check(Result.success(MonarchUsernameAvailabilityResult(valid = true, message = "Free"))),
        )
        assertEquals(MonarchUsernameAvailabilityStatus.Error, check(Result.failure(IOException())).status)
        assertTrue(MonarchUsernameAvailabilityInputTokens.announces(MonarchUsernameAvailabilityStatus.Error))
        assertFalse(MonarchUsernameAvailabilityInputTokens.announces(MonarchUsernameAvailabilityStatus.Checking))
    }

    @Test
    fun theColoursComeFromTheStatusRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            fun message(status: MonarchUsernameAvailabilityStatus) =
                MonarchUsernameAvailabilityInputTokens.messageColor(status, c)
            assertEquals(c.textMuted, message(MonarchUsernameAvailabilityStatus.Checking), theme.id)
            assertEquals(c.successStrong, message(MonarchUsernameAvailabilityStatus.Valid), theme.id)
            assertEquals(c.dangerStrong, message(MonarchUsernameAvailabilityStatus.Invalid), theme.id)
            assertEquals(c.warningStrong, message(MonarchUsernameAvailabilityStatus.Error), theme.id)
        }
    }
}

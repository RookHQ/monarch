package dev.monarchui.components.forms

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInput
import dev.monarchui.components.forms.typetoconfirminput.MonarchTypeToConfirmInputDefaults
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal object MonarchTypeToConfirmInputFixtures : ComponentFixtures {
    override val id = "type-to-confirm-input"
    override val fixtures = listOf(
        fixture("empty with helper") {
            MonarchTypeToConfirmInput(
                phrase = "acme-site",
                value = "",
                onValueChange = {},
                helperText = "This deletes the site.",
            )
        },
        fixture("mismatch shown") {
            MonarchTypeToConfirmInput(phrase = "acme-site", value = "acme", onValueChange = {}, showMismatch = true)
        },
        fixture("matching") {
            MonarchTypeToConfirmInput(phrase = "acme-site", value = "acme-site", onValueChange = {})
        },
        fixture("disabled") {
            MonarchTypeToConfirmInput(phrase = "acme-site", value = "", onValueChange = {}, enabled = false)
        },
        fixture("long phrase") {
            MonarchTypeToConfirmInput(
                phrase = "production-eu-west-analytics-warehouse",
                value = "",
                onValueChange = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchTypeToConfirmInputTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun matchingIsCaseSensitiveAndIgnoresSurroundingWhiteSpace() {
        assertTrue(MonarchTypeToConfirmInputDefaults.matches(" acme-site\n", "acme-site "))
        assertFalse(MonarchTypeToConfirmInputDefaults.matches("Acme-site", "acme-site"))
        assertFalse(MonarchTypeToConfirmInputDefaults.matches("acme - site", "acme-site"))
        assertTrue(MonarchTypeToConfirmInputDefaults.matches("", ""))
    }

    @Test
    fun theKeyboardDoesNotCorrectOrCapitalise() {
        assertEquals(false, MonarchTypeToConfirmInputDefaults.KeyboardOptions.autoCorrectEnabled)
    }

    @Test
    fun theDefaultLabelNamesThePhrase() {
        rule.setMonarchContent { MonarchTypeToConfirmInput(phrase = "acme-site", value = "", onValueChange = {}) }
        rule.onNodeWithText("Type “acme-site” to confirm", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theErrorShowsOnlyWhenAskedForAndTheTextDoesNotMatch() {
        var shown by mutableStateOf(false)
        var text by mutableStateOf("acme")
        rule.setMonarchContent {
            MonarchTypeToConfirmInput(
                phrase = "acme-site",
                value = text,
                onValueChange = { text = it },
                showMismatch = shown,
            )
        }
        rule.onNodeWithText("The text does not match.", useUnmergedTree = true).assertDoesNotExist()
        shown = true
        rule.waitForIdle()
        rule.onNodeWithText("The text does not match.", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("acme").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Error))
        text = "acme-site"
        rule.waitForIdle()
        rule.onNodeWithText("The text does not match.", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun theMismatchMessageCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTypeToConfirmInput(
                phrase = "acme-site",
                value = "x",
                onValueChange = {},
                showMismatch = true,
                mismatchMessage = "Type the site name exactly.",
            )
        }
        rule.onNodeWithText("Type the site name exactly.", useUnmergedTree = true).assertExists()
    }

    @Test
    fun aMatchIsAnnouncedAndReportedOnlyWhenItFlips() {
        val flips = mutableListOf<Boolean>()
        rule.setMonarchContent {
            var text by remember { mutableStateOf("acme-site") }
            MonarchTypeToConfirmInput(
                phrase = "acme-site",
                value = text,
                onValueChange = { text = it },
                onMatchChange = { flips += it },
                mismatchAnnouncement = "No longer matches",
            )
        }
        rule.waitForIdle()
        assertEquals(emptyList(), flips)
        rule.onNodeWithText("acme-site").performTextReplacement("acme")
        rule.waitForIdle()
        rule.onNodeWithContentDescription("No longer matches")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithText("acme").performTextInput("-site")
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Text matches")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        assertEquals(listOf(false, true), flips)
    }

    @Test
    fun aDisabledFieldCannotBeEdited() {
        rule.setMonarchContent {
            MonarchTypeToConfirmInput(phrase = "acme-site", value = "acme", onValueChange = {}, enabled = false)
        }
        rule.onNodeWithText("acme").assertIsNotEnabled()
    }

    @Test
    fun theLabelIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTypeToConfirmInput(
                phrase = "acme",
                value = "",
                onValueChange = {},
                helperText = "This deletes the site.",
            )
        }
        rule.onNodeWithText("This deletes the site.", useUnmergedTree = true).assertTextFullyVisible()
    }
}

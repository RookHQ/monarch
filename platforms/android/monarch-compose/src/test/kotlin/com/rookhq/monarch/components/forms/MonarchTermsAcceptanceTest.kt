package com.rookhq.monarch.components.forms

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import kotlin.test.assertTrue
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsAcceptance
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsAcceptanceDetails
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsAcceptanceLabels
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsAcceptanceTokens
import com.rookhq.monarch.components.forms.termsacceptance.MonarchTermsDocument
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
import kotlin.test.Test
import kotlin.test.assertEquals

private val Terms = MonarchTermsDocument("Terms of Service", "https://example.com/terms", version = "2026-11-01")
private val Privacy =
    MonarchTermsDocument("Privacy Policy", "https://example.com/privacy", version = "3", id = "privacy")
private val Cookies = MonarchTermsDocument("Cookie Policy", "https://example.com/cookies")
private val Documents = listOf(Terms, Privacy)

internal object MonarchTermsAcceptanceFixtures : ComponentFixtures {
    override val id = "terms-acceptance"
    override val fixtures = listOf(
        fixture("unticked, two documents") {
            MonarchTermsAcceptance(accepted = false, onAcceptedChange = { _, _ -> }, documents = Documents)
        },
        fixture("required error") {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = Documents,
                showRequiredError = true,
            )
        },
        fixture("accepted, three documents") {
            MonarchTermsAcceptance(
                accepted = true,
                onAcceptedChange = { _, _ -> },
                documents = Documents + Cookies,
                onOpenDocument = {},
            )
        },
        fixture("optional, disabled") {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = listOf(Terms),
                required = false,
                enabled = false,
            )
        },
    )
}

/** The bounds of the first letter of [part] inside the text of the node, in the node's own coordinates. */
private fun SemanticsNodeInteraction.boundsOfText(part: String): androidx.compose.ui.geometry.Rect {
    val node = fetchSemanticsNode()
    val results = mutableListOf<TextLayoutResult>()
    node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
    val layout = results.single()
    val start = layout.layoutInput.text.text.indexOf(part)
    check(start >= 0) { "no '$part' in the text" }
    // The first letter: a link may wrap onto the next line, so its whole box is not one rectangle.
    return layout.getBoundingBox(start)
}

/** Taps the first letter of [part] in the sentence: where the link is drawn. */
private fun SemanticsNodeInteraction.clickLink(part: String) {
    val bounds = boundsOfText(part)
    performTouchInput { click(bounds.center) }
}

/** Taps the check box at the start of the row, away from the links. */
private fun SemanticsNodeInteraction.clickBox(layoutDirection: LayoutDirection = LayoutDirection.Ltr) {
    performTouchInput {
        val x = if (layoutDirection == LayoutDirection.Ltr) 8f else width - 8f
        click(androidx.compose.ui.geometry.Offset(x, height / 2f))
    }
}

@RunWith(RobolectricTestRunner::class)
class MonarchTermsAcceptanceTest {
    @get:Rule
    val rule = createComposeRule()

    private val sentence = "I agree to the Terms of Service and Privacy Policy"

    @Test
    fun tickingReportsTheVersionsAndTheTime() {
        val reported = mutableListOf<Pair<Boolean, MonarchTermsAcceptanceDetails>>()
        var accepted by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = accepted,
                onAcceptedChange = { value, details ->
                    reported += value to details
                    accepted = value
                },
                documents = Documents + Cookies,
                clock = { 42L },
            )
        }
        val box = rule.onNodeWithText("I agree to the Terms of Service, Privacy Policy, and Cookie Policy")
        box.assertIsOff().assertTouchTargetAtLeast48dp(rule.density).clickBox()
        box.assertIsOn()
        assertEquals(true, reported.single().first)
        val versions = mapOf("https://example.com/terms" to "2026-11-01", "privacy" to "3")
        assertEquals(versions, reported.single().second.versions)
        assertEquals(42L, reported.single().second.dateMillis)
    }

    @Test
    fun aRequiredBoxSaysSoAndShowsTheErrorUntilTicked() {
        var accepted by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = accepted,
                onAcceptedChange = { value, _ -> accepted = value },
                documents = Documents,
                showRequiredError = true,
            )
        }
        val box = rule.onNode(hasText(sentence, substring = true))
        box.assert(SemanticsMatcher.expectValue(SemanticsProperties.Error, "Accept the terms to continue"))
        rule.onNodeWithText("Required", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Accept the terms to continue").assertExists()
        rule.onNodeWithContentDescription("Accept the terms to continue")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        box.clickBox()
        rule.onNodeWithText("Accept the terms to continue").assertDoesNotExist()
        box.assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Error))
    }

    @Test
    fun anOptionalBoxNeverShowsTheError() {
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = Documents,
                required = false,
                showRequiredError = true,
            )
        }
        rule.onNodeWithText("Accept the terms to continue").assertDoesNotExist()
        rule.onNodeWithText("Required", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun eachDocumentIsALinkThatOpensItsUrl() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchTermsAcceptance(accepted = false, onAcceptedChange = { _, _ -> }, documents = Documents)
            }
        }
        rule.onNode(hasText(sentence, substring = true), useUnmergedTree = true).clickLink("Privacy Policy")
        assertEquals(listOf("https://example.com/privacy"), opened)
    }

    @Test
    fun theAppCanOpenDocumentsItself() {
        val opened = mutableListOf<MonarchTermsDocument>()
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = Documents,
                onOpenDocument = { opened += it },
            )
        }
        rule.onNode(hasText(sentence, substring = true), useUnmergedTree = true).clickLink("Terms of Service")
        assertEquals(listOf(Terms), opened)
    }

    @Test
    fun disablingDisablesTheBoxAndTheLinks() {
        var changes = 0
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> changes++ },
                documents = Documents,
                enabled = false,
                onOpenDocument = { changes++ },
            )
        }
        val row = rule.onNode(hasText(sentence, substring = true)).assertIsNotEnabled()
        row.performClick()
        // A disabled box has no links in its sentence.
        rule.onNode(hasText(sentence, substring = true), useUnmergedTree = true).clickLink("Terms of Service")
        assertEquals(0, changes)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = listOf(Terms),
                showRequiredError = true,
                labels = MonarchTermsAcceptanceLabels(
                    label = { "Acepto los $it" },
                    requiredMessage = "Acepta los términos",
                    required = "Obligatorio",
                ),
            )
        }
        rule.onNode(hasText("Acepto los Terms of Service", substring = true)).assertExists()
        rule.onNodeWithText("Acepta los términos").assertExists()
        rule.onNodeWithText("Obligatorio", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theBoxIsAtTheStartRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchTermsAcceptance(accepted = false, onAcceptedChange = { _, _ -> }, documents = Documents)
        }
        // The box is at the start, which is the right edge: the sentence starts at the left edge of the row.
        val row = rule.onNode(hasText(sentence, substring = true)).getBoundsInRoot()
        val text = rule.onNode(hasText(sentence, substring = true), useUnmergedTree = true).getBoundsInRoot()
        assertTrue(text.left == row.left && text.right < row.right, "row $row text $text")
    }

    @Test
    fun theSentenceWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchTermsAcceptance(
                accepted = false,
                onAcceptedChange = { _, _ -> },
                documents = Documents + Cookies,
                showRequiredError = true,
            )
        }
        val all = "I agree to the Terms of Service, Privacy Policy, and Cookie Policy"
        rule.onNodeWithText(all, useUnmergedTree = true).assertTextFullyVisible()
        rule.onNodeWithText("Accept the terms to continue").assertTextFullyVisible()
    }

    @Test
    fun titlesJoinAsAListAndVersionsSkipDocumentsWithout() {
        val join = { items: List<String> ->
            MonarchTermsAcceptanceTokens.join(
                items,
                two = { a, b -> "$a & $b" },
                middle = { a, b -> "$a; $b" },
                last = { a, b -> "$a; & $b" },
            )
        }
        assertEquals("", join(emptyList()))
        assertEquals("A", join(listOf("A")))
        assertEquals("A & B", join(listOf("A", "B")))
        assertEquals("A; B; C; & D", join(listOf("A", "B", "C", "D")))
        assertEquals(mapOf("privacy" to "3"), MonarchTermsDocument.versions(listOf(Privacy, Cookies)))
        assertEquals("https://example.com/cookies", Cookies.key)
        assertEquals(true, MonarchTermsAcceptanceTokens.showsError(required = true, showRequiredError = true, false))
        assertEquals(false, MonarchTermsAcceptanceTokens.showsError(required = true, showRequiredError = true, true))
    }

    @Test
    fun theErrorColourComesFromTheRoleInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.dangerStrong, MonarchTermsAcceptanceTokens.errorColor(c), theme.id)
        }
    }
}

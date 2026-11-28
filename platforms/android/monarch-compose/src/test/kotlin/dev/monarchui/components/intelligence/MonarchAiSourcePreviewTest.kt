package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aisourcepreview.MonarchAiSourcePreview
import dev.monarchui.components.intelligence.aisourcepreview.MonarchAiSourcePreviewLabels
import dev.monarchui.components.intelligence.aisourcepreview.MonarchAiSourcePreviewTokens
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Doc = MonarchAiSourcePart("s1", "Q3 report", "https://www.example.com/q3", MonarchAiSourceKind.Doc)
private const val Content = "Summary.\n\nRevenue grew 12% over the quarter."

internal object MonarchAiSourcePreviewFixtures : ComponentFixtures {
    override val id = "ai-source-preview"
    override val fixtures = listOf(
        fixture("quote found") { MonarchAiSourcePreview(source = Doc, quote = "revenue  GREW", content = Content) },
        fixture("quote not found, with close") {
            MonarchAiSourcePreview(source = Doc, quote = "Costs fell.", content = "Summary.", onClose = {})
        },
        fixture("quote only") { MonarchAiSourcePreview(source = Doc, quote = "Revenue grew 12%.") },
        fixture("empty, no original") {
            MonarchAiSourcePreview(source = Doc.copy(url = null, kind = MonarchAiSourceKind.Record))
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiSourcePreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingWithTheHostAndKind() {
        rule.setMonarchContent { MonarchAiSourcePreview(source = Doc, content = Content) }
        rule.onNodeWithText("Q3 report").assert(isHeading())
        rule.onNodeWithText("example.com · Document").assertExists()
    }

    @Test
    fun theQuoteIsHighlightedInItsParagraph() {
        rule.setMonarchContent { MonarchAiSourcePreview(source = Doc, quote = "revenue\n grew", content = Content) }
        rule.onNodeWithText("Revenue grew 12% over the quarter.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Quoted passage"))
        rule.onNodeWithText("Summary.")
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
        // The quote is found, so it is not repeated in a block of its own.
        rule.onNodeWithText("Quoted passage").assertDoesNotExist()
    }

    @Test
    fun theHighlightedParagraphIsScrolledIntoView() {
        val long = (1..30).joinToString("\n\n") { "Paragraph $it of the source." } + "\n\nThe cited line."
        rule.setMonarchContent {
            MonarchAiSourcePreview(
                source = Doc,
                quote = "the cited line",
                content = long,
                modifier = Modifier.height(300.dp),
            )
        }
        rule.onNodeWithText("The cited line.").assertIsDisplayed()
        rule.onNodeWithText("Paragraph 1 of the source.").assertIsNotDisplayed()
    }

    @Test
    fun aQuoteThatIsNotInTheContentIsShownOnItsOwn() {
        rule.setMonarchContent { MonarchAiSourcePreview(source = Doc, quote = " Costs fell. ", content = "Summary.") }
        rule.onNodeWithText("Quoted passage").assertExists()
        rule.onNodeWithText("Costs fell.").assertExists()
    }

    @Test
    fun withNothingToShowItSaysSo() {
        rule.setMonarchContent { MonarchAiSourcePreview(source = Doc) }
        rule.onNodeWithText("No preview is available for this source.").assertExists()
    }

    @Test
    fun openOriginalOpensTheAddress() {
        val opened = mutableListOf<String>()
        val handler = object : UriHandler {
            override fun openUri(uri: String) {
                opened += uri
            }
        }
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchAiSourcePreview(source = Doc, originalUrl = "https://example.com/original")
            }
        }
        rule.onNodeWithText("Open original").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("https://example.com/original"), opened)
    }

    @Test
    fun anUnsafeAddressHasNoOpenOriginal() {
        rule.setMonarchContent { MonarchAiSourcePreview(source = Doc.copy(url = "javascript:alert(1)")) }
        rule.onNodeWithText("Open original").assertDoesNotExist()
    }

    @Test
    fun closeAndOpenOriginalCallBack() {
        var closed = 0
        var original = ""
        rule.setMonarchContent {
            MonarchAiSourcePreview(
                source = Doc,
                labels = MonarchAiSourcePreviewLabels(close = "Fermer"),
                onClose = { closed++ },
                onOpenOriginal = { original = it },
            )
        }
        rule.onNodeWithText("Fermer").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithText("Open original").performClick()
        assertEquals(1, closed)
        assertEquals("https://www.example.com/q3", original)
    }

    @Test
    fun theCloseButtonIsAtTheEndInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiSourcePreview(source = Doc, onClose = {})
        }
        rule.onNodeWithText("Q3 report").assertIsBefore(rule.onNodeWithText("Close"), LayoutDirection.Rtl)
    }

    @Test
    fun theTitleWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchAiSourcePreview(source = Doc, onClose = {}) }
        rule.onNodeWithText("Q3 report").assertTextFullyVisible()
    }

    @Test
    fun quotesAreMatchedLiterallyIgnoringCaseAndSpacing() {
        assertEquals(4..8, MonarchAiSourcePreviewTokens.quoteRange("Say (a.b) now", "(A.B)\n"))
        assertNull(MonarchAiSourcePreviewTokens.quoteRange("Say axb now", "a.b"))
        assertNull(MonarchAiSourcePreviewTokens.quoteRange("text", "   "))
        val split = MonarchAiSourcePreviewTokens.paragraphs("One two.\n\nThree four.", "two. three")
        assertEquals(listOf("two.", "Three"), split.map { it.mark })
        assertEquals(2, MonarchAiSourcePreviewTokens.paragraphs("A\n\n \n\nB", null).size)
        assertTrue(MonarchAiSourcePreviewTokens.showsQuoteBlock("x", null))
        assertFalse(MonarchAiSourcePreviewTokens.showsQuoteBlock("x", "a x b"))
        assertTrue(MonarchAiSourcePreviewTokens.empty(" ", ""))
        assertEquals(
            0,
            MonarchAiSourcePreviewTokens.centredScroll(top = 10, height = 10, viewport = 100, maxScroll = 50),
        )
        assertEquals(
            50,
            MonarchAiSourcePreviewTokens.centredScroll(top = 500, height = 10, viewport = 100, maxScroll = 50),
        )
    }

    @Test
    fun theHighlightColoursComeFromTheAiRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.aiSubtle, MonarchAiSourcePreviewTokens.markColor(c), theme.id)
            assertEquals(c.aiBorder, MonarchAiSourcePreviewTokens.quoteBarColor(c), theme.id)
        }
    }
}

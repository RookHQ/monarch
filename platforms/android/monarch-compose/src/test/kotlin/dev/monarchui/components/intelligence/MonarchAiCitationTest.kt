package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitation
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationDefaults
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationLabels
import dev.monarchui.components.intelligence.aicitation.MonarchAiCitationTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiCitationPart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.components.intelligence.aiconversation.MonarchAiTextPart
import dev.monarchui.components.typography.text.MonarchText
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
import kotlin.test.assertNull

private val Web = MonarchAiSourcePart(
    id = "s1",
    title = "Pricing",
    url = "https://www.example.com/pricing",
    kind = MonarchAiSourceKind.Web,
)
private val Doc = MonarchAiSourcePart(id = "s2", title = "Q3 plan", kind = MonarchAiSourceKind.Doc, product = "Dossier")

@OptIn(ExperimentalLayoutApi::class)
internal object MonarchAiCitationFixtures : ComponentFixtures {
    override val id = "ai-citation"
    override val fixtures = listOf(
        fixture("closed, after a sentence") {
            FlowRow {
                MonarchText(text = "Seats are billed monthly.")
                MonarchAiCitation(index = 1, source = Web)
            }
        },
        fixture("open, web page with a link") { MonarchAiCitation(index = 1, source = Web, expanded = true) },
        fixture("open, document with a quote and open") {
            MonarchAiCitation(
                index = 12,
                source = Doc,
                quote = "Seats are billed monthly, prorated to the day they were added.",
                expanded = true,
                onOpen = {},
            )
        },
        fixture("open, no action") {
            MonarchAiCitation(
                index = 3,
                source = MonarchAiSourcePart(
                    id = "s3",
                    title = "Unsafe",
                    url = "javascript:alert(1)",
                    kind = MonarchAiSourceKind.Web,
                ),
                expanded = true,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiCitationTest {
    @get:Rule
    val rule = createComposeRule()

    private val button = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)

    @Test
    fun theMarkerIsAButtonNamedAfterTheSourceWithA48dpTarget() {
        rule.setMonarchContent { MonarchAiCitation(index = 2, source = Web) }
        rule.onNode(button)
            .assertContentDescriptionEquals("Source 2: Pricing")
            .assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("[2]").assertDoesNotExist()
    }

    @Test
    fun aTapOpensTheCardAndASecondTapClosesIt() {
        rule.setMonarchContent { MonarchAiCitation(index = 1, source = Web, quote = "Billed monthly.") }
        rule.onNodeWithText("Source 1").assertDoesNotExist()
        rule.onNode(button).performClick()
        rule.onNodeWithText("Source 1").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        rule.onNodeWithText("Web page · example.com").assertExists()
        rule.onNodeWithText("Billed monthly.").assertExists()
        rule.onNodeWithContentDescription("Visit page", substring = true).assertExists()
    }

    @Test
    fun openSourceClosesTheCardAndCallsBack() {
        var opened: MonarchAiSourcePart? = null
        var expanded = true
        rule.setMonarchContent {
            MonarchAiCitation(
                index = 1,
                source = Doc,
                expanded = expanded,
                onExpandedChange = { expanded = it },
                onOpen = { opened = it },
            )
        }
        rule.onNodeWithText("Document · Dossier").assertExists()
        rule.onNodeWithText("Open source").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(Doc, opened)
        assertEquals(false, expanded)
    }

    @Test
    fun anUnsafeAddressHasNoAction() {
        rule.setMonarchContent {
            MonarchAiCitation(
                index = 1,
                source = MonarchAiSourcePart(
                    id = "x",
                    title = "X",
                    url = "file:///etc/hosts",
                    kind = MonarchAiSourceKind.File,
                ),
                expanded = true,
            )
        }
        rule.onNodeWithText("File").assertExists()
        rule.onNodeWithText("Visit page", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Open source").assertDoesNotExist()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiCitation(
                index = 4,
                source = Web,
                expanded = true,
                labels = MonarchAiCitationLabels(
                    marker = { i, title -> "Quelle $i: $title" },
                    card = { "Quelle $it" },
                    kind = { "Seite" },
                ),
            )
        }
        rule.onNode(button).assertContentDescriptionEquals("Quelle 4: Pricing")
        rule.onNodeWithText("Quelle 4").assertExists()
        rule.onNodeWithText("Seite · example.com").assertExists()
    }

    @Test
    fun theMarkerFollowsTheSentenceInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            Row {
                MonarchText(text = "Billed monthly.")
                MonarchAiCitation(index = 1, source = Web)
            }
        }
        rule.onNodeWithText("Billed monthly.").assertIsBefore(rule.onNode(button), LayoutDirection.Rtl)
    }

    @Test
    fun aLongQuoteWrapsAtTheLargestFontScale() {
        val quote = "Seats are billed monthly, prorated to the day they were added, with tax."
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiCitation(index = 1, source = Web, quote = quote, expanded = true)
        }
        rule.onNodeWithText(quote).assertTextFullyVisible()
    }

    @Test
    fun sourcesAreNumberedInOrderOfFirstAppearance() {
        val numbers = MonarchAiCitationDefaults.sourceNumbers(
            listOf(
                MonarchAiTextPart(text = "a"),
                Doc,
                MonarchAiCitationPart(id = "c", sourceId = "s2"),
                Web,
                Doc,
            ),
        )
        assertEquals(mapOf("s2" to 1, "s1" to 2), numbers)
    }

    @Test
    fun onlySafeAddressesAreOpenedOrShown() {
        val tokens = MonarchAiCitationTokens
        assertEquals("https://a.example/x", tokens.safeUrl("https://a.example/x"))
        assertEquals("mailto:a@example.com", tokens.safeUrl("mailto:a@example.com"))
        assertNull(tokens.safeUrl("javascript:alert(1)"))
        assertNull(tokens.safeUrl("/relative/path"))
        assertNull(tokens.safeUrl("file:///etc/hosts"))
        assertNull(tokens.safeUrl(null))
        assertEquals("example.com", tokens.host("https://www.example.com/a"))
        assertNull(tokens.host("mailto:a@example.com"))
        assertEquals("[7]", tokens.markerText(7))
    }

    @Test
    fun theColoursComeFromTheThemeInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiCitation, MonarchAiCitationTokens.marker(colors))
            assertEquals(colors.bgSubtle, MonarchAiCitationTokens.quoteBackground(colors))
            assertEquals(colors.borderStrong, MonarchAiCitationTokens.quoteBar(colors))
            assertEquals(colors.textMuted, MonarchAiCitationTokens.meta(colors))
        }
    }
}

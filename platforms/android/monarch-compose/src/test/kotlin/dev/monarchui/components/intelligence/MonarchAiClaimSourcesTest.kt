@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaim
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimCounts
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimEvidence
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimEvidenceStance
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSource
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSources
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSourcesFilter
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSourcesLabels
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSourcesTokens
import dev.monarchui.components.intelligence.aiclaimsources.MonarchAiClaimSupport
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourceKind
import dev.monarchui.components.intelligence.aiconversation.MonarchAiSourcePart
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

private const val March4th2026 = 1_772_582_400_000L

private val Report = MonarchAiClaimSource(
    id = "report",
    title = "Q3 report",
    kind = MonarchAiSourceKind.Doc,
    publishedMillis = March4th2026,
)
private val Passage = MonarchAiClaimEvidence(
    id = "e1",
    sourceId = "report",
    quote = "Churn was 2.1%.",
    location = "Page 4",
    stance = MonarchAiClaimEvidenceStance.Supports,
)
private val Supported = MonarchAiClaim(
    id = "a",
    text = "Churn fell.",
    support = MonarchAiClaimSupport.Supported,
    evidence = listOf(Passage),
)
private val Unsupported = MonarchAiClaim(
    id = "b",
    text = "Prices caused it.",
    support = MonarchAiClaimSupport.Unsupported,
)
private val Unchecked = MonarchAiClaim(id = "c", text = "Q4 looks better.", support = MonarchAiClaimSupport.Unchecked)
private val Short = MonarchAiClaimSourcesLabels(summary = { "" })

internal object MonarchAiClaimSourcesFixtures : ComponentFixtures {
    override val id = "ai-claim-sources"
    override val fixtures = listOf(
        fixture("closed claims with icons") {
            MonarchAiClaimSources(
                claims = listOf(Supported, Unsupported),
                sources = listOf(Report),
                selectedClaimId = "b",
                supportIcon = { MonarchIcon(TestIcon, contentDescription = null) },
                expandIcon = { MonarchIcon(TestIcon, contentDescription = null) },
            )
        },
        fixture("open claim with a passage") {
            MonarchAiClaimSources(
                claims = listOf(Supported.copy(evidence = listOf(Passage.copy(location = null, stance = null)))),
                sources = listOf(Report.copy(kind = null, publishedMillis = null)),
                expandedIds = setOf("a"),
                labels = Short,
            )
        },
        fixture("open unchecked claim with actions") {
            MonarchAiClaimSources(
                claims = listOf(Unchecked),
                sources = emptyList(),
                expandedIds = setOf("c"),
                onFlag = {},
                onVerify = {},
                labels = Short,
            )
        },
        fixture("nothing needs checking") {
            MonarchAiClaimSources(
                claims = listOf(Supported),
                sources = listOf(Report),
                filter = MonarchAiClaimSourcesFilter.NeedsChecking,
            )
        },
        fixture("no claims") { MonarchAiClaimSources(claims = emptyList(), sources = emptyList()) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiClaimSourcesTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchAiClaimSourcesTokens

    @Test
    fun eachClaimIsADisclosureThatSaysItsSupportInWords() {
        val opened = mutableListOf<Set<String>>()
        val selected = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(Supported, Unsupported),
                sources = listOf(Report),
                onExpandedChange = { opened += it },
                onSelectClaim = { selected += it },
            )
        }
        rule.onNodeWithText("2 claims: 1 supported, 1 without a source").assertExists()
        val header = rule.onNodeWithText("Prices caused it.", substring = true)
        assertEquals("Collapsed", header.stateDescription())
        header.assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("No source supports this", substring = true).assertExists()
        header.performClick()
        assertEquals("Expanded", header.stateDescription())
        header.assertIsSelected()
        rule.onNodeWithText("No source supports this claim. Check it yourself before you rely on it.").assertExists()
        assertEquals(listOf(setOf("b")), opened)
        assertEquals(listOf("b"), selected)
    }

    @Test
    fun anOpenClaimShowsThePassageWithItsSourceAndStance() {
        var openedSource: Pair<MonarchAiClaimSource, MonarchAiClaimEvidence>? = null
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(Supported),
                sources = listOf(Report),
                expandedIds = setOf("a"),
                onOpenSource = { source, evidence -> openedSource = source to evidence },
            )
        }
        rule.onNodeWithText("Churn was 2.1%.").assertExists()
        rule.onNodeWithText("Q3 report").assertExists()
        rule.onNodeWithText("Document · Page 4 · Published Mar 4, 2026").assertExists()
        rule.onNodeWithText("Supports the claim").assertExists()
        rule.onNodeWithContentDescription("Passages for: Churn fell.").assertExists()
        rule.onNodeWithContentDescription("Open source from Q3 report")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(Report to Passage, openedSource)
    }

    @Test
    fun openSourceNeedsACallbackOrASafeAddress() {
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(
                    Supported.copy(
                        evidence = listOf(
                            Passage,
                            Passage.copy(id = "e2", sourceId = "web"),
                            Passage.copy(id = "e3", sourceId = "gone"),
                        ),
                    ),
                ),
                sources = listOf(
                    Report.copy(url = "javascript:alert(1)"),
                    MonarchAiClaimSource(
                        MonarchAiSourcePart(
                            id = "web",
                            title = "Blog",
                            url = "https://example.com",
                            kind = MonarchAiSourceKind.Web,
                        ),
                    ),
                ),
                expandedIds = setOf("a"),
            )
        }
        rule.onNodeWithContentDescription("Open source from Q3 report").assertDoesNotExist()
        rule.onNodeWithContentDescription("Open source from Blog").assertExists()
        rule.onNodeWithText("Unknown source").assertExists()
        rule.onNodeWithContentDescription("Open source from Unknown source").assertDoesNotExist()
    }

    @Test
    fun copyQuoteReportsThePassage() {
        val copied = mutableListOf<MonarchAiClaimEvidence>()
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(Supported),
                sources = listOf(Report),
                expandedIds = setOf("a"),
                onCopyQuote = { copied += it },
            )
        }
        rule.onNodeWithContentDescription("Copy quote", substring = true).performClick()
        assertEquals(listOf(Passage), copied)
    }

    @Test
    fun checkAgainShowsOnlyForUncheckedClaimsAndReportAProblemForEvery() {
        val verified = mutableListOf<String>()
        val flagged = mutableListOf<String>()
        rule.setMonarchContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                MonarchAiClaimSources(
                    claims = listOf(Unchecked, Unsupported),
                    sources = emptyList(),
                    expandedIds = setOf("b", "c"),
                    onFlag = { flagged += it },
                    onVerify = { verified += it },
                )
            }
        }
        rule.onNodeWithText("Check again").performClick()
        assertEquals(listOf("c"), verified)
        rule.onAllNodes(hasText("Report a problem"))[1].performScrollTo().performClick()
        assertEquals(listOf("b"), flagged)
    }

    @Test
    fun theFilterShowsTheClaimsThatNeedCheckingAndAnnouncesTheResult() {
        val filters = mutableListOf<MonarchAiClaimSourcesFilter>()
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(Supported, Unsupported),
                sources = listOf(Report),
                onFilterChange = { filters += it },
            )
        }
        rule.onNodeWithContentDescription("Showing", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Needs checking").performClick()
        rule.onNodeWithText("Churn fell.", substring = true).assertDoesNotExist()
        rule.onNodeWithText("Prices caused it.", substring = true).assertExists()
        assertEquals(listOf(MonarchAiClaimSourcesFilter.NeedsChecking), filters)
        rule.onNodeWithContentDescription("Showing 1 of 2 claims")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun theEmptyStatesSaySo() {
        rule.setMonarchContent {
            Column {
                MonarchAiClaimSources(claims = emptyList(), sources = emptyList())
                MonarchAiClaimSources(
                    claims = listOf(Supported),
                    sources = emptyList(),
                    filter = MonarchAiClaimSourcesFilter.NeedsChecking,
                )
            }
        }
        rule.onNodeWithText("No claims").assertExists()
        rule.onNodeWithText("There are no claims to check.").assertExists()
        rule.onNodeWithText("Nothing needs checking: every claim is supported by a source.").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiClaimSources(
                claims = listOf(Unsupported),
                sources = emptyList(),
                labels = MonarchAiClaimSourcesLabels(
                    heading = "Fuentes",
                    summary = { "${it.total} afirmación" },
                    support = { "Sin fuente" },
                    passages = { "$it pasajes" },
                ),
            )
        }
        rule.onNodeWithText("Fuentes").assertExists()
        rule.onNodeWithText("1 afirmación").assertExists()
        rule.onNodeWithText("Sin fuente", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("0 pasajes", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theBadgeComesBeforeTheCountRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiClaimSources(claims = listOf(Supported), sources = listOf(Report))
        }
        rule.onNodeWithText("Supported", useUnmergedTree = true)
            .assertIsBefore(rule.onNodeWithText("1 passage", useUnmergedTree = true), LayoutDirection.Rtl)
    }

    @Test
    fun aLongClaimWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiClaimSources(
                claims = listOf(Supported.copy(text = "Churn fell to 2.1% in the third quarter")),
                sources = emptyList(),
                labels = Short,
            )
        }
        rule.onNodeWithText("Churn fell to 2.1% in the third quarter", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun claimsAreCountedFilteredAndToggled() {
        val claims = listOf(Supported, Unsupported, Unchecked)
        assertEquals(MonarchAiClaimCounts(3, 2, 1, 0, 1, 0, 1), MonarchAiClaimCounts.of(claims))
        assertEquals(listOf(Unsupported, Unchecked), tokens.shown(claims, MonarchAiClaimSourcesFilter.NeedsChecking))
        assertEquals(claims, tokens.shown(claims, MonarchAiClaimSourcesFilter.All))
        assertEquals(setOf("a", "b"), tokens.toggled(setOf("a"), "b"))
        assertEquals(emptySet(), tokens.toggled(setOf("a"), "a"))
        assertEquals(MonarchTone.Danger, tokens.tone(MonarchAiClaimSupport.Unsupported))
        assertEquals(MonarchTone.Warning, tokens.tone(MonarchAiClaimSupport.Conflicting))
        assertEquals(MonarchTone.Neutral, tokens.tone(MonarchAiClaimSupport.Unchecked))
        assertEquals("Mar 4, 2026", tokens.publishedDate(March4th2026, Locale.US))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.successStrong, tokens.stanceColor(MonarchAiClaimEvidenceStance.Supports, c), theme.id)
            assertEquals(c.dangerStrong, tokens.stanceColor(MonarchAiClaimEvidenceStance.Contradicts, c), theme.id)
            assertEquals(c.dangerBorder, tokens.quoteBar(MonarchAiClaimEvidenceStance.Contradicts, c), theme.id)
            assertEquals(c.borderStrong, tokens.quoteBar(null, c), theme.id)
            assertEquals(c.accentSubtle, tokens.headerBackground(selected = true, c), theme.id)
            assertEquals(c.accentStrong, tokens.selectedBar(c), theme.id)
            assertEquals(c.bgSubtle, tokens.quoteBackground(c), theme.id)
        }
    }
}

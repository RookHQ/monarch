package dev.monarchui.components.identity

import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.identity.consenthistory.MonarchConsentCategory
import dev.monarchui.components.identity.consenthistory.MonarchConsentHistory
import dev.monarchui.components.identity.consenthistory.MonarchConsentHistoryEntry
import dev.monarchui.components.identity.consenthistory.MonarchConsentHistoryLabels
import dev.monarchui.components.identity.consenthistory.MonarchConsentHistoryTokens
import dev.monarchui.components.identity.consenthistory.MonarchConsentMethod
import dev.monarchui.components.identity.consenthistory.MonarchConsentRecord
import dev.monarchui.components.identity.consenthistory.MonarchConsentRegime
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

private const val Day = 86_400_000L
private const val Start = 1_767_225_600_000L

private val Categories = listOf(
    MonarchConsentCategory("necessary", "Necessary"),
    MonarchConsentCategory("analytics", "Analytics"),
)

private val Older = MonarchConsentRecord(
    id = "1",
    dateMillis = Start,
    method = MonarchConsentMethod.AcceptAll,
    policyVersion = "2026-01",
    regime = MonarchConsentRegime.OptIn,
    categories = mapOf("analytics" to true, "necessary" to true),
)
private val Newer = MonarchConsentRecord(
    id = "2",
    dateMillis = Start + Day,
    method = MonarchConsentMethod.Custom,
    policyVersion = "2026-03",
    regime = MonarchConsentRegime.OptIn,
    categories = mapOf("analytics" to false, "zz-ads" to false, "necessary" to true),
    globalPrivacyControl = true,
)
private val Plain = MonarchConsentRecord(
    id = "3",
    dateMillis = Start,
    method = MonarchConsentMethod.Acknowledged,
    policyVersion = "1",
    regime = MonarchConsentRegime.Notice,
    categories = emptyMap(),
)

private fun dated(millis: Long) = "Day ${millis / Day}"

internal object MonarchConsentHistoryFixtures : ComponentFixtures {
    override val id = "consent-history"
    override val fixtures = listOf(
        fixture("one record with categories") {
            MonarchConsentHistory(records = listOf(Older), categories = Categories, formatDate = ::dated)
        },
        fixture("privacy signal, relative time") {
            MonarchConsentHistory(records = listOf(Plain.copyWithSignal()), showRelativeTime = true)
        },
        fixture("export") {
            MonarchConsentHistory(records = listOf(Plain), title = "Consent", formatDate = ::dated, onExport = {})
        },
        fixture("empty") { MonarchConsentHistory(records = emptyList(), onExport = {}) },
    )

    private fun MonarchConsentRecord.copyWithSignal() = MonarchConsentRecord(
        id = id,
        dateMillis = dateMillis,
        method = MonarchConsentMethod.GlobalPrivacyControl,
        policyVersion = policyVersion,
        regime = MonarchConsentRegime.OptOut,
        categories = categories,
        globalPrivacyControl = true,
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchConsentHistoryTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun recordsAreNewestFirstWithHeadings() {
        rule.setMonarchContent {
            MonarchConsentHistory(records = listOf(Older, Newer), categories = Categories, formatDate = ::dated)
        }
        rule.onNodeWithText("Consent history").assert(isHeading())
        val newer = rule.onNodeWithText(dated(Newer.dateMillis)).assert(isHeading()).getBoundsTop()
        val older = rule.onNodeWithText(dated(Older.dateMillis)).getBoundsTop()
        assertEquals(true, newer < older)
    }

    @Test
    fun eachLineAndCategoryIsOneElementInWords() {
        rule.setMonarchContent {
            MonarchConsentHistory(records = listOf(Newer), categories = Categories, formatDate = ::dated)
        }
        rule.onNodeWithContentDescription("Choice, Custom choice, Browser privacy signal present").assertExists()
        rule.onNodeWithContentDescription("Policy version, 2026-03").assertExists()
        rule.onNodeWithContentDescription("Rules applied, Asked before use (opt-in)").assertExists()
        rule.onNodeWithContentDescription("Analytics, Not allowed").assertExists()
        rule.onNodeWithContentDescription("Necessary, Allowed").assertExists()
        rule.onNodeWithContentDescription("zz-ads, Not allowed").assertExists()
    }

    @Test
    fun theDownloadIsBusyWhileItRuns() {
        var exports = 0
        rule.setMonarchContent {
            MonarchConsentHistory(records = listOf(Plain), onExport = { exports++; awaitCancellation() })
        }
        rule.onNodeWithText("Download history").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Download history").performClick()
        rule.waitForIdle()
        assertEquals(1, exports)
    }

    @Test
    fun aFailedDownloadIsShownAndAnnounced() {
        rule.setMonarchContent {
            MonarchConsentHistory(
                records = listOf(Plain),
                exportTitle = "Download CSV",
                onExport = { throw IOException() },
            )
        }
        rule.onNodeWithText("Download CSV").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("The download did not start. Try again.").assertExists()
        rule.onNodeWithContentDescription("The download did not start. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun anEmptyHistoryShowsTheEmptyStateAndNoDownload() {
        rule.setMonarchContent { MonarchConsentHistory(records = emptyList(), onExport = {}) }
        rule.onNodeWithText("No consent choices recorded", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("Download history").assertCountEquals(0)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchConsentHistory(
                records = listOf(Older),
                categories = Categories,
                title = "Historial",
                formatDate = ::dated,
                labels = MonarchConsentHistoryLabels(allowed = "Permitido", methodTitle = "Elección"),
            )
        }
        rule.onNodeWithText("Historial").assertExists()
        rule.onNodeWithContentDescription("Analytics, Permitido").assertExists()
        rule.onNodeWithContentDescription("Elección, Accepted all").assertExists()
    }

    @Test
    fun recordsStartAtTheRightRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchConsentHistory(records = listOf(Older), formatDate = ::dated)
        }
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val date = rule.onNodeWithText("Date").fetchSemanticsNode().boundsInRoot
        assertEquals(true, date.left > root.width / 2)
    }

    @Test
    fun linesWrapAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchConsentHistory(records = listOf(Newer), formatDate = ::dated)
        }
        rule.onNodeWithText("Browser privacy signal present", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun categoriesAreListedFirstThenUnknownIdsSorted() {
        val entries = MonarchConsentHistoryTokens.entries(Newer, Categories)
        assertEquals(
            listOf(
                MonarchConsentHistoryEntry("Necessary", true),
                MonarchConsentHistoryEntry("Analytics", false),
                MonarchConsentHistoryEntry("zz-ads", false),
            ),
            entries,
        )
        val same = Older.let {
            MonarchConsentRecord("3", it.dateMillis, it.method, it.policyVersion, it.regime, it.categories)
        }
        val sorted = MonarchConsentHistoryTokens.sorted(listOf(Older, Newer, same))
        assertEquals(listOf("2", "1", "3"), sorted.map { it.id })
        assertEquals("A, B", MonarchConsentHistoryTokens.spokenLine("A", "B", null))
    }

    @Test
    fun theColoursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val record = MonarchConsentHistoryTokens.recordColors(c)
            assertEquals(c.bgSurface, record.container, theme.id)
            assertEquals(c.borderSubtle, record.border, theme.id)
            assertEquals(c.successStrong, MonarchConsentHistoryTokens.categoryColor(allowed = true, c), theme.id)
            assertEquals(c.textMuted, MonarchConsentHistoryTokens.categoryColor(allowed = false, c), theme.id)
        }
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.getBoundsTop() =
        fetchSemanticsNode().boundsInRoot.top
}

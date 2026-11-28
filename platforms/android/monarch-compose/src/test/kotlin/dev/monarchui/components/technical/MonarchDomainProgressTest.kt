package dev.monarchui.components.technical

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.monarchui.components.technical.domainprogress.MonarchDomainDnsSnapshot
import dev.monarchui.components.technical.domainprogress.MonarchDomainNotify
import dev.monarchui.components.technical.domainprogress.MonarchDomainProblem
import dev.monarchui.components.technical.domainprogress.MonarchDomainProblemSeverity
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgress
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgressDefaults
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgressLabels
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgressRules
import dev.monarchui.components.technical.domainprogress.MonarchDomainProgressTokens
import dev.monarchui.components.technical.domainprogress.MonarchDomainStage
import dev.monarchui.components.technical.domainprogress.MonarchDomainStep
import dev.monarchui.components.technical.domainprogress.MonarchDomainStepState
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val Host = "book.example.com"
private const val Now = 1_772_636_400_000L
private const val Minute = 60_000L

internal object MonarchDomainProgressFixtures : ComponentFixtures {
    override val id = "domain-progress"
    override val fixtures = listOf(
        fixture("dns, rechecking, checked") {
            MonarchDomainProgress(
                host = Host,
                stage = MonarchDomainStage.Dns,
                rechecking = false,
                dnsFound = MonarchDomainDnsSnapshot(),
                checkedAt = Now - 2 * Minute,
                now = Now,
            )
        },
        fixture("ownership, blocking problem") {
            MonarchDomainProgress(
                host = Host,
                stage = MonarchDomainStage.Ownership,
                problems = listOf(MonarchDomainProblem("Add the TXT record.")),
                rechecking = false,
            )
        },
        fixture("certificate, info problem, email") {
            MonarchDomainProgress(
                host = Host,
                stage = MonarchDomainStage.Certificate,
                problems = listOf(MonarchDomainProblem("Pending.", severity = MonarchDomainProblemSeverity.Info)),
                notify = MonarchDomainNotify.Email,
            )
        },
        fixture("page, in app") {
            MonarchDomainProgress(host = Host, stage = MonarchDomainStage.Page, notify = MonarchDomainNotify.InApp)
        },
        fixture("live") { MonarchDomainProgress(host = Host, stage = MonarchDomainStage.Live) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchDomainProgressTest {
    @get:Rule
    val rule = createComposeRule()

    private val t = MonarchDomainProgressTokens
    private val r = MonarchDomainProgressRules

    @Test
    fun stagesMapToStepStates() {
        val done = MonarchDomainStepState.Done
        val current = MonarchDomainStepState.Current
        val todo = MonarchDomainStepState.Todo
        fun states(stage: MonarchDomainStage) = MonarchDomainStep.entries.map { r.states(stage, emptyList())[it] }
        assertEquals(listOf(current, todo, todo), states(MonarchDomainStage.Dns))
        assertEquals(listOf(current, todo, todo), states(MonarchDomainStage.Ownership))
        assertEquals(listOf(done, current, todo), states(MonarchDomainStage.Certificate))
        assertEquals(listOf(done, done, current), states(MonarchDomainStage.Page))
        assertEquals(listOf(done, done, done), states(MonarchDomainStage.Live))
    }

    @Test
    fun blockingProblemsMarkTheirStepUnlessItIsDone() {
        val error = MonarchDomainStepState.Error
        val onCurrent = r.states(MonarchDomainStage.Certificate, listOf(MonarchDomainProblem("x")))
        assertEquals(error, onCurrent[MonarchDomainStep.Certificate])
        val onDone = r.states(
            MonarchDomainStage.Certificate,
            listOf(MonarchDomainProblem("x", step = MonarchDomainStep.Dns)),
        )
        assertEquals(MonarchDomainStepState.Done, onDone[MonarchDomainStep.Dns])
        val info = r.states(
            MonarchDomainStage.Dns,
            listOf(MonarchDomainProblem("x", severity = MonarchDomainProblemSeverity.Info)),
        )
        assertEquals(MonarchDomainStepState.Current, info[MonarchDomainStep.Dns])
    }

    @Test
    fun blockingProblemsAreShownBeforeInformationalOnes() {
        val info = MonarchDomainProblem("Pending.", severity = MonarchDomainProblemSeverity.Info)
        val second = MonarchDomainProblem("Later.", severity = MonarchDomainProblemSeverity.Info)
        val blocking = MonarchDomainProblem("Wrong record.")
        assertEquals(listOf(info), r.shownProblems(listOf(info, second)))
        assertEquals(listOf(blocking), r.shownProblems(listOf(info, blocking)))
        assertEquals(MonarchTone.Warning, r.tone(blocking))
        assertEquals(MonarchTone.Neutral, r.tone(info))
    }

    @Test
    fun snapshotsAreOneLine() {
        val cname = MonarchDomainDnsSnapshot(cnames = listOf("a.example.net"), a = listOf("1.2.3.4"))
        assertEquals("CNAME → a.example.net", r.snapshotText(cname, "-"))
        assertEquals(
            "A → 1.2.3.4, 5.6.7.8 · AAAA → ::1",
            r.snapshotText(MonarchDomainDnsSnapshot(a = listOf("1.2.3.4", "5.6.7.8"), aaaa = listOf("::1")), "-"),
        )
        assertEquals("AAAA → ::1", r.snapshotText(MonarchDomainDnsSnapshot(aaaa = listOf("::1")), "-"))
        assertEquals("none", r.snapshotText(MonarchDomainDnsSnapshot(), "none"))
    }

    @Test
    fun theRecheckIntervalIsClampedAndStopsWhenLive() {
        val d = MonarchDomainProgressDefaults
        assertEquals(15_000L, d.recheckIntervalMillis(null))
        assertEquals(30_000L, d.recheckIntervalMillis(MonarchDomainStage.Dns))
        assertEquals(15_000L, d.recheckIntervalMillis(MonarchDomainStage.Dns, suggestedMillis = 1_000L))
        assertEquals(300_000L, d.recheckIntervalMillis(MonarchDomainStage.Page, suggestedMillis = 900_000L))
        assertNull(d.recheckIntervalMillis(MonarchDomainStage.Live))
        assertNull(d.recheckIntervalMillis(MonarchDomainStage.Certificate, stopped = true))
    }

    @Test
    fun coloursComeFromTheThemeRoles() {
        AllThemes.forEach { tokens ->
            val colors = MonarchColors(tokens.colors)
            assertEquals(colors.successSolid, t.marker(MonarchDomainStepState.Done, colors).fill)
            assertEquals(colors.accentOnSolid, t.marker(MonarchDomainStepState.Current, colors).content)
            assertEquals(colors.warningSolid, t.marker(MonarchDomainStepState.Error, colors).fill)
            assertEquals(colors.borderDefault, t.marker(MonarchDomainStepState.Todo, colors).border)
            assertEquals(colors.textMuted, t.title(MonarchDomainStepState.Todo, colors))
            assertEquals(colors.textDefault, t.title(MonarchDomainStepState.Current, colors))
            assertEquals(colors.successSolid, t.connector(MonarchDomainStepState.Done, colors))
            assertEquals(colors.borderDefault, t.connector(MonarchDomainStepState.Current, colors))
        }
    }

    @Test
    fun eachStepIsReadAsItsTitleWithStateAndDetail() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchDomainProgress(host = Host, stage = MonarchDomainStage.Certificate) }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Progress for book.example.com").assertExists()
        assertEquals(
            "in progress, Being issued now, usually 1 to 5 minutes.",
            rule.onNodeWithContentDescription("HTTPS certificate").stateDescription(),
        )
        assertEquals(
            "done, Your domain points here.",
            rule.onNodeWithContentDescription("DNS record found").stateDescription(),
        )
        assertEquals(
            "not started, We load book.example.com over HTTPS before calling it live.",
            rule.onNodeWithContentDescription("Live").stateDescription(),
        )
    }

    @Test
    fun problemsDnsAndTheLastCheckAreShownUntilLive() {
        var stage by mutableStateOf(MonarchDomainStage.Dns)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchDomainProgress(
                host = Host,
                stage = stage,
                problems = listOf(MonarchDomainProblem("Remove the old A record.")),
                dnsFound = MonarchDomainDnsSnapshot(a = listOf("203.0.113.10")),
                checkedAt = Now - 2 * Minute,
                now = Now,
                notify = MonarchDomainNotify.Email,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        assertEquals(
            "needs attention, Add the record shown below at your DNS provider.",
            rule.onNodeWithContentDescription("Waiting for your DNS record").stateDescription(),
        )
        rule.onNodeWithText("Remove the old A record.", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText(
            "Public DNS for book.example.com shows A → 203.0.113.10. We keep checking automatically, even after you " +
                "close this page, and email you when it is live.",
        ).assertExists()
        rule.onNodeWithText("Checked 2 minutes ago.").assertExists()
        stage = MonarchDomainStage.Live
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.onAllNodesWithText("Checked 2 minutes ago.").assertCountEquals(0)
    }

    @Test
    fun aNewActiveStepIsAnnouncedPolitely() {
        var stage by mutableStateOf(MonarchDomainStage.Dns)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchDomainProgress(host = Host, stage = stage) }
        rule.mainClock.advanceTimeByFrame()
        rule.onAllNodesWithContentDescription("Step 1 of 3: Waiting for your DNS record, in progress.")
            .assertCountEquals(0)
        stage = MonarchDomainStage.Certificate
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Step 2 of 3: HTTPS certificate, in progress.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchDomainProgress(
                host = Host,
                stage = MonarchDomainStage.Live,
                labels = MonarchDomainProgressLabels(
                    title = { step, _, _ -> "Schritt ${step.name}" },
                    state = { "fertig" },
                    listLabel = { "Fortschritt $it" },
                ),
            )
        }
        rule.onNodeWithContentDescription("Fortschritt book.example.com").assertExists()
        assertEquals(
            "fertig, Your site loads on book.example.com.",
            rule.onNodeWithContentDescription("Schritt Live").stateDescription(),
        )
    }

    @Test
    fun textIsNotCutAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDomainProgress(host = Host, stage = MonarchDomainStage.Live)
        }
        rule.onNodeWithText("We load", substring = true, useUnmergedTree = true).assertDoesNotExist()
        rule.onNodeWithText("Your site loads on book.example.com.", useUnmergedTree = true)
            .assertTextFullyVisible()
        rule.onNodeWithText("HTTPS certificate issued", useUnmergedTree = true).assertTextFullyVisible()
    }
}

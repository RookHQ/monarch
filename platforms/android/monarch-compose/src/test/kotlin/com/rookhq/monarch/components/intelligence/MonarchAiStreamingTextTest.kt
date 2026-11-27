package com.rookhq.monarch.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownBlock
import com.rookhq.monarch.components.intelligence.aimarkdown.MonarchAiMarkdownParser
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingText
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingTextAnnouncer
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingTextLabels
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingTextRepair
import com.rookhq.monarch.components.intelligence.aistreamingtext.MonarchAiStreamingTextTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

private const val Answer = "## Summary\n\nThe project has **three open tasks**. One is overdue.\n\n" +
    "1. Review the *migration plan*.\n2. Update the changelog.\n\n| Task | Due |\n| :-- | --: |\n| Plan | Today |"

internal object MonarchAiStreamingTextFixtures : ComponentFixtures {
    override val id = "ai-streaming-text"
    override val fixtures = listOf(
        fixture("streaming, half-written emphasis") {
            MonarchAiStreamingText(text = "The project has **three open", streaming = true)
        },
        fixture("finished answer") { MonarchAiStreamingText(text = Answer, streaming = false) },
        fixture("streaming code block") {
            MonarchAiStreamingText(text = "Run this:\n\n```bash\nnpm run release", streaming = true)
        },
        fixture("with a footnote") {
            MonarchAiStreamingText(text = "Revenue grew[^1].\n\n[^1]: Unaudited figures.", streaming = false)
        },
        fixture("without a caret or announcements") {
            MonarchAiStreamingText(text = "Thinking it through", streaming = true, showCaret = false, announces = false)
        },
        fixture("empty while waiting") { MonarchAiStreamingText(text = "", streaming = true) },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiStreamingTextTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun openEmphasisAndCodeSpansAreClosedForDisplay() {
        assertEquals("Some **bold**", MonarchAiStreamingTextRepair.complete("Some **bold"))
        assertEquals("Some *it*", MonarchAiStreamingTextRepair.complete("Some *it"))
        assertEquals("Run `npm`", MonarchAiStreamingTextRepair.complete("Run `npm"))
        assertEquals("**bold *both***", MonarchAiStreamingTextRepair.complete("**bold *both"))
        assertEquals("snake_case", MonarchAiStreamingTextRepair.complete("snake_case"))
        assertEquals("Done ", MonarchAiStreamingTextRepair.complete("Done **"))
    }

    @Test
    fun aLinkShowsItsTextUntilItsTargetArrives() {
        assertEquals("See docs", MonarchAiStreamingTextRepair.complete("See [docs](https://exa"))
        assertEquals("See docs", MonarchAiStreamingTextRepair.complete("See [docs]"))
        assertEquals("See do", MonarchAiStreamingTextRepair.complete("See [do"))
        val link = "See [docs](https://example.com)"
        assertEquals(link, MonarchAiStreamingTextRepair.complete(link))
    }

    @Test
    fun tableRowsWaitForTheDelimiterRowAndCodeIsLeftAlone() {
        assertEquals("", MonarchAiStreamingTextRepair.complete("| Task | Due |"))
        val table = "| Task | Due |\n| --- | --- |"
        assertEquals(table, MonarchAiStreamingTextRepair.complete(table))
        assertEquals("```bash\nrun **x", MonarchAiStreamingTextRepair.complete("```bash\nrun **x"))
    }

    @Test
    fun theAnnouncerSaysWholeSentencesNeverTokens() {
        val announcer = MonarchAiStreamingTextAnnouncer()
        assertNull(announcer.step("The proj", 0, true, true, 0).say("Done"))
        assertEquals("The project is late.", announcer.step("The project is late. Two", 0, true, true, 10).say("Done"))
        val waiting = announcer.step("The project is late. Two tasks remain. Th", 0, true, true, 500)
        assertNull(waiting.say("Done"))
        assertEquals(510L, waiting.retryAfterMillis)
        val next = announcer.step("The project is late. Two tasks remain. Th", 0, true, true, 1_010)
        assertEquals("Two tasks remain.", next.say("Done"))
        val end = announcer.step("The project is late. Two tasks remain. That is all", 0, false, true, 2_100)
        assertEquals("That is all\n\nDone", end.say("Done"))
    }

    @Test
    fun historyIsNotAnnouncedAndCompleteCanWaitForTheWholeReply() {
        val history = MonarchAiStreamingTextAnnouncer()
        assertNull(history.step("Earlier answer.", 15, false, true, 0).say("Done"))

        val announcer = MonarchAiStreamingTextAnnouncer()
        announcer.step("Text", 0, true, false, 0)
        assertEquals("Text", announcer.step("Text", 4, false, false, 2_000).say("Done"))
        assertNull(announcer.step("Text", 4, false, false, 3_000).say("Done"))
        assertEquals("Done", announcer.step("Text", 4, false, true, 4_000).say("Done"))
    }

    @Test
    fun aDifferentAnswerStartsOver() {
        val announcer = MonarchAiStreamingTextAnnouncer()
        assertEquals("First answer.", announcer.step("First answer. More", 0, true, true, 0).say("Done"))
        assertEquals("New one.", announcer.step("New one. And", 0, true, true, 5_000).say("Done"))
    }

    @Test
    fun theSentenceBoundaryFollowsClosingQuotesAndSkipsUnfinishedOnes() {
        assertEquals(12, MonarchAiStreamingTextAnnouncer.sentenceBoundary("He said \"x.\" Then", 0))
        assertEquals(3, MonarchAiStreamingTextAnnouncer.sentenceBoundary("abc", 3))
        assertEquals(0, MonarchAiStreamingTextAnnouncer.sentenceBoundary("No end yet", 0))
    }

    @Test
    fun codeBlocksAndTablesAreNamedNotRead() {
        val blocks = MonarchAiMarkdownParser.parse("```kotlin\nval x = 1\n```\n\n| a |\n| - |\n| 1 |")
        assertEquals(
            listOf("Code block", "Table"),
            blocks.map { MonarchAiStreamingTextTokens.spoken(it, "Code block", "Table") },
        )
    }

    @Test
    fun finishedBlocksKeepTheirIdentityWhileTokensArrive() {
        val first = MonarchAiMarkdownParser.parseIncremental("Para one.\n\nPara tw", null)
        val second = MonarchAiMarkdownParser.parseIncremental("Para one.\n\nPara two is here", first)
        assertSame(first.blocks[0], second.blocks[0])
        assertTrue(second.blocks[1] is MonarchAiMarkdownBlock.Paragraph)
        val sources = MonarchAiStreamingTextTokens.sources(second.text, second.offsets)
        assertEquals(listOf("Para one.\n\n", "Para two is here"), sources)
        assertEquals(
            listOf("Para one.\n\n", "Para two is here"),
            MonarchAiStreamingTextTokens.display(sources, second.blocks, streaming = true),
        )
    }

    @Test
    fun halfWrittenSyntaxNeverShows() {
        rule.setMonarchContent { MonarchAiStreamingText(text = "Some **bold wor", streaming = true) }
        rule.onNodeWithText("Some bold wor", useUnmergedTree = true).assertExists()
        rule.onAllNodesWithText("**", substring = true, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun sentencesAreAnnouncedPolitelyThenTheEnd() {
        var text by mutableStateOf("The plan is ready. Two")
        var streaming by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiStreamingText(text = text, streaming = streaming) }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("The plan is ready.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.mainClock.advanceTimeBy(1_200)
        text = "The plan is ready. Two tasks remain"
        streaming = false
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Two tasks remain\n\nResponse complete").assertExists()
    }

    @Test
    fun theSpokenStringsCanBeReplacedAndAnnouncementsTurnedOff() {
        var streaming by mutableStateOf(true)
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiStreamingText(
                text = "```kotlin\nval x = 1\n```\n\nDone",
                streaming = streaming,
                labels = MonarchAiStreamingTextLabels(complete = "Fertig", codeBlock = "Code"),
            )
        }
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Code").assertExists()
        rule.mainClock.advanceTimeBy(1_200)
        streaming = false
        Snapshot.sendApplyNotifications()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithContentDescription("Done\n\nFertig").assertExists()
    }

    @Test
    fun anAnswerThatWasNeverStreamedIsNotAnnounced() {
        rule.setMonarchContent {
            MonarchAiStreamingText(text = "An earlier answer. It is complete.", streaming = false)
        }
        rule.waitForIdle()
        rule.onAllNodes(hasContentDescription("An earlier answer.", substring = true)).assertCountEquals(0)
    }

    @Test
    fun noAnnouncementsWhenTurnedOff() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent { MonarchAiStreamingText(text = "One. Two", streaming = true, announces = false) }
        rule.mainClock.advanceTimeBy(100)
        rule.onAllNodes(hasContentDescription("One.")).assertCountEquals(0)
    }

    @Test
    fun textWrapsAtTheLargestFontScale() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiStreamingText(
                text = "The quarterly summary is ready for review by the finance team",
                streaming = true,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("The quarterly summary is ready for review by the finance team", useUnmergedTree = true)
            .assertTextFullyVisible()
    }

    @Test
    fun theCaretIsTheSolidAiColourInEveryTheme() {
        for (theme in AllThemes) {
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.aiSolid, MonarchAiStreamingTextTokens.caretColor(colors))
        }
        assertTrue(MonarchAiStreamingTextTokens.rendersWhole("A[^1]\n\n[^1]: note"))
    }
}

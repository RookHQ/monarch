package dev.monarchui.components.intelligence

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChipLabels
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTray
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayFile
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayLabels
import dev.monarchui.components.intelligence.aiattachmenttray.MonarchAiAttachmentTrayTokens
import dev.monarchui.components.intelligence.aiconversation.MonarchAiFilePart
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val Ready = MonarchAiAttachmentTrayFile("1", "report.pdf", sizeBytes = 482_000, contentType = "application/pdf")
private val Uploading = MonarchAiAttachmentTrayFile("2", "photo.png", contentType = "image/png", progress = 0.4f)
private val Failed = MonarchAiAttachmentTrayFile("3", "data.csv", errorMessage = "File is too large")

internal object MonarchAiAttachmentTrayFixtures : ComponentFixtures {
    override val id = "ai-attachment-tray"
    override val fixtures = listOf(
        fixture("ready, uploading and failed") {
            MonarchAiAttachmentTray(files = listOf(Ready, Uploading, Failed), onRemove = {}, onRetry = {})
        },
        fixture("long name, disabled") {
            MonarchAiAttachmentTray(
                files = listOf(Ready.copy(name = "board-meeting-minutes-final-version-approved-by-legal.pdf")),
                onRemove = {},
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAiAttachmentTrayTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTrayIsNamedAndEachFileShowsItsState() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAiAttachmentTray(files = listOf(Uploading, Failed), onRemove = {}, onRetry = {})
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithContentDescription("Attachments").assertExists()
        rule.onNodeWithText("File is too large", substring = true).assertExists()
        rule.onNodeWithContentDescription("Retry data.csv").assertExists()
    }

    @Test
    fun removeAndRetryPassTheIdAndAreAnnounced() {
        var files by mutableStateOf(listOf(Ready, Failed))
        val retried = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiAttachmentTray(
                files = files,
                onRemove = { id -> files = files.filterNot { it.id == id } },
                onRetry = { retried += it },
            )
        }
        rule.onNodeWithContentDescription("Retry data.csv").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("3"), retried)
        rule.onNodeWithContentDescription("Remove report.pdf").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("report.pdf removed")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun aFinishedUploadIsAnnounced() {
        var files by mutableStateOf(listOf(Uploading))
        rule.setMonarchContent { MonarchAiAttachmentTray(files = files, onRemove = {}) }
        rule.waitForIdle()
        files = listOf(Uploading.copy(progress = 1f, sizeBytes = 1_000))
        rule.waitForIdle()
        rule.onNodeWithContentDescription("photo.png uploaded")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
    }

    @Test
    fun disabledButtonsDoNotAct() {
        var removed = 0
        rule.setMonarchContent {
            MonarchAiAttachmentTray(files = listOf(Ready), onRemove = { removed++ }, enabled = false)
        }
        rule.onNodeWithContentDescription("Remove report.pdf").assertIsNotEnabled().performClick()
        assertEquals(0, removed)
    }

    @Test
    fun noFilesDrawsNothing() {
        rule.setMonarchContent { MonarchAiAttachmentTray(files = emptyList(), onRemove = {}) }
        rule.onNodeWithContentDescription("Attachments").assertDoesNotExist()
    }

    @Test
    fun labelsReplaceTheBuiltInStrings() {
        var files by mutableStateOf(listOf(Ready))
        rule.setMonarchContent {
            MonarchAiAttachmentTray(
                files = files,
                onRemove = { files = emptyList() },
                labels = MonarchAiAttachmentTrayLabels(
                    label = "Pièces jointes",
                    removed = { "$it supprimé" },
                    chip = MonarchAttachmentChipLabels(remove = { "Supprimer $it" }),
                ),
            )
        }
        rule.onNodeWithContentDescription("Pièces jointes").assertExists()
        rule.onNodeWithContentDescription("Supprimer report.pdf").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("report.pdf supprimé").assertExists()
    }

    @Test
    fun textFitsAtLargeFontsAndFilesWrapOntoFurtherLines() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAttachmentTray(files = listOf(Ready, Failed), onRemove = {}, removeIcon = null)
        }
        rule.onAllNodesWithText("Remove", useUnmergedTree = true)[0].assertTextFullyVisible()
        rule.onNodeWithText("data.csv", useUnmergedTree = true).assertExists()
    }

    @Test
    fun filesFollowTheReadingDirection() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiAttachmentTray(files = listOf(Ready, Failed), onRemove = {})
        }
        // The first file starts the first line at the right edge; the next one is after it, on that line or below.
        val first = rule.onNodeWithText("report.pdf", useUnmergedTree = true).getBoundsInRoot()
        val second = rule.onNodeWithText("data.csv", useUnmergedTree = true).getBoundsInRoot()
        assertTrue(first.top < second.top || first.left > second.left)
    }

    @Test
    fun statusAnnouncementAndFilePartConversion() {
        val tokens = MonarchAiAttachmentTrayTokens
        assertEquals(MonarchUploadStatus.Done, tokens.status(Ready))
        assertEquals(MonarchUploadStatus.Uploading, tokens.status(Uploading))
        assertEquals(MonarchUploadStatus.Done, tokens.status(Uploading.copy(progress = 1.4f)))
        assertEquals(MonarchUploadStatus.Failed, tokens.status(Ready.copy(failed = true)))
        val message = tokens.announcement(
            previous = listOf(Uploading, Ready),
            current = listOf(Uploading.copy(progress = null)),
            removedIds = setOf("1"),
            ready = { "$it up" },
            removed = { "$it gone" },
        )
        assertEquals("photo.png up. report.pdf gone", message)
        val part = MonarchAiFilePart(name = "a.pdf", mediaType = "application/pdf", size = 10, fileId = "file_1")
        val file = MonarchAiAttachmentTrayFile(part, progress = 0.5f)
        assertEquals("file_1", file.id)
        assertEquals("application/pdf", file.contentType)
        assertEquals(10L, file.sizeBytes)
    }
}

package com.rookhq.monarch.components.files

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueue
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueCounts
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueEvent
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueItem
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueLabels
import com.rookhq.monarch.components.files.uploadqueue.MonarchUploadQueueTokens
import com.rookhq.monarch.components.files.attachmentchip.MonarchUploadStatus
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
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
import kotlin.test.assertTrue

private fun item(
    id: String,
    status: MonarchUploadStatus,
    size: Long? = null,
    progress: Float? = null,
    error: String? = null,
) = MonarchUploadQueueItem(
    id = id,
    name = "$id.pdf",
    status = status,
    size = size,
    progress = progress,
    errorMessage = error,
)

internal object MonarchUploadQueueFixtures : ComponentFixtures {
    override val id = "upload-queue"
    override val fixtures = listOf(
        fixture("uploading and queued, with every action") {
            MonarchUploadQueue(
                items = listOf(
                    item("report", MonarchUploadStatus.Uploading, size = 2_400_000, progress = 0.4f),
                    item("photo", MonarchUploadStatus.Queued),
                ),
                onCancel = {},
                onRetry = {},
                onRemove = {},
                onClearFinished = {},
            )
        },
        fixture("done, failed and cancelled") {
            MonarchUploadQueue(
                items = listOf(
                    item("done", MonarchUploadStatus.Done, size = 1_000),
                    item("failed", MonarchUploadStatus.Failed, error = "Too large"),
                    item("cancelled", MonarchUploadStatus.Cancelled),
                ),
                showSummary = false,
                onRetry = {},
                onRemove = {},
            )
        },
        fixture("indeterminate, with a detail line") {
            MonarchUploadQueue(
                items = listOf(
                    MonarchUploadQueueItem(
                        id = "1",
                        name = "contract.docx",
                        status = MonarchUploadStatus.Uploading,
                        detail = "Checking for viruses",
                    ),
                ),
            )
        },
        fixture("empty") { MonarchUploadQueue(items = emptyList()) },
        fixture("a long name, not retryable") {
            MonarchUploadQueue(
                items = listOf(
                    MonarchUploadQueueItem(
                        id = "1",
                        name = "Quarterly board meeting minutes with every appendix and the signed resolutions.pdf",
                        status = MonarchUploadStatus.Failed,
                        retryable = false,
                    ),
                ),
                showSummary = false,
                onRetry = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchUploadQueueTest {
    @get:Rule
    val rule = createComposeRule()

    private val tokens = MonarchUploadQueueTokens

    @Test
    fun countsAndTotalProgressFollowTheWeb() {
        val items = listOf(
            item("a", MonarchUploadStatus.Done, size = 300),
            item("b", MonarchUploadStatus.Uploading, size = 100, progress = 0.5f),
            item("c", MonarchUploadStatus.Failed, size = 1_000),
            item("d", MonarchUploadStatus.Cancelled),
        )
        val counts = MonarchUploadQueueCounts.of(items)
        assertEquals(MonarchUploadQueueCounts(4, 0, 1, 1, 1, 1), counts)
        assertEquals(1, counts.active)
        assertEquals(3, counts.finished)
        // Failed and cancelled are left out; a and b both have sizes, so (300 + 50) / 400.
        assertEquals(0.875f, tokens.totalProgress(items))
        // Without sizes every upload weighs the same: (1 + 0.5 + 0) / 3.
        val unsized = listOf(
            item("a", MonarchUploadStatus.Done),
            item("b", MonarchUploadStatus.Uploading, progress = 0.5f),
            item("c", MonarchUploadStatus.Queued),
        )
        assertEquals(0.5f, tokens.totalProgress(unsized))
        assertEquals(0f, tokens.totalProgress(emptyList()))
        assertEquals(40, tokens.percent(0.4f))
        assertEquals(100, tokens.percent(3f))
        assertEquals(null, tokens.percent(null))
    }

    @Test
    fun eventsAnnounceDoneFailedAndTheEndOfABatch() {
        val before = mapOf("a" to MonarchUploadStatus.Uploading, "b" to MonarchUploadStatus.Uploading)
        assertEquals(emptyList(), tokens.events(null, listOf(item("a", MonarchUploadStatus.Done))))
        val halfway = listOf(item("a", MonarchUploadStatus.Done), item("b", MonarchUploadStatus.Uploading))
        val midway = tokens.events(before, halfway)
        assertEquals(listOf<MonarchUploadQueueEvent>(MonarchUploadQueueEvent.Done("a.pdf")), midway)
        val end = listOf(
            item("a", MonarchUploadStatus.Done),
            item("b", MonarchUploadStatus.Failed, error = "Too large"),
        )
        assertEquals(
            listOf(
                MonarchUploadQueueEvent.Done("a.pdf"),
                MonarchUploadQueueEvent.Failed("b.pdf", "Too large"),
                MonarchUploadQueueEvent.Finished(MonarchUploadQueueCounts.of(end)),
            ),
            tokens.events(before, end),
        )
        // One upload alone is announced, but not as the end of a batch.
        val alone = mapOf("a" to MonarchUploadStatus.Uploading)
        val single = tokens.events(alone, listOf(item("a", MonarchUploadStatus.Done)))
        assertEquals(listOf<MonarchUploadQueueEvent>(MonarchUploadQueueEvent.Done("a.pdf")), single)
    }

    @Test
    fun theHeaderIsAHeadingWithTheSummaryAndTheRowsAreReadWithTheirStatus() {
        rule.setMonarchContent {
            MonarchUploadQueue(
                items = listOf(
                    item("a", MonarchUploadStatus.Done),
                    item("b", MonarchUploadStatus.Uploading, progress = 0.4f),
                    item("c", MonarchUploadStatus.Failed, error = "Too large"),
                ),
            )
        }
        rule.onNodeWithText("Uploads").assert(isHeading())
        rule.onNodeWithText("1 of 3 uploaded · 1 failed").assertExists()
        rule.onNode(hasText("b.pdf") and hasText("Uploading… 40%")).assertExists()
        rule.onNode(hasText("c.pdf") and hasText("Too large")).assertExists()
        rule.onNodeWithContentDescription("Upload progress of b.pdf").assertExists()
        rule.onNodeWithContentDescription("Total upload progress").assertExists()
    }

    @Test
    fun theButtonsAreNamedWithTheFileAndCallBackWithTheId() {
        val calls = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchUploadQueue(
                items = listOf(
                    item("a", MonarchUploadStatus.Uploading),
                    item("b", MonarchUploadStatus.Failed),
                    item("c", MonarchUploadStatus.Done),
                ),
                onCancel = { calls += "cancel $it" },
                onRetry = { calls += "retry $it" },
                onRemove = { calls += "remove $it" },
                onClearFinished = { calls += "clear" },
            )
        }
        listOf(
            "Cancel upload of a.pdf",
            "Retry upload of b.pdf",
            "Remove b.pdf from the list",
            "Remove c.pdf from the list",
        ).forEach { name ->
            rule.onNodeWithContentDescription(name).assertTouchTargetAtLeast48dp(rule.density).performClick()
        }
        rule.onNodeWithText("Clear finished").performClick()
        rule.onNodeWithContentDescription("Cancel upload of c.pdf").assertDoesNotExist()
        rule.onNodeWithContentDescription("Retry upload of a.pdf").assertDoesNotExist()
        assertEquals(listOf("cancel a", "retry b", "remove b", "remove c", "clear"), calls)
    }

    @Test
    fun aFinishedUploadIsAnnouncedThroughTheLiveRegion() {
        var items by mutableStateOf(
            listOf(item("a", MonarchUploadStatus.Uploading), item("b", MonarchUploadStatus.Uploading)),
        )
        rule.setMonarchContent { MonarchUploadQueue(items = items, modifier = Modifier.testTag("queue")) }
        rule.waitForIdle()
        items = listOf(item("a", MonarchUploadStatus.Done), item("b", MonarchUploadStatus.Uploading))
        rule.waitForIdle()
        assertEquals("a.pdf uploaded.", liveRegion())
        items = listOf(item("a", MonarchUploadStatus.Done), item("b", MonarchUploadStatus.Failed, error = "Offline"))
        rule.waitForIdle()
        assertEquals("b.pdf failed: Offline. Uploads finished: 1 uploaded, 1 failed.", liveRegion())
    }

    @Test
    fun everyStringCanBeReplaced() {
        rule.setMonarchContent {
            MonarchUploadQueue(
                items = listOf(item("a", MonarchUploadStatus.Uploading, size = 2_048, progress = 0.5f)),
                labels = MonarchUploadQueueLabels(
                    title = "Envois",
                    uploading = { "Envoi $it %" },
                    summary = { "${it.done} sur ${it.total}" },
                    formatSize = { "$it o" },
                    cancel = "Annuler",
                    cancelLabel = { "Annuler $it" },
                ),
                onCancel = {},
            )
        }
        rule.onNodeWithText("Envois").assertExists()
        rule.onNodeWithText("0 sur 1").assertExists()
        rule.onNode(hasText("2048 o") and hasText("Envoi 50 %")).assertExists()
        rule.onNodeWithContentDescription("Annuler a.pdf").assertExists()
    }

    @Test
    fun theIconIsAtTheStartAndTheNameWrapsAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchUploadQueue(
                items = listOf(item("quarterly-report", MonarchUploadStatus.Done)),
                modifier = Modifier.testTag("queue"),
                showSummary = false,
            )
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            val queue = rule.onNodeWithTag("queue").getUnclippedBoundsInRoot()
            val name = rule.onNodeWithText("quarterly-report.pdf", useUnmergedTree = true)
                .assertTextFullyVisible()
                .getUnclippedBoundsInRoot()
            if (direction == LayoutDirection.Ltr) {
                assertTrue(name.left > queue.left, "the icon is before the name")
            } else {
                assertTrue(name.right < queue.right, "the icon is before the name")
            }
        }
    }

    @Test
    fun statusColoursComeFromTheSuccessDangerAndMutedRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.successStrong, tokens.statusColor(MonarchUploadStatus.Done, colors), theme.id)
            assertEquals(colors.dangerStrong, tokens.statusColor(MonarchUploadStatus.Failed, colors), theme.id)
            listOf(MonarchUploadStatus.Queued, MonarchUploadStatus.Uploading, MonarchUploadStatus.Cancelled).forEach {
                assertEquals(colors.textMuted, tokens.statusColor(it, colors), theme.id)
            }
            assertEquals(colors.textDefault, tokens.nameColor(colors), theme.id)
            assertEquals(colors.textMuted, tokens.mutedColor(colors), theme.id)
        }
    }

    private fun liveRegion(): String? {
        val node: SemanticsNode = rule.onNodeWithTag("queue").fetchSemanticsNode().children.single {
            it.config.getOrNull(SemanticsProperties.LiveRegion) == LiveRegionMode.Polite
        }
        return node.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString()
    }
}

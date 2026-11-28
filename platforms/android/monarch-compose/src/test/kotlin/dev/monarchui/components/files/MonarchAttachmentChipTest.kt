package dev.monarchui.components.files

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChip
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChipLabels
import dev.monarchui.components.files.attachmentchip.MonarchAttachmentChipTokens
import dev.monarchui.components.files.attachmentchip.MonarchUploadStatus
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.TestIcon
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

private val removeIcon: @Composable () -> Unit = {
    MonarchIcon(TestIcon, null, size = MonarchIconSize.Sm)
}

internal object MonarchAttachmentChipFixtures : ComponentFixtures {
    override val id = "attachment-chip"
    override val fixtures = listOf(
        fixture("done, opens, remove as a word") {
            MonarchAttachmentChip(name = "report.pdf", sizeBytes = 482_000L, onOpen = {}, onRemove = {})
        },
        fixture("uploading and queued with icons") {
            Column {
                MonarchAttachmentChip(
                    name = "photo.jpg",
                    status = MonarchUploadStatus.Uploading,
                    progress = 0.45f,
                    removeIcon = removeIcon,
                    onRemove = {},
                )
                MonarchAttachmentChip(name = "notes.txt", status = MonarchUploadStatus.Queued)
            }
        },
        fixture("uploading of unknown length") {
            MonarchAttachmentChip(name = "video.mp4", status = MonarchUploadStatus.Uploading)
        },
        fixture("failed with retry, and cancelled") {
            Column {
                MonarchAttachmentChip(
                    name = "budget.xlsx",
                    status = MonarchUploadStatus.Failed,
                    onRetry = {},
                    onRemove = {},
                )
                MonarchAttachmentChip(name = "draft.docx", status = MonarchUploadStatus.Cancelled)
            }
        },
        fixture("disabled") {
            MonarchAttachmentChip(name = "report.pdf", enabled = false, removeIcon = removeIcon, onRemove = {})
        },
        fixture("long text") {
            MonarchAttachmentChip(
                name = "Quarterly board report for the finance committee, final version.pdf",
                status = MonarchUploadStatus.Failed,
                errorMessage = "The file is larger than the 25 MB the workspace allows for one attachment.",
                onRemove = {},
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchAttachmentChipTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theNameAndSizeAreOneElement() {
        rule.setMonarchContent { MonarchAttachmentChip(name = "report.pdf", sizeBytes = 2_048L) }
        rule.onNodeWithText("report.pdf")
            .assert(
                SemanticsMatcher("name and size") { node ->
                    val texts = node.config[SemanticsProperties.Text].map { it.text }
                    texts.size == 2 && texts[0] == "report.pdf" && texts[1].endsWith("B")
                },
            )
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun theNameOpensTheFileOnlyOnceUploaded() {
        var opened = 0
        rule.setMonarchContent {
            Column {
                MonarchAttachmentChip(name = "done.pdf", onOpen = { opened++ })
                MonarchAttachmentChip(name = "busy.pdf", status = MonarchUploadStatus.Uploading, onOpen = { opened++ })
            }
        }
        rule.onNodeWithText("done.pdf")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, opened)
        rule.onNodeWithText("busy.pdf").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
    }

    @Test
    fun uploadingShowsANamedProgressBarAndThePercentage() {
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchAttachmentChip(name = "photo.jpg", status = MonarchUploadStatus.Uploading, progress = 0.45f)
        }
        rule.mainClock.advanceTimeByFrame()
        rule.onNodeWithText("Uploading 45%", useUnmergedTree = true).assertExists()
        rule.onNodeWithContentDescription("Uploading photo.jpg")
            .assert(hasProgressBarRangeInfo(ProgressBarRangeInfo(0.45f, 0f..1f)))
    }

    @Test
    fun aFailureIsWrittenAndAnnouncedAndRetryCallsBack() {
        var retries = 0
        rule.setMonarchContent {
            MonarchAttachmentChip(name = "budget.xlsx", status = MonarchUploadStatus.Failed, onRetry = { retries++ })
        }
        rule.onNodeWithText("Upload failed", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        rule.onNodeWithContentDescription("Retry budget.xlsx").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun removeIsNamedAfterTheFileAsAWordOrAnIcon() {
        var removed = 0
        rule.setMonarchContent {
            Column {
                MonarchAttachmentChip(name = "a.pdf", onRemove = { removed++ })
                MonarchAttachmentChip(name = "b.pdf", removeIcon = removeIcon, onRemove = { removed++ })
            }
        }
        rule.onNodeWithContentDescription("Remove a.pdf").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNodeWithContentDescription("Remove b.pdf").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(2, removed)
    }

    @Test
    fun disabledButtonsDoNotCallBack() {
        var removed = 0
        rule.setMonarchContent { MonarchAttachmentChip(name = "a.pdf", enabled = false, onRemove = { removed++ }) }
        rule.onNodeWithContentDescription("Remove a.pdf").assertIsNotEnabled().performClick()
        assertEquals(0, removed)
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        rule.setMonarchContent {
            MonarchAttachmentChip(
                name = "a.pdf",
                status = MonarchUploadStatus.Queued,
                onRemove = {},
                removeIcon = null,
                labels = MonarchAttachmentChipLabels(
                    remove = { "Delete $it" },
                    queued = "In line",
                    removeText = "Delete",
                ),
            )
        }
        rule.onNodeWithContentDescription("Delete a.pdf").assertExists()
        rule.onNodeWithText("In line", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("Delete", useUnmergedTree = true).assertExists()
    }

    @Test
    fun theButtonsFollowTheNameInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAttachmentChip(name = "a.pdf", removeIcon = removeIcon, onRemove = {})
        }
        rule.onNodeWithText("a.pdf")
            .assertIsBefore(rule.onNodeWithContentDescription("Remove a.pdf"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongErrorWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAttachmentChip(
                name = "budget.xlsx",
                modifier = Modifier.width(300.dp),
                status = MonarchUploadStatus.Failed,
                errorMessage = "The file is larger than the workspace allows.",
                onRemove = {},
            )
        }
        rule.onNodeWithText("The file is larger", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheChip() {
        val t = MonarchAttachmentChipTokens
        assertEquals(1f, t.fraction(1.4f))
        assertNull(t.fraction(null))
        assertNull(t.fraction(Float.NaN))
        assertTrue(t.opens(MonarchUploadStatus.Done, hasCallback = true))
        assertFalse(t.opens(MonarchUploadStatus.Failed, hasCallback = true))
        assertTrue(MonarchUploadStatus.Queued.active)
        assertFalse(MonarchUploadStatus.Done.active)
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        val t = MonarchAttachmentChipTokens
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val done = t.colors(MonarchUploadStatus.Done, colors)
            assertEquals(colors.bgSurface, done.container, theme.id)
            assertEquals(colors.borderDefault, done.border, theme.id)
            val failed = t.colors(MonarchUploadStatus.Failed, colors)
            assertEquals(colors.dangerSubtle, failed.container, theme.id)
            assertEquals(colors.dangerBorder, failed.border, theme.id)
            assertEquals(colors.dangerStrong, t.metaColor(MonarchUploadStatus.Failed, colors), theme.id)
            assertEquals(colors.textMuted, t.metaColor(MonarchUploadStatus.Done, colors), theme.id)
            assertEquals(colors.textDefault, t.nameColor(opens = true, colors), theme.id)
        }
    }
}

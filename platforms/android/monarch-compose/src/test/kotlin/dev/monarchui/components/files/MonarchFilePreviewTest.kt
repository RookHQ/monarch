package dev.monarchui.components.files

import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import dev.monarchui.components.files.filepreview.MonarchFilePreview
import dev.monarchui.components.files.filepreview.MonarchFilePreviewKind
import dev.monarchui.components.files.filepreview.MonarchFilePreviewLabels
import dev.monarchui.components.files.filepreview.MonarchFilePreviewTokens
import dev.monarchui.components.files.filepreview.MonarchFilePreviewTokens.Message
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
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
import kotlin.test.assertTrue

private val Picture = ColorPainter(Color.Gray)

internal object MonarchFilePreviewFixtures : ComponentFixtures {
    override val id = "file-preview"
    override val fixtures = listOf(
        fixture("image") { MonarchFilePreview(name = "diagram.png", image = Picture) },
        fixture("text with the download footer") {
            MonarchFilePreview(
                name = "notes.md",
                text = "# Notes\nShip the beta on Friday.",
                onDownload = {},
                alwaysShowDownload = true,
            )
        },
        fixture("no preview for the type") { MonarchFilePreview(name = "archive.zip", onDownload = {}) },
        fixture("failed") { MonarchFilePreview(name = "photo.heic", failed = true, onDownload = {}) },
        fixture("waiting for the picture") { MonarchFilePreview(name = "photo.jpg") },
        fixture("video in the app's player") {
            MonarchFilePreview(name = "tour.mp4", player = { Text("Player") })
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFilePreviewTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theKindComesFromTheFamily() {
        assertEquals(MonarchFilePreviewKind.Image, MonarchFilePreviewKind.of("a.webp"))
        assertEquals(MonarchFilePreviewKind.Pdf, MonarchFilePreviewKind.of("scan", "application/pdf"))
        assertEquals(MonarchFilePreviewKind.Text, MonarchFilePreviewKind.of("main.kt"))
        assertEquals(MonarchFilePreviewKind.Text, MonarchFilePreviewKind.of("README.md"))
        assertEquals(MonarchFilePreviewKind.Video, MonarchFilePreviewKind.of("clip.mov"))
        assertEquals(MonarchFilePreviewKind.Audio, MonarchFilePreviewKind.of("x", "audio/mpeg"))
        assertEquals(MonarchFilePreviewKind.None, MonarchFilePreviewKind.of("budget.xlsx"))
    }

    @Test
    fun theResolutionShowsContentOnlyWhenItIsThereAndHasNotFailed() {
        val tokens = MonarchFilePreviewTokens
        assertEquals(MonarchFilePreviewKind.Pdf, tokens.resolve(MonarchFilePreviewKind.Pdf, true, false).kind)
        val failed = tokens.resolve(MonarchFilePreviewKind.Pdf, true, true)
        assertEquals(MonarchFilePreviewKind.None, failed.kind)
        assertEquals(Message.Failed, failed.message)
        assertEquals(Message.Unavailable, tokens.resolve(MonarchFilePreviewKind.None, false, false).message)
        val waiting = tokens.resolve(MonarchFilePreviewKind.Image, false, false)
        assertEquals(MonarchFilePreviewKind.None, waiting.kind)
        assertEquals(null, waiting.message)
        assertEquals(Dp.Infinity, tokens.maxHeight(Dp(0f)))
        assertEquals(Dp(560f), tokens.maxHeight(Dp(800f)))
        val video = MonarchFilePreviewKind.Video
        assertTrue(tokens.hasContent(video, image = false, pdf = false, text = false, player = true))
        assertFalse(tokens.hasContent(video, image = true, pdf = true, text = true, player = false))
        val none = MonarchFilePreviewKind.None
        assertFalse(tokens.hasContent(none, image = true, pdf = true, text = true, player = true))
        assertTrue(tokens.showsFooter(alwaysShowDownload = true, MonarchFilePreviewKind.Text))
        assertFalse(tokens.showsFooter(alwaysShowDownload = true, MonarchFilePreviewKind.None))
        assertFalse(tokens.showsFooter(alwaysShowDownload = false, MonarchFilePreviewKind.Text))
    }

    @Test
    fun anImageIsNamedByItsDescriptionOrItsFileName() {
        rule.setMonarchContent { MonarchFilePreview(name = "diagram.png", image = Picture) }
        rule.onNodeWithContentDescription("diagram.png").assertExists()
    }

    @Test
    fun anEmptyImageDescriptionHidesThePicture() {
        rule.setMonarchContent { MonarchFilePreview(name = "diagram.png", image = Picture, imageDescription = "") }
        rule.onNodeWithContentDescription("diagram.png").assertDoesNotExist()
    }

    @Test
    fun textIsShownInANamedScrollingBlock() {
        rule.setMonarchContent { MonarchFilePreview(name = "notes.txt", text = "Ship on Friday.") }
        rule.onNodeWithContentDescription("Preview of notes.txt").assertExists()
        rule.onNodeWithText("Ship on Friday.").assertExists()
    }

    @Test
    fun aTypeWithoutPreviewFallsBackToTheIconTheNameAndDownload() {
        var downloads = 0
        rule.setMonarchContent { MonarchFilePreview(name = "data.bin", onDownload = { downloads++ }) }
        rule.onNodeWithText("data.bin").assertExists()
        rule.onNodeWithText("There is no preview for this file type.").assertExists()
        rule.onNodeWithContentDescription("File")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image))
        val download = rule.onNodeWithContentDescription("Download data.bin")
        download.assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(1, downloads)
    }

    @Test
    fun aFailedPreviewSaysSo() {
        rule.setMonarchContent { MonarchFilePreview(name = "photo.png", image = Picture, failed = true) }
        rule.onNodeWithText("The preview could not be loaded.").assertExists()
        rule.onNodeWithContentDescription("photo.png").assertDoesNotExist()
    }

    @Test
    fun whileThePictureLoadsTheFallbackHasNoMessage() {
        rule.setMonarchContent { MonarchFilePreview(name = "photo.png") }
        rule.onNodeWithText("photo.png").assertExists()
        rule.onNodeWithText("There is no preview for this file type.").assertDoesNotExist()
        rule.onNodeWithText("The preview could not be loaded.").assertDoesNotExist()
    }

    @Test
    fun aPdfThatCannotBeOpenedFallsBackToTheFailedMessage() {
        rule.setMonarchContent {
            MonarchFilePreview(name = "report.pdf", pdf = Uri.parse("content://com.example.missing/report.pdf"))
        }
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithText("The preview could not be loaded.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("The preview could not be loaded.").assertExists()
    }

    @Test
    fun videoAndAudioUseTheAppsPlayer() {
        rule.setMonarchContent {
            MonarchFilePreview(name = "tour.mp4", player = { Text("Player", Modifier.testTag("player")) })
        }
        rule.onNodeWithTag("player").assertExists()
        rule.onNodeWithContentDescription("Preview of tour.mp4").assertExists()
    }

    @Test
    fun withoutAPlayerVideoFallsBack() {
        rule.setMonarchContent { MonarchFilePreview(name = "tour.mp4") }
        rule.onNodeWithText("tour.mp4").assertExists()
    }

    @Test
    fun aCustomFallbackAndAForcedKindReplaceTheDetection() {
        rule.setMonarchContent {
            MonarchFilePreview(
                name = "notes.txt",
                kind = MonarchFilePreviewKind.None,
                text = "x",
                fallback = { Text("Open in the editor") },
            )
        }
        rule.onNodeWithText("Open in the editor").assertExists()
        rule.onNodeWithText("x").assertDoesNotExist()
    }

    @Test
    fun theDownloadFooterSitsAtTheEndInBothDirections() {
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchFilePreview(
                name = "notes.txt",
                modifier = Modifier.testTag("preview"),
                text = "x",
                onDownload = {},
                alwaysShowDownload = true,
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            val box = rule.onNodeWithTag("preview").fetchSemanticsNode().boundsInRoot
            val button = rule.onNodeWithContentDescription("Download notes.txt").fetchSemanticsNode().boundsInRoot
            if (direction == LayoutDirection.Ltr) {
                assertTrue(button.left > box.center.x, "at the end in LTR")
            } else {
                assertTrue(button.right < box.center.x, "at the end in RTL")
            }
        }
    }

    @Test
    fun theLabelsReplaceTheBuiltInStrings() {
        var failed by mutableStateOf(false)
        rule.setMonarchContent {
            MonarchFilePreview(
                name = "data.bin",
                onDownload = {},
                failed = failed,
                labels = MonarchFilePreviewLabels(
                    unavailable = "Pas d'aperçu.",
                    failed = "Échec.",
                    download = "Télécharger",
                    downloadName = { "Télécharger $it" },
                ),
            )
        }
        rule.onNodeWithText("Pas d'aperçu.").assertExists()
        rule.onNodeWithContentDescription("Télécharger data.bin").assertExists()
        failed = true
        rule.onNodeWithText("Échec.").assertExists()
    }

    @Test
    fun theFallbackTextWrapsAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchFilePreview(
                name = "A very long file name for the quarterly board meeting.pdf",
                kind = MonarchFilePreviewKind.None,
            )
        }
        rule.onNodeWithText("A very long file name for the quarterly board meeting.pdf").assertTextFullyVisible()
        rule.onNodeWithText("There is no preview for this file type.").assertTextFullyVisible()
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val preview = MonarchFilePreviewTokens.colors(colors)
            assertEquals(colors.bgSubtle, preview.container, theme.id)
            assertEquals(colors.borderDefault, preview.border, theme.id)
            assertEquals(colors.bgSurface, preview.surface, theme.id)
            assertEquals(colors.borderSubtle, preview.divider, theme.id)
            assertEquals(colors.textDefault, preview.name, theme.id)
            assertEquals(colors.textMuted, preview.message, theme.id)
            assertEquals(colors.bgInverse, preview.media, theme.id)
        }
    }
}

@file:OptIn(ExperimentalMonarchApi::class)

package dev.monarchui.components.intelligence

import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.monarchui.ExperimentalMonarchApi
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvas
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvasLabels
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvasPlacement
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvasTab
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiArtifactCanvasTokens
import dev.monarchui.components.intelligence.aiartifactcanvas.MonarchAiCanvasArtifact
import dev.monarchui.components.intelligence.aiconversation.MonarchAiArtifactKind
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
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private val Inline = MonarchAiArtifactCanvasPlacement.Inline
private val Sheet = MonarchAiArtifactCanvasPlacement.Sheet

private val Doc = MonarchAiCanvasArtifact(
    id = "summary",
    kind = MonarchAiArtifactKind.Document,
    title = "Q3 summary",
    version = 3,
    content = "Revenue grew over the quarter.",
)
private val Code = MonarchAiCanvasArtifact(
    id = "script",
    kind = MonarchAiArtifactKind.Code,
    title = "Import script",
    version = 1,
    content = "val total = 42",
    language = "kotlin",
)
private val Page =
    Doc.copy(id = "page", kind = MonarchAiArtifactKind.Html, title = "Landing page", content = "<h1>Hi</h1>")
private val Picture =
    Doc.copy(id = "logo", kind = MonarchAiArtifactKind.Image, title = "Logo", content = "https://x.test/a.png")

private fun View.webViews(): List<WebView> = when (this) {
    is WebView -> listOf(this)
    is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).webViews() }
    else -> emptyList()
}

internal object MonarchAiArtifactCanvasFixtures : ComponentFixtures {
    override val id = "ai-artifact-canvas"
    override val fixtures = listOf(
        fixture("document, every action") {
            MonarchAiArtifactCanvas(
                artifact = Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                onDownload = {},
                onSave = {},
            )
        },
        fixture("code, no preview, close icon") {
            MonarchAiArtifactCanvas(
                artifact = Code,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        },
        fixture("image without its picture") {
            MonarchAiArtifactCanvas(
                artifact = Picture,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
            )
        },
        fixture("web page") {
            MonarchAiArtifactCanvas(
                artifact = Page,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchAiArtifactCanvasTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theTitleIsAHeadingWithTheKindAndVersionUnderIt() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Doc, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.onNodeWithText("Q3 summary").assert(isHeading())
        rule.onNodeWithText("Document · v3").assertExists()
    }

    @Test
    fun thePreviewTabShowsTheDocumentAndTheCodeTabItsSource() {
        val picked = mutableListOf<MonarchAiArtifactCanvasTab>()
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                onTabChange = { picked += it },
            )
        }
        rule.onNode(hasText("Preview") and hasClickAction()).assertIsSelected()
        rule.onNodeWithText("Revenue grew over the quarter.").assertExists()
        rule.onNode(hasText("Code") and hasClickAction()).assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Code") and hasClickAction()).assertIsSelected()
        assertEquals(listOf(MonarchAiArtifactCanvasTab.Code), picked)
    }

    @Test
    fun aHoistedTabIsFollowed() {
        var tab by mutableStateOf(MonarchAiArtifactCanvasTab.Code)
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                tab = tab,
                onTabChange = { tab = it },
            )
        }
        rule.onNode(hasText("Code") and hasClickAction()).assertIsSelected()
        rule.onNode(hasText("Preview") and hasClickAction()).performClick()
        assertEquals(MonarchAiArtifactCanvasTab.Preview, tab)
        rule.onNode(hasText("Preview") and hasClickAction()).assertIsSelected()
    }

    @Test
    fun codeHasNoPreviewTab() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Code, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.onNode(hasText("Preview") and hasClickAction()).assertDoesNotExist()
        rule.onNodeWithText("Code · v1").assertExists()
    }

    @Test
    fun theActionsCallBack() {
        var closed = 0
        val downloaded = mutableListOf<String>()
        val copied = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = { closed++ },
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                onCopy = { copied += it.id },
                onDownload = { downloaded += it.id },
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithContentDescription("Close artifact").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.onNode(hasText("Download") and hasClickAction()).performClick()
        rule.onNode(hasContentDescription("Copy", substring = true) and hasClickAction()).performClick()
        assertEquals(1, closed)
        assertEquals(listOf("summary"), downloaded)
        assertEquals(listOf("summary"), copied)
    }

    @Test
    fun saveShowsProgressUntilItReturns() {
        val done = CompletableDeferred<Unit>()
        var saves = 0
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                onSave = {
                    saves++
                    done.await()
                },
            )
        }
        rule.onNode(hasText("Save") and hasClickAction()).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("Save") and hasClickAction()).performClick()
        assertEquals(1, saves, "a second tap while saving does nothing")
        done.complete(Unit)
        rule.waitForIdle()
        rule.onNode(hasText("Save") and hasClickAction()).performClick()
        rule.waitForIdle()
        assertEquals(2, saves)
    }

    @Test
    fun withoutACloseIconTheCloseButtonShowsItsLabel() {
        var closed = false
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = { closed = true },
                placement = MonarchAiArtifactCanvasPlacement.Inline,
            )
        }
        rule.onNode(hasText("Close artifact") and hasClickAction()).performClick()
        assertTrue(closed)
    }

    @Test
    fun aClosedCanvasShowsNothing() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Doc, open = false, onDismissRequest = {}, placement = Inline)
        }
        rule.onNodeWithText("Q3 summary").assertDoesNotExist()
    }

    @Test
    fun aSheetShowsTheSameContent() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Doc, open = true, onDismissRequest = {}, placement = Sheet)
        }
        rule.onNodeWithText("Q3 summary").assert(isHeading())
        rule.onNodeWithText("Revenue grew over the quarter.").assertExists()
    }

    @Test
    fun aWebPageIsShownInANamedSandbox() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Page, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.onNodeWithContentDescription("Preview of Landing page").assertExists()
        rule.onNodeWithText("Sandboxed preview: scripts, forms and network requests are blocked.").assertExists()
        rule.onNodeWithText("Web page · v3").assertExists()
    }

    @Test
    fun theWebPageSandboxTurnsOffScriptsNetworkFilesWindowsAndNavigation() {
        lateinit var root: View
        rule.setMonarchContent {
            root = LocalView.current
            MonarchAiArtifactCanvas(Page, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.waitForIdle()
        val web = root.webViews().single()
        val settings = web.settings
        assertFalse(settings.javaScriptEnabled)
        assertFalse(settings.javaScriptCanOpenWindowsAutomatically)
        assertFalse(settings.supportMultipleWindows())
        assertTrue(settings.blockNetworkLoads)
        assertTrue(settings.blockNetworkImage)
        assertFalse(settings.allowFileAccess)
        assertFalse(settings.allowContentAccess)
        assertFalse(settings.domStorageEnabled)
        assertEquals(WebSettings.MIXED_CONTENT_NEVER_ALLOW, settings.mixedContentMode)
        val client = assertNotNull(shadowOf(web).webViewClient)
        assertTrue(client.shouldOverrideUrlLoading(web, null as WebResourceRequest?), "every navigation is refused")
    }

    @Test
    fun anImageWithoutItsPictureSaysSo() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Picture, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.onNodeWithText("The image is not available.").assertExists()
    }

    @Test
    fun aCustomPreviewReplacesTheBuiltInOne() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Code,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                preview = { Text("Drawn by the app: ${it.title}") },
            )
        }
        rule.onNode(hasText("Preview") and hasClickAction()).assertIsSelected()
        rule.onNodeWithText("Drawn by the app: Import script").assertExists()
    }

    @Test
    fun theStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                onSave = {},
                labels = MonarchAiArtifactCanvasLabels(
                    kind = { "Texte" },
                    version = { "version $it" },
                    preview = "Aperçu",
                    code = "Source",
                    close = "Fermer",
                    save = "Enregistrer",
                    label = { "Artefact : $it" },
                ),
            )
        }
        rule.onNodeWithText("Texte · version 3").assertExists()
        rule.onNodeWithText("Aperçu").assertExists()
        rule.onNodeWithText("Source").assertExists()
        rule.onNodeWithText("Fermer").assertExists()
        rule.onNodeWithText("Enregistrer").assertExists()
    }

    @Test
    fun theCloseButtonIsAtTheStartInRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchAiArtifactCanvas(
                Doc,
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
                closeIcon = { Icon(TestIcon, contentDescription = null) },
            )
        }
        rule.onNodeWithText("Q3 summary")
            .assertIsBefore(rule.onNodeWithContentDescription("Close artifact"), LayoutDirection.Rtl)
    }

    @Test
    fun aLongTitleWrapsAtTheLargestFontSize() {
        val title = "Quarterly revenue summary for the finance team"
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchAiArtifactCanvas(
                Doc.copy(title = title),
                open = true,
                onDismissRequest = {},
                placement = MonarchAiArtifactCanvasPlacement.Inline,
            )
        }
        rule.onNodeWithText(title).assertTextFullyVisible()
    }

    @Test
    fun theRules() {
        val t = MonarchAiArtifactCanvasTokens
        assertTrue(t.asSheet(MonarchAiArtifactCanvasPlacement.Auto, 400.dp))
        assertFalse(t.asSheet(MonarchAiArtifactCanvasPlacement.Auto, 800.dp))
        assertTrue(t.asSheet(MonarchAiArtifactCanvasPlacement.Sheet, 800.dp))
        assertFalse(t.asSheet(MonarchAiArtifactCanvasPlacement.Inline, 400.dp))
        assertTrue(t.hasBuiltInPreview(MonarchAiArtifactKind.Html))
        assertFalse(t.hasBuiltInPreview(MonarchAiArtifactKind.Chart))
        assertEquals(MonarchAiArtifactCanvasTab.Code, t.shownTab(MonarchAiArtifactCanvasTab.Preview, false))
        assertEquals(MonarchAiArtifactCanvasTab.Preview, t.shownTab(MonarchAiArtifactCanvasTab.Preview, true))
        assertEquals("markdown", t.defaultLanguage(MonarchAiArtifactKind.Table))
        assertEquals("json", t.defaultLanguage(MonarchAiArtifactKind.Chart))
        assertEquals(null, t.defaultLanguage(MonarchAiArtifactKind.Code))
    }

    @Test
    fun webPagesAreMadeInert() {
        val t = MonarchAiArtifactCanvasTokens
        val html = "<p onclick=\"steal()\">Hi</p><script>alert(1)</script><iframe src=\"https://x.test\"></iframe>" +
            "<meta http-equiv=\"refresh\" content=\"0;url=https://x.test\"><base href=\"https://x.test\">"
        val inert = t.inertHtml(html)
        assertEquals("<p>Hi</p>", inert)
        val document = t.sandboxedDocument(html)
        assertTrue(document.contains("Content-Security-Policy"))
        assertTrue(document.contains("default-src 'none'"))
        assertTrue(document.contains("script-src 'none'"))
        assertFalse(document.contains("<script>"))
    }

    @Test
    fun theColoursComeFromTheThemeRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.textDefault, MonarchAiArtifactCanvasTokens.titleColor(c), theme.id)
            assertEquals(c.textMuted, MonarchAiArtifactCanvasTokens.mutedColor(c), theme.id)
            assertEquals(c.warningStrong, MonarchAiArtifactCanvasTokens.warningColor(c), theme.id)
        }
    }

    @Test
    fun theCanvasIsNamedForTalkBack() {
        rule.setMonarchContent {
            MonarchAiArtifactCanvas(Doc, open = true, onDismissRequest = {}, placement = Inline)
        }
        rule.onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Artifact: Q3 summary")).assertExists()
    }
}

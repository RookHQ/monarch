package com.rookhq.monarch.components.files

import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedBlock
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedBlockLabels
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedBlockTokens
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedProvider
import com.rookhq.monarch.components.files.embedblock.MonarchEmbedResolution
import com.rookhq.monarch.components.files.embedblock.embedBlockWebView
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val YouTubeShare = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
private const val YouTubeEmbed = "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"

private fun View.webViews(): List<WebView> = when (this) {
    is WebView -> listOf(this)
    is ViewGroup -> (0 until childCount).flatMap { getChildAt(it).webViews() }
    else -> emptyList()
}

private fun request(url: String, gesture: Boolean = true) = object : WebResourceRequest {
    override fun getUrl(): Uri = Uri.parse(url)
    override fun isForMainFrame(): Boolean = true
    override fun isRedirect(): Boolean = false
    override fun hasGesture(): Boolean = gesture
    override fun getMethod(): String = "GET"
    override fun getRequestHeaders(): Map<String, String> = emptyMap()
}

internal object MonarchEmbedBlockFixtures : ComponentFixtures {
    override val id = "embed-block"
    override val fixtures = listOf(
        fixture("frame with a caption") { MonarchEmbedBlock(url = YouTubeShare, caption = "Product tour") },
        fixture("four by three frame") { MonarchEmbedBlock(url = "https://codepen.io/rook/pen/abc123") },
        fixture("link card") { MonarchEmbedBlock(url = "https://example.com/changelog", onOpenLink = {}) },
        fixture("invalid") { MonarchEmbedBlock(url = "javascript:alert(1)") },
        fixture("long caption") {
            MonarchEmbedBlock(
                url = "not a url",
                caption = "The recording of the quarterly planning session, with the questions from the floor",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchEmbedBlockTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun anAllowListedLinkLoadsOnlyTheProvidersEmbedAddress() {
        lateinit var root: View
        rule.setMonarchContent {
            root = LocalView.current
            MonarchEmbedBlock(url = YouTubeShare)
        }
        rule.onNode(hasContentDescription("YouTube content")).assertExists()
        val web = root.webViews().single()
        assertEquals(YouTubeEmbed, shadowOf(web).lastLoadedUrl)
        assertTrue(web.settings.javaScriptEnabled)
        assertFalse(web.settings.allowFileAccess)
        assertFalse(web.settings.allowContentAccess)
        assertFalse(web.settings.javaScriptCanOpenWindowsAutomatically)
    }

    @Test
    fun theCaptionIsShownAndNamesTheFrame() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchEmbedBlock(url = YouTubeShare, caption = "The product tour, recorded at the launch in May")
        }
        rule.onNode(hasContentDescription("The product tour, recorded at the launch in May")).assertExists()
        rule.onNodeWithText("The product tour", substring = true).assertTextFullyVisible()
    }

    @Test
    fun anyOtherWebLinkIsALinkCardThatOpensOutside() {
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            Column {
                MonarchEmbedBlock(url = "https://www.example.com/changelog", onOpenLink = { opened += it })
            }
        }
        rule.onNodeWithText("example.com", substring = true).assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("https://www.example.com/changelog"), opened)
    }

    @Test
    fun aLinkThatCannotBeShownSaysSo() {
        rule.setMonarchContent {
            Column {
                MonarchEmbedBlock(url = "javascript:alert(1)")
                MonarchEmbedBlock(url = "data:text/html,hi", labels = MonarchEmbedBlockLabels(invalid = "Not shown"))
            }
        }
        rule.onNodeWithText("This link can't be shown.").assertExists()
        rule.onNodeWithText("Not shown").assertExists()
    }

    @Test
    fun linksThatLeaveTheProviderOpenOutsideTheFrame() {
        val opened = mutableListOf<String>()
        val web = embedBlockWebView(ApplicationProvider.getApplicationContext(), YouTubeEmbed) { opened += it }
        val client = shadowOf(web).webViewClient
        assertFalse(client.shouldOverrideUrlLoading(web, request("https://www.youtube-nocookie.com/embed/next")))
        assertTrue(client.shouldOverrideUrlLoading(web, request("https://www.youtube.com/watch?v=dQw4w9WgXcQ")))
        assertTrue(client.shouldOverrideUrlLoading(web, request("intent://evil")))
        assertTrue(client.shouldOverrideUrlLoading(web, request("https://ads.example.com", gesture = false)))
        assertEquals(listOf("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), opened)
    }

    @Test
    fun theResolutionOfUrls() {
        val r = MonarchEmbedResolution
        fun frame(url: String) = assertIs<MonarchEmbedResolution.Frame>(r.resolve(url)).src
        fun link(url: String, providers: List<MonarchEmbedProvider> = MonarchEmbedProvider.Defaults) =
            assertIs<MonarchEmbedResolution.Link>(r.resolve(url, providers)).host
        fun invalid(url: String) = assertIs<MonarchEmbedResolution.Invalid>(r.resolve(url))
        assertEquals(YouTubeEmbed, frame(YouTubeShare))
        assertEquals(YouTubeEmbed, frame(" https://youtu.be/dQw4w9WgXcQ "))
        assertEquals(YouTubeEmbed, frame("https://YouTube.com/shorts/dQw4w9WgXcQ"))
        assertEquals("https://player.vimeo.com/video/76979871", frame("https://vimeo.com/channels/staff/76979871"))
        assertEquals("https://www.loom.com/embed/0123456789abcdef", frame("https://loom.com/share/0123456789abcdef"))
        assertTrue(frame("https://www.figma.com/design/abc/File").startsWith("https://www.figma.com/embed?"))
        assertEquals("https://codepen.io/rook/embed/abc1?default-tab=result", frame("https://codepen.io/rook/pen/abc1"))
        assertEquals("youtube.com", link("http://youtube.com/watch?v=dQw4w9WgXcQ"))
        link("https://youtube.com:8443/watch?v=dQw4w9WgXcQ")
        link("https://youtube.com/watch?v=short")
        assertEquals("example.com", link("https://www.example.com"))
        invalid("javascript:alert(1)")
        invalid("https://user:pw@youtube.com/watch?v=dQw4w9WgXcQ")
        invalid("/relative/path")
        invalid("not a url")
        // Without the provider in the list, the same link is only a card.
        link(YouTubeShare, listOf(MonarchEmbedProvider.Vimeo))
    }

    @Test
    fun theFrameStaysOnTheHostOfItsEmbedAddress() {
        val t = MonarchEmbedBlockTokens
        assertTrue(t.staysInFrame(YouTubeEmbed, Uri.parse("https://WWW.youtube-nocookie.com/x")))
        assertFalse(t.staysInFrame(YouTubeEmbed, Uri.parse("http://www.youtube-nocookie.com/x")))
        assertFalse(t.staysInFrame(YouTubeEmbed, Uri.parse("https://evil.example/x")))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            assertEquals(c.bgMuted, MonarchEmbedBlockTokens.frameColor(c), theme.id)
            assertEquals(c.textMuted, MonarchEmbedBlockTokens.mutedColor(c), theme.id)
        }
    }
}

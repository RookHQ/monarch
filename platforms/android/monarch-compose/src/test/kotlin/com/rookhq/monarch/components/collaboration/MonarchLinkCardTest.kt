package com.rookhq.monarch.components.collaboration

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.rookhq.monarch.components.collaboration.linkcard.MonarchLinkCard
import com.rookhq.monarch.components.collaboration.linkcard.MonarchLinkCardSize
import com.rookhq.monarch.components.collaboration.linkcard.MonarchLinkCardTokens
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.MonarchTestEnvironment
import com.rookhq.monarch.testing.TestIcon
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import com.rookhq.monarch.theme.MonarchTheme
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val Post = "https://www.example.com/blog/march"

internal object MonarchLinkCardFixtures : ComponentFixtures {
    override val id = "link-card"
    override val fixtures = listOf(
        fixture("md, picture loading") {
            MonarchLinkCard(
                title = "Release notes for March",
                url = Post,
                description = "Faster search, shared views and a new API.",
                hasImage = true,
            )
        },
        fixture("md, picture, video, dismiss icon") {
            MonarchLinkCard(
                title = "Product tour",
                url = Post,
                image = ColorPainter(MonarchTheme.colors.data1),
                imageAspectRatio = 1.9f,
                video = true,
                onDismiss = {},
                dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
        fixture("sm, thumbnail") {
            MonarchLinkCard(
                title = "Release notes for March",
                url = Post,
                description = "Faster search, shared views and a new API.",
                hasImage = true,
                size = MonarchLinkCardSize.Sm,
            )
        },
        fixture("no picture, dismiss text") {
            MonarchLinkCard(title = "Contact sales", url = "mailto:sales@example.com", onDismiss = {})
        },
        fixture("not a safe link") { MonarchLinkCard(title = "Internal page", url = "intranet://wiki/home") },
        fixture("long title in a narrow width") {
            MonarchLinkCard(
                title = "A very long page title that goes on and on about everything that changed this quarter",
                url = Post,
                siteName = "The Example Engineering Blog",
                modifier = Modifier.width(220.dp),
                onDismiss = {},
                dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchLinkCardTest {
    @get:Rule
    val rule = createComposeRule()

    private class RecordingUriHandler : UriHandler {
        val opened = mutableListOf<String>()
        override fun openUri(uri: String) {
            opened += uri
        }
    }

    @Test
    fun tappingTheCardOpensTheLinkThroughTheUriHandler() {
        val handler = RecordingUriHandler()
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchLinkCard(title = "Release notes", url = Post, description = "What changed")
            }
        }
        val card = rule.onNode(hasText("Release notes") and hasClickAction())
        card.assert(hasText("example.com")).assert(hasText("What changed"))
        card.performClick()
        assertEquals(listOf(Post), handler.opened)
    }

    @Test
    fun onOpenReplacesTheUriHandler() {
        val handler = RecordingUriHandler()
        val opened = mutableListOf<String>()
        rule.setMonarchContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                MonarchLinkCard(title = "Release notes", url = Post, onOpen = { opened += it })
            }
        }
        rule.onNode(hasText("Release notes") and hasClickAction()).performClick()
        assertEquals(listOf(Post), opened)
        assertTrue(handler.opened.isEmpty())
    }

    @Test
    fun aVideoSaysSoInTextAndTheSiteDefaultsToTheHost() {
        rule.setMonarchContent { MonarchLinkCard(title = "Tour", url = Post, hasImage = true, video = true) }
        rule.onNode(hasText("Tour") and hasText("Video") and hasText("example.com")).assertExists()
    }

    @Test
    fun aLinkThatIsNotSafeDoesNotOpen() {
        rule.setMonarchContent { MonarchLinkCard(title = "Internal page", url = "javascript:alert(1)") }
        rule.onNode(hasText("Internal page") and hasClickAction()).assertDoesNotExist()
        rule.onNodeWithText("Internal page").assertExists()
    }

    @Test
    fun theRemoveButtonIsSeparateNamedAndAtTheEnd() {
        var dismissed = 0
        var opened = 0
        val environment = MonarchTestEnvironment()
        rule.setMonarchContent(environment) {
            MonarchLinkCard(
                title = "Release notes",
                url = Post,
                onOpen = { opened++ },
                onDismiss = { dismissed++ },
                dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
            )
        }
        listOf(LayoutDirection.Ltr, LayoutDirection.Rtl).forEach { direction ->
            environment.layoutDirection = direction
            rule.waitForIdle()
            rule.onNodeWithText("Release notes", useUnmergedTree = true)
                .assertIsBefore(rule.onNodeWithContentDescription("Remove the preview of Release notes"), direction)
        }
        rule.onNodeWithContentDescription("Remove the preview of Release notes")
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, dismissed)
        assertEquals(0, opened)
    }

    @Test
    fun withoutAnIconTheRemoveButtonHasText() {
        var dismissed = 0
        rule.setMonarchContent {
            MonarchLinkCard(title = "Release notes", url = Post, onDismiss = { dismissed++ }, removeLabel = "Retirer")
        }
        rule.onNodeWithContentDescription("Remove the preview of Release notes")
            .assert(hasText("Retirer"))
            .performClick()
        assertEquals(1, dismissed)
    }

    @Test
    fun theBuiltInStringsCanBeReplaced() {
        rule.setMonarchContent {
            MonarchLinkCard(
                title = "Visite",
                url = Post,
                video = true,
                onDismiss = {},
                dismissIcon = { MonarchIcon(imageVector = TestIcon, contentDescription = null) },
                dismissLabel = "Retirer l'aperçu",
                videoLabel = "Vidéo",
            )
        }
        rule.onNodeWithContentDescription("Retirer l'aperçu").assertExists()
        rule.onNode(hasText("Visite") and hasText("Vidéo")).assertExists()
    }

    @Test
    fun theTitleIsNotCutAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) { MonarchLinkCard(title = "Release notes", url = Post) }
        rule.onNodeWithText("Release notes", useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfLinksAndPictures() {
        assertTrue(MonarchLinkCardTokens.isSafe("https://example.com"))
        assertTrue(MonarchLinkCardTokens.isSafe("mailto:a@example.com"))
        assertFalse(MonarchLinkCardTokens.isSafe("javascript:alert(1)"))
        assertFalse(MonarchLinkCardTokens.isSafe("not a url"))
        assertEquals("example.com", MonarchLinkCardTokens.siteName(null, Post))
        assertEquals("Blog", MonarchLinkCardTokens.siteName("Blog", Post))
        assertNull(MonarchLinkCardTokens.siteName(null, "mailto:a@example.com"))
        assertEquals(MonarchLinkCardTokens.MAX_RATIO, MonarchLinkCardTokens.aspectRatio(null))
        assertEquals(MonarchLinkCardTokens.MIN_RATIO, MonarchLinkCardTokens.aspectRatio(0.5f))
        assertEquals(1.9f, MonarchLinkCardTokens.aspectRatio(1.9f))
    }

    @Test
    fun thePlaceholderUsesTheMutedRole() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgMuted, MonarchLinkCardTokens.placeholderColor(colors), theme.id)
        }
    }
}

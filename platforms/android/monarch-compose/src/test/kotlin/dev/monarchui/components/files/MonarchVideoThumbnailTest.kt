package dev.monarchui.components.files

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.files.videothumbnail.MonarchVideoThumbnail
import dev.monarchui.components.files.videothumbnail.MonarchVideoThumbnailTokens
import dev.monarchui.components.files.videothumbnail.captureVideoFrame
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.MonarchTestEnvironment
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.testing.stateDescription
import dev.monarchui.theme.MonarchColors
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Poster = ColorPainter(Color.Gray)

internal object MonarchVideoThumbnailFixtures : ComponentFixtures {
    override val id = "video-thumbnail"
    override val fixtures = listOf(
        fixture("a button with a poster and a length") {
            MonarchVideoThumbnail(title = "Product tour", image = Poster, onPlay = {}, durationMillis = 65_000L)
        },
        fixture("a disabled button") {
            MonarchVideoThumbnail(title = "Product tour", image = Poster, onPlay = {}, enabled = false)
        },
        fixture("an image without a play mark") {
            MonarchVideoThumbnail(title = "Clip", image = Poster, showPlayIcon = false, durationMillis = 3_725_000L)
        },
        fixture("placeholder, square, fitted") {
            MonarchVideoThumbnail(
                title = "",
                image = null,
                modifier = Modifier.width(160.dp),
                aspectRatio = 1f,
                contentScale = ContentScale.Fit,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchVideoThumbnailTest {
    @get:Rule
    val rule = createComposeRule()

    private val isButton = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button)
    private val isImage = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image)

    @Test
    fun withOnPlayItIsOneButtonNamedPlayAndTheTitle() {
        var plays = 0
        rule.setMonarchContent {
            MonarchVideoThumbnail(
                title = "Product tour",
                image = Poster,
                onPlay = { plays++ },
                durationMillis = 65_000L,
            )
        }
        val button = rule.onNodeWithContentDescription("Play Product tour")
        button.assert(isButton).assertHasClickAction()
        assertEquals("1 minute, 5 seconds", button.stateDescription())
        rule.onNodeWithText("1:05").assertDoesNotExist()
        button.performClick()
        assertEquals(1, plays)
    }

    @Test
    fun withoutOnPlayItIsAnImageNamedByTheTitle() {
        rule.setMonarchContent { MonarchVideoThumbnail(title = "  Clip  ", image = Poster) }
        rule.onNodeWithContentDescription("Clip").assert(isImage).assertHasNoClickAction()
    }

    @Test
    fun anEmptyTitleGivesTheBuiltInName() {
        rule.setMonarchContent { MonarchVideoThumbnail(title = " ", image = null) }
        rule.onNodeWithContentDescription("Video").assertExists()
    }

    @Test
    fun playLabelReplacesTheButtonName() {
        rule.setMonarchContent {
            MonarchVideoThumbnail(title = "Tour", image = null, onPlay = {}, playLabel = "Lire la vidéo")
        }
        rule.onNodeWithContentDescription("Lire la vidéo").assert(isButton)
    }

    @Test
    fun aDisabledThumbnailDoesNotPlay() {
        var plays = 0
        rule.setMonarchContent {
            MonarchVideoThumbnail(title = "Tour", image = Poster, onPlay = { plays++ }, enabled = false)
        }
        rule.onNodeWithContentDescription("Play Tour").assertIsNotEnabled().performClick()
        assertEquals(0, plays)
    }

    @Test
    fun theButtonIsAtLeast48dpAndKeepsItsAspectRatio() {
        rule.setMonarchContent {
            MonarchVideoThumbnail(
                title = "Tour",
                image = Poster,
                modifier = Modifier.width(160.dp).testTag("thumb"),
                onPlay = {},
            )
        }
        rule.onNodeWithTag("thumb").assertHeightIsEqualTo(90.dp).assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun theHiddenBadgeSitsAtTheEndInsideTheFrameAtTheLargestFontScale() {
        val environment = MonarchTestEnvironment(fontScale = LargestFontScale)
        rule.setMonarchContent(environment) {
            MonarchVideoThumbnail(
                title = "Tour",
                image = Poster,
                modifier = Modifier.testTag("thumb"),
                durationMillis = 5_000L,
            )
        }
        LayoutDirection.entries.forEach { direction ->
            environment.layoutDirection = direction
            // The badge is hidden from TalkBack, so it is the one child node left in the unmerged tree.
            val badge = rule.onNodeWithTag("thumb", useUnmergedTree = true).onChildren().onLast()
            val box = rule.onNodeWithTag("thumb").fetchSemanticsNode().boundsInRoot
            val bounds = badge.fetchSemanticsNode().boundsInRoot
            if (direction == LayoutDirection.Ltr) {
                assertTrue(bounds.left > box.center.x && bounds.right <= box.right, "badge at the end in LTR")
            } else {
                assertTrue(bounds.right < box.center.x && bounds.left >= box.left, "badge at the end in RTL")
            }
        }
    }

    @Test
    fun lengthsAreFormattedForTheBadgeAndForSpeech() {
        val locale = Locale.US
        assertEquals("0:00", MonarchVideoThumbnailTokens.durationText(0L, locale))
        assertEquals("1:05", MonarchVideoThumbnailTokens.durationText(64_600L, locale))
        assertEquals("1:01:05", MonarchVideoThumbnailTokens.durationText(3_665_000L, locale))
        assertEquals("0:00", MonarchVideoThumbnailTokens.durationText(-5L, locale))
        assertNull(MonarchVideoThumbnailTokens.durationText(null, locale))
        assertEquals("1 hour, 1 minute, 5 seconds", MonarchVideoThumbnailTokens.spokenDuration(3_665_000L, locale))
        assertEquals("2 minutes", MonarchVideoThumbnailTokens.spokenDuration(120_000L, locale))
        assertEquals("0 seconds", MonarchVideoThumbnailTokens.spokenDuration(0L, locale))
    }

    @Test
    fun theAspectRatioFallsBackTo16By9() {
        assertEquals(16f / 9f, MonarchVideoThumbnailTokens.aspectRatio(null))
        assertEquals(16f / 9f, MonarchVideoThumbnailTokens.aspectRatio(0f))
        assertEquals(16f / 9f, MonarchVideoThumbnailTokens.aspectRatio(Float.NaN))
        assertEquals(1f, MonarchVideoThumbnailTokens.aspectRatio(1f))
    }

    @Test
    fun theCaptureTimeStaysInsideTheVideo() {
        assertEquals(1_000L, MonarchVideoThumbnailTokens.captureTimeMillis(null, 60_000L))
        assertEquals(500L, MonarchVideoThumbnailTokens.captureTimeMillis(null, 5_000L))
        assertEquals(4_950L, MonarchVideoThumbnailTokens.captureTimeMillis(9_000L, 5_000L))
        assertEquals(0L, MonarchVideoThumbnailTokens.captureTimeMillis(-1L, 5_000L))
        assertEquals(0L, MonarchVideoThumbnailTokens.captureTimeMillis(null, 0L))
    }

    @Test
    fun captureVideoFrameReadsTheFrameAtTheCaptureTime() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val uri = Uri.parse("content://test/video.mp4")
        val source = DataSource.toDataSource(context, uri)
        ShadowMediaMetadataRetriever.addMetadata(source, MediaMetadataRetriever.METADATA_KEY_DURATION, "20000")
        val frame = Bitmap.createBitmap(4, 3, Bitmap.Config.ARGB_8888)
        ShadowMediaMetadataRetriever.addFrame(source, 1_000_000L, frame)
        val captured = runBlocking { captureVideoFrame(context, uri) }
        assertNotNull(captured)
        assertEquals(4, captured.width)
        assertNull(runBlocking { captureVideoFrame(context, Uri.parse("content://test/none.mp4")) })
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            val enabled = MonarchVideoThumbnailTokens.colors(enabled = true, colors)
            assertEquals(colors.bgEmphasis, enabled.background, theme.id)
            assertEquals(colors.textMuted, enabled.placeholderGlyph, theme.id)
            assertEquals(colors.bgSurface, enabled.playDisc, theme.id)
            assertEquals(colors.textDefault, enabled.playGlyph, theme.id)
            assertEquals(colors.bgInverse, enabled.badge, theme.id)
            assertEquals(colors.textInverse, enabled.badgeText, theme.id)
            val disabled = MonarchVideoThumbnailTokens.colors(enabled = false, colors)
            assertEquals(colors.bgMuted, disabled.playDisc, theme.id)
            assertEquals(colors.textDisabled, disabled.playGlyph, theme.id)
        }
    }
}

package com.rookhq.monarch.components.files

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isHeading
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityOptionsCompat
import com.rookhq.monarch.components.files.fileupload.MonarchFileUploadLabels
import com.rookhq.monarch.components.files.fileupload.MonarchPickedFile
import com.rookhq.monarch.components.files.mediaupload.MediaUploadRefusal
import com.rookhq.monarch.components.files.mediaupload.MonarchMediaUpload
import com.rookhq.monarch.components.files.mediaupload.MonarchMediaUploadKind
import com.rookhq.monarch.components.files.mediaupload.MonarchMediaUploadLabels
import com.rookhq.monarch.components.files.mediaupload.MonarchMediaUploadResult
import com.rookhq.monarch.components.files.mediaupload.MonarchMediaUploadTokens
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertIsBefore
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MediaAuthority = "com.rookhq.monarch.test.media"
private const val MiB = 1024L * 1024

/** A provider with a photo, a large photo and a video of known name, type and size. */
class MonarchMediaUploadTestProvider : ContentProvider() {
    private class Entry(val name: String, val type: String, val size: Long)

    private val files = mapOf(
        "photo" to Entry("photo.png", "image/png", MiB),
        "big" to Entry("big.jpg", "image/jpeg", 30 * MiB),
        "clip" to Entry("clip.mp4", "video/mp4", 2 * MiB),
    )

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val entry = files.getValue(uri.lastPathSegment.orEmpty())
        return MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply {
            addRow(arrayOf<Any>(entry.name, entry.size))
        }
    }

    override fun getType(uri: Uri): String? = files[uri.lastPathSegment]?.type

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, s: String?, a: Array<out String>?): Int = 0
}

private fun media(id: String): Uri = Uri.parse("content://$MediaAuthority/$id")

/** A registry that answers every launch at once with [result] and records the input. */
private class MediaRegistry(private val result: () -> Uri?) : ActivityResultRegistry() {
    val inputs = mutableListOf<Any?>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        inputs += input
        dispatchResult(requestCode, result())
    }
}

@Composable
private fun WithMediaRegistry(registry: ActivityResultRegistry, content: @Composable () -> Unit) {
    val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner, content = content)
}

/** Sizes in whole MB, so the texts do not depend on the system's formatter. */
private val testLabels = MonarchMediaUploadLabels(
    fileUpload = MonarchFileUploadLabels(formatSize = { "${it / MiB} MB" }),
)

private val stored: suspend (MonarchPickedFile, Any) -> MonarchMediaUploadResult = { file, _ ->
    MonarchMediaUploadResult("https://cdn.example.com/${file.name}")
}

internal object MonarchMediaUploadFixtures : ComponentFixtures {
    override val id = "media-upload"
    override val fixtures = listOf(
        fixture("empty, images") {
            MonarchMediaUpload(
                label = "Cover image",
                value = null,
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                maxImageSize = 5 * MiB,
                labels = testLabels,
            )
        },
        fixture("empty, images and videos with help") {
            MonarchMediaUpload(
                label = "Hero media",
                value = null,
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                helperText = "Shown at the top of the page.",
                kinds = setOf(MonarchMediaUploadKind.Image, MonarchMediaUploadKind.Video),
                maxVideoDurationMillis = 90_000L,
            )
        },
        fixture("filled image") {
            MonarchMediaUpload(
                label = "Cover image",
                value = "https://cdn.example.com/cover.jpg",
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
            )
        },
        fixture("filled video with play") {
            MonarchMediaUpload(
                label = "Intro",
                value = "https://cdn.example.com/intro.mp4",
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                kinds = setOf(MonarchMediaUploadKind.Video),
                onPlay = {},
            )
        },
        fixture("disabled") {
            MonarchMediaUpload(
                label = "Cover image",
                value = null,
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                enabled = false,
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS")
class MonarchMediaUploadTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    @Before
    fun registerProvider() {
        Robolectric.setupContentProvider(MonarchMediaUploadTestProvider::class.java, MediaAuthority)
    }

    @Test
    fun anEmptyFieldOffersThePhotoPickerWithItsLimits() {
        rule.setMonarchContent {
            MonarchMediaUpload(
                label = "Cover image",
                value = null,
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                maxImageSize = 5 * MiB,
                labels = testLabels,
            )
        }
        rule.onNodeWithText("Cover image").assert(isHeading())
        rule.onNodeWithText("Add an image").assertExists()
        rule.onNodeWithText("PNG, JPG, GIF or WebP up to 5 MB").assertExists()
        rule.onNodeWithText("Choose image").assertTouchTargetAtLeast48dp(rule.density)
    }

    @Test
    fun aPickedImageIsUploadedWithProgressAndAnnounced() {
        val registry = MediaRegistry { media("photo") }
        val gate = CompletableDeferred<Unit>()
        var value by mutableStateOf<String?>(null)
        val kinds = mutableListOf<MonarchMediaUploadKind>()
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = value,
                    onValueChange = { value = it },
                    onUpload = { file, context ->
                        kinds += context.kind
                        context.reportProgress(0.4f)
                        gate.await()
                        MonarchMediaUploadResult("https://cdn.example.com/${file.name}")
                    },
                    labels = testLabels,
                )
            }
        }
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        val request = registry.inputs.single()
        assertIs<PickVisualMediaRequest>(request)
        assertEquals(PickVisualMedia.ImageOnly, request.mediaType)
        rule.onNodeWithText("Uploading photo.png · 40%").assertExists()
        rule.onNodeWithText("Cancel upload").assertTouchTargetAtLeast48dp(rule.density)
        gate.complete(Unit)
        rule.waitForIdle()
        assertEquals("https://cdn.example.com/photo.png", value)
        assertEquals(listOf(MonarchMediaUploadKind.Image), kinds)
        rule.onNode(polite and hasContentDescription("Image uploaded")).assertExists()
        rule.onNodeWithText("Replace image").assertExists()
    }

    @Test
    fun aTooLargeImageIsRefusedWithoutUploading() {
        val registry = MediaRegistry { media("big") }
        var uploads = 0
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = null,
                    onValueChange = {},
                    onUpload = { f, c ->
                        uploads++
                        stored(f, c)
                    },
                    maxImageSize = 5 * MiB,
                    labels = testLabels,
                )
            }
        }
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        assertEquals(0, uploads)
        rule.onNodeWithText("big.jpg is larger than 5 MB.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun aVideoIsRefusedWithTheReasonWhenOnlyImagesAreTaken() {
        val registry = MediaRegistry { media("clip") }
        var uploads = 0
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = null,
                    onValueChange = {},
                    onUpload = { f, c ->
                        uploads++
                        stored(f, c)
                    },
                    videoUnavailable = "Videos need the Pro plan.",
                )
            }
        }
        rule.onNodeWithText("Videos need the Pro plan.", substring = true).assertExists()
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        assertEquals(PickVisualMedia.ImageAndVideo, (registry.inputs.single() as PickVisualMediaRequest).mediaType)
        assertEquals(0, uploads)
        rule.onNodeWithText(
            "clip.mp4 is a video. Only images can be added here.",
            substring = true,
            useUnmergedTree = true,
        )
            .assertExists()
    }

    @Test
    fun aVideoWhoseLengthCannotBeReadIsRefused() {
        val registry = MediaRegistry { media("clip") }
        var uploads = 0
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Intro",
                    value = null,
                    onValueChange = {},
                    onUpload = { f, c ->
                        uploads++
                        stored(f, c)
                    },
                    kinds = setOf(MonarchMediaUploadKind.Image, MonarchMediaUploadKind.Video),
                    maxVideoDurationMillis = 90_000L,
                )
            }
        }
        rule.onNodeWithText("MP4, MOV or WebM, 1:30 min at most", substring = true).assertExists()
        rule.onNodeWithText("Choose image or video").performClick()
        rule.waitUntil(timeoutMillis = 5_000L) {
            rule.onAllNodes(hasContentDescription("The length of clip.mp4 could not be read.")).fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(0, uploads)
    }

    @Test
    fun aFailedUploadShowsTheMessageAndKeepsTheValue() {
        val registry = MediaRegistry { media("photo") }
        var value by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = value,
                    onValueChange = { value = it },
                    onUpload = { _, _ -> error("Storage is full.") },
                )
            }
        }
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        assertNull(value)
        rule.onNodeWithText("Storage is full.", substring = true).assertExists()
        rule.onNode(polite and hasContentDescription("Storage is full.")).assertExists()
    }

    @Test
    fun cancellingStopsTheUploadAndIsAnnounced() {
        val registry = MediaRegistry { media("photo") }
        val never = CompletableDeferred<Unit>()
        var value by mutableStateOf<String?>(null)
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = value,
                    onValueChange = { value = it },
                    onUpload = { file, context ->
                        context.reportProgress(0.1f)
                        never.await()
                        MonarchMediaUploadResult(file.name)
                    },
                )
            }
        }
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Cancel upload").performClick()
        rule.waitForIdle()
        assertNull(value)
        rule.onNode(polite and hasContentDescription("Upload cancelled")).assertExists()
        rule.onNodeWithText("Choose image").assertExists()
    }

    @Test
    fun aFilledImageHasAPreviewReplaceAndRemove() {
        var value by mutableStateOf<String?>("https://cdn.example.com/cover.jpg")
        rule.setMonarchContent {
            MonarchMediaUpload(
                label = "Cover image",
                value = value,
                onValueChange = { value = it },
                onUpload = { f, c -> stored(f, c) },
                previewLabel = "Cover of the report",
            )
        }
        rule.onNodeWithContentDescription("Cover of the report").assertExists()
        rule.onNodeWithText("Replace image").assertTouchTargetAtLeast48dp(rule.density)
        rule.onNodeWithText("Remove image").assertTouchTargetAtLeast48dp(rule.density).performClick()
        rule.waitForIdle()
        assertNull(value)
        rule.onNode(polite and hasContentDescription("Image removed")).assertExists()
    }

    @Test
    fun aFilledVideoIsAPlayButton() {
        var plays = 0
        rule.setMonarchContent {
            MonarchMediaUpload(
                label = "Intro",
                value = "https://cdn.example.com/intro.mp4",
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                kinds = setOf(MonarchMediaUploadKind.Video),
                onPlay = { plays++ },
            )
        }
        rule.onNodeWithContentDescription("Play video")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchTargetAtLeast48dp(rule.density)
            .performClick()
        assertEquals(1, plays)
        rule.onNodeWithText("Replace video").assertExists()
        rule.onNodeWithText("Remove video").assertExists()
    }

    @Test
    fun warningsAreShownAfterTheUpload() {
        val registry = MediaRegistry { media("photo") }
        rule.setMonarchContent {
            WithMediaRegistry(registry) {
                MonarchMediaUpload(
                    label = "Cover image",
                    value = null,
                    onValueChange = {},
                    onUpload = { _, _ -> MonarchMediaUploadResult("u", warnings = listOf("The image was resized.")) },
                )
            }
        }
        rule.onNodeWithText("Choose image").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Uploaded with warnings.", substring = true, useUnmergedTree = true).assertExists()
        rule.onNodeWithText("The image was resized.", substring = true, useUnmergedTree = true).assertExists()
    }

    @Test
    fun replaceComesBeforeRemoveRightToLeft() {
        rule.setMonarchContent(layoutDirection = LayoutDirection.Rtl) {
            MonarchMediaUpload(
                label = "Cover image",
                value = "https://cdn.example.com/cover.jpg",
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
            )
        }
        rule.onNodeWithText("Replace image").assertIsBefore(rule.onNodeWithText("Remove image"), LayoutDirection.Rtl)
    }

    @Test
    fun theHintIsFullyVisibleAtTheLargestFontScale() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchMediaUpload(
                label = "Cover image",
                value = null,
                onValueChange = {},
                onUpload = { f, c -> stored(f, c) },
                maxImageSize = 5 * MiB,
                labels = testLabels,
            )
        }
        rule.onNodeWithText("PNG, JPG, GIF or WebP up to 5 MB").assertTextFullyVisible()
    }

    @Test
    fun theRulesDecideKindsRefusalsAndProgress() {
        val tokens = MonarchMediaUploadTokens
        assertEquals(MonarchMediaUploadKind.Video, tokens.kindOf("video/mp4", "a.bin"))
        assertEquals(MonarchMediaUploadKind.Image, tokens.kindOf("image/png", "a.mov"))
        assertEquals(MonarchMediaUploadKind.Video, tokens.kindOf(null, "clip.MOV"))
        assertEquals(MonarchMediaUploadKind.Video, tokens.kindOfValue("https://x.test/a.webm?t=1"))
        assertEquals(MonarchMediaUploadKind.Image, tokens.kindOfValue("https://x.test/a.jpg"))
        val images = setOf(MonarchMediaUploadKind.Image)
        assertEquals(listOf("image/*"), tokens.accept(images, explainsVideo = false))
        assertEquals(listOf("image/*", "video/*"), tokens.accept(images, explainsVideo = true))
        assertEquals(
            MediaUploadRefusal.VideoNotAllowed,
            tokens.refusal(MonarchMediaUploadKind.Video, 1, images, null, null),
        )
        assertEquals(MediaUploadRefusal.TooLarge, tokens.refusal(MonarchMediaUploadKind.Image, 10, images, 5, null))
        assertNull(tokens.refusal(MonarchMediaUploadKind.Image, 5, images, 5, null))
        assertNull(tokens.refusal(MonarchMediaUploadKind.Image, null, images, 5, null))
        assertTrue(tokens.tooLong(91_000, 90_000))
        assertFalse(tokens.tooLong(91_000, null))
        assertEquals(40, tokens.percent(0.4f))
        assertEquals(100, tokens.percent(1.5f))
        assertNull(tokens.percent(null))
    }

    @Test
    fun coloursComeFromRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.bgMuted, MonarchMediaUploadTokens.placeholder(colors))
            assertEquals(colors.textMuted, MonarchMediaUploadTokens.statusColor(colors))
        }
    }
}

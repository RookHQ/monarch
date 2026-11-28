package dev.monarchui.components.files

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
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
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import dev.monarchui.components.files.fileupload.MonarchFileRejection
import dev.monarchui.components.files.fileupload.MonarchFileRejectionCode
import dev.monarchui.components.files.fileupload.MonarchFileUpload
import dev.monarchui.components.files.fileupload.MonarchFileUploadLabels
import dev.monarchui.components.files.fileupload.MonarchFileUploadPicker
import dev.monarchui.components.files.fileupload.MonarchFileUploadTokens
import dev.monarchui.components.files.fileupload.MonarchFileUploadValidator
import dev.monarchui.components.files.fileupload.MonarchFileUploadVariant
import dev.monarchui.components.files.fileupload.MonarchPickedFile
import dev.monarchui.testing.AllThemes
import dev.monarchui.testing.ComponentFixtures
import dev.monarchui.testing.LargestFontScale
import dev.monarchui.testing.assertIsBefore
import dev.monarchui.testing.assertTextFullyVisible
import dev.monarchui.testing.assertTouchTargetAtLeast48dp
import dev.monarchui.testing.fixture
import dev.monarchui.testing.setMonarchContent
import dev.monarchui.theme.MonarchColors
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val Authority = "dev.monarchui.test.files"
private const val MB = 1024L * 1024

/** A provider with a few files of known name, type and size. */
class MonarchFileUploadTestProvider : ContentProvider() {
    private class Entry(val name: String, val type: String, val size: Long)

    private val files = mapOf(
        "report" to Entry("report.pdf", "application/pdf", 2 * MB),
        "photo" to Entry("photo.png", "image/png", MB),
        "big" to Entry("scan.pdf", "application/pdf", 30 * MB),
        "table" to Entry("table.csv", "text/csv", 1_000),
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

private fun file(id: String): Uri = Uri.parse("content://$Authority/$id")

/** A registry that answers every launch at once with [result] and records the input. */
private class AnsweringRegistry(private val result: (Any?) -> Any?) : ActivityResultRegistry() {
    val inputs = mutableListOf<Any?>()

    override fun <I, O> onLaunch(
        requestCode: Int,
        contract: ActivityResultContract<I, O>,
        input: I,
        options: ActivityOptionsCompat?,
    ) {
        inputs += input
        dispatchResult(requestCode, result(input))
    }
}

@Composable
private fun WithRegistry(registry: ActivityResultRegistry, content: @Composable () -> Unit) {
    val owner = object : ActivityResultRegistryOwner {
        override val activityResultRegistry: ActivityResultRegistry = registry
    }
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides owner, content = content)
}

internal object MonarchFileUploadFixtures : ComponentFixtures {
    override val id = "file-upload"
    override val fixtures = listOf(
        fixture("zone with limits") {
            MonarchFileUpload(
                onFiles = { _, _ -> },
                accept = listOf("application/pdf"),
                maxSize = 10 * MB,
                maxFiles = 5,
            )
        },
        fixture("compact") { MonarchFileUpload(onFiles = { _, _ -> }, variant = MonarchFileUploadVariant.Compact) },
        fixture("limit reached and disabled") {
            Column {
                MonarchFileUpload(onFiles = { _, _ -> }, maxFiles = 2, currentCount = 2)
                MonarchFileUpload(onFiles = { _, _ -> }, enabled = false, variant = MonarchFileUploadVariant.Compact)
            }
        },
        fixture("invalid with a long hint") {
            MonarchFileUpload(
                onFiles = { _, _ -> },
                invalid = true,
                errorText = "Attach the signed contract",
                hint = "The signed contract as a PDF, scanned in color, with every page included",
            )
        },
    )
}

@RunWith(RobolectricTestRunner::class)
class MonarchFileUploadTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun registerProvider() {
        Robolectric.setupContentProvider(MonarchFileUploadTestProvider::class.java, Authority)
    }

    @Test
    fun browseOpensTheDocumentPickerAndAcceptedFilesAreHandedOver() {
        val registry = AnsweringRegistry { listOf(file("report"), file("photo")) }
        var accepted = listOf<MonarchPickedFile>()
        var rejected = listOf<MonarchFileRejection>()
        rule.setMonarchContent {
            WithRegistry(registry) {
                MonarchFileUpload(
                    onFiles = { a, r ->
                        accepted = a
                        rejected = r
                    },
                    accept = listOf("application/pdf", "image/*"),
                )
            }
        }
        rule.onNodeWithText("Browse files").assertTouchTargetAtLeast48dp(rule.density).performClick()
        assertEquals(listOf("application/pdf", "image/*"), (registry.inputs.single() as Array<*>).toList())
        assertEquals(listOf("report.pdf", "photo.png"), accepted.map { it.name })
        assertEquals(listOf("application/pdf", "image/png"), accepted.map { it.mimeType })
        assertEquals(2 * MB, accepted.first().size)
        assertEquals(emptyList(), rejected)
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        rule.onNode(polite and hasContentDescription("2 files added.")).assertExists()
    }

    @Test
    fun refusedFilesAreExplainedByTypeSizeAndCount() {
        val registry = AnsweringRegistry { listOf(file("table"), file("big"), file("report"), file("photo")) }
        var rejected = listOf<MonarchFileRejection>()
        rule.setMonarchContent {
            WithRegistry(registry) {
                MonarchFileUpload(
                    onFiles = { _, r -> rejected = r },
                    accept = listOf("application/pdf", "image/*"),
                    maxSize = 10 * MB,
                    maxFiles = 2,
                    currentCount = 1,
                )
            }
        }
        rule.onNodeWithText("Browse files").performClick()
        assertEquals(
            listOf(
                MonarchFileRejectionCode.InvalidType,
                MonarchFileRejectionCode.TooLarge,
                MonarchFileRejectionCode.TooMany,
            ),
            rejected.map { it.code },
        )
        rule.onNodeWithText("table.csv is not an allowed file type.", substring = true, useUnmergedTree = true)
            .assertExists()
        assertTrue(rejected[1].message.startsWith("scan.pdf is larger than"))
        assertEquals("photo.png was not added: up to 2 files.", rejected[2].message)
    }

    @Test
    fun oneFileUsesTheSingleDocumentPickerAndTheAppsOwnCheck() {
        val registry = AnsweringRegistry { file("report") }
        var rejected = listOf<MonarchFileRejection>()
        rule.setMonarchContent {
            WithRegistry(registry) {
                MonarchFileUpload(
                    onFiles = { _, r -> rejected = r },
                    maxFiles = 1,
                    validate = { if (it.name.startsWith("report")) "Reports are uploaded elsewhere." else null },
                )
            }
        }
        rule.onNodeWithText("Browse files").performClick()
        assertEquals(listOf("*/*"), (registry.inputs.single() as Array<*>).toList())
        assertEquals(MonarchFileRejectionCode.Custom, rejected.single().code)
        assertEquals("Reports are uploaded elsewhere.", rejected.single().message)
    }

    @Test
    fun theMediaPickerAsksForImagesOnly() {
        val registry = AnsweringRegistry { listOf(file("photo")) }
        var accepted = 0
        rule.setMonarchContent {
            WithRegistry(registry) {
                MonarchFileUpload(
                    onFiles = { a, _ -> accepted = a.size },
                    picker = MonarchFileUploadPicker.Media,
                    accept = listOf("image/*"),
                )
            }
        }
        rule.onNodeWithText("Browse files").performClick()
        val request = assertIs<PickVisualMediaRequest>(registry.inputs.single())
        assertEquals(PickVisualMedia.ImageOnly, request.mediaType)
        assertEquals(1, accepted)
    }

    @Test
    fun theLimitsAreTheHintAndAFullOrDisabledPickerCannotOpen() {
        val registry = AnsweringRegistry { emptyList<Uri>() }
        rule.setMonarchContent {
            WithRegistry(registry) {
                Column {
                    MonarchFileUpload(
                        onFiles = { _, _ -> },
                        maxSize = 10 * MB,
                        maxFiles = 5,
                        labels = MonarchFileUploadLabels(formatSize = { "${it / MB} MB" }),
                    )
                    MonarchFileUpload(
                        onFiles = { _, _ -> },
                        maxFiles = 2,
                        currentCount = 2,
                        labels = MonarchFileUploadLabels(browse = "Add"),
                    )
                }
            }
        }
        rule.onNodeWithText("Up to 10 MB each · 5 files max").assertExists()
        rule.onNodeWithText("Limit reached: 2 files attached.").assertExists()
        rule.onNodeWithText("Add").assertIsNotEnabled().performClick()
        assertEquals(0, registry.inputs.size)
    }

    @Test
    fun anInvalidPickerSetsTheErrorOnTheButton() {
        rule.setMonarchContent {
            MonarchFileUpload(onFiles = { _, _ -> }, invalid = true, errorText = "Attach the signed contract")
        }
        rule.onNode(
            hasText("Browse files") and
                SemanticsMatcher.expectValue(SemanticsProperties.Error, "Attach the signed contract"),
        ).assertExists()
    }

    @Test
    fun theButtonFollowsTheTextRightToLeftAndTheHintWraps() {
        rule.setMonarchContent(fontScale = LargestFontScale, layoutDirection = LayoutDirection.Rtl) {
            MonarchFileUpload(onFiles = { _, _ -> }, hint = "The signed contract as a PDF, with every page")
        }
        rule.onNodeWithText("Choose files to upload")
            .assertIsBefore(rule.onNodeWithText("Browse files"), LayoutDirection.Rtl)
        rule.onNodeWithText("The signed contract", substring = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfAccept() {
        val t = MonarchFileUploadTokens
        assertTrue(t.matchesAccept("a.PDF", null, listOf(".pdf")))
        assertTrue(t.matchesAccept("a", "image/jpeg", listOf(" image/* ")))
        assertEquals(false, t.matchesAccept("a.txt", "text/plain", listOf("image/*", ".pdf")))
        assertTrue(t.matchesAccept("a.txt", "text/plain", emptyList()))
        assertContentEquals(arrayOf("text/csv"), t.documentTypes(listOf(".csv")) { "text/csv" })
        assertContentEquals(arrayOf("*/*"), t.documentTypes(listOf(".xyz")) { null })
        assertEquals(PickVisualMedia.VideoOnly, t.mediaType(listOf("video/mp4")))
        assertEquals(PickVisualMedia.ImageAndVideo, t.mediaType(emptyList()))
        assertTrue(t.full(maxFiles = 2, currentCount = 3))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val c = MonarchColors(theme.colors)
            val t = MonarchFileUploadTokens
            assertEquals(c.dangerBorder, t.zoneColors(invalid = true, enabled = true, colors = c).border, theme.id)
            assertEquals(c.borderDefault, t.zoneColors(invalid = false, enabled = true, colors = c).border, theme.id)
            assertEquals(c.bgSubtle, t.zoneColors(invalid = false, enabled = true, colors = c).container, theme.id)
            assertEquals(c.bgMuted, t.zoneColors(invalid = false, enabled = false, colors = c).container, theme.id)
            assertEquals(c.textDisabled, t.hintColor(false, c), theme.id)
            assertEquals(c.textMuted, t.hintColor(true, c), theme.id)
        }
    }

    @Test
    fun filesFromAnotherSourceGetTheSameChecksAndMessages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fun picked(name: String, type: String, size: Long) =
            MonarchPickedFile(Uri.parse("content://paste/$name"), name, type, size)
        val result = MonarchFileUploadValidator.validate(
            context = context,
            files = listOf(
                picked("a.pdf", "application/pdf", 10),
                picked("b.exe", "application/octet-stream", 10),
                picked("c.pdf", "application/pdf", 5_000),
                picked("d.pdf", "application/pdf", 10),
                picked("e.pdf", "application/pdf", 10),
            ),
            accept = listOf(".pdf"),
            maxSize = 1_000,
            maxFiles = 3,
            currentCount = 1,
            labels = MonarchFileUploadLabels(formatSize = { "$it B" }),
            validate = { if (it.name == "d.pdf") "Already attached." else null },
        )
        assertEquals(listOf("a.pdf", "e.pdf"), result.accepted.map { it.name })
        assertEquals(
            listOf(
                MonarchFileRejectionCode.InvalidType,
                MonarchFileRejectionCode.TooLarge,
                MonarchFileRejectionCode.Custom,
            ),
            result.rejected.map { it.code },
        )
        assertEquals("Already attached.", result.rejected.last().message)
    }
}

package com.rookhq.monarch.components.datadisplay

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportButton
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportButtonLabels
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportButtonTokens
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportFormat
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportPhase
import com.rookhq.monarch.components.datadisplay.downloadreportbutton.MonarchDownloadReportResult
import com.rookhq.monarch.testing.AllThemes
import com.rookhq.monarch.testing.ComponentFixtures
import com.rookhq.monarch.testing.LargestFontScale
import com.rookhq.monarch.testing.assertTextFullyVisible
import com.rookhq.monarch.testing.assertTouchTargetAtLeast48dp
import com.rookhq.monarch.testing.fixture
import com.rookhq.monarch.testing.setMonarchContent
import com.rookhq.monarch.testing.stateDescription
import com.rookhq.monarch.theme.MonarchColors
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val Pdf = MonarchDownloadReportFormat("PDF", "pdf", "For printing", "application/pdf")
private val Formats = listOf(MonarchDownloadReportFormat.Csv, Pdf)

internal object MonarchDownloadReportButtonFixtures : ComponentFixtures {
    override val id = "download-report-button"
    override val fixtures = listOf(
        fixture("one format") { MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Downloaded }) },
        fixture("several formats") {
            MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Downloaded }, formats = Formats)
        },
        fixture("disabled") {
            MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Downloaded }, enabled = false)
        },
        fixture("long label") {
            MonarchDownloadReportButton(
                onDownload = { MonarchDownloadReportResult.Downloaded },
                formats = listOf(MonarchDownloadReportFormat("Spreadsheet with every invoice line", "xlsx")),
            )
        },
    )
}

/** An activity result registry that answers the save dialog with [uri] and keeps the MIME type it was asked for. */
private class SaveDialog(private val uri: Uri?) : ActivityResultRegistryOwner {
    var mimeType: String? = null
    var fileName: String? = null

    override val activityResultRegistry: ActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I, O> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?,
        ) {
            fileName = input as String
            mimeType = contract.createIntent(context, input).type
            dispatchResult(requestCode, uri)
        }
    }

    lateinit var context: Context
}

@RunWith(RobolectricTestRunner::class)
class MonarchDownloadReportButtonTest {
    @get:Rule
    val rule = createComposeRule()

    private val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)

    private fun announced(text: String) = polite and hasContentDescription(text)

    @Test
    fun oneFormatIsAPlainButtonThatShowsProgressAndAnnouncesTheResult() {
        val ready = CompletableDeferred<Unit>()
        val asked = mutableListOf<String>()
        rule.mainClock.autoAdvance = false
        rule.setMonarchContent {
            MonarchDownloadReportButton(
                onDownload = {
                    asked += it.id
                    ready.await()
                    MonarchDownloadReportResult.Downloaded
                },
                progress = 1.5f,
            )
        }
        rule.mainClock.advanceTimeByFrame()
        val button = rule.onNodeWithText("Download CSV").assertTouchTargetAtLeast48dp(rule.density)
        button.performClick()
        rule.mainClock.advanceTimeByFrame()
        assertEquals("Preparing CSV…", button.stateDescription())
        rule.onNode(announced("Preparing CSV…")).assertExists()
        rule.onNodeWithContentDescription("Download progress").assertExists()
        button.performClick()
        assertEquals(listOf("csv"), asked)
        ready.complete(Unit)
        rule.mainClock.advanceTimeByFrame()
        rule.waitForIdle()
        rule.onNode(announced("CSV downloaded")).assertExists()
        rule.onNodeWithContentDescription("Download progress").assertDoesNotExist()
    }

    @Test
    fun severalFormatsOpenAMenuWithTheirDescriptions() {
        val asked = mutableListOf<String>()
        rule.setMonarchContent {
            MonarchDownloadReportButton(
                onDownload = {
                    asked += it.id
                    MonarchDownloadReportResult.Downloaded
                },
                formats = Formats,
            )
        }
        rule.onNodeWithText("Download").performClick()
        rule.onNodeWithText("For printing", useUnmergedTree = true).assertExists()
        rule.onNodeWithText("PDF", useUnmergedTree = true).performClick()
        rule.waitForIdle()
        assertEquals(listOf("pdf"), asked)
        rule.onNode(announced("PDF downloaded")).assertExists()
    }

    @Test
    fun aQueuedFileLeavesANoteThatIsAnnounced() {
        rule.setMonarchContent {
            MonarchDownloadReportButton(onDownload = { MonarchDownloadReportResult.Queued() })
        }
        rule.onNodeWithText("Download CSV").performClick()
        rule.waitForIdle()
        val note = "The file is being prepared. You can leave this screen; it will be ready to download shortly."
        rule.onNodeWithText(note).assertExists()
        rule.onNode(announced(note)).assertExists()
    }

    @Test
    fun aFailureOffersTryAgainWithTheSameFormat() {
        var attempts = 0
        rule.setMonarchContent {
            MonarchDownloadReportButton(
                onDownload = {
                    attempts++
                    if (attempts == 1) error("offline")
                    MonarchDownloadReportResult.Downloaded
                },
            )
        }
        rule.onNodeWithText("Download CSV").performClick()
        rule.waitForIdle()
        rule.onNode(polite and hasText("The CSV download failed.")).assertExists()
        rule.onNodeWithText("Try again").performClick()
        rule.waitForIdle()
        assertEquals(2, attempts)
        rule.onNodeWithText("The CSV download failed.").assertDoesNotExist()
    }

    @Test
    fun theErrorMessageAndTheLabelsCanBeReplaced() {
        rule.setMonarchContent {
            Column {
                MonarchDownloadReportButton(
                    onDownload = { error("quota") },
                    errorMessage = { error, format -> "${format.label}: ${error.message}" },
                    labels = MonarchDownloadReportButtonLabels(single = { "Export $it" }),
                )
                MonarchDownloadReportButton(
                    onDownload = { MonarchDownloadReportResult.Downloaded },
                    formats = Formats,
                    labels = MonarchDownloadReportButtonLabels(label = "Export"),
                )
            }
        }
        rule.onNodeWithText("Export CSV").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("CSV: quota").assertExists()
        rule.onNodeWithText("Export").assertExists()
    }

    @Test
    fun aReadyFileIsSavedWhereThePersonChose() {
        val uri = Uri.parse("content://reports/invoices.csv")
        val saved = ByteArrayOutputStream()
        val dialog = SaveDialog(uri)
        rule.setMonarchContent {
            val context = LocalContext.current
            dialog.context = context
            shadowOf(context.contentResolver).registerOutputStream(uri, saved)
            ProvideSaveDialog(dialog) {
                MonarchDownloadReportButton(
                    onDownload = {
                        MonarchDownloadReportResult.File("invoices.csv") { it.write("id,total\n".toByteArray()) }
                    },
                )
            }
        }
        rule.onNodeWithText("Download CSV").performClick()
        rule.waitUntil(timeoutMillis = 5_000) { saved.size() > 0 }
        rule.waitForIdle()
        assertEquals("text/csv", dialog.mimeType)
        assertEquals("invoices.csv", dialog.fileName)
        assertEquals("id,total\n", saved.toString())
        rule.onNode(announced("CSV downloaded")).assertExists()
    }

    @Test
    fun aCancelledSaveDialogReturnsToIdleWithoutAMessage() {
        val dialog = SaveDialog(null)
        rule.setMonarchContent {
            dialog.context = LocalContext.current
            ProvideSaveDialog(dialog) {
                MonarchDownloadReportButton(
                    onDownload = { MonarchDownloadReportResult.File("invoices.csv") {} },
                )
            }
        }
        rule.onNodeWithText("Download CSV").performClick()
        rule.waitForIdle()
        assertNull(rule.onNodeWithText("Download CSV").stateDescription())
        rule.onNode(announced("CSV downloaded")).assertDoesNotExist()
        rule.onNodeWithText("The CSV download failed.").assertDoesNotExist()
    }

    @Test
    fun aDisabledButtonDoesNotStart() {
        var asked = 0
        rule.setMonarchContent {
            MonarchDownloadReportButton(
                onDownload = {
                    asked++
                    MonarchDownloadReportResult.Downloaded
                },
                enabled = false,
            )
        }
        rule.onNodeWithText("Download CSV").assertIsNotEnabled().performClick()
        assertEquals(0, asked)
    }

    @Test
    fun aLongLabelWrapsAtTheLargestFontSize() {
        rule.setMonarchContent(fontScale = LargestFontScale) {
            MonarchDownloadReportButton(
                onDownload = { MonarchDownloadReportResult.Downloaded },
                formats = listOf(MonarchDownloadReportFormat("Spreadsheet with every invoice line", "xlsx")),
            )
        }
        rule.onNodeWithText("Download Spreadsheet", substring = true, useUnmergedTree = true).assertTextFullyVisible()
    }

    @Test
    fun theRulesOfTheDownload() {
        val t = MonarchDownloadReportButtonTokens
        val csv = MonarchDownloadReportFormat.Csv
        val preparing = MonarchDownloadReportPhase.Preparing(csv)
        assertTrue(t.busy(preparing))
        assertFalse(t.busy(MonarchDownloadReportPhase.Idle))
        assertEquals(1f, t.shownProgress(preparing, 3f))
        assertEquals(0f, t.shownProgress(preparing, -1f))
        assertNull(t.shownProgress(preparing, Float.NaN))
        assertNull(t.shownProgress(MonarchDownloadReportPhase.Idle, 0.5f))
        assertEquals(MonarchDownloadReportPhase.Idle, t.after(csv, MonarchDownloadReportResult.Downloaded))
        assertIs<MonarchDownloadReportPhase.Saving>(t.after(csv, MonarchDownloadReportResult.File("a.csv") {}))
        assertEquals(csv, t.runningFormat(preparing))
    }

    @Test
    fun coloursComeFromTheRolesInEveryTheme() {
        AllThemes.forEach { theme ->
            val colors = MonarchColors(theme.colors)
            assertEquals(colors.textMuted, MonarchDownloadReportButtonTokens.noteColor(colors), theme.id)
        }
    }
}

@Composable
private fun ProvideSaveDialog(dialog: SaveDialog, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalActivityResultRegistryOwner provides dialog, content = content)
}

package dev.monarchui.sample.sections.datagrid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeDialog
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeField
import dev.monarchui.components.datagrid.mergedialog.MonarchMergeRecord
import dev.monarchui.components.feedback.consequencelist.MonarchConsequence
import dev.monarchui.components.feedback.consequencelist.MonarchConsequenceKind
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme
import kotlinx.coroutines.delay

/** The catalogue page of MergeDialog: a full-screen dialog whose first merge fails and whose second succeeds. */
object MonarchMergeDialogSection : CataloguePage {
    override val title = "MergeDialog"

    private const val MERGE_MILLIS = 1200L
    private const val INVOICES = 12

    private val records = listOf(
        MonarchMergeRecord(
            id = "a",
            title = "Acme Ltd",
            description = "Created 3 Mar 2025 · 12 invoices",
            values = mapOf("email" to "hello@acme.com", "phone" to "", "city" to "London"),
        ),
        MonarchMergeRecord(
            id = "b",
            title = "ACME Limited",
            description = "Created 9 Jun 2025 · 2 invoices",
            values = mapOf("email" to "info@acme.com", "phone" to "+44 20 7946 0000", "city" to "London"),
        ),
    )
    private val fields = listOf(
        MonarchMergeField("email", "Email"),
        MonarchMergeField("phone", "Phone"),
        MonarchMergeField("city", "City"),
    )

    @Composable
    override fun Content() {
        var open by rememberSaveable { mutableStateOf(false) }
        var merging by rememberSaveable { mutableStateOf(false) }
        var failed by rememberSaveable { mutableStateOf(false) }
        var attempts by rememberSaveable { mutableIntStateOf(0) }
        var result by rememberSaveable { mutableStateOf("") }
        LaunchedEffect(merging) {
            if (merging) {
                delay(MERGE_MILLIS)
                attempts++
                merging = false
                if (attempts == 1) {
                    failed = true
                } else {
                    open = false
                }
            }
        }
        CatalogueCaption("Full-screen dialog; the first merge fails")
        MonarchButton(text = "Merge duplicates", onClick = { open = true }, variant = MonarchButtonVariant.Primary)
        if (result.isNotEmpty()) MonarchText(text = result, color = MonarchTheme.colors.textMuted)
        if (open) {
            Dialog(
                onDismissRequest = { if (!merging) open = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Surface(modifier = Modifier.fillMaxSize(), color = MonarchTheme.colors.bgSurface) {
                    MonarchMergeDialog(
                        records = records,
                        fields = fields,
                        onMerge = { selection ->
                            result = "Kept ${selection.primaryId}; email from ${selection.fields["email"]}"
                            failed = false
                            merging = true
                        },
                        modifier = Modifier.safeDrawingPadding(),
                        consequences = listOf(
                            MonarchConsequence(
                                MonarchConsequenceKind.Changed,
                                "invoices move to the kept company",
                                count = INVOICES,
                            ),
                            MonarchConsequence(MonarchConsequenceKind.Removed, "The other company is deleted"),
                        ),
                        merging = merging,
                        failed = failed,
                        onCancel = { open = false },
                    )
                }
            }
        }
    }
}

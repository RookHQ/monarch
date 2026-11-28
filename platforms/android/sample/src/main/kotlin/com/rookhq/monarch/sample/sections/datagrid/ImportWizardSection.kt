package com.rookhq.monarch.sample.sections.datagrid

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datagrid.columnmapping.MonarchColumnMapping
import com.rookhq.monarch.components.datagrid.columnmapping.MonarchColumnMappingDefaults
import com.rookhq.monarch.components.datagrid.columnmapping.MonarchColumnMappingField
import com.rookhq.monarch.components.datagrid.columnmapping.MonarchColumnMappingSource
import com.rookhq.monarch.components.datagrid.previewrows.MonarchPreviewRow
import com.rookhq.monarch.components.datagrid.previewrows.MonarchPreviewRowIssue
import com.rookhq.monarch.components.datagrid.previewrows.MonarchPreviewRowStatus
import com.rookhq.monarch.components.datagrid.previewrows.MonarchPreviewRows
import com.rookhq.monarch.components.datagrid.previewrows.MonarchPreviewRowsColumn
import com.rookhq.monarch.components.feedback.banner.MonarchBanner
import com.rookhq.monarch.components.feedback.banner.MonarchBannerAnnouncement
import com.rookhq.monarch.components.files.fileupload.MonarchFileUpload
import com.rookhq.monarch.components.workflow.stepper.MonarchStepper
import com.rookhq.monarch.components.workflow.stepper.MonarchStepperStep
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The catalogue page of ImportWizard: import contacts from a chosen CSV file or from a sample file. */
object ImportWizardSection : CataloguePage {
    override val title = "ImportWizard"

    private const val HEIGHT_STEPS = 5
    private const val SAMPLES = 3
    private const val IMPORT_MILLIS = 600L
    private const val RESULTS = 3

    private val sample = """
        Full name,E-mail,Company
        Ada Moss,ada@example.com,Example Ltd
        Bea Lund,,Sample Co
        Cy Park,cy@example.com,
    """.trimIndent()

    private val fields = listOf(
        MonarchColumnMappingField(id = "name", label = "Name", required = true, aliases = listOf("Full name")),
        MonarchColumnMappingField(id = "email", label = "Email", required = true, aliases = listOf("E-mail", "Mail")),
        MonarchColumnMappingField(id = "company", label = "Company"),
    )

    private val steps = listOf(
        MonarchStepperStep(id = "upload", label = "Upload"),
        MonarchStepperStep(id = "map", label = "Map columns"),
        MonarchStepperStep(id = "preview", label = "Preview"),
        MonarchStepperStep(id = "results", label = "Results"),
    )

    /** Splits [text] into rows of cells. A real import uses a CSV reader that handles quoted fields. */
    private fun parse(text: String): List<List<String>> =
        text.lines().filter { it.isNotBlank() }.map { line -> line.split(',').map { it.trim() } }

    @Composable
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    override fun Content() {
        val resolver = LocalContext.current.contentResolver
        val scope = rememberCoroutineScope()
        var step by rememberSaveable { mutableIntStateOf(0) }
        var fileName by rememberSaveable { mutableStateOf("") }
        var table by remember { mutableStateOf(listOf<List<String>>()) }
        var mapping by remember { mutableStateOf(mapOf<String, String>()) }
        var showIssues by remember { mutableStateOf(false) }
        var filter by remember { mutableStateOf<MonarchPreviewRowStatus?>(null) }
        var importing by remember { mutableStateOf(false) }
        var imported by remember { mutableIntStateOf(0) }
        var readError by remember { mutableStateOf<String?>(null) }
        val headers = table.firstOrNull().orEmpty()
        val sources = headers.mapIndexed { column, header ->
            MonarchColumnMappingSource(header, table.drop(1).take(SAMPLES).map { it.getOrElse(column) { "" } })
        }
        val previewRows = table.drop(1).mapIndexed { index, cells ->
            val values = mapping.entries.associate { (column, field) ->
                field to cells.getOrNull(headers.indexOf(column))
            }
            val missing = fields.filter { it.required && values[it.id].isNullOrBlank() }
            val errors = missing.map { MonarchPreviewRowIssue("${it.label} is required", it.label) }
            MonarchPreviewRow(index + 1, values, errors = errors)
        }
        val ready = previewRows.filter { it.outcome != MonarchPreviewRowStatus.Error }
        fun load(name: String, rows: List<List<String>>) {
            if (rows.size < 2) {
                readError = "This file has no data rows under the header row."
                return
            }
            readError = null
            fileName = name
            table = rows
            val found = rows.first().map { MonarchColumnMappingSource(it) }
            mapping = MonarchColumnMappingDefaults.mapping(MonarchColumnMappingDefaults.suggest(found, fields))
            showIssues = false
            step = 1
        }
        LaunchedEffect(importing) {
            if (importing) {
                delay(IMPORT_MILLIS)
                imported = ready.size
                importing = false
                step = RESULTS
            }
        }
        CatalogueCaption("Import contacts")
        MonarchStepper(steps = steps, current = step)
        when (step) {
            0 -> {
                MonarchFileUpload(
                    onFiles = { accepted, _ ->
                        val file = accepted.firstOrNull()
                        if (file != null) {
                            scope.launch {
                                val text = withContext(Dispatchers.IO) {
                                    runCatching {
                                        resolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
                                    }.getOrNull()
                                }
                                load(file.name, parse(text.orEmpty()))
                            }
                        }
                    },
                    accept = listOf("text/csv", "text/comma-separated-values"),
                    maxFiles = 1,
                    hint = "A CSV file whose first row holds the column names.",
                    errorText = readError,
                )
                MonarchButton(text = "Use the sample file", onClick = { load("contacts.csv", parse(sample)) })
            }
            1 -> {
                MonarchColumnMapping(
                    sources = sources,
                    mapping = mapping,
                    fields = fields,
                    onMappingChange = { mapping = it },
                    modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
                    showIssues = showIssues,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                    MonarchButton(text = "Back", onClick = { step = 0 })
                    MonarchButton(
                        text = "Check rows",
                        onClick = {
                            showIssues = true
                            if (MonarchColumnMappingDefaults.issues(mapping, fields).isEmpty) step = 2
                        },
                        variant = MonarchButtonVariant.Primary,
                    )
                }
            }
            2 -> {
                MonarchPreviewRows(
                    rows = previewRows,
                    columns = fields.map { MonarchPreviewRowsColumn(it.id, it.label) },
                    label = "Preview of $fileName",
                    filter = filter,
                    onFilterChange = { filter = it },
                    modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * HEIGHT_STEPS),
                )
                if (importing) {
                    MonarchProgress(modifier = Modifier.fillMaxWidth(), label = "Importing ${ready.size} rows")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                    MonarchButton(text = "Back", onClick = { step = 1 }, enabled = !importing)
                    MonarchButton(
                        text = if (ready.size == 1) "Import 1 row" else "Import ${ready.size} rows",
                        onClick = { importing = true },
                        variant = MonarchButtonVariant.Primary,
                        enabled = ready.isNotEmpty() && !importing,
                    )
                }
            }
            else -> {
                MonarchBanner(
                    message = "$imported created, ${previewRows.size - imported} skipped.",
                    tone = MonarchTone.Success,
                    title = "Import finished",
                    announcement = MonarchBannerAnnouncement.Polite,
                )
                MonarchButton(
                    text = "Import another file",
                    onClick = {
                        table = emptyList()
                        mapping = emptyMap()
                        step = 0
                    },
                )
            }
        }
    }
}

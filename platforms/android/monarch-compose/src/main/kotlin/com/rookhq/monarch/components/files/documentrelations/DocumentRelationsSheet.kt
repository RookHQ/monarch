package com.rookhq.monarch.components.files.documentrelations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.R
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNotice
import com.rookhq.monarch.components.feedback.inlinenotice.MonarchInlineNoticeVariant
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeIcon
import com.rookhq.monarch.components.files.filetypeicon.MonarchFileTypeIconSize
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.internal.MonarchSearchListSheet
import com.rookhq.monarch.internal.monarchString
import com.rookhq.monarch.theme.MonarchTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** The add sheet: title, description, the type, the search and the matching documents. */
@Composable
@Suppress("LongParameterList", "LongMethod")
internal fun DocumentRelationsSheet(
    types: List<MonarchDocumentRelationType>,
    initialType: String,
    candidates: List<MonarchDocumentRelationTarget>,
    relations: List<MonarchDocumentRelation>,
    onSearch: ((String) -> Unit)?,
    onAdd: suspend (String, String) -> Unit,
    labels: MonarchDocumentRelationsLabels,
    texts: DocumentRelationsTexts,
    onAdded: (title: String) -> Unit,
    onClose: () -> Unit,
) {
    val tokens = MonarchDocumentRelationsTokens
    val colors = MonarchTheme.colors
    val scope = rememberCoroutineScope()
    var type by rememberSaveable { mutableStateOf(initialType) }
    var query by rememberSaveable { mutableStateOf("") }
    var pending by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val shown = tokens.candidates(candidates, relations, query, searchesOnServer = onSearch != null)
    val add: (MonarchDocumentRelationTarget) -> Unit = { target ->
        if (pending == null) {
            pending = target.id
            error = null
            scope.launch {
                val failure = runCatching { onAdd(target.id, type) }.exceptionOrNull()
                if (failure is CancellationException) throw failure
                pending = null
                if (failure == null) onAdded(target.title) else error = failure.message ?: texts.failed
            }
        }
    }
    MonarchSearchListSheet(
        title = monarchString(labels.sheetTitle, R.string.monarch_documentrelations_sheet_title),
        query = query,
        onQueryChange = {
            query = it
            onSearch?.invoke(it.trim())
        },
        onDismiss = { if (pending == null) onClose() },
        searchPlaceholder = monarchString(labels.search, R.string.monarch_documentrelations_search),
        searching = false,
        containerColor = tokens.sheetColor(colors),
        gap = tokens.Gap,
        status = {
            Text(
                text = monarchString(labels.sheetDescription, R.string.monarch_documentrelations_sheet_description),
                style = MonarchTheme.typography.body,
                color = tokens.secondaryColor(true, colors),
            )
            MonarchSelect(
                options = types.map { MonarchSelectOption(it.value, it.label) },
                selected = type,
                onSelect = { type = it },
                modifier = Modifier.fillMaxWidth(),
                label = monarchString(labels.type, R.string.monarch_documentrelations_type),
                enabled = pending == null,
            )
        },
        buttons = {
            error?.let {
                MonarchInlineNotice(message = it, tone = MonarchTone.Danger, variant = MonarchInlineNoticeVariant.Plain)
            }
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_documentrelations_cancel),
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(bottom = tokens.SheetPadding),
                variant = MonarchButtonVariant.Ghost,
                enabled = pending == null,
            )
        },
    ) {
        item(key = "results-heading") { DocumentRelationsResultsHeading(labels, empty = shown.isEmpty()) }
        items(shown, key = { it.id }) { target ->
            DocumentRelationsResult(
                target = target,
                pending = pending == target.id,
                enabled = pending == null,
                working = texts.working,
                onClick = { add(target) },
            )
        }
    }
}

/** The "Results" heading over the matches, with the polite "No results" line when nothing matches. */
@Composable
private fun DocumentRelationsResultsHeading(labels: MonarchDocumentRelationsLabels, empty: Boolean) {
    val tokens = MonarchDocumentRelationsTokens
    val colors = MonarchTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(tokens.Gap)) {
        Text(
            text = monarchString(labels.results, R.string.monarch_documentrelations_results),
            modifier = Modifier.semantics { heading() },
            style = MonarchTheme.typography.caption,
            color = tokens.secondaryColor(true, colors),
        )
        if (empty) {
            Text(
                text = monarchString(labels.noResults, R.string.monarch_documentrelations_no_results),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MonarchTheme.typography.body,
                color = tokens.secondaryColor(true, colors),
            )
        }
    }
}

/** One document in the sheet: a button read as its title and description. */
@Composable
private fun DocumentRelationsResult(
    target: MonarchDocumentRelationTarget,
    pending: Boolean,
    enabled: Boolean,
    working: String,
    onClick: () -> Unit,
) {
    val tokens = MonarchDocumentRelationsTokens
    val colors = MonarchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.RowMinHeight)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = tokens.RowVerticalPadding),
        horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        target.fileName?.let {
            MonarchFileTypeIcon(fileName = it, size = MonarchFileTypeIconSize.Sm, decorative = true)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(tokens.TextGap)) {
            Text(
                target.title,
                style = MonarchTheme.typography.body,
                color = tokens.titleColor(openable = false, enabled = enabled || pending, colors),
            )
            target.description?.let {
                Text(
                    it,
                    style = MonarchTheme.typography.caption,
                    color = tokens.secondaryColor(enabled || pending, colors),
                )
            }
        }
        if (pending) {
            MonarchProgress(
                modifier = Modifier.size(tokens.ProgressSize),
                variant = MonarchProgressVariant.Circular,
                label = working,
            )
        }
    }
}

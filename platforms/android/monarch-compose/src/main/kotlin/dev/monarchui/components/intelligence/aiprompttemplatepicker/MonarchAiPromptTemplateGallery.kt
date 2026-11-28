package dev.monarchui.components.intelligence.aiprompttemplatepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import dev.monarchui.R
import dev.monarchui.components.actions.button.MonarchButton
import dev.monarchui.components.actions.button.MonarchButtonVariant
import dev.monarchui.components.datadisplay.card.MonarchCard
import dev.monarchui.components.datadisplay.tag.MonarchTag
import dev.monarchui.components.feedback.emptystate.MonarchEmptyState
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateKind
import dev.monarchui.components.feedback.emptystate.MonarchEmptyStateSize
import dev.monarchui.components.search.searchbar.MonarchSearchBar
import dev.monarchui.internal.monarchString
import dev.monarchui.theme.MonarchTheme

/**
 * The gallery page of `MonarchAiPromptTemplatePicker`: the heading, the
 * search field, the category filters and a card per matching template, or an
 * empty state.
 */
private const val GridColumns = 2

@Suppress("LongParameterList")
@Composable
internal fun MonarchAiPromptTemplateGallery(
    templates: List<MonarchAiPromptTemplate>,
    categories: List<MonarchAiPromptTemplateCategory>,
    query: String,
    onQueryChange: (String) -> Unit,
    category: String?,
    onCategoryChange: (String?) -> Unit,
    labels: MonarchAiPromptTemplatePickerLabels,
    chosenId: String?,
    onChoose: (MonarchAiPromptTemplate) -> Unit,
    onContinue: () -> Unit,
    onCancel: (() -> Unit)?,
) {
    val tokens = MonarchAiPromptTemplatePickerTokens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = monarchString(labels.title, R.string.monarch_aiprompttemplatepicker_title),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = MonarchTheme.typography.heading,
            color = MonarchTheme.colors.textDefault,
        )
        if (onCancel != null) {
            MonarchButton(
                text = monarchString(labels.cancel, R.string.monarch_aiprompttemplatepicker_cancel),
                onClick = onCancel,
                variant = MonarchButtonVariant.Ghost,
            )
        }
    }
    if (templates.isEmpty()) {
        MonarchEmptyState(
            title = monarchString(labels.empty, R.string.monarch_aiprompttemplatepicker_empty),
            size = MonarchEmptyStateSize.Sm,
        )
        return
    }
    MonarchSearchBar(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = {},
        modifier = Modifier.fillMaxWidth(),
        placeholder = monarchString(labels.search, R.string.monarch_aiprompttemplatepicker_search),
    )
    if (categories.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(tokens.Gap),
            verticalArrangement = Arrangement.spacedBy(tokens.Gap),
        ) {
            MonarchTag(
                text = monarchString(labels.allCategories, R.string.monarch_aiprompttemplatepicker_all_categories),
                selected = category == null,
                onSelectedChange = { onCategoryChange(null) },
            )
            categories.forEach { item ->
                MonarchTag(
                    text = item.label,
                    selected = category == item.id,
                    onSelectedChange = { onCategoryChange(if (it) item.id else null) },
                )
            }
        }
    }
    val shown = tokens.filtered(templates, query, category)
    if (shown.isEmpty()) {
        MonarchEmptyState(
            title = labels.noResults?.invoke(query.trim())
                ?: stringResource(R.string.monarch_aiprompttemplatepicker_no_results, query.trim()),
            kind = MonarchEmptyStateKind.Filtered,
            size = MonarchEmptyStateSize.Sm,
            onClearFilters = {
                onQueryChange("")
                onCategoryChange(null)
            },
        )
    }
    // A grid of cards, two across on a phone (the web shows three on a wide window).
    shown.chunked(GridColumns).forEach { rowTemplates ->
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.Gap), modifier = Modifier.height(IntrinsicSize.Max)) {
            for (column in 0 until GridColumns) {
                val template = rowTemplates.getOrNull(column)
                if (template != null) {
                    TemplateCard(template, template.id == chosenId, onChoose, Modifier.weight(1f).fillMaxHeight())
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    // As the web: the hint and the button sit under the gallery, off until a template is chosen.
    Row(horizontalArrangement = Arrangement.spacedBy(tokens.Gap), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (chosenId == null) stringResource(R.string.monarch_aiprompttemplatepicker_choose) else "",
            modifier = Modifier.weight(1f),
            style = MonarchTheme.typography.caption,
            color = MonarchTheme.colors.textMuted,
        )
        MonarchButton(
            text = stringResource(R.string.monarch_aiprompttemplatepicker_continue),
            onClick = onContinue,
            variant = MonarchButtonVariant.Primary,
            enabled = chosenId != null,
        )
    }
}

@Composable
private fun TemplateCard(
    template: MonarchAiPromptTemplate,
    chosen: Boolean,
    onChoose: (MonarchAiPromptTemplate) -> Unit,
    modifier: Modifier,
) {
    MonarchCard(onClick = { onChoose(template) }, modifier = modifier, selected = chosen) {
        Column(verticalArrangement = Arrangement.spacedBy(MonarchAiPromptTemplatePickerTokens.RowGap)) {
            Text(
                text = template.title,
                style = MonarchTheme.typography.bodyStrong,
                color = MonarchTheme.colors.textDefault,
            )
            val description = template.description
            if (!description.isNullOrBlank()) {
                Text(text = description, style = MonarchTheme.typography.caption, color = MonarchTheme.colors.textMuted)
            }
        }
    }
}

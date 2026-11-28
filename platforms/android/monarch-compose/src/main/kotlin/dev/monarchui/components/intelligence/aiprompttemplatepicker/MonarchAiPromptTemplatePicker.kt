package dev.monarchui.components.intelligence.aiprompttemplatepicker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * Picks a saved prompt and fills in its blanks: a searchable list of
 * templates, then a page with one field per `{{blank}}` and a live preview;
 * Use template hands the filled text to the app.
 *
 * The content of a sheet: present it in a `MonarchBottomSheet`.
 * Built by Monarch from `MonarchSearchBar`, selectable `MonarchTag`s for the
 * categories, a `MonarchCard` per template, `MonarchEmptyState`,
 * `MonarchTextField`s and `MonarchButton`s, in a scrolling `Column`. The
 * gallery and the template page are two pages of the same content; Back
 * returns to the gallery with the search kept.
 *
 * When Use template finds an empty required field, every such field shows
 * "Enter …" and the first error is announced; otherwise [onUse] receives the
 * filled prompt. Put it in the prompt field; do not send it without the
 * person seeing it.
 *
 * @param templates the templates, each with a title, a body with `{{blanks}}`, and optional description,
 * category, keywords and variables.
 * @param onUse receives the filled prompt, the template and the trimmed values of its blanks.
 * @param modifier the modifier of the picker.
 * @param categories adds category filters above the list.
 * @param initialTemplateId opens this template's page at first.
 * @param labels replacements for every built-in string.
 * @param onCancel adds a Cancel button to the gallery, for example to hide the sheet.
 */
@Composable
public fun MonarchAiPromptTemplatePicker(
    templates: List<MonarchAiPromptTemplate>,
    onUse: (text: String, template: MonarchAiPromptTemplate, values: Map<String, String>) -> Unit,
    modifier: Modifier = Modifier,
    categories: List<MonarchAiPromptTemplateCategory> = emptyList(),
    initialTemplateId: String? = null,
    labels: MonarchAiPromptTemplatePickerLabels = MonarchAiPromptTemplatePickerLabels(),
    onCancel: (() -> Unit)? = null,
) {
    var selectedId by rememberSaveable { mutableStateOf(initialTemplateId) }
    var chosenId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = templates.firstOrNull { it.id == selectedId }
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(MonarchAiPromptTemplatePickerTokens.Padding),
        verticalArrangement = Arrangement.spacedBy(MonarchAiPromptTemplatePickerTokens.SectionGap),
    ) {
        if (selected == null) {
            MonarchAiPromptTemplateGallery(
                templates = templates,
                categories = categories,
                query = query,
                onQueryChange = { query = it },
                category = category,
                onCategoryChange = { category = it },
                labels = labels,
                chosenId = chosenId,
                onChoose = { chosenId = it.id },
                onContinue = { selectedId = chosenId },
                onCancel = onCancel,
            )
        } else {
            MonarchAiPromptTemplateForm(
                template = selected,
                labels = labels,
                onBack = { selectedId = null },
                onUse = onUse,
            )
        }
    }
}

@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.actions.button.MonarchButtonSize
import com.rookhq.monarch.components.actions.button.MonarchButtonVariant
import com.rookhq.monarch.components.feedback.emptystate.MonarchEmptyState
import com.rookhq.monarch.components.forms.select.MonarchSelect
import com.rookhq.monarch.components.forms.select.MonarchSelectOption
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.forms.textfield.MonarchTextField
import com.rookhq.monarch.components.interaction.reorderablelist.MonarchReorderableList
import com.rookhq.monarch.components.overlays.bottomsheet.MonarchBottomSheet
import com.rookhq.monarch.components.overlays.menu.MonarchMenu
import com.rookhq.monarch.components.overlays.menu.MonarchMenuItem
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFieldRenderer
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldDefinition
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldType
import com.rookhq.monarch.components.workflow.fieldrenderer.MonarchFormFieldValue
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

private val BuilderTypes = listOf(
    MonarchSelectOption(MonarchFormFieldType.ShortText, "Short text"),
    MonarchSelectOption(MonarchFormFieldType.LongText, "Long text"),
    MonarchSelectOption(MonarchFormFieldType.Email, "Email"),
    MonarchSelectOption(MonarchFormFieldType.Date, "Date"),
    MonarchSelectOption(MonarchFormFieldType.YesNo, "Yes / no"),
)
private const val BuilderListSteps = 3

private fun MonarchFormFieldDefinition.builderEdited(
    label: String = this.label,
    type: MonarchFormFieldType = this.type,
    required: Boolean = this.required,
) = MonarchFormFieldDefinition(id, type, label, required = required, options = options)

/** The catalogue page of FormBuilder: a recipe of tabs, a menu, a reorderable list, a sheet and the field renderer. */
object FormBuilderSection : CataloguePage {
    override val title = "FormBuilder"

    @Composable
    override fun Content() {
        val fields = remember {
            mutableStateListOf(
                MonarchFormFieldDefinition("name", MonarchFormFieldType.ShortText, "Full name", required = true),
                MonarchFormFieldDefinition("email", MonarchFormFieldType.Email, "Work email"),
            )
        }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var editing by remember { mutableStateOf<String?>(null) }
        val answers = remember { mutableStateMapOf<String, MonarchFormFieldValue?>() }
        CatalogueCaption("Build the form, reorder it, then preview it")
        Column(verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3)) {
            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Build") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Preview") })
            }
            if (tab == 0) {
                BuilderFieldList(fields) { editing = it }
            } else {
                fields.filter { it.isVisible(answers) }.forEach { field ->
                    MonarchFieldRenderer(
                        field = field,
                        value = answers[field.id],
                        onValueChange = { answers[field.id] = it },
                    )
                }
            }
        }
        val index = fields.indexOfFirst { it.id == editing }
        if (index >= 0) BuilderSettingsSheet(fields, index) { editing = null }
    }
}

@Composable
private fun BuilderFieldList(fields: SnapshotStateList<MonarchFormFieldDefinition>, onEdit: (String) -> Unit) {
    var paletteOpen by remember { mutableStateOf(false) }
    var added by rememberSaveable { mutableIntStateOf(0) }
    Box {
        MonarchButton(text = "Add a field", onClick = { paletteOpen = true }, variant = MonarchButtonVariant.Primary)
        MonarchMenu(expanded = paletteOpen, onDismissRequest = { paletteOpen = false }) {
            BuilderTypes.forEach { option ->
                MonarchMenuItem(
                    text = option.label,
                    onClick = {
                        added++
                        fields += MonarchFormFieldDefinition("field$added", option.value, option.label)
                        paletteOpen = false
                    },
                )
            }
        }
    }
    if (fields.isEmpty()) {
        MonarchEmptyState(title = "No fields yet", message = "Add a field to start the form.")
        return
    }
    MonarchReorderableList(
        items = fields,
        key = { it.id },
        itemLabel = { it.label },
        onReorder = { from, to -> fields.add(to, fields.removeAt(from)) },
        modifier = Modifier.heightIn(max = MonarchTheme.spacing.space24 * BuilderListSteps),
    ) { field, _ ->
        Column(modifier = Modifier.weight(1f)) {
            MonarchText(text = field.label)
            MonarchText(
                text = BuilderTypes.firstOrNull { it.value == field.type }?.label.orEmpty() +
                    if (field.required) ", required" else "",
                style = MonarchTextStyle.Caption,
                color = MonarchTheme.colors.textMuted,
            )
        }
        MonarchButton(
            text = "Edit",
            onClick = { onEdit(field.id) },
            modifier = Modifier.semantics { contentDescription = "Edit ${field.label}" },
            variant = MonarchButtonVariant.Ghost,
            size = MonarchButtonSize.Sm,
        )
    }
}

@Composable
private fun BuilderSettingsSheet(
    fields: SnapshotStateList<MonarchFormFieldDefinition>,
    index: Int,
    onClose: () -> Unit,
) {
    val field = fields[index]
    MonarchBottomSheet(onDismissRequest = onClose, title = "Field settings") {
        Column(
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
        ) {
            MonarchTextField(
                value = field.label,
                onValueChange = { fields[index] = field.builderEdited(label = it) },
                label = "Question",
                errorText = if (field.label.isBlank()) "Write the question." else null,
            )
            MonarchSelect(
                options = BuilderTypes,
                selected = field.type,
                onSelect = { fields[index] = field.builderEdited(type = it) },
                label = "Type",
            )
            MonarchSwitchRow(
                checked = field.required,
                onCheckedChange = { fields[index] = field.builderEdited(required = it) },
                label = "Required",
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2)) {
                MonarchButton(
                    text = "Remove field",
                    onClick = {
                        fields.removeAt(index)
                        onClose()
                    },
                    variant = MonarchButtonVariant.Danger,
                )
                MonarchButton(
                    text = "Done",
                    onClick = { this@MonarchBottomSheet.dismiss() },
                    variant = MonarchButtonVariant.Primary,
                )
            }
        }
    }
}

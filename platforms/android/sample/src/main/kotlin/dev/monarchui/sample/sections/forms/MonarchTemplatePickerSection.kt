package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePicker
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerBlank
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerCategory
import dev.monarchui.components.forms.templatepicker.MonarchTemplatePickerItem
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val GalleryHeightSteps = 5

private val Templates = listOf(
    MonarchTemplatePickerItem("standup", "Daily stand-up", "Yesterday, today and blockers.", "meetings"),
    MonarchTemplatePickerItem("retro", "Retrospective", "What went well and what to change.", "meetings", "New"),
    MonarchTemplatePickerItem("one-on-one", "One-on-one", "Notes and actions for a regular check-in.", "meetings"),
    MonarchTemplatePickerItem("roadmap", "Roadmap", "Themes by quarter.", "planning", keywords = listOf("okr")),
    MonarchTemplatePickerItem("brief", "Project brief", "Goals, scope and owners.", "planning", badge = "Built-in"),
    MonarchTemplatePickerItem("budget", "Budget", "Spending by month.", "planning", enabled = false),
)

/** The catalogue page of TemplatePicker. */
object MonarchTemplatePickerSection : CataloguePage {
    override val title = "TemplatePicker"

    @Composable
    override fun Content() {
        var selected by rememberSaveable { mutableStateOf<String?>(null) }
        var query by rememberSaveable { mutableStateOf("") }
        CatalogueCaption("Filters, search and a blank card. Budget is disabled.")
        MonarchText(
            text = "Chosen: ${selected ?: "nothing yet"}",
            style = MonarchTextStyle.Caption,
            color = MonarchTheme.colors.textMuted,
        )
        MonarchTemplatePicker(
            label = "Templates",
            templates = Templates,
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.fillMaxWidth().height(MonarchTheme.spacing.space24 * GalleryHeightSteps),
            categories = listOf(
                MonarchTemplatePickerCategory("meetings", "Meetings"),
                MonarchTemplatePickerCategory("planning", "Planning"),
            ),
            query = query,
            onQueryChange = { query = it },
            blank = MonarchTemplatePickerBlank(description = "Start with an empty page."),
        )
    }
}

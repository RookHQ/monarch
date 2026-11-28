package dev.monarchui.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.components.workflow.statuspicker.MonarchStatusCategory
import dev.monarchui.components.workflow.statuspicker.MonarchStatusGlyph
import dev.monarchui.components.workflow.statuspicker.MonarchStatusOption
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPicker
import dev.monarchui.components.workflow.statuspicker.MonarchStatusPickerSize
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private val SampleStatuses = listOf(
    MonarchStatusOption("triage", "Triage", MonarchStatusCategory.Backlog),
    MonarchStatusOption("todo", "Todo", MonarchStatusCategory.Unstarted),
    MonarchStatusOption("doing", "In progress", MonarchStatusCategory.Started),
    MonarchStatusOption("review", "In review", MonarchStatusCategory.Started),
    MonarchStatusOption("done", "Done", MonarchStatusCategory.Completed),
    MonarchStatusOption("dropped", "Won't do", MonarchStatusCategory.Cancelled),
)

/** The catalogue page of StatusPicker. */
object MonarchStatusPickerSection : CataloguePage {
    override val title = "StatusPicker"

    @Composable
    override fun Content() {
        var status by rememberSaveable { mutableStateOf<String?>("doing") }
        CatalogueCaption("With the name")
        MonarchStatusPicker(statuses = SampleStatuses, value = status, onValueChange = { status = it })
        MonarchStatusPicker(
            statuses = SampleStatuses,
            value = status,
            onValueChange = { status = it },
            size = MonarchStatusPickerSize.Sm,
        )
        CatalogueCaption("Glyph only")
        MonarchStatusPicker(
            statuses = SampleStatuses,
            value = status,
            onValueChange = { status = it },
            showLabel = false,
        )
        CatalogueCaption("No status, disabled")
        MonarchStatusPicker(statuses = SampleStatuses, value = null, onValueChange = {}, enabled = false)
        CatalogueCaption("Glyphs in a list")
        SampleStatuses.forEach { option ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchStatusGlyph(option.category)
                MonarchText(text = option.name, style = MonarchTextStyle.Caption)
            }
        }
    }
}

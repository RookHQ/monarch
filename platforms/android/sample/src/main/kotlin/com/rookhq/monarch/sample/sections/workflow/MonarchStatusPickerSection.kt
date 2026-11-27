package com.rookhq.monarch.sample.sections.workflow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusCategory
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusGlyph
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusOption
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusPicker
import com.rookhq.monarch.components.workflow.statuspicker.MonarchStatusPickerSize
import com.rookhq.monarch.foundation.MonarchTextStyle
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

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

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
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityGlyph
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityLevel
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPicker
import dev.monarchui.components.workflow.prioritypicker.MonarchPriorityPickerSize
import dev.monarchui.foundation.MonarchTextStyle
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of PriorityPicker. */
object MonarchPriorityPickerSection : CataloguePage {
    override val title = "PriorityPicker"

    @Composable
    override fun Content() {
        var level by rememberSaveable { mutableStateOf<MonarchPriorityLevel?>(MonarchPriorityLevel.High) }
        CatalogueCaption("With the name")
        MonarchPriorityPicker(value = level, onValueChange = { level = it })
        MonarchPriorityPicker(value = level, onValueChange = { level = it }, size = MonarchPriorityPickerSize.Sm)
        CatalogueCaption("Shape only")
        MonarchPriorityPicker(value = level, onValueChange = { level = it }, showLabel = false)
        CatalogueCaption("Disabled")
        MonarchPriorityPicker(value = level, onValueChange = {}, enabled = false)
        CatalogueCaption("Glyphs in a list")
        MonarchPriorityLevel.entries.forEach { option ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonarchPriorityGlyph(option)
                MonarchText(text = option.name, style = MonarchTextStyle.Caption)
            }
        }
    }
}

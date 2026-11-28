package dev.monarchui.sample.sections.forms

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.forms.colorpicker.MonarchColorPicker
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of ColorPicker. */
object MonarchColorPickerSection : CataloguePage {
    override val title = "ColorPicker"

    @Composable
    override fun Content() {
        var color by rememberSaveable { mutableStateOf<String?>("#2563eb") }
        CatalogueCaption("Swatches and hex field")
        MonarchColorPicker(
            value = color,
            onValueChange = { color = it },
            label = "Label color",
            helperText = "Selected: ${color ?: "none"}",
        )
        CatalogueCaption("Disabled")
        MonarchColorPicker(value = "#16a34a", onValueChange = {}, enabled = false, allowCustom = false)
    }
}

package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Checkbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.forms.checkboxrow.MonarchCheckboxRow
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Checkbox. */
object MonarchCheckboxRowSection : CataloguePage {
    override val title = "Checkbox"

    @Composable
    override fun Content() {
        var updates by rememberSaveable { mutableStateOf(true) }
        MonarchCheckboxRow(
            checked = updates,
            onCheckedChange = { updates = it },
            label = "Send me product updates",
            description = "At most once a week.",
        )
        MonarchCheckboxRow(checked = true, onCheckedChange = {}, label = "Required by policy", enabled = false)
        CatalogueCaption("Material 3 Checkbox, without a visible label")
        Checkbox(
            checked = updates,
            onCheckedChange = { updates = it },
            modifier = Modifier.semantics { contentDescription = "Product updates" },
        )
    }
}

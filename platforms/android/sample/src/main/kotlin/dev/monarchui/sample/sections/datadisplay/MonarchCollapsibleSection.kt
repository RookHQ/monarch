package dev.monarchui.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.datadisplay.collapsible.MonarchCollapsible
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.foundation.icon.MonarchIcon
import dev.monarchui.foundation.icon.MonarchIconSize
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.sampleicons.SampleIcons

/** The catalogue page of Collapsible. */
object MonarchCollapsibleSection : CataloguePage {
    override val title = "Collapsible"

    @Composable
    override fun Content() {
        var advanced by rememberSaveable { mutableStateOf(false) }
        var details by rememberSaveable { mutableStateOf(true) }
        var beta by rememberSaveable { mutableStateOf(false) }
        CatalogueCaption("Text only")
        MonarchCollapsible(expanded = advanced, onExpandedChange = { advanced = it }, label = "Advanced settings") {
            MonarchSwitchRow(checked = beta, onCheckedChange = { beta = it }, label = "Try features in beta")
        }
        CatalogueCaption("With an icon that turns")
        MonarchCollapsible(
            expanded = details,
            onExpandedChange = { details = it },
            label = "Details",
            trailingIcon = {
                MonarchIcon(imageVector = SampleIcons.ChevronDown, contentDescription = null, size = MonarchIconSize.Sm)
            },
        ) {
            MonarchText(text = "The invoice was sent on 3 March and paid on 10 March.")
        }
        CatalogueCaption("Disabled")
        MonarchCollapsible(expanded = false, onExpandedChange = {}, label = "Managed by your admin", enabled = false) {
            MonarchText(text = "Hidden")
        }
    }
}

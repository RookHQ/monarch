package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.collapsible.MonarchCollapsible
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons

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

package dev.monarchui.sample.sections.forms

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.monarchui.components.forms.switchrow.MonarchSwitchRow
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of Switch. */
object MonarchSwitchRowSection : CataloguePage {
    override val title = "Switch"

    @Composable
    override fun Content() {
        var notifications by rememberSaveable { mutableStateOf(false) }
        MonarchSwitchRow(
            checked = notifications,
            onCheckedChange = { notifications = it },
            label = "Notifications",
            description = "Takes effect immediately.",
        )
        MonarchSwitchRow(checked = true, onCheckedChange = {}, label = "Managed by your admin", enabled = false)
        CatalogueCaption("Material 3 Switch, without a visible label")
        Switch(
            checked = notifications,
            onCheckedChange = { notifications = it },
            modifier = Modifier.semantics { contentDescription = "Notifications" },
        )
    }
}

package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.statusdot.MonarchStatusDot
import com.rookhq.monarch.components.datadisplay.statusdot.MonarchStatusDotSize
import com.rookhq.monarch.components.forms.switchrow.MonarchSwitchRow
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of StatusDot. */
object MonarchStatusDotSection : CataloguePage {
    override val title = "StatusDot"

    @Composable
    override fun Content() {
        var pulsing by rememberSaveable { mutableStateOf(true) }
        MonarchStatusDotSize.entries.forEach { size ->
            CatalogueCaption(size.name)
            MonarchTone.entries.forEach { tone -> MonarchStatusDot(label = tone.name, tone = tone, size = size) }
        }
        CatalogueCaption("Pulsing")
        MonarchSwitchRow(checked = pulsing, onCheckedChange = { pulsing = it }, label = "Pulse")
        MonarchStatusDot(label = "Recording", tone = MonarchTone.Danger, pulsing = pulsing)
        CatalogueCaption("Label hidden, read by TalkBack")
        MonarchStatusDot(label = "Online", tone = MonarchTone.Success, labelHidden = true)
    }
}

package com.rookhq.monarch.sample.sections.datadisplay

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.provenance.MonarchProvenance
import com.rookhq.monarch.components.datadisplay.provenance.MonarchProvenanceVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Provenance. */
object MonarchProvenanceSection : CataloguePage {
    override val title = "Provenance"

    @Composable
    override fun Content() {
        val checkedAt = remember { System.currentTimeMillis() - 2 * DateUtils.HOUR_IN_MILLIS }
        var opened by rememberSaveable { mutableIntStateOf(0) }
        CatalogueCaption("Chip")
        MonarchProvenance(source = "Registry", checkedAt = checkedAt)
        CatalogueCaption("Inline")
        MonarchProvenance(source = "Imported file", variant = MonarchProvenanceVariant.Inline, checkedAt = checkedAt)
        CatalogueCaption("Opens the source record")
        MonarchProvenance(source = "Companies House", checkedAt = checkedAt, onClick = { opened++ })
        MonarchText(text = "Opened $opened times")
    }
}

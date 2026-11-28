package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgress
import com.rookhq.monarch.components.datadisplay.progress.MonarchProgressVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of Progress. */
object MonarchProgressSection : CataloguePage {
    override val title = "Progress"

    @Composable
    override fun Content() {
        CatalogueCaption("Linear: determinate and indeterminate")
        MonarchProgress(modifier = Modifier.fillMaxWidth(), progress = 0.6f, label = "Uploading report")
        MonarchProgress(modifier = Modifier.fillMaxWidth(), label = "Loading invoices")
        CatalogueCaption("Circular, determinate")
        MonarchProgress(progress = 0.6f, variant = MonarchProgressVariant.Circular, label = "Syncing")
    }
}

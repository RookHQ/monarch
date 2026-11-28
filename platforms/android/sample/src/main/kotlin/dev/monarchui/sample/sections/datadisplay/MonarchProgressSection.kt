package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.monarchui.components.datadisplay.progress.MonarchProgress
import dev.monarchui.components.datadisplay.progress.MonarchProgressVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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

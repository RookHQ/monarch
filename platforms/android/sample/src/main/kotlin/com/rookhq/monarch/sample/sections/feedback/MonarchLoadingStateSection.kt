package com.rookhq.monarch.sample.sections.feedback

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingState
import com.rookhq.monarch.components.feedback.loadingstate.MonarchLoadingStateVariant
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

/** The catalogue page of LoadingState. */
object MonarchLoadingStateSection : CataloguePage {
    override val title = "LoadingState"

    private const val ROWS = 3

    @Composable
    override fun Content() {
        MonarchLoadingStateVariant.entries.forEach { variant ->
            CatalogueCaption(variant.name)
            MonarchLoadingState(label = "Loading tickets", variant = variant, rows = ROWS)
        }
    }
}

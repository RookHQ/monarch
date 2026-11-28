package dev.monarchui.sample.sections.feedback

import androidx.compose.runtime.Composable
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingState
import dev.monarchui.components.feedback.loadingstate.MonarchLoadingStateVariant
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

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

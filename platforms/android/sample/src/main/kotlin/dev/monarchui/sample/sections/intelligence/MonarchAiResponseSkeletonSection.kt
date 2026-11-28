package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import dev.monarchui.components.intelligence.airesponseskeleton.MonarchAiResponseSkeleton
import dev.monarchui.components.intelligence.airesponseskeleton.MonarchAiResponseSkeletonShape
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of AiResponseSkeleton. */
object MonarchAiResponseSkeletonSection : CataloguePage {
    override val title = "AiResponseSkeleton"

    @Composable
    override fun Content() {
        CatalogueCaption("Text")
        MonarchAiResponseSkeleton()
        CatalogueCaption("Cards")
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Cards, lines = 2)
        CatalogueCaption("Table")
        MonarchAiResponseSkeleton(shape = MonarchAiResponseSkeletonShape.Table, lines = 3, columns = 4)
    }
}

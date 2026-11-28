package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.intelligence.airesponseskeleton.MonarchAiResponseSkeleton
import com.rookhq.monarch.components.intelligence.airesponseskeleton.MonarchAiResponseSkeletonShape
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage

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

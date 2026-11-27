package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadge
import com.rookhq.monarch.components.datadisplay.badge.MonarchBadgeVariant
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Badge. */
object MonarchBadgeSection : CataloguePage {
    override val title = "Badge"

    @Composable
    override fun Content() {
        MonarchBadgeVariant.entries.forEach { variant ->
            CatalogueCaption(variant.name)
            ToneRow(variant)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    private fun ToneRow(variant: MonarchBadgeVariant) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
        ) {
            MonarchTone.entries.forEach { tone -> MonarchBadge(text = tone.name, tone = tone, variant = variant) }
        }
    }
}

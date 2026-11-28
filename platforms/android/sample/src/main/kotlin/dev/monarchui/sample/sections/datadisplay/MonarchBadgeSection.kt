package dev.monarchui.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import dev.monarchui.components.datadisplay.badge.MonarchBadge
import dev.monarchui.components.datadisplay.badge.MonarchBadgeVariant
import dev.monarchui.foundation.MonarchTone
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

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

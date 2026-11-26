package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.rookhq.monarch.components.datadisplay.statusbadge.LocalMonarchStatusRegistry
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadge
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeSize
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusBadgeVariant
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusDefinition
import com.rookhq.monarch.components.datadisplay.statusbadge.MonarchStatusRegistry
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.foundation.icon.MonarchIcon
import com.rookhq.monarch.foundation.icon.MonarchIconSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.sampleicons.SampleIcons
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of StatusBadge. */
object MonarchStatusBadgeSection : CataloguePage {
    override val title = "StatusBadge"

    private val ticketStatuses = MonarchStatusRegistry(
        mapOf(
            "open" to MonarchStatusDefinition(
                label = "Open",
                tone = MonarchTone.Info,
                icon = { MonarchIcon(SampleIcons.Dot, contentDescription = null, size = MonarchIconSize.Sm) },
            ),
            "waiting" to MonarchStatusDefinition("Waiting on customer", MonarchTone.Warning),
            "closed" to MonarchStatusDefinition("Closed", MonarchTone.Neutral),
        ),
    )

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val gap = Arrangement.spacedBy(MonarchTheme.spacing.space2)
        MonarchStatusBadgeVariant.entries.forEach { variant ->
            CatalogueCaption("Service health, ${variant.name}")
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                listOf("operational", "degraded", "partial", "down", "maintenance", "unknown").forEach {
                    MonarchStatusBadge(status = it, variant = variant)
                }
            }
        }
        CatalogueCaption("A product registry, small")
        CompositionLocalProvider(LocalMonarchStatusRegistry provides ticketStatuses) {
            FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                listOf("open", "waiting", "closed", "pending_merge").forEach {
                    MonarchStatusBadge(status = it, size = MonarchStatusBadgeSize.Sm)
                }
            }
        }
    }
}

package com.rookhq.monarch.sample.sections.charts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.charts.sparkline.MonarchSparkline
import com.rookhq.monarch.components.charts.sparkline.MonarchSparklineVariant
import com.rookhq.monarch.components.typography.text.MonarchText
import com.rookhq.monarch.foundation.MonarchTone
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of Sparkline. */
object MonarchSparklineSection : CataloguePage {
    override val title = "Sparkline"

    private val signUps = listOf(3.0, 5.0, 4.0, 8.0, 7.0, 9.0, 6.0, 11.0)
    private val latency = listOf(120.0, 180.0, null, 260.0, 410.0)
    private val netChange = listOf(4.0, -2.0, 6.0, 3.0, -1.0, 5.0)
    private val single = listOf(4.0)

    @Composable
    override fun Content() {
        var variantIndex by rememberSaveable { mutableIntStateOf(0) }
        val variant = MonarchSparklineVariant.entries[variantIndex]
        CatalogueCaption("Next to a figure")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "Sign-ups 11")
            MonarchSparkline(values = signUps, variant = variant, label = "Sign-ups, last 8 days")
        }
        MonarchButton(
            text = "Variant: ${variant.name}",
            onClick = { variantIndex = (variantIndex + 1) % MonarchSparklineVariant.entries.size },
        )
        CatalogueCaption("A tone and a gap")
        MonarchSparkline(
            values = latency,
            tone = MonarchTone.Warning,
            variant = MonarchSparklineVariant.Area,
            label = "p95 latency",
        )
        CatalogueCaption("Bars, full width")
        MonarchSparkline(
            values = netChange,
            variant = MonarchSparklineVariant.Bar,
            fullWidth = true,
            label = "Net change per day",
        )
        CatalogueCaption("Not enough data")
        MonarchSparkline(values = single)
    }
}

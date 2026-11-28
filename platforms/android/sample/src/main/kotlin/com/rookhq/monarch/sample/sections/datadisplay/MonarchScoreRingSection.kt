package com.rookhq.monarch.sample.sections.datadisplay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRing
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingColor
import com.rookhq.monarch.components.datadisplay.scorering.MonarchScoreRingSize
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of ScoreRing. */
object MonarchScoreRingSection : CataloguePage {
    override val title = "ScoreRing"

    private const val START_SCORE = 72f
    private const val MAX_SCORE = 100f
    private const val RATING = 4.2
    private const val RATING_MAX = 5.0
    private val bands = listOf(92.0, 71.0, 45.0, 12.0)

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        var score by rememberSaveable { mutableFloatStateOf(START_SCORE) }
        val gap = Arrangement.spacedBy(MonarchTheme.spacing.space4)
        CatalogueCaption("Auto color from the bands")
        MonarchScoreRing(value = score.toDouble(), label = "Health", description = "Drag the slider")
        Slider(value = score, onValueChange = { score = it }, valueRange = 0f..MAX_SCORE)
        CatalogueCaption("Small, in a row")
        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
            bands.forEach { MonarchScoreRing(value = it, size = MonarchScoreRingSize.Sm) }
            MonarchScoreRing(value = null, size = MonarchScoreRingSize.Sm)
        }
        CatalogueCaption("Accent, out of 5, large")
        MonarchScoreRing(
            value = RATING,
            max = RATING_MAX,
            color = MonarchScoreRingColor.Accent,
            size = MonarchScoreRingSize.Lg,
            label = "Average rating",
            format = { "%.1f".format(it) },
        )
    }
}

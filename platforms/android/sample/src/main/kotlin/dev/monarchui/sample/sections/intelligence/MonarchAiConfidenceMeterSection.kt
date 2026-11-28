package dev.monarchui.sample.sections.intelligence

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import dev.monarchui.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeter
import dev.monarchui.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterSize
import dev.monarchui.components.intelligence.aiconfidencemeter.MonarchAiConfidenceMeterVariant
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

private const val StartingConfidence = 0.72f
private const val LowConfidence = 0.42f
private const val HighConfidence = 0.91f

/** The catalogue page of AiConfidenceMeter. */
object MonarchAiConfidenceMeterSection : CataloguePage {
    override val title = "AiConfidenceMeter"

    @Composable
    override fun Content() {
        var value by rememberSaveable { mutableFloatStateOf(StartingConfidence) }
        CatalogueCaption("Drag to change the confidence")
        Slider(value = value, onValueChange = { value = it })
        MonarchAiConfidenceMeter(value = value, showValue = true)
        MonarchAiConfidenceMeter(
            value = value,
            variant = MonarchAiConfidenceMeterVariant.Bar,
            explanation = "Matches two of three sources.",
        )
        CatalogueCaption("Beside a value")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonarchText(text = "Invoice total: 1,240.00")
            MonarchAiConfidenceMeter(value = LowConfidence, size = MonarchAiConfidenceMeterSize.Sm)
        }
        CatalogueCaption("Small bar")
        MonarchAiConfidenceMeter(
            value = HighConfidence,
            variant = MonarchAiConfidenceMeterVariant.Bar,
            size = MonarchAiConfidenceMeterSize.Sm,
        )
    }
}

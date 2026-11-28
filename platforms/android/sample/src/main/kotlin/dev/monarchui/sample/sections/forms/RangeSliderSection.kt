package dev.monarchui.sample.sections.forms

import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

/** The catalogue page of RangeSlider: the Material 3 RangeSlider in the Monarch theme, 48dp high. */
object RangeSliderSection : CataloguePage {
    override val title = "RangeSlider"

    private const val LOW = 20f
    private const val HIGH = 150f
    private const val MAX = 200f
    private const val STEPS = 39

    @Composable
    override fun Content() {
        var low by rememberSaveable { mutableFloatStateOf(LOW) }
        var high by rememberSaveable { mutableFloatStateOf(HIGH) }
        CatalogueCaption("Price, in steps of 5, with the range shown")
        MonarchText(text = "€${low.toInt()} – €${high.toInt()}")
        RangeSlider(
            value = low..high,
            onValueChange = {
                low = it.start
                high = it.endInclusive
            },
            valueRange = 0f..MAX,
            steps = STEPS,
        )
        CatalogueCaption("Disabled")
        RangeSlider(value = LOW..HIGH, onValueChange = {}, valueRange = 0f..MAX, enabled = false)
    }
}

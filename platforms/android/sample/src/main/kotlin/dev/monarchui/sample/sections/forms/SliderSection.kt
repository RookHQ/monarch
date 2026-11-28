package dev.monarchui.sample.sections.forms

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage
import dev.monarchui.theme.MonarchTheme

/** The catalogue page of Slider: the Material 3 Slider in the Monarch theme, with a 48dp thumb. */
object SliderSection : CataloguePage {
    override val title = "Slider"

    private const val START_VOLUME = 40f
    private const val MAX = 100f
    private const val STEPS = 9
    private const val START_LEVEL = 0.5f
    private val ThumbSize = DpSize(4.dp, 48.dp)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        var volume by rememberSaveable { mutableFloatStateOf(START_VOLUME) }
        var level by rememberSaveable { mutableFloatStateOf(START_LEVEL) }
        val volumeSource = remember { MutableInteractionSource() }
        val levelSource = remember { MutableInteractionSource() }
        CatalogueCaption("Volume, in steps of 10, with the value shown")
        Row(
            horizontalArrangement = Arrangement.spacedBy(MonarchTheme.spacing.space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier
                    .weight(1f)
                    .semantics { stateDescription = "${volume.toInt()} percent" },
                valueRange = 0f..MAX,
                steps = STEPS,
                interactionSource = volumeSource,
                thumb = { SliderDefaults.Thumb(interactionSource = volumeSource, thumbSize = ThumbSize) },
            )
            MonarchText(text = "${volume.toInt()}%")
        }
        CatalogueCaption("Continuous")
        Slider(
            value = level,
            onValueChange = { level = it },
            interactionSource = levelSource,
            thumb = { SliderDefaults.Thumb(interactionSource = levelSource, thumbSize = ThumbSize) },
        )
        CatalogueCaption("Disabled")
        Slider(value = START_LEVEL, onValueChange = {}, enabled = false)
    }
}

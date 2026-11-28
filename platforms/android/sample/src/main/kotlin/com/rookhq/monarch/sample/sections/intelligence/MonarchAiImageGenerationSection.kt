package com.rookhq.monarch.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.ColorPainter
import com.rookhq.monarch.components.actions.button.MonarchButton
import com.rookhq.monarch.components.intelligence.aiimagegeneration.MonarchAiGeneratedImage
import com.rookhq.monarch.components.intelligence.aiimagegeneration.MonarchAiImageGeneration
import com.rookhq.monarch.components.intelligence.aiimagegeneration.MonarchAiImageGenerationStatus
import com.rookhq.monarch.sample.CatalogueCaption
import com.rookhq.monarch.sample.CataloguePage
import com.rookhq.monarch.theme.MonarchTheme

/** The catalogue page of AiImageGeneration. */
object MonarchAiImageGenerationSection : CataloguePage {
    override val title = "AiImageGeneration"

    private const val STEP = 0.25f
    private const val REFINING_FROM = 0.5f

    @Composable
    override fun Content() {
        val picture = ColorPainter(MonarchTheme.colors.aiSubtle)
        val alts = remember { mutableStateListOf("A lighthouse at dusk", "") }
        var selection by rememberSaveable { mutableStateOf<String?>(null) }
        var progress by rememberSaveable { mutableFloatStateOf(0f) }
        CatalogueCaption("Finished: alt text, choice and variations")
        MonarchAiImageGeneration(
            images = alts.mapIndexed { index, alt -> MonarchAiGeneratedImage("image-$index", picture, alt) },
            status = MonarchAiImageGenerationStatus.Done,
            onAltChange = { id, alt -> alts[id.removePrefix("image-").toInt()] = alt },
            selection = selection,
            onSelect = { selection = it },
            onVariations = {},
        )
        CatalogueCaption("Generating")
        val status = when {
            progress >= 1f -> MonarchAiImageGenerationStatus.Done
            progress >= REFINING_FROM -> MonarchAiImageGenerationStatus.Progressive
            else -> MonarchAiImageGenerationStatus.Pending
        }
        MonarchAiImageGeneration(
            images = listOf(
                MonarchAiGeneratedImage("one", if (progress >= REFINING_FROM) picture else null, "A red bicycle"),
            ),
            status = status,
            onAltChange = { _, _ -> },
            progress = progress,
        )
        MonarchButton(
            text = if (progress >= 1f) "Start again" else "Advance",
            onClick = { progress = if (progress >= 1f) 0f else progress + STEP },
        )
        CatalogueCaption("Failed")
        MonarchAiImageGeneration(
            images = emptyList(),
            status = MonarchAiImageGenerationStatus.Error,
            onAltChange = { _, _ -> },
            onRetry = {},
        )
    }
}

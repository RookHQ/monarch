package dev.monarchui.sample.sections.intelligence

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.monarchui.components.actions.button.MonarchButtonSize
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelCapability
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelOption
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelPicker
import dev.monarchui.components.intelligence.aimodelpicker.MonarchAiModelSpeed
import dev.monarchui.components.typography.text.MonarchText
import dev.monarchui.sample.CatalogueCaption
import dev.monarchui.sample.CataloguePage

private val SampleModels = listOf(
    MonarchAiModelOption(
        id = "large",
        provider = "Rook",
        name = "Rook Large",
        description = "Best for long documents",
        capabilities = listOf(
            MonarchAiModelCapability.Vision,
            MonarchAiModelCapability.Tools,
            MonarchAiModelCapability.Reasoning,
        ),
        contextTokens = 200_000,
        speed = MonarchAiModelSpeed.Balanced,
        costTier = 3,
    ),
    MonarchAiModelOption(
        id = "fast",
        provider = "Rook",
        name = "Rook Fast",
        description = "Quick answers and summaries",
        capabilities = listOf(MonarchAiModelCapability.Tools),
        contextTokens = 32_768,
        speed = MonarchAiModelSpeed.Fast,
        costTier = 1,
    ),
    MonarchAiModelOption(
        id = "hosted",
        provider = "Workspace",
        name = "Hosted model",
        capabilities = listOf(MonarchAiModelCapability.Web),
        requiresKey = true,
    ),
    MonarchAiModelOption(
        id = "legacy",
        provider = "Workspace",
        name = "Legacy model",
        description = "Not in your plan",
        enabled = false,
    ),
)

/** The catalogue page of AiModelPicker. */
object MonarchAiModelPickerSection : CataloguePage {
    override val title = "AiModelPicker"

    @Composable
    override fun Content() {
        var model by rememberSaveable { mutableStateOf<String?>("large") }
        var note by rememberSaveable { mutableStateOf<String?>(null) }
        CatalogueCaption("Choose a model; the hosted model asks for a key")
        MonarchAiModelPicker(
            selection = model,
            models = SampleModels,
            onSelectionChange = { model = it },
            onConfigureKey = { note = "Add a key for ${it.name}" },
        )
        note?.let { MonarchText(text = it) }
        CatalogueCaption("Small, with nothing selected")
        var small by rememberSaveable { mutableStateOf<String?>(null) }
        MonarchAiModelPicker(
            selection = small,
            models = SampleModels,
            onSelectionChange = { small = it },
            size = MonarchButtonSize.Sm,
        )
    }
}
